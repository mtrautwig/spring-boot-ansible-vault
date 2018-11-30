/*
    Copyright 2018 Marcus Trautwig

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package de.trautwig.spring.boot.ansible.vault.io;

import org.springframework.util.StreamUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;

import static de.trautwig.spring.boot.ansible.vault.io.Hexlify.unhexlify;
import static java.util.Optional.ofNullable;

/**
 * Reads a file in "Ansible Vault" format. This file is supposed to contain sensitive data, but is encrypted such that
 * it can be placed into public source control.
 * <p>
 * See https://docs.ansible.com/ansible/latest/user_guide/vault.html#vault-format
 */
public class AnsibleVaultInputStream extends InputStream {
    private final InputStream vaultStream;
    private char[] rawContents;
    private int rawOffset = 0;
    private int rawLength;
    private byte[] payload;
    private int payloadOffset;
    private int payloadLength;

    public AnsibleVaultInputStream(File vaultFile, char[] password) throws IOException, GeneralSecurityException {
        this(new FileInputStream(vaultFile), password);
    }

    public AnsibleVaultInputStream(InputStream vaultStream, char[] password) throws IOException, GeneralSecurityException {
        this.vaultStream = vaultStream;
        rawContents = StreamUtils.copyToString(vaultStream, StandardCharsets.UTF_8).toCharArray();
        rawLength = rawContents.length;
        readHeader();
        readPayload(password);
    }

    private void readHeader() throws IOException {
        final String expectedFormatId = "$ANSIBLE_VAULT;";
        if (null == findExpectedHeader(expectedFormatId)) {
            throw new IOException("header " + expectedFormatId + " expected");
        }

        final String expectedVersion = "1.1;";
        if (null == findExpectedHeader(expectedVersion)) {
            throw new IOException("header version " + expectedVersion + " expected");
        }

        String algorithm = findUntilNextLineBreak();
        if (algorithm == null) {
            throw new IOException("Crypto algorithm header not found");
        }
        if (!"AES256".equals(algorithm)) {
            throw new IOException("Unsupported crypto algorithm: " + algorithm);
        }
    }

    private void readPayload(char[] password) throws IOException, GeneralSecurityException {
        stripRemainingLineBreaks();
        byte[] payload = unhexlify(rawContents, rawOffset, rawLength - rawOffset);
        rawContents = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(payload)).array();
        rawLength = rawContents.length;
        rawOffset = 0;

        byte[] salt = unhexlify(ofNullable(findUntilNextLineBreak()).orElseThrow(() -> new IOException("cannot determine end of salt")));
        byte[] expectedHmac = unhexlify(ofNullable(findUntilNextLineBreak()).orElseThrow(() -> new ImagingOpException("cannot determine end of HMAC")));
        AnsibleVaultEncryptionKeys keys = new AnsibleVaultEncryptionKeys(password, salt);
        stripRemainingLineBreaks();
        byte[] ciphertext = unhexlify(rawContents, rawOffset, rawLength - rawOffset);

        verifyHmac(keys, expectedHmac, ciphertext);
        decryptPayload(keys, ciphertext);
        rawContents = null;
    }

    private void verifyHmac(AnsibleVaultEncryptionKeys keys, byte[] expectedHmac, byte[] ciphertext) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keys.getHmacKey());
        byte[] actualHmac = mac.doFinal(ciphertext);
        if (!Arrays.equals(actualHmac, expectedHmac)) {
            throw new SignatureException("HMAC does not match, either the given password is invalid or the file has been modified");
        }
    }

    private void decryptPayload(AnsibleVaultEncryptionKeys keys, byte[] ciphertext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keys.getCipherKey(), new IvParameterSpec(keys.getIv()));
        this.payload = cipher.doFinal(ciphertext);
        this.payloadOffset = 0;
        this.payloadLength = this.payload.length;

        // Cipher did not strip PKCS5Padding on OpenJDK, do in application
        int padding = this.payload[this.payloadLength - 1];
        if (padding <= cipher.getBlockSize() && padding > 0) {
            this.payloadLength -= padding;
        }
    }

    private String findExpectedHeader(String expected) {
        final String actual = new String(rawContents, rawOffset, Math.min(expected.length(), rawLength - rawOffset));
        if (expected.equals(actual)) {
            rawOffset += actual.length();
            return actual;
        }
        return null;
    }

    private String findUntilNextLineBreak() {
        for (int i = rawOffset; i < rawLength; i++) {
            if (rawContents[i] == '\n' || rawContents[i] == '\r') {
                int startOffset = rawOffset;
                rawOffset = i + 1;
                return new String(rawContents, startOffset, i - startOffset);
            }
        }
        return null;
    }

    private void stripRemainingLineBreaks() {
        int lowMark = rawOffset;
        for (int highMark = rawOffset; highMark < rawLength; highMark++) {
            if (rawContents[highMark] != '\n' && rawContents[highMark] != '\r') {
                rawContents[lowMark] = rawContents[highMark];
                lowMark++;
            }
        }
        rawLength = lowMark;
    }

    @Override
    public int read() throws IOException {
        if (payloadOffset >= payloadLength) {
            return -1;
        }
        return payload[payloadOffset++];
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (payloadOffset >= payloadLength) {
            return -1;
        }
        int realLen = Math.min(len, payloadLength - payloadOffset);
        System.arraycopy(payload, payloadOffset, b, off, realLen);
        payloadOffset += realLen;
        return realLen;
    }

    @Override
    public void close() throws IOException {
        if (payload != null) {
            Arrays.fill(payload, (byte) 0x00);
            payload = null;
            vaultStream.close();
        }
    }

    /**
     * Check if the current JVM is capable of handling the Ansible Vault crypto. Users of Oracle JavaSE may need to
     * install the "Unlimited Strength Jurisdiction Policy Files".
     *
     * @return true if the JVM meets all requirements
     */
    public boolean areRequirementsMet() {
        try {
            return (Cipher.getMaxAllowedKeyLength("AES/CTR/NoPadding") >= 256);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}

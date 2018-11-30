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

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Derive Encryption and HMAC keys and an Initialization Vector (IV) from a given password and a random salt.
 * See https://docs.ansible.com/ansible/latest/user_guide/vault.html#vault-payload-format-1-1
 */
class AnsibleVaultEncryptionKeys {

    private final SecretKey cipherKey;
    private final SecretKey hmacKey;
    private final byte[] iv;

    public AnsibleVaultEncryptionKeys(char[] password, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey secretKey = keyFactory.generateSecret(new PBEKeySpec(password, salt, 10_000, (32 + 32 + 16) * 8));
        byte[] secretKeyBytes = secretKey.getEncoded();
        if (secretKeyBytes.length != 80) {
            throw new IllegalArgumentException("unexpected key length: " + secretKeyBytes.length);
        }

        cipherKey = new SecretKeySpec(secretKeyBytes, 0, 32, "AES");
        hmacKey = new SecretKeySpec(secretKeyBytes, 32, 32, "AES");
        iv = Arrays.copyOfRange(secretKeyBytes, 64, 64 + 16);
    }

    /**
     * Get the secret key used for encrypting the Vault
     */
    public SecretKey getCipherKey() {
        return cipherKey;
    }

    /**
     * Get the secret key used for signing/validating the Vault
     */
    public SecretKey getHmacKey() {
        return hmacKey;
    }

    /**
     * Get the Initialization Vector used for encrypting the Vault
     */
    public byte[] getIv() {
        return iv;
    }

}

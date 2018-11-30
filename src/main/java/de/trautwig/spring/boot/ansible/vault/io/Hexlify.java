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

/**
 * Utility class for converting byte[] to/from its hexadecimal representation
 *
 * @author Marcus Trautwig
 */
public final class Hexlify {
    private static final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Encode binary data to its hexadecimal representation
     *
     * @param data   the source to be encoded. Must not be {@code null}.
     * @param offset the offset (starting at 0) of the first byte to encode
     * @param len    the number of bytes to encode
     * @return a char[] with the corresponding 2-digit hex representation, twice the size of the given input
     */
    public static char[] hexlify(byte[] data, int offset, int len) {
        char[] result = new char[len * 2];
        for (int i = 0; i < len; i++) {
            result[2 * i] = hexChars[data[offset + i] >> 4];
            result[2 * i + 1] = hexChars[data[offset + i] & 0x0F];
        }
        return result;
    }

    /**
     * Decode binary data from a hexadecimal String
     *
     * @param str the source to be decoded. Must not be {@code null}. Must only contain hexadecimal digits (0-f or 0-F) and the length must be a multiple of 2.
     * @return a byte[] with the corresponding binary data, half the size of the given input.
     * @throws IllegalArgumentException if the source
     */
    public static byte[] unhexlify(String str) {
        char[] data = str.toCharArray();
        return unhexlify(data, 0, data.length);
    }

    /**
     * Decode binary data from a hexadecimal character sequence
     *
     * @param data   the source to be decoded. Must not be {@code null}. Must only contain hexadecimal digits (0-f or 0-F) and the length must be a multiple of 2.
     * @param offset the offset (starting at 0) of the first character to decode
     * @param len    the number of bytes to decode
     * @return a byte[] with the corresponding binary data, half the size of the given input.
     * @throws IllegalArgumentException if the source
     */
    public static byte[] unhexlify(char[] data, int offset, int len) {
        if (len % 2 != 0) {
            throw new IllegalArgumentException("buffer underflow");
        }

        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) ((fromHex(data[offset + i]) << 4) | fromHex(data[offset + i + 1]));
        }
        return result;
    }

    private static int fromHex(char chr) {
        if (chr >= '0' && chr <= '9') {
            return (chr - '0');
        } else if (chr >= 'a' && chr <= 'f') {
            return 10 + (chr - 'a');
        } else if (chr >= 'A' && chr <= 'F') {
            return 10 + (chr - 'A');
        } else {
            throw new IllegalArgumentException("unexpected input: " + chr);
        }
    }

    private Hexlify() {
    }
}

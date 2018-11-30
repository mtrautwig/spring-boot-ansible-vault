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

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SignatureException;

import static org.hamcrest.Matchers.equalTo;

public class AnsibleVaultInputStreamTest {

    @Test
    public void loadsSuccessfully() throws Exception {
        AnsibleVaultInputStream in = new AnsibleVaultInputStream(getClass().getResourceAsStream("/vault_hello.yml"), "demo".toCharArray());
        byte[] buffer = new byte[1024];
        int len = in.read(buffer, 0, buffer.length);
        Assert.assertThat(new String(buffer, 0, len), equalTo("Hello World!\n"));
    }

    @Test(expected = IOException.class)
    public void checksFileType() throws Exception {
        ByteArrayInputStream vaultStream = new ByteArrayInputStream(new String("PNG\n").getBytes());
        AnsibleVaultInputStream in = new AnsibleVaultInputStream(vaultStream, "demo".toCharArray());
        byte[] buffer = new byte[1024];
        in.read(buffer, 0, buffer.length);
    }

    @Test(expected = SignatureException.class)
    public void wrongPasswordFails() throws Exception {
        AnsibleVaultInputStream in = new AnsibleVaultInputStream(getClass().getResourceAsStream("/vault_hello.yml"), "ThisPasswordIsWrong".toCharArray());
        byte[] buffer = new byte[1024];
        in.read(buffer, 0, buffer.length);
    }

    @Test(expected = SignatureException.class)
    public void detectsHmacMismatch() throws Exception {
        AnsibleVaultInputStream in = new AnsibleVaultInputStream(getClass().getResourceAsStream("/vault_tampered.yml"), "demo".toCharArray());
        byte[] buffer = new byte[1024];
        in.read(buffer, 0, buffer.length);
    }
}

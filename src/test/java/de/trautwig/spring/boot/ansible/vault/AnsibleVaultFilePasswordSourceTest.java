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
package de.trautwig.spring.boot.ansible.vault;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@RunWith(SpringRunner.class)
public class AnsibleVaultFilePasswordSourceTest {

    @Test
    public void defaultFileIsUsed() throws IOException {
        Environment environment = new MockEnvironment();
        Path secretFile = Files.createFile(Paths.get("vault.secret"));
        try {
            Files.write(secretFile, "Hello World!".getBytes());
            char[] password = new AnsibleVaultFilePasswordSource().getVaultPassword(environment);
            Assert.assertThat(new String(password), equalTo("Hello World!"));
        } finally {
            Files.deleteIfExists(secretFile);
        }
    }

    @Test
    public void customFileCanBeSpecified() throws IOException {
        Path secretFile = Files.createTempFile("vault", ".secret");
        Environment environment = new MockEnvironment().withProperty(AnsibleVaultEnvironment.VAULT_SECRET_PROPERTY, "@" + secretFile.toString());
        try {
            Files.write(secretFile, "Hello World!".getBytes());
            char[] password = new AnsibleVaultFilePasswordSource().getVaultPassword(environment);
            Assert.assertThat(new String(password), equalTo("Hello World!"));
        } finally {
            Files.deleteIfExists(secretFile);
        }
    }

    @Test(expected = RuntimeException.class)
    public void nonExistingSpecifiedFileTreatedAsError() {
        Environment environment = new MockEnvironment().withProperty(AnsibleVaultEnvironment.VAULT_SECRET_PROPERTY, "@does-not-exist");
        new AnsibleVaultFilePasswordSource().getVaultPassword(environment);
    }

    @Test
    public void nonExistingDefaultFileNotAnError() {
        Environment environment = new MockEnvironment();
        char[] password = new AnsibleVaultFilePasswordSource().getVaultPassword(environment);
        Assert.assertThat(password, nullValue());
    }
}

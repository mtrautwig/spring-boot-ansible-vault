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

import org.springframework.core.env.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Take the Ansible Vault password from a text file on the hard drive. The name of the text file is either
 * 'vault.secret' in the current working directory, or can be set using the property 'ansible.vault.secret' as follows
 * ('@' followed by file path):
 * <ul>
 * <li>OS environment variable:<br><pre>ANSIBLE_VAULT_SECRET=@/path/to/vault.secret java ...</pre></li>
 * <li>Java System property:<br><pre>java -Dansible.vault.secret=@vault.secret ...</pre></li>
 * <li>Command-line argument:<br><pre>java ... --ansible.vault.secret=@config/secrets ...</pre></li>
 * <li>Application Property files</li>
 */
public class AnsibleVaultFilePasswordSource implements AnsibleVaultPasswordSource {
    private static final String DEFAULT_PASSWORD_FILE = "vault.secret";

    @Override
    public char[] getVaultPassword(Environment environment) {
        String passwordFile = environment.getProperty(AnsibleVaultEnvironment.VAULT_SECRET_PROPERTY);
        if (passwordFile != null && passwordFile.startsWith("@") && passwordFile.length() > 1) {
            return loadPassword(new File(passwordFile.substring(1)));
        }

        File defaultFile = new File(DEFAULT_PASSWORD_FILE);
        if (defaultFile.exists()) {
            return loadPassword(defaultFile);
        }

        return null;
    }

    public char[] loadPassword(File passwordFile) {
        try {
            byte[] passwordBytes = Files.readAllBytes(passwordFile.toPath());
            try {
                CharBuffer password = Charset.defaultCharset().decode(trim(passwordBytes));
                return password.length() == 0 ? null : password.array();
            } finally {
                Arrays.fill(passwordBytes, (byte) 0x00);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ByteBuffer trim(byte[] data) {
        int start = 0;
        for (; start < data.length; start++) {
            if (data[start] > ' ') {
                break;
            }
        }

        int end = data.length-1;
        for (; end >= start; end--) {
            if (data[end] > ' ') {
                end++;
                break;
            }
        }

        return ByteBuffer.wrap(data, start, end-start);
    }
}

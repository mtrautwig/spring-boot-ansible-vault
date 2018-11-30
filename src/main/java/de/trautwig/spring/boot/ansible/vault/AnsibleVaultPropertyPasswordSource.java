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

import java.util.Optional;

/**
 * Take the Ansible Vault password from the Environment property 'ansible.vault.secret'. This property can be set using:
 * <ul>
 * <li>OS environment variables (interesting when using Docker):<br><pre>ANSIBLE_VAULT_SECRET=... java ...</pre></li>
 * <li>Java System properties:<br><pre>java -Dansible.vault.secret=...</pre></li>
 * <li>Command-line arguments:<br><pre>java ... --ansible.vault.secret=...</pre></li>
 * <li>Application Property files, but do <strong>NOT</strong> put the file containing the password under Version Control!</li>
 * </ul>
 */
public class AnsibleVaultPropertyPasswordSource implements AnsibleVaultPasswordSource {

    @Override
    public char[] getVaultPassword(Environment environment) {
        String password = environment.getProperty(AnsibleVaultEnvironment.VAULT_SECRET_PROPERTY);
        return Optional.ofNullable(password).map(String::toCharArray).orElse(null);
    }

}

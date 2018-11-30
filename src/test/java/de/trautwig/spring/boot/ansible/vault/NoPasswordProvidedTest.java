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
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.is;

public class NoPasswordProvidedTest {

    @Test
    public void throwExceptionWithVaultButWithoutPassword() {
        ConfigurableEnvironment environment = new MockEnvironment();
        SpringApplication application = new SpringApplication();
        application.setEnvironment(environment);

        try {
            new AnsibleVaultEnvironment().postProcessEnvironment(environment, application);
            Assert.fail("an existing vault which cannot be opened should throw an exception");
        } catch (Exception e) {
            Assert.assertThat(e.getMessage().contains("ansible.vault.secret"), is(true));
        }
    }

    @Test
    public void continueIfVaultDoesNotExist() {
        MockEnvironment environment = new MockEnvironment().withProperty(AnsibleVaultEnvironment.VAULT_NAME_PROPERTY, "@does-not-exist");
        SpringApplication application = new SpringApplication();
        application.setEnvironment(environment);

        new AnsibleVaultEnvironment().postProcessEnvironment(environment, application);
    }

}

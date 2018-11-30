Spring Boot Configuration Properties Encryption with "ansible-vault"
====================================================================

This library reads _Ansible Vault_ encrypted configuration files and adds them to the Spring Environment. By adding all
your application's sensitive properties, like service credentials, to a Vault, they can be put under Version Control
without exposing them to the public.

This avoids that each team member needs to setup everything individually and adjust it over time. All authorized
persons and machines running your application just need to know the shared password and provide it during application 
startup. 

Requirements
------------

* JavaSE 8 or newer
  * Depending on your JVM, you may need to install the "Unlimited Strength Jurisdiction Policy Files"
* Spring Boot 2.0.x or later
* ```ansible-vault``` from [Ansible](https://www.ansible.com/) for creating/managing your encrypted properties files 

Usage
-----

### Create a password file (recommended)

Use your favorite editor and create a text file `vault.secret` that contains a random password. All leading and trailing
whitespace characters are ignored. This password will be used to encrypt your sensitive configuration properties.
Distribute this password securely to all authorized persons, but **do not put it under Version Control**.

### Create the Vault

First, use the `ansible-vault` tool to create a Vault (_vault.secret_ is the name of your password file):

```$ ansible-vault --vault-id=@vault.secret create src/main/resources/vault.yml``` 

This will prompt for your Vault password and then open your default text editor, where you can edit the unencrypted 
contents of the file. These will be encrypted using the given Vault password after you close your editor.

Please check the manual page of _ansible-vault_ for more commands used for editing and rekeying.

### Vault contents

The Vault file contents are expected to be in YAML format. There may be a separate file per profile, e.g. if the 
profile 'production' is active, a file 'vault-production.yml' is also considered. Note that multiple profiles in the
same file ("Multi-profile YAML Documents") are not yet supported! 
Please see the Spring Boot documentation, section "Externalized Configuration" for details.

### Provide the password

The application needs to know the Vault password when starting up, in order to decrypt the Vault. By default, the
password is read from a file 'vault.secret' (which you created a few steps ago) in the current working directory.
If this file does not exist, the password has to be specified as ``Environment`` property 'ansible.vault.secret'.

### Using sensitive credentials

The properties defined in the Vault file can be used like any Application Property. For example, you may get them
directly from the injected ``Environment``:

```environment.getRequiredProperty("secret")```

Or you may bind them with ```@Value```:

```@Value("${secret}") String secret;```

Security Considerations
-----------------------

* **Never** store your vault password under Version Control.
* If you provide the Vault password using command-line arguments, system properties or environment variables,
  it may be visible to other users on the same machine. Use a password file or set the password property in an
  external properties file (which must not be put into Version Control) in such cases. Make sure to set the file
  permissions accordingly!
* The decrypted contents of your Vault will remain in memory while the application is running. Any user who can create 
  or access memory dumps will be able to extract them.
* Spring may expose the Environment - containing all decrypted contents of your Vault - via JMX or HTTP, if enabled 

Alternatives
------------

* [BlackBox](https://github.com/StackExchange/blackbox) - uses custom tooling around GnuPG
* [HashiCorp Vault](https://www.vaultproject.io/) - if you prefer storing credentials centrally in a service
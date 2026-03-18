# spring-boot-sftp-server
SFTP Server managed by Spring Boot endpoints.

An example of an Apache Minah SSHD implementation. 
The implementation enholds endpoints which configures the SFTP server and manage users. 

## Kerberos infrastructure

The GSSAuthenticator in Mina SSHD uses a Kerberos accept credential based on a service principal and keytab by default. If servicePrincipalName is not set, it falls back to host/<canonical-hostname>; keytabFile can be configured explicitly.

So in practice, the following must be correct:

the service principal exists, e.g. host/sftp.example.local@EXAMPLE.LOCAL

the keytab belongs to that principal

the server hostname matches the principal

the client attempts gssapi-with-mic authentication

## Useful JVM flags

For Java Kerberos, this is usually required:

-Djava.security.krb5.conf=/etc/krb5.conf
-Dsun.security.krb5.debug=true
-Dsun.security.spnego.debug=true

If your krb5.conf is already correctly configured at the OS level, only the debug flags are optional.

## Testing from the client

For example, using OpenSSH:

kinit ricky@EXAMPLE.LOCAL
ssh -vvv -p 2222 \
-o GSSAPIAuthentication=yes \
-o PreferredAuthentications=gssapi-with-mic,password \
ricky@sftp.example.local

Or using SFTP:

sftp -vvv -P 2222 \
-o GSSAPIAuthentication=yes \
-o PreferredAuthentications=gssapi-with-mic,password \
ricky@sftp.example.local

# SSH public/private key authentication
## Structure of your SSH authorized keys

With this implementation, the server expects a file per user with the same name as the username:
``` 
./data/authorized_keys/testuser
./data/authorized_keys/alice
./data/authorized_keys/bob
```

The contents of such a file follow the standard OpenSSH authorized_keys format, for example:

`ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIExampleKeyDataHere testuser@laptop`

AuthorizedKeysAuthenticator is specifically designed to read and validate these authorized_keys files.

## Testing from the client

First, generate a key:

`ssh-keygen -t ed25519 -f ~/.ssh/test_sftp_key`

Then place the public key in the correct file, for example:
``` shell
mkdir -p ./data/authorized_keys
cat ~/.ssh/test_sftp_key.pub > ./data/authorized_keys/testuser
```
Now test the connection:

`sftp -i ~/.ssh/test_sftp_key -P 2222 testuser@localhost `

Or using SSH:

`ssh -i ~/.ssh/test_sftp_key -p 2222 testuser@localhost`
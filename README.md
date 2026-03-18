# spring-boot-sftp-server
SFTP Server managed by Spring Boot endpoints.

An example of an Apache Minah SSHD implementation. 
The implementation enholds endpoints which configures the SFTP server and manage users. 

## What your Kerberos infrastructure needs

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
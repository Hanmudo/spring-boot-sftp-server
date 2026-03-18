package com.workerrobotics.sftpserver.service;

import com.workerrobotics.sftpserver.config.KerberosProperties;
import com.workerrobotics.sftpserver.mapper.KerberosPrincipalMapper;
import org.apache.sshd.server.auth.gss.GSSAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApplicationGssAuthenticator extends GSSAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(ApplicationGssAuthenticator.class);

    private final UserRegistryService userRegistry;
    private final KerberosPrincipalMapper principalMapper;
    private final KerberosProperties kerberosProperties;

    public ApplicationGssAuthenticator(
            UserRegistryService userRegistry,
            KerberosPrincipalMapper principalMapper,
            KerberosProperties kerberosProperties
    ) {
        this.userRegistry = userRegistry;
        this.principalMapper = principalMapper;
        this.kerberosProperties = kerberosProperties;

        setServicePrincipalName(kerberosProperties.getServicePrincipal());
        setKeytabFile(kerberosProperties.getKeytab());
    }

    @Override
    public boolean validateInitialUser(ServerSession session, String user) {
        if (user == null || user.isBlank()) {
            return false;
        }

        boolean exists = userRegistry.findByUsername(user).isPresent();
        if (!exists) {
            log.warn("Kerberos init user afgewezen: onbekende gebruiker '{}'", user);
        }
        return exists;
    }

    @Override
    public boolean validateIdentity(ServerSession session, String identity) {
        if (identity == null || identity.isBlank()) {
            return false;
        }

        try {
            String mappedUser = principalMapper.toApplicationUsername(identity);
            boolean exists = userRegistry.findByUsername(mappedUser).isPresent();

            if (!exists) {
                log.warn("Kerberos identity afgewezen: '{}' mapped naar onbekende user '{}'", identity, mappedUser);
                return false;
            }

            String requestedUser = session.getUsername();
            boolean matchesRequestedUser = requestedUser != null && requestedUser.equals(mappedUser);

            if (!matchesRequestedUser) {
                log.warn(
                        "Kerberos identity mismatch: session user='{}', kerberos identity='{}', mapped user='{}'",
                        requestedUser, identity, mappedUser
                );
                return false;
            }

            log.info("Kerberos login geaccepteerd voor principal '{}' als lokale gebruiker '{}'", identity, mappedUser);
            return true;
        } catch (Exception e) {
            log.warn("Kerberos identity validatie mislukt voor '{}': {}", identity, e.getMessage());
            return false;
        }
    }
}

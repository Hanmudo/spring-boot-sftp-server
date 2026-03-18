package com.workerrobotics.sftpserver.service;

import com.workerrobotics.sftpserver.config.PublicKeyAuthProperties;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;

@Component
public class ApplicationPublickeyAuthenticator implements PublickeyAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(ApplicationPublickeyAuthenticator.class);

    private final PublicKeyAuthProperties publicKeyAuthProperties;
    private final UserRegistryService userRegistry;

    public ApplicationPublickeyAuthenticator(
            PublicKeyAuthProperties publicKeyAuthProperties,
            UserRegistryService userRegistry
    ) {
        this.publicKeyAuthProperties = publicKeyAuthProperties;
        this.userRegistry = userRegistry;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        try {
            if (username == null || username.isBlank()) {
                return false;
            }

            if (userRegistry.findByUsername(username).isEmpty()) {
                log.warn("Public key login afgewezen: onbekende gebruiker '{}'", username);
                return false;
            }

            Path baseDir = Paths.get(publicKeyAuthProperties.getAuthorizedKeysDirectory()).toAbsolutePath();
            Path userAuthorizedKeys = baseDir.resolve(username);

            if (!Files.exists(userAuthorizedKeys) || !Files.isRegularFile(userAuthorizedKeys)) {
                log.warn("Geen authorized_keys bestand gevonden voor gebruiker '{}': {}", username, userAuthorizedKeys);
                return false;
            }

            PublickeyAuthenticator delegate = new AuthorizedKeysAuthenticator(userAuthorizedKeys);
            boolean authenticated = delegate.authenticate(username, key, session);

            if (authenticated) {
                log.info("Public key login geaccepteerd voor gebruiker '{}'", username);
            } else {
                log.warn("Public key login afgewezen voor gebruiker '{}'", username);
            }

            return authenticated;
        } catch (Exception e) {
            log.warn("Fout bij public key authenticatie voor '{}': {}", username, e.getMessage());
            return false;
        }
    }
}

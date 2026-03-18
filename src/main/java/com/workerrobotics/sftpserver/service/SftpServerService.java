package com.workerrobotics.sftpserver.service;

import com.workerrobotics.sftpserver.config.KerberosProperties;
import com.workerrobotics.sftpserver.config.SftpProperties;
import com.workerrobotics.sftpserver.model.SftpServerConfig;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthFactory;
import org.apache.sshd.server.auth.gss.UserAuthGSSFactory;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import com.workerrobotics.sftpserver.config.PublicKeyAuthProperties;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SftpServerService {

    private static final Logger log = LoggerFactory.getLogger(SftpServerService.class);

    private final SftpProperties properties;
    private final UserRegistryService userRegistry;
    private final KerberosProperties kerberosProperties;
    private final ApplicationGssAuthenticator gssAuthenticator;
    private final PublicKeyAuthProperties publicKeyAuthProperties;
    private final ApplicationPublickeyAuthenticator publickeyAuthenticator;

    private SshServer sshServer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ReentrantLock serverLock = new ReentrantLock();

    private volatile SftpServerConfig currentConfig;

    public SftpServerService(
            SftpProperties properties,
            UserRegistryService userRegistry,
            KerberosProperties kerberosProperties,
            ApplicationGssAuthenticator gssAuthenticator,
            PublicKeyAuthProperties publicKeyAuthProperties,
            ApplicationPublickeyAuthenticator publickeyAuthenticator
    ) {
        this.properties = properties;
        this.userRegistry = userRegistry;
        this.kerberosProperties = kerberosProperties;
        this.gssAuthenticator = gssAuthenticator;
        this.publicKeyAuthProperties = publicKeyAuthProperties;
        this.publickeyAuthenticator = publickeyAuthenticator;
        this.currentConfig = new SftpServerConfig(
                properties.getPort(),
                properties.getRootDirectory(),
                properties.getIdleTimeoutSeconds(),
                properties.getAuthTimeoutSeconds(),
                properties.getMaxSessions()
        );
    }

    public void start() throws IOException {
        serverLock.lock();
        try {
            if (running.get()) {
                throw new IllegalStateException("SFTP-server is al actief op poort " + currentConfig.port());
            }
            sshServer = buildServer(currentConfig);
            sshServer.start();
            running.set(true);
            log.info("SFTP-server gestart op poort {}", currentConfig.port());
        } finally {
            serverLock.unlock();
        }
    }

    public void stop() throws IOException {
        serverLock.lock();
        try {
            if (!running.get()) {
                throw new IllegalStateException("SFTP-server is niet actief");
            }
            sshServer.stop(true);
            sshServer = null;
            running.set(false);
            log.info("SFTP-server gestopt");
        } finally {
            serverLock.unlock();
        }
    }

    public void reload(SftpServerConfig config) throws IOException {
        serverLock.lock();
        try {
            boolean wasRunning = running.get();
            if (wasRunning) {
                sshServer.stop(true);
                sshServer = null;
                running.set(false);
            }
            currentConfig = config;
            if (wasRunning) {
                sshServer = buildServer(currentConfig);
                sshServer.start();
                running.set(true);
                log.info("SFTP-server herladen op poort {} met rootdir {}", config.port(), config.rootDirectory());
            } else {
                log.info("Configuratie bijgewerkt (server niet actief)");
            }
        } finally {
            serverLock.unlock();
        }
    }

    public SftpServerConfig getConfig() {
        return currentConfig;
    }

    public boolean isRunning() {
        return running.get();
    }

    private SshServer buildServer(SftpServerConfig config) throws IOException {
        Path rootDir = Paths.get(config.rootDirectory()).toAbsolutePath();
        Files.createDirectories(rootDir);

        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(config.port());
        server.setKeyPairProvider(buildKeyProvider());
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        server.setFileSystemFactory(buildFileSystemFactory(rootDir));

        configureAuthentication(server);

        if (config.idleTimeoutSeconds() > 0) {
            CoreModuleProperties.IDLE_TIMEOUT.set(server, Duration.ofSeconds(config.idleTimeoutSeconds()));
        }

        if (config.authTimeoutSeconds() > 0) {
            CoreModuleProperties.AUTH_TIMEOUT.set(server, Duration.ofSeconds(config.authTimeoutSeconds()));
        }

        return server;
    }

    private void configureAuthentication(SshServer server) {
        List<UserAuthFactory> authFactories = new ArrayList<>();

        if (kerberosProperties.isEnabled()) {
            validateKerberosSetup();
            server.setGSSAuthenticator(gssAuthenticator);
            authFactories.add(UserAuthGSSFactory.INSTANCE);
            log.info("Kerberos/GSSAPI authenticatie ingeschakeld voor principal {}",
                    kerberosProperties.getServicePrincipal());
        }

        if (publicKeyAuthProperties.isEnabled()) {
            validatePublicKeySetup();
            server.setPublickeyAuthenticator(publickeyAuthenticator);
            authFactories.add(UserAuthPublicKeyFactory.INSTANCE);
            log.info("SSH public key authenticatie ingeschakeld met directory {}",
                    Paths.get(publicKeyAuthProperties.getAuthorizedKeysDirectory()).toAbsolutePath());
        }

        boolean passwordAllowed =
                (!kerberosProperties.isEnabled() && !publicKeyAuthProperties.isEnabled())
                        || kerberosProperties.isPasswordFallback()
                        || publicKeyAuthProperties.isPasswordFallback();

        if (passwordAllowed) {
            server.setPasswordAuthenticator(buildPasswordAuthenticator());
            authFactories.add(UserAuthPasswordFactory.INSTANCE);
            log.info("Password authenticatie ingeschakeld");
        }

        if (authFactories.isEmpty()) {
            throw new IllegalStateException("Geen authenticatiemethode geconfigureerd");
        }

        server.setUserAuthFactories(authFactories);
    }

    private void validateKerberosSetup() {
        if (kerberosProperties.getServicePrincipal() == null || kerberosProperties.getServicePrincipal().isBlank()) {
            throw new IllegalStateException("Kerberos is actief maar sftp.kerberos.service-principal ontbreekt");
        }

        if (kerberosProperties.getKeytab() == null || kerberosProperties.getKeytab().isBlank()) {
            throw new IllegalStateException("Kerberos is actief maar sftp.kerberos.keytab ontbreekt");
        }

        Path keytabPath = Paths.get(kerberosProperties.getKeytab()).toAbsolutePath();
        if (!Files.exists(keytabPath)) {
            throw new IllegalStateException("Kerberos keytab bestaat niet: " + keytabPath);
        }

        if (!Files.isReadable(keytabPath)) {
            throw new IllegalStateException("Kerberos keytab is niet leesbaar: " + keytabPath);
        }
    }

    private void validatePublicKeySetup() {
        if (publicKeyAuthProperties.getAuthorizedKeysDirectory() == null
                || publicKeyAuthProperties.getAuthorizedKeysDirectory().isBlank()) {
            throw new IllegalStateException(
                    "Public key authenticatie is actief maar sftp.public-key.authorized-keys-directory ontbreekt"
            );
        }

        Path dir = Paths.get(publicKeyAuthProperties.getAuthorizedKeysDirectory()).toAbsolutePath();

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Kon authorized_keys directory niet aanmaken: " + dir, e);
        }

        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("Authorized keys pad is geen directory: " + dir);
        }

        if (!Files.isReadable(dir)) {
            throw new IllegalStateException("Authorized keys directory is niet leesbaar: " + dir);
        }
    }

    private KeyPairProvider buildKeyProvider() {
        Path keyPath = Paths.get(properties.getHostKeyPath()).toAbsolutePath();
        SimpleGeneratorHostKeyProvider keyProvider = new SimpleGeneratorHostKeyProvider(keyPath);
        keyProvider.setAlgorithm("RSA");
        keyProvider.setKeySize(4096);
        return keyProvider;
    }

    private PasswordAuthenticator buildPasswordAuthenticator() {
        return (username, password, session) -> userRegistry.authenticate(username, password);
    }

    private VirtualFileSystemFactory buildFileSystemFactory(Path rootDir) {
        VirtualFileSystemFactory factory = new VirtualFileSystemFactory(rootDir);
        userRegistry.findAll().forEach(user -> {
            if (user.homeDirectory() != null && !user.homeDirectory().isBlank()) {
                Path userHome = rootDir.resolve(user.homeDirectory());
                try {
                    Files.createDirectories(userHome);
                } catch (IOException e) {
                    log.warn("Kon thuismap niet aanmaken voor {}: {}", user.username(), e.getMessage());
                }
                factory.setUserHomeDir(user.username(), userHome);
            }
        });
        return factory;
    }
}
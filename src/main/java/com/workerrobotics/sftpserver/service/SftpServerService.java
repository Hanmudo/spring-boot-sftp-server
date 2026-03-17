package com.workerrobotics.sftpserver.service;

import com.workerrobotics.sftpserver.config.SftpProperties;
import com.workerrobotics.sftpserver.model.SftpServerConfig;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Beheert de levenscyclus van de Apache Mina SSHD SFTP-server.
 * De server kan worden gestart, gestopt en herladen via de REST-API.
 */
@Service
public class SftpServerService {

    private static final Logger log = LoggerFactory.getLogger(SftpServerService.class);

    private final SftpProperties properties;
    private final UserRegistryService userRegistry;

    private SshServer sshServer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ReentrantLock serverLock = new ReentrantLock();

    private volatile SftpServerConfig currentConfig;

    public SftpServerService(SftpProperties properties, UserRegistryService userRegistry) {
        this.properties = properties;
        this.userRegistry = userRegistry;
        this.currentConfig = new SftpServerConfig(
                properties.getPort(),
                properties.getRootDirectory(),
                properties.getIdleTimeoutSeconds(),
                properties.getAuthTimeoutSeconds(),
                properties.getMaxSessions()
        );
    }

    /**
     * Start de SFTP-server met de huidige configuratie.
     *
     * @throws IOException als de server niet gestart kan worden
     * @throws IllegalStateException als de server al actief is
     */
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

    /**
     * Stopt de actieve SFTP-server.
     *
     * @throws IOException als de server niet gestopt kan worden
     * @throws IllegalStateException als de server niet actief is
     */
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

    /**
     * Herlaadt de server met een nieuwe configuratie (stop + start).
     *
     * @param config de nieuwe configuratie
     * @throws IOException als de server niet herladen kan worden
     */
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

    /**
     * Geeft de huidige configuratie terug.
     */
    public SftpServerConfig getConfig() {
        return currentConfig;
    }

    /**
     * Geeft aan of de server actief is.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Bouwt een geconfigureerde SshServer instantie.
     */
    private SshServer buildServer(SftpServerConfig config) throws IOException {
        Path rootDir = Paths.get(config.rootDirectory()).toAbsolutePath();
        Files.createDirectories(rootDir);

        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(config.port());
        server.setKeyPairProvider(buildKeyProvider());
        server.setPasswordAuthenticator(buildPasswordAuthenticator());
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        server.setFileSystemFactory(buildFileSystemFactory(rootDir));

        if (config.idleTimeoutSeconds() > 0) {
            CoreModuleProperties.IDLE_TIMEOUT.set(
                    server,
                    Duration.ofSeconds(config.idleTimeoutSeconds())
            );
        }

        if (config.authTimeoutSeconds() > 0) {
            CoreModuleProperties.AUTH_TIMEOUT.set(
                    server,
                    Duration.ofSeconds(config.authTimeoutSeconds())
            );
        }

        return server;
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
        // Stel per-gebruiker homedirectory in als die is geconfigureerd
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
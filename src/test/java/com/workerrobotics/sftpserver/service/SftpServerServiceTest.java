package com.workerrobotics.sftpserver.service;

import com.workerrobotics.sftpserver.config.KerberosProperties;
import com.workerrobotics.sftpserver.config.SftpProperties;
import com.workerrobotics.sftpserver.model.SftpServerConfig;
import com.workerrobotics.sftpserver.model.SftpUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests voor SftpServerService.
 * Elke test gebruikt een eigen tijdelijke map en een vrije poort om conflicten te vermijden.
 */
@DisplayName("SftpServerService")
class SftpServerServiceTest {

    @TempDir
    Path tempDir;

    private SftpServerService service;
    private UserRegistryService userRegistry;
    private SftpProperties properties;
    private KerberosProperties kerberosProperties;

    // Gebruik een hoge poort om rechtenconflicten te vermijden; let op: tests lopen sequentieel
    private static final int BASE_PORT = 22200;
    private static int portOffset = 0;

    private int nextPort() {
        return BASE_PORT + portOffset++;
    }

    @BeforeEach
    void setUp() {
        properties = new SftpProperties();
        properties.setHostKeyPath(tempDir.resolve("test-host-key").toString());
        properties.setRootDirectory(tempDir.resolve("sftp-root").toString());
        properties.setPort(nextPort());

        userRegistry = new UserRegistryService();
        userRegistry.addOrUpdate(SftpUser.of("testuser", "testpass", null));

        kerberosProperties = new KerberosProperties();
        kerberosProperties.setEnabled(false);
        kerberosProperties.setPasswordFallback(true);

        service = new SftpServerService(
                properties,
                userRegistry,
                kerberosProperties,
                null
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        if (service.isRunning()) {
            service.stop();
        }
    }

    @Nested
    @DisplayName("initiële status")
    class InitialState {

        @Test
        @DisplayName("server is standaard niet actief")
        void notRunningByDefault() {
            assertThat(service.isRunning()).isFalse();
        }

        @Test
        @DisplayName("configuratie is geladen vanuit properties")
        void configLoadedFromProperties() {
            SftpServerConfig config = service.getConfig();

            assertThat(config.rootDirectory()).isEqualTo(properties.getRootDirectory());
            assertThat(config.idleTimeoutSeconds()).isEqualTo(properties.getIdleTimeoutSeconds());
            assertThat(config.authTimeoutSeconds()).isEqualTo(properties.getAuthTimeoutSeconds());
        }
    }

    @Nested
    @DisplayName("start")
    class Start {

        @Test
        @DisplayName("start de server succesvol")
        void startSuccess() throws IOException {
            service.start();

            assertThat(service.isRunning()).isTrue();
        }

        @Test
        @DisplayName("gooit IllegalStateException als server al actief is")
        void startWhenAlreadyRunning() throws IOException {
            service.start();

            assertThatThrownBy(() -> service.start())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("al actief");
        }
    }

    @Nested
    @DisplayName("stop")
    class Stop {

        @Test
        @DisplayName("stopt de server succesvol")
        void stopSuccess() throws IOException {
            service.start();

            service.stop();

            assertThat(service.isRunning()).isFalse();
        }

        @Test
        @DisplayName("gooit IllegalStateException als server niet actief is")
        void stopWhenNotRunning() {
            assertThatThrownBy(() -> service.stop())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("niet actief");
        }
    }

    @Nested
    @DisplayName("reload")
    class Reload {

        @Test
        @DisplayName("herlaadt de server met nieuwe configuratie als server actief was")
        void reloadWhileRunning() throws IOException {
            service.start();
            int newPort = nextPort();
            SftpServerConfig newConfig = new SftpServerConfig(
                    newPort,
                    tempDir.resolve("new-root").toString(),
                    600,
                    60,
                    5
            );

            service.reload(newConfig);

            assertThat(service.isRunning()).isTrue();
            assertThat(service.getConfig().port()).isEqualTo(newPort);
            assertThat(service.getConfig().idleTimeoutSeconds()).isEqualTo(600);
        }

        @Test
        @DisplayName("slaat configuratie op maar start niet als server gestopt was")
        void reloadWhenNotRunning() throws IOException {
            int newPort = nextPort();
            SftpServerConfig newConfig = new SftpServerConfig(
                    newPort,
                    tempDir.resolve("new-root").toString(),
                    120,
                    30,
                    10
            );

            service.reload(newConfig);

            assertThat(service.isRunning()).isFalse();
            assertThat(service.getConfig().port()).isEqualTo(newPort);
        }

        @Test
        @DisplayName("maakt de root-directory aan als die niet bestaat")
        void createsRootDirectory() throws IOException {
            Path newRoot = tempDir.resolve("auto-created-root");
            SftpServerConfig newConfig = new SftpServerConfig(
                    nextPort(),
                    newRoot.toString(),
                    300,
                    30,
                    10
            );

            service.reload(newConfig);

            // Start om de directory-aanmaak te triggeren
            service.start();

            assertThat(newRoot).exists().isDirectory();
        }
    }

    @Nested
    @DisplayName("getConfig")
    class GetConfig {

        @Test
        @DisplayName("geeft de huidige configuratie terug")
        void returnsCurrentConfig() {
            SftpServerConfig config = service.getConfig();

            assertThat(config).isNotNull();
            assertThat(config.authTimeoutSeconds()).isGreaterThan(0);
        }
    }
}
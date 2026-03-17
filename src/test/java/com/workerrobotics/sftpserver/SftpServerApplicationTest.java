package com.workerrobotics.sftpserver;

import com.workerrobotics.sftpserver.service.SftpServerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("SftpServerApplication integratie")
class SftpServerApplicationTest {

    @Autowired
    private SftpServerService sftpServerService;

    @Test
    @DisplayName("applicatie start correct op")
    void contextLoads() {
        assertThat(sftpServerService).isNotNull();
    }

    @Test
    @DisplayName("server is na opstarten niet automatisch actief")
    void serverNotAutoStarted() {
        assertThat(sftpServerService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("standaard configuratie is aanwezig")
    void defaultConfigPresent() {
        assertThat(sftpServerService.getConfig()).isNotNull();
        assertThat(sftpServerService.getConfig().port()).isGreaterThan(0);
    }
}
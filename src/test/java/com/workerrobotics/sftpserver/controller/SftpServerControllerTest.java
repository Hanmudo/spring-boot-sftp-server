package com.workerrobotics.sftpserver.controller;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import com.workerrobotics.sftpserver.model.SftpServerConfig;
import com.workerrobotics.sftpserver.service.SftpServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SftpServerController.class)
@DisplayName("SftpServerController")
class SftpServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SftpServerService sftpServerService;

    private SftpServerConfig defaultConfig;

    @BeforeEach
    void setUp() {
        defaultConfig = new SftpServerConfig(2222, "/tmp/sftp", 300, 30, 10);
        when(sftpServerService.getConfig()).thenReturn(defaultConfig);
    }

    @Nested
    @DisplayName("GET /api/server/status")
    class GetStatus {

        @Test
        @DisplayName("geeft status actief terug als server draait")
        void statusRunning() throws Exception {
            when(sftpServerService.isRunning()).thenReturn(true);

            mockMvc.perform(get("/api/server/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.running").value(true))
                    .andExpect(jsonPath("$.config.port").value(2222));
        }

        @Test
        @DisplayName("geeft status inactief terug als server gestopt is")
        void statusNotRunning() throws Exception {
            when(sftpServerService.isRunning()).thenReturn(false);

            mockMvc.perform(get("/api/server/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.running").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/server/config")
    class GetConfig {

        @Test
        @DisplayName("geeft de huidige configuratie terug")
        void getConfig() throws Exception {
            mockMvc.perform(get("/api/server/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.port").value(2222))
                    .andExpect(jsonPath("$.rootDirectory").value("/tmp/sftp"))
                    .andExpect(jsonPath("$.idleTimeoutSeconds").value(300));
        }
    }

    @Nested
    @DisplayName("POST /api/server/start")
    class StartServer {

        @Test
        @DisplayName("start de server en geeft 200 terug")
        void startSuccess() throws Exception {
            doNothing().when(sftpServerService).start();

            mockMvc.perform(post("/api/server/start"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("gestart"))
                    .andExpect(jsonPath("$.poort").value("2222"));
        }

        @Test
        @DisplayName("geeft 400 terug als server al actief is")
        void startAlreadyRunning() throws Exception {
            doThrow(new IllegalStateException("SFTP-server is al actief"))
                    .when(sftpServerService).start();

            mockMvc.perform(post("/api/server/start"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fout").value("SFTP-server is al actief"));
        }

        @Test
        @DisplayName("geeft 500 terug bij IOException")
        void startIOException() throws Exception {
            doThrow(new IOException("Poort bezet"))
                    .when(sftpServerService).start();

            mockMvc.perform(post("/api/server/start"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.fout").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/server/stop")
    class StopServer {

        @Test
        @DisplayName("stopt de server en geeft 200 terug")
        void stopSuccess() throws Exception {
            doNothing().when(sftpServerService).stop();

            mockMvc.perform(post("/api/server/stop"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("gestopt"));
        }

        @Test
        @DisplayName("geeft 400 terug als server niet actief is")
        void stopNotRunning() throws Exception {
            doThrow(new IllegalStateException("SFTP-server is niet actief"))
                    .when(sftpServerService).stop();

            mockMvc.perform(post("/api/server/stop"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fout").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/server/config")
    class UpdateConfig {

        @Test
        @DisplayName("past configuratie aan en geeft 200 terug")
        void updateConfigSuccess() throws Exception {
            SftpServerConfig newConfig = new SftpServerConfig(2223, "/tmp/sftp-new", 600, 60, 5);
            when(sftpServerService.getConfig()).thenReturn(newConfig);
            when(sftpServerService.isRunning()).thenReturn(true);
            doNothing().when(sftpServerService).reload(any(SftpServerConfig.class));

            mockMvc.perform(put("/api/server/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newConfig)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("configuratie bijgewerkt"))
                    .andExpect(jsonPath("$.actief").value(true));
        }

        @Test
        @DisplayName("geeft 400 terug bij ongeldige poort")
        void updateConfigInvalidPort() throws Exception {
            SftpServerConfig invalid = new SftpServerConfig(0, "/tmp/sftp", 300, 30, 10);

            mockMvc.perform(put("/api/server/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("geeft 400 terug bij lege rootDirectory")
        void updateConfigBlankRootDir() throws Exception {
            SftpServerConfig invalid = new SftpServerConfig(2222, "  ", 300, 30, 10);

            mockMvc.perform(put("/api/server/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("geeft 500 terug bij IOException tijdens reload")
        void updateConfigIOException() throws Exception {
            doThrow(new IOException("Herstart mislukt"))
                    .when(sftpServerService).reload(any(SftpServerConfig.class));

            mockMvc.perform(put("/api/server/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(defaultConfig)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("roept reload aan op de service")
        void updateConfigCallsReload() throws Exception {
            doNothing().when(sftpServerService).reload(any(SftpServerConfig.class));

            mockMvc.perform(put("/api/server/config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(defaultConfig)));

            verify(sftpServerService, times(1)).reload(any(SftpServerConfig.class));
        }
    }
}
package com.workerrobotics.sftpserver.controller;

import com.workerrobotics.sftpserver.model.SftpServerConfig;
import com.workerrobotics.sftpserver.service.SftpServerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * REST-controller voor het beheren van de SFTP-server levenscyclus en configuratie.
 */
@RestController
@RequestMapping("/api/server")
public class SftpServerController {

    private static final Logger log = LoggerFactory.getLogger(SftpServerController.class);

    private final SftpServerService sftpServerService;

    public SftpServerController(SftpServerService sftpServerService) {
        this.sftpServerService = sftpServerService;
    }

    /**
     * Geeft de huidige serverstatus en configuratie terug.
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "running", sftpServerService.isRunning(),
                "config", sftpServerService.getConfig()
        );
    }

    /**
     * Geeft de huidige configuratie terug.
     */
    @GetMapping("/config")
    public SftpServerConfig getConfig() {
        return sftpServerService.getConfig();
    }

    /**
     * Start de SFTP-server.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start() {
        try {
            sftpServerService.start();
            return ResponseEntity.ok(Map.of("status", "gestart", "poort",
                    String.valueOf(sftpServerService.getConfig().port())));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("fout", e.getMessage()));
        } catch (IOException e) {
            log.error("Kon SFTP-server niet starten", e);
            return ResponseEntity.internalServerError().body(Map.of("fout", e.getMessage()));
        }
    }

    /**
     * Stopt de SFTP-server.
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stop() {
        try {
            sftpServerService.stop();
            return ResponseEntity.ok(Map.of("status", "gestopt"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("fout", e.getMessage()));
        } catch (IOException e) {
            log.error("Kon SFTP-server niet stoppen", e);
            return ResponseEntity.internalServerError().body(Map.of("fout", e.getMessage()));
        }
    }

    /**
     * Past de configuratie aan en herlaadt de server indien actief.
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@Valid @RequestBody SftpServerConfig config) {
        try {
            sftpServerService.reload(config);
            return ResponseEntity.ok(Map.of(
                    "status", "configuratie bijgewerkt",
                    "config", sftpServerService.getConfig(),
                    "actief", sftpServerService.isRunning()
            ));
        } catch (IOException e) {
            log.error("Kon server niet herladen met nieuwe configuratie", e);
            return ResponseEntity.internalServerError().body(Map.of("fout", e.getMessage()));
        }
    }
}

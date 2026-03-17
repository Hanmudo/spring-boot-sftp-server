package com.workerrobotics.sftpserver.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Dynamische configuratie van de SFTP-server.
 */
public record SftpServerConfig(

        @Min(value = 1, message = "Poort moet minimaal 1 zijn")
        @Max(value = 65535, message = "Poort mag maximaal 65535 zijn")
        int port,

        @NotBlank(message = "Root-directory mag niet leeg zijn")
        String rootDirectory,

        @Min(value = 0, message = "Idle timeout mag niet negatief zijn")
        int idleTimeoutSeconds,

        @Min(value = 1, message = "Auth timeout moet minimaal 1 seconde zijn")
        int authTimeoutSeconds,

        int maxSessions
) {}

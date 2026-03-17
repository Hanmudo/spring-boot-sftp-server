package com.workerrobotics.sftpserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externe configuratie-properties voor de SFTP-server (application.yml).
 */
@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {

    private int port = 2222;
    private String hostKeyPath = "./sftp-host-key";
    private String rootDirectory = "./sftp-root";
    private int idleTimeoutSeconds = 300;
    private int authTimeoutSeconds = 30;
    private int maxSessions = 10;

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getHostKeyPath() { return hostKeyPath; }
    public void setHostKeyPath(String hostKeyPath) { this.hostKeyPath = hostKeyPath; }

    public String getRootDirectory() { return rootDirectory; }
    public void setRootDirectory(String rootDirectory) { this.rootDirectory = rootDirectory; }

    public int getIdleTimeoutSeconds() { return idleTimeoutSeconds; }
    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) { this.idleTimeoutSeconds = idleTimeoutSeconds; }

    public int getAuthTimeoutSeconds() { return authTimeoutSeconds; }
    public void setAuthTimeoutSeconds(int authTimeoutSeconds) { this.authTimeoutSeconds = authTimeoutSeconds; }

    public int getMaxSessions() { return maxSessions; }
    public void setMaxSessions(int maxSessions) { this.maxSessions = maxSessions; }
}

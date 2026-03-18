package com.workerrobotics.sftpserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sftp.public-key")
public class PublicKeyAuthProperties {

    /**
     * Zet SSH public key authenticatie aan/uit.
     */
    private boolean enabled = false;

    /**
     * Basismap waar per gebruiker een authorized_keys bestand staat.
     * Voorbeeld:
     *   /etc/sftp/authorized_keys/alice
     *   /etc/sftp/authorized_keys/bob
     */
    private String authorizedKeysDirectory;

    /**
     * Als true, mag password-auth naast public key auth blijven bestaan.
     */
    private boolean passwordFallback = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAuthorizedKeysDirectory() {
        return authorizedKeysDirectory;
    }

    public void setAuthorizedKeysDirectory(String authorizedKeysDirectory) {
        this.authorizedKeysDirectory = authorizedKeysDirectory;
    }

    public boolean isPasswordFallback() {
        return passwordFallback;
    }

    public void setPasswordFallback(boolean passwordFallback) {
        this.passwordFallback = passwordFallback;
    }
}

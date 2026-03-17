package com.workerrobotics.sftpserver.view;

/**
 * API-view van een SFTP-gebruiker zonder gevoelige gegevens.
 */
public record SftpUserView(
        String username,
        String homeDirectory,
        boolean enabled
) {}

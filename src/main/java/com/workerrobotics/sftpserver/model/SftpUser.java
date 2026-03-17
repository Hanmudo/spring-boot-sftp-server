package com.workerrobotics.sftpserver.model;

import com.workerrobotics.sftpserver.view.SftpUserView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Representeert een SFTP-gebruiker met inloggegevens en thuismap.
 */
public record SftpUser(

        @NotBlank(message = "Gebruikersnaam mag niet leeg zijn")
        @Pattern(regexp = "^[a-zA-Z0-9_\\-]{1,64}$",
                message = "Gebruikersnaam mag alleen letters, cijfers, _ en - bevatten (max 64 tekens)")
        String username,

        @NotBlank(message = "Wachtwoord mag niet leeg zijn")
        String password,

        /**
         * Relatief pad t.o.v. de geconfigureerde root-directory.
         * Leeg of null betekent: root-directory zelf.
         */
        String homeDirectory,

        boolean enabled
) {

    /**
     * Handige fabrieksmethode voor een standaard ingeschakelde gebruiker.
     */
    public static SftpUser of(String username, String password, String homeDirectory) {
        return new SftpUser(username, password, homeDirectory, true);
    }

    /**
     * Geeft een versie van de gebruiker terug zonder wachtwoord (voor API-responses).
     */
    public SftpUserView toView() {
        return new SftpUserView(username, homeDirectory, enabled);
    }
}
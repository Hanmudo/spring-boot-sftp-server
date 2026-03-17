package com.workerrobotics.sftpserver.service;

import com.workerrobotics.sftpserver.model.SftpUser;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry voor SFTP-gebruikers.
 * Thread-safe via ConcurrentHashMap.
 */
@Service
public class UserRegistryService {

    private final Map<String, SftpUser> users = new ConcurrentHashMap<>();

    /**
     * Voegt een gebruiker toe of overschrijft een bestaande.
     *
     * @param user de te registreren gebruiker
     * @return de opgeslagen gebruiker
     */
    public SftpUser addOrUpdate(SftpUser user) {
        users.put(user.username(), user);
        return user;
    }

    /**
     * Verwijdert een gebruiker op basis van gebruikersnaam.
     *
     * @param username de gebruikersnaam
     * @return true als de gebruiker bestond en is verwijderd
     */
    public boolean remove(String username) {
        return users.remove(username) != null;
    }

    /**
     * Zoekt een gebruiker op gebruikersnaam.
     *
     * @param username de gebruikersnaam
     * @return een Optional met de gevonden gebruiker
     */
    public Optional<SftpUser> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    /**
     * Valideert gebruikersnaam en wachtwoord.
     *
     * @param username de gebruikersnaam
     * @param password het wachtwoord in plain-text
     * @return true als de combinatie geldig is en de gebruiker ingeschakeld is
     */
    public boolean authenticate(String username, String password) {
        return findByUsername(username)
                .filter(SftpUser::enabled)
                .map(u -> u.password().equals(password))
                .orElse(false);
    }

    /**
     * Geeft alle geregistreerde gebruikers terug.
     *
     * @return onveranderlijke collectie van gebruikers
     */
    public Collection<SftpUser> findAll() {
        return users.values();
    }

    /**
     * Geeft het aantal geregistreerde gebruikers terug.
     */
    public int count() {
        return users.size();
    }
}

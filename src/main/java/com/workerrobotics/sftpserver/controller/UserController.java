package com.workerrobotics.sftpserver.controller;

import com.workerrobotics.sftpserver.model.SftpUser;
import com.workerrobotics.sftpserver.service.UserRegistryService;
import com.workerrobotics.sftpserver.view.SftpUserView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-controller voor het beheren van SFTP-gebruikers.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRegistryService userRegistry;

    public UserController(UserRegistryService userRegistry) {
        this.userRegistry = userRegistry;
    }

    /**
     * Geeft alle geregistreerde gebruikers terug (zonder wachtwoorden).
     */
    @GetMapping
    public List<SftpUserView> getAllUsers() {
        return userRegistry.findAll().stream()
                .map(SftpUser::toView)
                .toList();
    }

    /**
     * Geeft een specifieke gebruiker terug (zonder wachtwoord).
     */
    @GetMapping("/{username}")
    public ResponseEntity<SftpUserView> getUser(@PathVariable String username) {
        return userRegistry.findByUsername(username)
                .map(u -> ResponseEntity.ok(u.toView()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Voegt een nieuwe gebruiker toe of werkt een bestaande bij.
     */
    @PostMapping
    public ResponseEntity<SftpUserView> createOrUpdateUser(@Valid @RequestBody SftpUser user) {
        SftpUser saved = userRegistry.addOrUpdate(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toView());
    }

    /**
     * Verwijdert een gebruiker.
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        if (userRegistry.remove(username)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

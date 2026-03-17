package com.workerrobotics.sftpserver.service;

import com.workerrobotics.sftpserver.model.SftpUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRegistryService")
class UserRegistryServiceTest {

    private UserRegistryService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistryService();
    }

    @Nested
    @DisplayName("addOrUpdate")
    class AddOrUpdate {

        @Test
        @DisplayName("voegt een nieuwe gebruiker toe")
        void addNewUser() {
            SftpUser user = SftpUser.of("alice", "geheim", "alice-home");

            SftpUser result = service.addOrUpdate(user);

            assertThat(result).isEqualTo(user);
            assertThat(service.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("overschrijft een bestaande gebruiker")
        void overwriteExistingUser() {
            service.addOrUpdate(SftpUser.of("alice", "oud-wachtwoord", "home"));
            SftpUser updated = SftpUser.of("alice", "nieuw-wachtwoord", "home");

            service.addOrUpdate(updated);

            assertThat(service.count()).isEqualTo(1);
            assertThat(service.findByUsername("alice"))
                    .isPresent()
                    .hasValueSatisfying(u -> assertThat(u.password()).isEqualTo("nieuw-wachtwoord"));
        }

        @Test
        @DisplayName("kan meerdere gebruikers opslaan")
        void addMultipleUsers() {
            service.addOrUpdate(SftpUser.of("alice", "ww1", null));
            service.addOrUpdate(SftpUser.of("bob", "ww2", null));
            service.addOrUpdate(SftpUser.of("charlie", "ww3", null));

            assertThat(service.count()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("verwijdert een bestaande gebruiker en geeft true terug")
        void removeExistingUser() {
            service.addOrUpdate(SftpUser.of("alice", "ww", null));

            boolean removed = service.remove("alice");

            assertThat(removed).isTrue();
            assertThat(service.count()).isZero();
        }

        @Test
        @DisplayName("geeft false terug voor een onbekende gebruiker")
        void removeNonExistingUser() {
            boolean removed = service.remove("onbekend");

            assertThat(removed).isFalse();
        }
    }

    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("vindt een bestaande gebruiker")
        void findExistingUser() {
            SftpUser user = SftpUser.of("alice", "ww", "home");
            service.addOrUpdate(user);

            Optional<SftpUser> result = service.findByUsername("alice");

            assertThat(result).isPresent().contains(user);
        }

        @Test
        @DisplayName("geeft leeg terug voor onbekende gebruiker")
        void findNonExistingUser() {
            Optional<SftpUser> result = service.findByUsername("onbekend");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("authenticatie slaagt bij correct wachtwoord en ingeschakelde gebruiker")
        void authenticateSuccess() {
            service.addOrUpdate(SftpUser.of("alice", "juist-wachtwoord", null));

            boolean result = service.authenticate("alice", "juist-wachtwoord");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("authenticatie mislukt bij verkeerd wachtwoord")
        void authenticateWrongPassword() {
            service.addOrUpdate(SftpUser.of("alice", "juist-wachtwoord", null));

            boolean result = service.authenticate("alice", "fout-wachtwoord");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("authenticatie mislukt voor onbekende gebruiker")
        void authenticateUnknownUser() {
            boolean result = service.authenticate("onbekend", "wachtwoord");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("authenticatie mislukt voor uitgeschakelde gebruiker")
        void authenticateDisabledUser() {
            SftpUser disabled = new SftpUser("alice", "wachtwoord", null, false);
            service.addOrUpdate(disabled);

            boolean result = service.authenticate("alice", "wachtwoord");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("authenticatie mislukt bij leeg wachtwoord")
        void authenticateEmptyPassword() {
            service.addOrUpdate(SftpUser.of("alice", "wachtwoord", null));

            boolean result = service.authenticate("alice", "");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("geeft alle gebruikers terug")
        void findAllUsers() {
            service.addOrUpdate(SftpUser.of("alice", "ww1", null));
            service.addOrUpdate(SftpUser.of("bob", "ww2", null));

            Collection<SftpUser> all = service.findAll();

            assertThat(all).hasSize(2);
            assertThat(all).extracting(SftpUser::username)
                    .containsExactlyInAnyOrder("alice", "bob");
        }

        @Test
        @DisplayName("geeft lege collectie terug als er geen gebruikers zijn")
        void findAllEmpty() {
            assertThat(service.findAll()).isEmpty();
        }
    }
}
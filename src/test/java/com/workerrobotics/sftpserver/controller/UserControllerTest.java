package com.workerrobotics.sftpserver.controller;

import tools.jackson.databind.ObjectMapper;

import com.workerrobotics.sftpserver.model.SftpUser;
import com.workerrobotics.sftpserver.service.UserRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRegistryService userRegistry;

    private SftpUser alice;
    private SftpUser bob;

    @BeforeEach
    void setUp() {
        alice = SftpUser.of("alice", "ww-alice", "alice-home");
        bob = SftpUser.of("bob", "ww-bob", "bob-home");
    }

    @Nested
    @DisplayName("GET /api/users")
    class GetAllUsers {

        @Test
        @DisplayName("geeft alle gebruikers terug zonder wachtwoorden")
        void getAllUsers() throws Exception {
            when(userRegistry.findAll()).thenReturn(List.of(alice, bob));

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].username").value("alice"))
                    .andExpect(jsonPath("$[0].homeDirectory").value("alice-home"))
                    .andExpect(jsonPath("$[0].enabled").value(true))
                    .andExpect(jsonPath("$[0].password").doesNotExist());
        }

        @Test
        @DisplayName("geeft lege lijst terug als er geen gebruikers zijn")
        void getAllUsersEmpty() throws Exception {
            when(userRegistry.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{username}")
    class GetUser {

        @Test
        @DisplayName("geeft 200 OK terug voor bekende gebruiker")
        void getUserFound() throws Exception {
            when(userRegistry.findByUsername("alice")).thenReturn(Optional.of(alice));

            mockMvc.perform(get("/api/users/alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("geeft 404 terug voor onbekende gebruiker")
        void getUserNotFound() throws Exception {
            when(userRegistry.findByUsername("onbekend")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/users/onbekend"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/users")
    class CreateUser {

        @Test
        @DisplayName("maakt een gebruiker aan en geeft 201 Created terug")
        void createUser() throws Exception {
            when(userRegistry.addOrUpdate(any(SftpUser.class))).thenReturn(alice);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(alice)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("geeft 400 terug bij ongeldige gebruikersnaam")
        void createUserInvalidUsername() throws Exception {
            SftpUser invalid = new SftpUser("invalid user!", "ww", null, true);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("geeft 400 terug bij leeg wachtwoord")
        void createUserEmptyPassword() throws Exception {
            SftpUser invalid = new SftpUser("alice", "", null, true);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("geeft 400 terug bij ontbrekende gebruikersnaam")
        void createUserMissingUsername() throws Exception {
            String body = """
                    {"password": "ww", "homeDirectory": null, "enabled": true}
                    """;

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("slaat op via de registry")
        void createUserCallsRegistry() throws Exception {
            when(userRegistry.addOrUpdate(any(SftpUser.class))).thenReturn(alice);

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(alice)));

            verify(userRegistry, times(1)).addOrUpdate(any(SftpUser.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{username}")
    class DeleteUser {

        @Test
        @DisplayName("verwijdert een bestaande gebruiker en geeft 204 terug")
        void deleteUserFound() throws Exception {
            when(userRegistry.remove("alice")).thenReturn(true);

            mockMvc.perform(delete("/api/users/alice"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("geeft 404 terug bij onbekende gebruiker")
        void deleteUserNotFound() throws Exception {
            when(userRegistry.remove("onbekend")).thenReturn(false);

            mockMvc.perform(delete("/api/users/onbekend"))
                    .andExpect(status().isNotFound());
        }
    }
}
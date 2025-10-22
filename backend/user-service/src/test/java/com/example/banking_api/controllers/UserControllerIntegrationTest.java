package com.example.banking_api.controllers;

import com.example.banking_api.users.User;
import com.example.banking_api.users.UserRepository;
import com.example.banking_api.users.records.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withReuse(true);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @Container
    static GenericContainer<?> rabbitMQ = new GenericContainer<>("rabbitmq:3-management")
            .withExposedPorts(5672)
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "");
        registry.add("REDIS_HOST", redis::getHost);
        registry.add("REDIS_PORT", redis::getFirstMappedPort);
        registry.add("REDIS_PASSWORD", () -> "");

        // RabbitMQ configuration
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getFirstMappedPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("RABBIT_HOST", rabbitMQ::getHost);
        registry.add("RABBIT_PORT", rabbitMQ::getFirstMappedPort);
        registry.add("RABBIT_USERNAME", () -> "guest");
        registry.add("RABBIT_PASSWORD", () -> "guest");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should get user by ID and return 200 status")
    void shouldGetUserByIdAndReturn200Status() throws Exception {
        User user = createTestUser();
        User savedUser = userRepository.save(user);

        mockMvc.perform(get("/api/v1/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("qwerty"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Should return 404 when user not found by ID")
    void shouldReturn404WhenUserNotFoundById() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/users/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should update user and return 200 status")
    void shouldUpdateUserAndReturn200Status() throws Exception {
        User user = createTestUser();
        User savedUser = userRepository.save(user);

        UpdateUserDTO updateDTO = new UpdateUserDTO("test@example.com", "qwerty");

        mockMvc.perform(put("/api/v1/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("qwerty"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        Optional<User> updatedUser = userRepository.findById(savedUser.getId());
        assertTrue(updatedUser.isPresent());
        assertEquals("qwerty", updatedUser.get().getName());
        assertEquals("test@example.com", updatedUser.get().getEmail());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent user")
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        UpdateUserDTO updateDTO = new UpdateUserDTO("test@example.com", "qwerty");

        mockMvc.perform(put("/api/v1/users/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should delete user and return 200 status")
    void shouldDeleteUserAndReturn200Status() throws Exception {
        User user = createTestUser();
        User savedUser = userRepository.save(user);

        mockMvc.perform(delete("/api/v1/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedUser.id").value(savedUser.getId().toString()));

        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertFalse(deletedUser.isPresent());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent user")
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/users/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 400 when email is empty for password reset initialization")
    void shouldReturn400WhenEmailIsEmptyForPasswordResetInit() throws Exception {
        UserEmailDTO emailDTO = new UserEmailDTO("");

        mockMvc.perform(put("/api/v1/users/password-reset-initialization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email cannot be empty"));
    }

    @Test
    @DisplayName("Should return 400 when email is null for password reset initialization")
    void shouldReturn400WhenEmailIsNullForPasswordResetInit() throws Exception {
        UserEmailDTO emailDTO = new UserEmailDTO(null);

        mockMvc.perform(put("/api/v1/users/password-reset-initialization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email cannot be empty"));
    }

    @Test
    @DisplayName("Should return 400 for invalid request body in password reset")
    void shouldReturn400ForInvalidRequestBodyInPasswordReset() throws Exception {
        ResetPasswordDTO resetDTO = new ResetPasswordDTO("", "");

        mockMvc.perform(put("/api/v1/users/password-reset")
                        .param("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email is required"))
                .andExpect(jsonPath("$.password").value("Password is required"));
    }

    @Test
    @DisplayName("Should return 400 when token parameter is missing for password reset")
    void shouldReturn400WhenTokenParameterIsMissingForPasswordReset() throws Exception {
        ResetPasswordDTO resetDTO = new ResetPasswordDTO("test@example.com", "newPassword");

        mockMvc.perform(put("/api/v1/users/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 415 for unsupported media type")
    void shouldReturn415ForUnsupportedMediaType() throws Exception {
        UpdateUserDTO updateDTO = new UpdateUserDTO("test@example.com", "qwerty");

        mockMvc.perform(put("/api/v1/users/" + UUID.randomUUID())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isUnsupportedMediaType());
    }

    private User createTestUser() {
        return User.builder()
                .name("qwerty")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

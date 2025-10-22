package com.example.banking_api.repositories;

import com.example.banking_api.users.User;
import com.example.banking_api.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
public class UserRepositoryIntegrationTest {
    private final Logger logger = LoggerFactory.getLogger(UserRepositoryIntegrationTest.class);
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUser() {
        User user = userSetUp();

        userRepository.delete(user);
        entityManager.flush();
        logger.info("User deleted: {}", user);

        assertTrue(userRepository.findUserByEmail(user.getEmail()).isEmpty());
    }

    @Test
    @DisplayName("Should return true if user exists")
    void shouldReturnTrueIfUserExists() {
        User user = userSetUp();

        User savedUser = userRepository.save(user);
        entityManager.flush();
        logger.info("User saved: {}", savedUser);

        assertTrue(userRepository.userExists(savedUser.getId()));
    }

    @Test
    @DisplayName("Should return false if user does not exists")
    void shouldReturnFalseIfUserDoesNotExists() {
        User user = userSetUp();
        user.setId(UUID.randomUUID());

        assertFalse(userRepository.userExists(user.getId()));
    }

    @Test
    @DisplayName("Should find user by id")
    void shouldFindUserById() {
        User user = userSetUp();

        User savedUser = userRepository.save(user);
        entityManager.flush();

        Optional<User> foundUser = userRepository.findUserById(user.getId());

        assertTrue(foundUser.isPresent());
        logger.info("User found: {}", foundUser.get());
        assertEquals(savedUser.getId(), foundUser.get().getId());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        User user = userSetUp();

        User savedUser = userRepository.save(user);
        entityManager.flush();

        Optional<User> foundUser = userRepository.findUserByEmail(user.getEmail());

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUser() {
        User userInit = userSetUp();

        User savedUser = userRepository.save(userInit);
        savedUser.setName("newName");
        User updatedUser = userRepository.save(savedUser);
        entityManager.flush();
        Optional<User> foundUser = userRepository.findUserById(updatedUser.getId());

        assertTrue(foundUser.isPresent());
        assertEquals("newName", foundUser.get().getName());
        assertEquals(updatedUser.getId(), foundUser.get().getId());
    }

    private User userSetUp()
    {
        User user = new User();

        user.setName("test");
        user.setPassword("test");
        user.setEmail("test@example.com");

        return user;
    }
}

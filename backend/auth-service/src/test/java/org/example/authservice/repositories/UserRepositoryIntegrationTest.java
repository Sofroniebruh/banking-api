package org.example.authservice.repositories;

import org.example.authservice.users.Role;
import org.example.authservice.users.User;
import org.example.authservice.users.UserRepository;
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
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class UserRepositoryIntegrationTest
{
    private final Logger logger = LoggerFactory.getLogger(UserRepositoryIntegrationTest.class);
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp()
    {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save user, generate ID, createdAt, updatedAt")
    void shouldSaveUser()
    {
        User user = userSetUp();
        User savedUser = userRepository.save(user);

        entityManager.flush();
        logger.info("User saved: {}", savedUser);

        assertNotNull(savedUser.getId());
        assertEquals(user.getEmail(), savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUser()
    {
        User user = userSetUp();

        userRepository.delete(user);
        entityManager.flush();
        logger.info("User deleted: {}", user);

        assertTrue(userRepository.findUserByEmail(user.getEmail()).isEmpty());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail()
    {
        User user = userSetUp();

        userRepository.save(user);
        entityManager.flush();

        Optional<User> foundUser = userRepository.findUserByEmail(user.getEmail());

        assertTrue(foundUser.isPresent());
        assertEquals(user.getEmail(), foundUser.get().getEmail());

        logger.info("User found: {}", foundUser);
    }

    @Test
    @DisplayName("Should return empty if requested user was not found")
    void shouldReturnEmptyIfUserNotFound()
    {
        Optional<User> optionalUser = userRepository.findUserByEmail("test@example.com");

        assertTrue(optionalUser.isEmpty());
    }

    @Test
    @DisplayName("Should successfully update user")
    void shouldUpdateUser()
    {
        User user = userSetUp();

        User savedUser = userRepository.save(user);
        savedUser.setName("new-name");
        User updatedUser = userRepository.save(savedUser);

        entityManager.flush();
        entityManager.clear();

        Optional<User> optionalUser = userRepository.findUserByEmail(savedUser.getEmail());

        assertTrue(optionalUser.isPresent());

        User foundUser = optionalUser.get();

        logger.info("User found: {}", foundUser);

        assertEquals(updatedUser.getName(), foundUser.getName());
        assertTrue(foundUser.getUpdatedAt().isAfter(foundUser.getCreatedAt()));
    }

    private User userSetUp()
    {
        User user = new User();

        user.setRoles(new ArrayList<>(List.of(Role.USER)));
        user.setName("test");
        user.setPassword("test");
        user.setEmail("test@example.com");

        return user;
    }
}

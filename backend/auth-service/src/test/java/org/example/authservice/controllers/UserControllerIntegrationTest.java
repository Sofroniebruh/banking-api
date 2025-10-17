package org.example.authservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.authservice.users.Role;
import org.example.authservice.users.User;
import org.example.authservice.users.UserRepository;
import org.example.authservice.users.records.AuthUserDTO;
import org.example.authservice.users.records.CreateUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = org.example.authservice.TestConfiguration.class
)
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${INTERNAL_SERVICE_SECRET}")
    private String INTERNAL_SERVICE_SECRET;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create use and return 201 status code")
    void createUseAndReturn201Status() throws Exception {
        CreateUserDTO userDTO = new CreateUserDTO("testName", "test@gmail.com", "qwerty123");

        mockMvc.perform(post("/api/v1/auth/registration")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.name").value("testName"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.roles").exists())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        Optional<User> savedUser = userRepository.findUserByEmail("test@gmail.com");
        assertTrue(savedUser.isPresent());
        assertNotEquals("qwerty123", savedUser.get().getPassword());
    }

    @Test
    @DisplayName("Should return error 400 bad request if the dto is incomplete")
    void shouldReturn400BadRequestIfDtoIsIncomplete() throws Exception {
        CreateUserDTO userDTO = new CreateUserDTO(null, "", "qwerty123");

        mockMvc.perform(post("/api/v1/auth/registration")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Name is required"))
                .andExpect(jsonPath("$.email").value("Email is required"))
                .andReturn();

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("Should return 401 status if user is already registered")
    void shouldReturn401StatusIfUserIsAlreadyRegistered() throws Exception {
        User user = userSetup();

        userRepository.save(user);

        CreateUserDTO userDTO = new CreateUserDTO("qwerty", "test@gmail.com", "qwerty123");

        mockMvc.perform(post("/api/v1/auth/registration")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))

                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("User with email test@gmail.com already exists"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should authenticate user with valid credentials")
    void shouldAuthenticateUserWithValidCredentials() throws Exception {
        User user = userSetup();

        userRepository.save(user);

        AuthUserDTO userDTO = new AuthUserDTO("test@gmail.com", "test");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.roles").exists())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();
    }

    @Test
    @DisplayName("Should return error 400 bad request if the dto is incomplete for login")
    void shouldReturn400BadRequestIfDtoIsIncompleteForLogin() throws Exception {
        AuthUserDTO userDTO = new AuthUserDTO(null, "test@gmail.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email is required"))
                .andReturn();
    }

    @Test
    @DisplayName("Should return 401 status if credentials are wrong")
    void shouldReturn401StatusIfCredentialsAreWrong() throws Exception {
        User user = userSetup();

        userRepository.save(user);

        AuthUserDTO userDTO = new AuthUserDTO("test@gmail.com", "test1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))

                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();
    }

    private User userSetup() {
        User user = new User();

        user.setName("test");
        user.setEmail("test@gmail.com");
        user.setPassword(passwordEncoder.encode("test"));
        user.setRoles(new ArrayList<>(List.of(Role.USER)));

        return user;
    }
}

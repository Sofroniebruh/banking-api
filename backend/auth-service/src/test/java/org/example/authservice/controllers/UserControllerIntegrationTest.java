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

@SpringBootTest
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

    @Test
    @DisplayName("Should return 401 status if user email was not found")
    void shouldReturn401StatusIfUserEmailIsNotFound() throws Exception {
        AuthUserDTO userDTO = new AuthUserDTO("newEmail@gmail.com", "test@gmail.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))

                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();
    }

    @Test
    @DisplayName("Should validate token and return user info")
    void shouldValidateTokenAndReturnUserInfo() throws Exception {
        User user = userSetup();
        userRepository.save(user);

        AuthUserDTO loginDTO = new AuthUserDTO("test@gmail.com", "test");
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> responseMap = objectMapper.readValue(loginResponse, Map.class);
        String accessToken = (String) responseMap.get("accessToken");

        mockMvc.perform(post("/api/v1/auth/validate")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", accessToken))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.roles").exists());
    }

    @Test
    @DisplayName("Should return 400 when access token cookie is missing")
    void shouldReturn400WhenAccessTokenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/validate")
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when access token is invalid")
    void shouldReturn401WhenAccessTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/validate")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "invalid-token"))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should refresh token and return new user info")
    void shouldRefreshTokenAndReturnNewUserInfo() throws Exception {
        User user = userSetup();
        userRepository.save(user);

        AuthUserDTO loginDTO = new AuthUserDTO("test@gmail.com", "test");
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> responseMap = objectMapper.readValue(loginResponse, Map.class);
        String refreshToken = (String) responseMap.get("refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("Should return 400 when refresh token cookie is missing")
    void shouldReturn400WhenRefreshTokenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when refresh token is invalid")
    void shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "invalid-refresh-token"))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 403 when internal service header is missing for registration")
    void shouldReturn403WhenInternalServiceHeaderIsMissingForRegistration() throws Exception {
        CreateUserDTO userDTO = new CreateUserDTO("testName", "test@gmail.com", "qwerty123");

        mockMvc.perform(post("/api/v1/auth/registration")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 when internal service header is missing for login")
    void shouldReturn403WhenInternalServiceHeaderIsMissingForLogin() throws Exception {
        AuthUserDTO userDTO = new AuthUserDTO("test@gmail.com", "test");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 when internal service header is invalid")
    void shouldReturn403WhenInternalServiceHeaderIsInvalid() throws Exception {
        CreateUserDTO userDTO = new CreateUserDTO("testName", "test@gmail.com", "qwerty123");

        mockMvc.perform(post("/api/v1/auth/registration")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", "invalid-secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 500 for malformed JSON in registration")
    void shouldReturn400ForMalformedJsonInRegistration() throws Exception {
        String malformedJson = "{\"name\": \"test\", \"email\": \"test@test.com\", \"password\":}";

        mockMvc.perform(post("/api/v1/auth/registration")
                        .contentType("application/json")
                        .content(malformedJson)
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return 415 for unsupported media type")
    void shouldReturn415ForUnsupportedMediaType() throws Exception {
        CreateUserDTO userDTO = new CreateUserDTO("testName", "test@gmail.com", "qwerty123");

        mockMvc.perform(post("/api/v1/auth/registration")
                        .contentType("text/plain")
                        .content(objectMapper.writeValueAsString(userDTO))
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET))
                .andExpect(status().isUnsupportedMediaType());
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

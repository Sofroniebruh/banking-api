package com.example.banking_api.users;

import static org.junit.jupiter.api.Assertions.*;

import com.example.banking_api.config.exceptions.TokenValidationException;
import com.example.banking_api.config.exceptions.UserNotFoundException;
import com.example.banking_api.config.exceptions.UserValidationException;
import com.example.banking_api.emails.EmailResponseDTO;
import com.example.banking_api.emails.UserEmailRabbitService;
import com.example.banking_api.jwts.ResetTokenActions;
import com.example.banking_api.redis.TokenManager;
import com.example.banking_api.users.records.DeletedUser;
import com.example.banking_api.users.records.ResetPasswordDTO;
import com.example.banking_api.users.records.UpdateUserDTO;
import com.example.banking_api.users.records.UserDTO;
import io.jsonwebtoken.ExpiredJwtException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserEmailRabbitService userEmailRabbitService;
    @Mock private ResetTokenActions resetTokenActions;
    @Mock private MeterRegistry meterRegistry;
    @Mock private TokenManager tokenManager;
    private UserService userService;
    private final String USER_ERROR_COUNTER = "user-service.user.errors.counter";
    private final String EMAIL_ERROR_COUNTER = "user-service.emails.errors.counter";
    private final String EMAIL_SUCCESS_COUNTER = "user-service.emails.counter";
    private final String DELETE_USER_COUNTER = "user-service.users.deleted.counter";
    private final String RESET_PASSWORD_LINK = "http://localhost:8082/reset-password";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        userService = new UserService(
                passwordEncoder,
                userRepository,
                meterRegistry,
                resetTokenActions,
                tokenManager,
                RESET_PASSWORD_LINK,
                userEmailRabbitService
        );
    }

    @Test
    @DisplayName("Should return user when found")
    void shouldReturnUserWhenFound() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId);

        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));

        UserDTO result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(result.id(), userId);
        assertEquals(result.email(), user.getEmail());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found")
    void shouldThrowUserNotFoundExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId);

        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));

        DeletedUser result = userService.deleteUserById(userId);

        assertNotNull(result);
        assertEquals(result.deletedUser().id(), userId);
        assertEquals(1.0, meterRegistry.counter(DELETE_USER_COUNTER).count());

        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found for deletion")
    void shouldThrowUserNotFoundExceptionWhenUserNotFoundForDeletion() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUserById(userId));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should update user email successfully")
    void shouldUpdateUserEmailSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId);
        UpdateUserDTO updateDTO = new UpdateUserDTO("test1@example.com", null);

        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDTO result = userService.updateUserById(userId, updateDTO);

        assertNotNull(result);
        assertEquals("test1@example.com", user.getEmail());

        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should update user name successfully")
    void shouldUpdateUserNameSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId);
        UpdateUserDTO updateDTO = new UpdateUserDTO(null, "qwerty");

        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDTO result = userService.updateUserById(userId, updateDTO);

        assertNotNull(result);
        assertEquals("qwerty", user.getName());

        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw UserValidationException when no valid fields to update")
    void shouldThrowUserValidationExceptionWhenNoValidFieldsToUpdate() {
        UUID userId = UUID.randomUUID();
        UpdateUserDTO updateDTO = new UpdateUserDTO(null, null);

        assertThrows(UserValidationException.class, () -> userService.updateUserById(userId, updateDTO));
    }

    @Test
    @DisplayName("Should throw UserValidationException for invalid email")
    void shouldThrowUserValidationExceptionForInvalidEmail() {
        UUID userId = UUID.randomUUID();
        UpdateUserDTO updateDTO = new UpdateUserDTO("qwerty", null);

        assertThrows(UserValidationException.class, () -> userService.updateUserById(userId, updateDTO));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());
    }

    @Test
    @DisplayName("Should throw UserValidationException for invalid name")
    void shouldThrowUserValidationExceptionForInvalidName() {
        UUID userId = UUID.randomUUID();
        UpdateUserDTO updateDTO = new UpdateUserDTO(null, "A");

        assertThrows(UserValidationException.class, () -> userService.updateUserById(userId, updateDTO));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found for update")
    void shouldThrowUserNotFoundExceptionWhenUserNotFoundForUpdate() {
        UUID userId = UUID.randomUUID();
        UpdateUserDTO updateDTO = new UpdateUserDTO("test1@example.com", "qwerty");

        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUserById(userId, updateDTO));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());
    }

    @Test
    @DisplayName("Should send email successfully")
    void shouldSendEmailSuccessfully() {
        String email = "test@example.com";
        User user = createTestUser(UUID.randomUUID());
        user.setEmail(email);
        String token = "qwerty";
        EmailResponseDTO successResponse = new EmailResponseDTO();
        successResponse.setSuccess(true);
        successResponse.setData("Email sent successfully!");

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(resetTokenActions.generatePasswordResetToken(eq(email), any(Duration.class))).thenReturn(token);
        when(userEmailRabbitService.sendEmailAndReceive(anyString(), eq(email))).thenReturn(successResponse);

        ResponseEntity<?> result = userService.requestEmailSending(email);

        assertEquals(HttpStatus.OK ,result.getStatusCode());
        assertEquals(1.0, meterRegistry.counter(EMAIL_SUCCESS_COUNTER).count());
    }

    @Test
    @DisplayName("Should handle failed email sending")
    void shouldHandleFailedEmailSending() {
        String email = "test@example.com";
        User user = createTestUser(UUID.randomUUID());
        user.setEmail(email);
        String token = "test-token";
        EmailResponseDTO failureResponse = new EmailResponseDTO();
        failureResponse.setSuccess(false);
        failureResponse.setStatusCode(500);
        failureResponse.setError("Email service unavailable");

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(resetTokenActions.generatePasswordResetToken(eq(email), any(Duration.class))).thenReturn(token);
        when(userEmailRabbitService.sendEmailAndReceive(anyString(), eq(email))).thenReturn(failureResponse);

        ResponseEntity<?> result = userService.requestEmailSending(email);

        assertEquals(500, result.getStatusCode().value());
        assertEquals(1.0, meterRegistry.counter(EMAIL_ERROR_COUNTER).count());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when email not found")
    void shouldThrowUserNotFoundExceptionWhenEmailNotFound() {
        String email = "nonexistent@example.com";

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.requestEmailSending(email));
    }

    @Test
    @DisplayName("Should update password successfully")
    void shouldUpdatePasswordSuccessfully() {
        String email = "test@example.com";
        String token = "qwerty";
        String newPassword = "qwerty123!";
        String encodedPassword = "encoded";

        User user = createTestUser(UUID.randomUUID());
        user.setEmail(email);
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, newPassword);

        when(resetTokenActions.isTokenValid(token, email)).thenReturn(true);
        when(resetTokenActions.getEmailFromToken(token)).thenReturn(email);
        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDTO result = userService.updatePassword(resetDTO, token);

        assertNotNull(result);
        assertEquals(encodedPassword, user.getPassword());

        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw TokenValidationException for invalid token")
    void shouldThrowTokenValidationExceptionForInvalidToken() {
        String email = "test@example.com";
        String token = "qwerty";
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, "qwerty123");

        when(resetTokenActions.isTokenValid(token, email)).thenReturn(false);

        assertThrows(TokenValidationException.class, () -> userService.updatePassword(resetDTO, token));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());
    }

    @Test
    @DisplayName("Should handle expired JWT token")
    void shouldHandleExpiredJwtToken() {
        String email = "test@example.com";
        String token = "qwerty";
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, "qwerty123");

        when(resetTokenActions.isTokenValid(token, email)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        assertThrows(ExpiredJwtException.class, () -> userService.updatePassword(resetDTO, token));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by email from token")
    void shouldThrowUserNotFoundExceptionWhenUserNotFoundByEmailFromToken() {
        String email = "test@example.com";
        String token = "qwerty";
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, "qwerty123");

        when(resetTokenActions.isTokenValid(token, email)).thenReturn(true);
        when(resetTokenActions.getEmailFromToken(token)).thenReturn(email);
        when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updatePassword(resetDTO, token));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());
    }

    private User createTestUser(UUID id) {
        return User.builder()
            .id(id)
            .name("qwerty")
            .email("test@example.com")
            .password("qwerty")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
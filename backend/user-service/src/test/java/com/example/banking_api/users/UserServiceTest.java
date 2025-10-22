package com.example.banking_api.users;

import static org.junit.jupiter.api.Assertions.*;

import com.example.banking_api.config.exceptions.TokenValidationException;
import com.example.banking_api.config.exceptions.UserNotFoundException;
import com.example.banking_api.config.exceptions.UserValidationException;
import com.example.banking_api.emails.EmailResponseDTO;
import com.example.banking_api.emails.UserEmailRabbitService;
import com.example.banking_api.jwts.ResetTokenActions;
import com.example.banking_api.redis.TokenManager;
import com.example.banking_api.redis.exceptions.RedisOperationException;
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
import java.util.concurrent.TimeUnit;

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
    @DisplayName("When requesting email sending successfully store token in the db")
    void shouldRequestEmailSendingSuccessfullyStoreTokenInDb() {
        String email = "test@example.com";
        String token = "qwerty";
        String link = "http://localhost:8082/reset-password?token=" + token;
        User user = createTestUser(UUID.randomUUID());
        EmailResponseDTO emailResponseDTO = new EmailResponseDTO();

        emailResponseDTO.setSuccess(true);
        emailResponseDTO.setStatusCode(200);
        emailResponseDTO.setData("Success!");

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(resetTokenActions.generatePasswordResetToken(user.getEmail(), Duration.ofMinutes(15))).thenReturn(token);
        when(userEmailRabbitService.sendEmailAndReceive(link, email)).thenReturn(emailResponseDTO);

        userService.requestEmailSending(email);

        assertEquals(1.0, meterRegistry.counter(EMAIL_SUCCESS_COUNTER).count());

        verify(tokenManager, times(1)).storeActiveToken(email, token, 15, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("Should throw an exception if the redis insertion failed")
    void shouldThrowAnExceptionIfTheRedisInsertionFailed() {
        String email = "test@example.com";
        String token = "qwerty";
        User user = createTestUser(UUID.randomUUID());

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(resetTokenActions.generatePasswordResetToken(user.getEmail(), Duration.ofMinutes(15))).thenReturn(token);
        doThrow(RedisOperationException.class).when(tokenManager).storeActiveToken(email, token, 15, TimeUnit.MINUTES);

        assertThrows(RedisOperationException.class, () -> userService.requestEmailSending(email));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());

        verify(userEmailRabbitService, never()).sendEmailAndReceive(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw TokenValidationException if redis did not find any tokens associated")
    void shouldThrowAnExceptionIfRedisDidNotFindAnyTokens() {
        String email = "test@example.com";
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, "qwerty123");

        when(tokenManager.getActiveToken(email)).thenReturn(null);

        assertThrows(TokenValidationException.class, () -> userService.updatePassword(resetDTO, email));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());

        verify(userRepository, never()).findUserByEmail(email);
    }

    @Test
    @DisplayName("Should throw a TokenValidationException when stored unused token for reset differs from the one received for reset")
    void shouldThrowAnExceptionIfStoredUnusedTokenForReset() {
        String email = "test@example.com";
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, "qwerty123");
        String token1 = "qwerty123";
        String token2 = "qwerty123!";

        when(tokenManager.getActiveToken(email)).thenReturn(token1);

        assertThrows(TokenValidationException.class, () -> userService.updatePassword(resetDTO, token2));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());

        verify(userRepository, never()).findUserByEmail(email);
    }

    @Test
    @DisplayName("Should throw a TokenValidationException if the token was already used")
    void shouldThrowAnExceptionIfTheTokenWasAlreadyUsed() {
        String email = "test@example.com";
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, "qwerty123");
        String token = "qwerty123";

        when(tokenManager.getActiveToken(email)).thenReturn(token);
        when(tokenManager.isTokenUsed(email)).thenReturn(true);

        assertThrows(TokenValidationException.class, () -> userService.updatePassword(resetDTO, token));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());

        verify(userRepository, never()).findUserByEmail(email);
    }

    @Test
    @DisplayName("Should throw a ExpiredJwtException if the token was expired")
    void shouldThrowAnExceptionIfTheTokenWasInvalid() {
        String email = "test@example.com";
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, "qwerty123");
        String token = "qwerty123";

        when(tokenManager.getActiveToken(email)).thenReturn(token);
        when(tokenManager.isTokenUsed(email)).thenReturn(false);
        when(resetTokenActions.isTokenValid(token, email)).thenThrow(ExpiredJwtException.class);

        assertThrows(ExpiredJwtException.class, () -> userService.updatePassword(resetDTO, token));
        assertEquals(1.0, meterRegistry.counter(USER_ERROR_COUNTER).count());

        verify(userRepository, never()).findUserByEmail(email);
    }

    @Test
    @DisplayName("Should successfully update the password")
    void shouldSuccessfullyUpdatePassword() {
        String email = "test@example.com";
        User user = createTestUser(UUID.randomUUID());
        ResetPasswordDTO resetDTO = new ResetPasswordDTO(email, "qwerty123");
        String token = "qwerty123";

        when(tokenManager.getActiveToken(email)).thenReturn(token);
        when(tokenManager.isTokenUsed(email)).thenReturn(false);
        when(resetTokenActions.isTokenValid(token, email)).thenReturn(true);
        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(resetDTO.password())).thenReturn("encoded");
        when(userRepository.save(user)).thenReturn(user);

        assertDoesNotThrow(() -> userService.updatePassword(resetDTO, token));
        assertEquals(0.0, meterRegistry.counter(USER_ERROR_COUNTER).count());
        assertEquals("encoded", user.getPassword());

        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenManager, times(1)).markTokenAsUsed(email, token, 15, TimeUnit.MINUTES);
    }

    private User createTestUser(UUID id)
    {
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
package com.example.banking_api.users;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserEmailRabbitService userEmailRabbitService;
    private final ResetTokenActions resetTokenActions;
    private final String USER_SERVICE_RESET_PASSWORD_LINK;
    private final TokenManager tokenManager;
    //My metrics
    private final Counter userErrorCounter;
    private final Counter emailCounter;
    private final Counter failedEmailCounter;
    private final Counter deletedUserCounter;

    public UserService(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            MeterRegistry meterRegistry,
            ResetTokenActions resetTokenActions,
            TokenManager tokenManager,
            @Value("${USER_SERVICE_RESET_PASSWORD_LINK}") String USER_SERVICE_RESET_PASSWORD_LINK,
            UserEmailRabbitService userEmailRabbitService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userEmailRabbitService = userEmailRabbitService;
        this.resetTokenActions = resetTokenActions;
        this.USER_SERVICE_RESET_PASSWORD_LINK = USER_SERVICE_RESET_PASSWORD_LINK;
        this.tokenManager = tokenManager;
        this.userErrorCounter = Counter.builder("user-service.user.errors.counter")
                .description("Error counter for user service")
                .register(meterRegistry);
        this.emailCounter = Counter.builder("user-service.emails.counter")
                .description("Counter for emails sent to reset password")
                .register(meterRegistry);
        this.failedEmailCounter = Counter.builder("user-service.emails.errors.counter")
                .description("Counter for errors during sending emails to reset password")
                .register(meterRegistry);
        this.deletedUserCounter = Counter.builder("user-service.users.deleted.counter")
                .description("Counter for deleted users")
                .register(meterRegistry);
    }

    public UserDTO getUserById(UUID id) {
        try {
            Optional<User> optionalUser = userRepository.findUserById(id);

            if (optionalUser.isEmpty()) {
                throw new UserNotFoundException("User not found");
            }

            User user = optionalUser.get();

            return UserDTO.fromEntity(user);
        } catch (UserNotFoundException ex) {
            logger.error(ex.getMessage());
            userErrorCounter.increment();

            throw ex;
        }
    }

    @Transactional
    public DeletedUser deleteUserById(UUID id) {
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> {
                    String error = String.format("User with id %s not found", id);

                    logger.error(error);
                    userErrorCounter.increment();

                    return new UserNotFoundException(error);
                });

        userRepository.delete(user);

        UserDTO deletedUser = UserDTO.fromEntity(user);

        logger.info("User with id {} deleted successfully", id);
        deletedUserCounter.increment();

        return DeletedUser.fromEntity(deletedUser);
    }

    @Transactional
    public UserDTO updateUserById(UUID id, UpdateUserDTO updateUserDTO) {
        validateUpdateRequest(updateUserDTO);

        User user = userRepository.findUserById(id)
                .orElseThrow(() -> {
                    userErrorCounter.increment();
                    logger.error("User not found with id: {}", id);

                    return new UserNotFoundException(String.format("User with id %s not found", id));
                });

        boolean updated = false;

        if (hasValue(updateUserDTO.email())) {
            user.setEmail(updateUserDTO.email().trim().toLowerCase());
            updated = true;
        }

        if (hasValue(updateUserDTO.name())) {
            user.setName(updateUserDTO.name().trim());
            updated = true;
        }

        if (!updated) {
            throw new UserValidationException(Map.of("general", "No valid fields to update"));
        }

        User savedUser = userRepository.save(user);
        logger.info("Successfully updated user: {}", id);

        return UserDTO.fromEntity(savedUser);
    }

    public ResponseEntity<?> requestEmailSending(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(String.format("User with email %s not found", email)));
        String token = resetTokenActions.generatePasswordResetToken(user.getEmail(), Duration.ofMinutes(15));
        String link = String.format("%s?token=%s", USER_SERVICE_RESET_PASSWORD_LINK ,token);

        try {
            tokenManager.storeActiveToken(email, token, 15, TimeUnit.MINUTES);
        } catch (RedisOperationException ex) {
            logger.error("Failed to store token for email: {}", email, ex);
            userErrorCounter.increment();
            throw ex;
        }

        EmailResponseDTO response = userEmailRabbitService.sendEmailAndReceive(link, user.getEmail());

        if (response.isSuccess()) {
            emailCounter.increment();

            String message = response.getData() != null ? response.getData() : "Email sent successfully!";
            return ResponseEntity.ok().body(Map.of("message", message));
        }

        failedEmailCounter.increment();

        return ResponseEntity.status(response.getStatusCode()).body(response.getError());
    }

    @Transactional
    public UserDTO updatePassword(ResetPasswordDTO updateUserDTO, String token) {
        try {
            String email = updateUserDTO.email();
            String storedToken = tokenManager.getActiveToken(email);

            if (storedToken == null) {
                throw new TokenValidationException("Token is invalid or expired");
            }
            if (!storedToken.equals(token)) {
                throw new TokenValidationException("Token is invalid");
            }
            if (tokenManager.isTokenUsed(email)) {
                throw new TokenValidationException("Token has already been used");
            }
            if (!resetTokenActions.isTokenValid(token, email)) {
                throw new TokenValidationException("Token is invalid");
            }

            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(String.format("User with email %s not found", email)));

            user.setPassword(passwordEncoder.encode(updateUserDTO.password()));
            User savedUser = userRepository.save(user);

            tokenManager.markTokenAsUsed(email, token, 15, TimeUnit.MINUTES);

            return UserDTO.fromEntity(savedUser);
        } catch (ExpiredJwtException | UserNotFoundException | TokenValidationException ex) {
            logger.error(ex.getMessage());
            userErrorCounter.increment();

            throw ex;
        } catch (Exception ex) {
            logger.error(ex.getMessage());

            throw new RuntimeException("An error occurred while updating password");
        }
    }

    private boolean hasValue(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private void validateUpdateRequest(UpdateUserDTO dto) {
        UserValidationException exception = new UserValidationException();

        boolean hasEmail = hasValue(dto.email());
        boolean hasName = hasValue(dto.name());

        if (!hasEmail && !hasName) {
            exception.addFieldError("general", "At least one field is required");

            throw exception;
        }

        if (hasEmail) validateEmail(dto.email(), exception);
        if (hasName) validateName(dto.name(), exception);

        if (!exception.getFieldErrors().isEmpty()) {
            userErrorCounter.increment();
            logger.warn("Validation failed: {}", exception.getFieldErrors());

            throw exception;
        }
    }

    private void validateEmail(String email, UserValidationException exception) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        if (email == null || email.trim().isEmpty()) {
            exception.addFieldError("email", "Email cannot be empty");

            return;
        }

        String trimmedEmail = email.trim();

        if (trimmedEmail.length() < 5) {
            exception.addFieldError("email", "Email is too short");
        } else if (trimmedEmail.length() > 50) {
            exception.addFieldError("email", "Email is too long (max 50 characters)");
        }

        if (!trimmedEmail.matches(emailRegex)) {
            exception.addFieldError("email", "Invalid email format");
        }

        if (trimmedEmail.contains("..")) {
            exception.addFieldError("email", "Email cannot contain consecutive dots");
        }

        if (trimmedEmail.startsWith(".") || trimmedEmail.endsWith(".")) {
            exception.addFieldError("email", "Email cannot start or end with a dot");
        }
    }

    private void validateName(String name, UserValidationException exception) {
        if (name == null || name.trim().isEmpty()) {
            exception.addFieldError("name", "Name cannot be empty");

            return;
        }

        String trimmedName = name.trim();

        if (trimmedName.length() < 2) {
            exception.addFieldError("name", "Name must be at least 2 characters long");
        } else if (trimmedName.length() > 50) {
            exception.addFieldError("name", "Name is too long (max 50 characters)");
        }

        String nameRegex = "^[a-zA-Z'-]+$";
        if (!trimmedName.matches(nameRegex)) {
            exception.addFieldError("name", "Name can only contain letters, hyphens, and apostrophes");
        }
    }
}

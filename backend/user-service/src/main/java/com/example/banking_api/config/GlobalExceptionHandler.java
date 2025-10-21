package com.example.banking_api.config;

import com.example.banking_api.config.exceptions.EmailServiceException;
import com.example.banking_api.config.exceptions.TokenValidationException;
import com.example.banking_api.config.exceptions.UserValidationException;
import com.example.banking_api.config.responses.ErrorResponse;
import com.example.banking_api.config.exceptions.UserNotFoundException;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFoundException(UserNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.from(ex.getMessage(), Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UserValidationException.class)
    public ResponseEntity<?> handleUserValidationException(UserValidationException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("message", ex.getMessage());
        errors.put("errors", ex.getFieldErrors());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(EmailServiceException.class)
    public ResponseEntity<?> handleEmailServiceException(EmailServiceException ex) {
        ErrorResponse errorResponse = ErrorResponse.from(ex.getMessage(), Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpiredJwtException() {
        ErrorResponse errorResponse = ErrorResponse.from("Your token has expired", Instant.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<?> handleTokenValidationException(TokenValidationException ex) {
        ErrorResponse errorResponse = ErrorResponse.from(ex.getMessage(), Instant.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationError(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        ErrorResponse errorResponse = ErrorResponse.from(ex.getMessage(), Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

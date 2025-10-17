package org.example.authservice.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultHeader;
import org.example.authservice.config.ErrorResponse;
import org.example.authservice.config.GlobalExceptionHandler;
import org.example.authservice.users.exceptions.InvalidTokenException;
import org.example.authservice.users.exceptions.TokenGeneratorException;
import org.example.authservice.users.exceptions.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {
    @Mock private GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should return proper status code and a message for UserException")
    void handleUserException() {
        String errorMessage = "User exception occurred";
        UserException ex = new UserException(errorMessage);
        ResponseEntity<?> response = globalExceptionHandler.handleUserException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(errorMessage, errorResponse.getError());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Should return proper status code and a message for ExpiredJwtException")
    void handleExpiredJwtException() {
        String errorMessage = "Expired jwt exception occurred";
        Header header = new DefaultHeader();
        Claims claims = new DefaultClaims();
        ExpiredJwtException ex = new ExpiredJwtException(header, claims, errorMessage);
        ResponseEntity<?> response = globalExceptionHandler.handleExpiredJwtException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(errorMessage, errorResponse.getError());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Should return proper status code and a message for TokenGeneratorException")
    void handleTokenGeneratorException() {
        String errorMessage = "Token generator exception occurred";
        TokenGeneratorException ex = new TokenGeneratorException(errorMessage);
        ResponseEntity<?> response = globalExceptionHandler.handleTokenException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(errorMessage, errorResponse.getError());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Should return proper status code and a message for UsernameNotFoundException")
    void handleUsernameNotFoundException() {
        ResponseEntity<?> response = globalExceptionHandler.handleUsernameNotFoundException();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("Invalid email or password", errorResponse.getError());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Should return proper status code and a message for InvalidTokenException")
    void handleInvalidTokenException() {
        String errorMessage = "Invalid token";
        InvalidTokenException ex = new InvalidTokenException(errorMessage);
        ResponseEntity<?> response = globalExceptionHandler.handleInvalidTokenException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(errorMessage, errorResponse.getError());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Should return proper status code and a message for BadCredentialsException")
    void handleBadCredentialsException() {
        ResponseEntity<?> response = globalExceptionHandler.handleBadCredentialsException();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("Invalid email or password", errorResponse.getError());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Should return proper status code and a message for RuntimeException")
    void handleRuntimeException() {
        String errorMessage = "Runtime exception occurred";
        RuntimeException ex = new RuntimeException(errorMessage);
        ResponseEntity<?> response = globalExceptionHandler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(errorMessage, errorResponse.getError());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Should return proper status code and a message for MethodArgumentNotValidException")
    void handleMethodArgumentNotValidException() {
        MethodParameter mockMethodParameter = mock(MethodParameter.class);
        BindingResult mockBindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError("user", "email", "must not be empty");
        when(mockBindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(mockBindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        when(mockBindingResult.getErrorCount()).thenReturn(1);
        when(mockBindingResult.hasErrors()).thenReturn(true);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, mockBindingResult);
        ResponseEntity<?> response = globalExceptionHandler.handleValidationError(ex);

        assertTrue(ex.getBindingResult().hasErrors());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1, ex.getBindingResult().getErrorCount());
        assertEquals("email", ex.getBindingResult().getFieldErrors().get(0).getField());
        assertEquals("must not be empty", ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
    }
}

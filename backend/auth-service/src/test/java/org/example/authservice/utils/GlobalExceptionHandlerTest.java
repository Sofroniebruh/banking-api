package org.example.authservice.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultHeader;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        String errorBody = "User exception occurred";
        UserException ex = new UserException(errorBody);
        ResponseEntity<?> response = globalExceptionHandler.handleUserException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(errorBody, response.getBody());
    }

    @Test
    @DisplayName("Should return proper status code and a message for ExpiredJwtException")
    void handleExpiredJwtException() {
        String errorBody = "Expired jwt exception occurred";
        Header header = new DefaultHeader();
        Claims claims = new DefaultClaims();
        ExpiredJwtException ex = new ExpiredJwtException(header, claims, errorBody);
        ResponseEntity<?> response = globalExceptionHandler.handleExpiredJwtException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(errorBody, response.getBody());
    }

    @Test
    @DisplayName("Should return proper status code and a message for TokenGeneratorException")
    void handleTokenGeneratorException() {
        String errorBody = "Token generator exception occurred";
        TokenGeneratorException ex = new TokenGeneratorException(errorBody);
        ResponseEntity<?> response = globalExceptionHandler.handleTokenException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(errorBody, response.getBody());
    }

    @Test
    @DisplayName("Should return proper status code and a message for UsernameNotFoundException")
    void handleUsernameNotFoundException() {
        UsernameNotFoundException ex = new UsernameNotFoundException("");
        ResponseEntity<?> response = globalExceptionHandler.handleUsernameNotFoundException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password", response.getBody());
    }

    @Test
    @DisplayName("Should return proper status code and a message for InvalidTokenException")
    void handleInvalidTokenException() {
        String errorBody = "Invalid token";
        InvalidTokenException ex = new InvalidTokenException(errorBody);
        ResponseEntity<?> response = globalExceptionHandler.handleInvalidTokenException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(errorBody, response.getBody());
    }

    @Test
    @DisplayName("Should return proper status code and a message for BadCredentialsException")
    void handleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("");
        ResponseEntity<?> response = globalExceptionHandler.handleBadCredentialsException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password", response.getBody());
    }

    @Test
    @DisplayName("Should return proper status code and a message for RuntimeException")
    void handleRuntimeException() {
        String errorBody = "Runtime exception occurred";
        RuntimeException ex = new RuntimeException(errorBody);
        ResponseEntity<?> response = globalExceptionHandler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(errorBody, response.getBody());
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

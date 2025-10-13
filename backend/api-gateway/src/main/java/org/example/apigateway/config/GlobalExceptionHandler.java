package org.example.apigateway.config;

import org.example.apigateway.config.exceptions.AuthenticationServiceUnavailable;
import org.example.apigateway.config.exceptions.AuthServiceClientException;
import org.example.apigateway.validation.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthServiceClientException.class)
    public ResponseEntity<String> handleAuthServiceClientException(AuthServiceClientException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBody());
    }

    @ExceptionHandler(AuthenticationServiceUnavailable.class)
    public ResponseEntity<String> handleAuthenticationServiceUnavailable(AuthenticationServiceUnavailable e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"" + e.getMessage() + "\"}");
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> handleHttpClientErrorException(HttpClientErrorException e) {
        logger.warn(e.getMessage());
        logger.warn(e.getResponseBodyAsString());
        logger.warn(String.valueOf(e));
        return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString().isEmpty() ? "{\"error\":\"Forbidden request\"} " : e.getResponseBodyAsString());
    }
}

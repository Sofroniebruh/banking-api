package org.example.apigateway.config;

import org.example.apigateway.config.exceptions.AuthenticationServiceUnavailable;
import org.example.apigateway.config.exceptions.AuthServiceClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthServiceClientException.class)
    public ResponseEntity<String> handleAuthServiceClientException(AuthServiceClientException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBody());
    }

    @ExceptionHandler(AuthenticationServiceUnavailable.class)
    public ResponseEntity<String> handleAuthenticationServiceUnavailable(AuthenticationServiceUnavailable e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"" + e.getMessage() + "\"}");
    }
}

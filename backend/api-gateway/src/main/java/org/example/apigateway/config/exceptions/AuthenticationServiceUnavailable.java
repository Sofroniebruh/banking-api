package org.example.apigateway.config.exceptions;

public class AuthenticationServiceUnavailable extends RuntimeException {
    public AuthenticationServiceUnavailable(String message) {
        super(message);
    }
}

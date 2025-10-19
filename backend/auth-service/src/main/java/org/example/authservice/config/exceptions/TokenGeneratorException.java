package org.example.authservice.config.exceptions;

public class TokenGeneratorException extends RuntimeException {
    public TokenGeneratorException(String message) {
        super(message);
    }
}

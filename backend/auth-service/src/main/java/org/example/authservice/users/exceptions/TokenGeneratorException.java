package org.example.authservice.users.exceptions;

public class TokenGeneratorException extends RuntimeException {
    public TokenGeneratorException(String message) {
        super(message);
    }
}

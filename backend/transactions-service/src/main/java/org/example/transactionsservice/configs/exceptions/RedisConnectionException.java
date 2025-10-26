package org.example.transactionsservice.configs.exceptions;

public class RedisConnectionException extends RuntimeException {
    public RedisConnectionException(String message) {
        super(message);
    }
}

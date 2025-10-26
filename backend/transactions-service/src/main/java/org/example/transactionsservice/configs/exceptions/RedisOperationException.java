package org.example.transactionsservice.configs.exceptions;

public class RedisOperationException extends RuntimeException {
    public RedisOperationException(String message) {
        super(message);
    }
}

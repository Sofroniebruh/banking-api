package com.example.banking_api.redis.exceptions;

public class RedisConnectionException extends RedisOperationException {
    public RedisConnectionException(String message) {
        super(message);
    }

    public RedisConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
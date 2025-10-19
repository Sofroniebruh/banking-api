package com.example.banking_api.config.responses;

import java.time.Instant;

public record ErrorResponse(
        String error,
        Instant timestamp
) {
    public static ErrorResponse from(String error, Instant timestamp) {
        return new ErrorResponse(error, timestamp);
    }
}

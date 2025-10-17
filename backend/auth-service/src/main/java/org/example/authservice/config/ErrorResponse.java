package org.example.authservice.config;

import lombok.Data;

import java.time.Instant;

@Data
public class ErrorResponse {
    private String error;
    private Instant timestamp;

    public ErrorResponse(String error) {
        this.error = error;
        this.timestamp = Instant.now();
    }
}

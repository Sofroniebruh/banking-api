package org.example.apigateway.config.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class AuthServiceClientException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final String responseBody;

    public AuthServiceClientException(String message, HttpStatusCode statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
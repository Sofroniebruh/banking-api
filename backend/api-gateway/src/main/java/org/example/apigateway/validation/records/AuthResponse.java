package org.example.apigateway.validation.records;

import org.example.apigateway.users.User;

public record AuthResponse(
        User user, String token
) {
}

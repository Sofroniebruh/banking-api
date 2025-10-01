package org.example.apigateway.validation.records;

import org.example.apigateway.users.Role;

import java.util.List;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        String email,
        List<Role> roles,
        String accessToken,
        String refreshToken
) {

}

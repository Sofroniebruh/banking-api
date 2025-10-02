package org.example.apigateway.validation.records;

import org.example.apigateway.users.Role;

import java.util.List;
import java.util.UUID;

public record UserTokenInfoDTO(
        String email,
        UUID id,
        List<Role> roles
) {
}
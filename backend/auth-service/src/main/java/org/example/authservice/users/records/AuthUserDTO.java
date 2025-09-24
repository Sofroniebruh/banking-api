package org.example.authservice.users.records;

import jakarta.validation.constraints.NotBlank;

public record AuthUserDTO(
        @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "Password is required") String password
) {
}

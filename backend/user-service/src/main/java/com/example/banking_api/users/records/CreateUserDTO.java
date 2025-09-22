package com.example.banking_api.users.records;

import jakarta.validation.constraints.NotNull;

public record CreateUserDTO(
        String username,
        @NotNull(message = "Email is required") String email,
        @NotNull(message = "Password is required") String password
) {
}

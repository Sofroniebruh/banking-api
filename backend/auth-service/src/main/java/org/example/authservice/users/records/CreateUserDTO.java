package org.example.authservice.users.records;

import jakarta.validation.constraints.NotBlank;

public record CreateUserDTO(
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Email is required") String email,
    @NotBlank(message = "Password is required") String password
) {
}

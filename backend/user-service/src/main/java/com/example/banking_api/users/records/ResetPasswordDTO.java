package com.example.banking_api.users.records;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordDTO(
        @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "Password is required") String password
) {
}

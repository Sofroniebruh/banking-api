package com.example.banking_api.users.records;

import jakarta.validation.constraints.NotBlank;

public record UserEmailDTO(
        @NotBlank(message = "Email is required") String email
) {

}

package org.example.accountservice.accounts.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccountDTO(
        @NotNull(message = "User id is required") UUID userId,
        @NotBlank(message = "Currency is required") String currency
) {

}

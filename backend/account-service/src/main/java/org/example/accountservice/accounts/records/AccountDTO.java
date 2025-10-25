package org.example.accountservice.accounts.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountDTO(
        @NotNull(message = "User id is required") String userId,
        @NotBlank(message = "Currency is required") String currency
) {

}

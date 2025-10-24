package org.example.accountservice.accounts.records;

import jakarta.validation.constraints.NotBlank;

public record UpdateAccountDTO(
        @NotBlank(message = "Currency is required") String currency
) {

}

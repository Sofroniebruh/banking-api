package org.example.accountservice.accounts.records;

import jakarta.validation.constraints.NotNull;
import org.example.accountservice.accounts.AccountCurrency;

public record UpdateAccountDTO(
        @NotNull(message = "Currency is required") AccountCurrency currency
) {

}

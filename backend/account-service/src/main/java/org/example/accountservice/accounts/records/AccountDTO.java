package org.example.accountservice.accounts.records;

import jakarta.validation.constraints.NotNull;
import org.example.accountservice.accounts.AccountCurrency;

public record AccountDTO(
        @NotNull(message = "Currency is required") AccountCurrency currency
) {

}

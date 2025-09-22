package com.example.banking_api.accounts.records;

import com.example.banking_api.accounts.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountDTO(
        @NotNull(message = "User id is required") UUID id,
        BigDecimal initialDeposit,
        @NotBlank(message = "Account type is required") AccountType accountType
) {

}

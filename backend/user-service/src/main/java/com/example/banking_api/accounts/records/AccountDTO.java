package com.example.banking_api.accounts.records;

import com.example.banking_api.accounts.Account;
import com.example.banking_api.accounts.AccountType;

import java.util.UUID;

public record AccountDTO(
        UUID id,
        AccountType accountType,
        UUID userId
) {
    public static AccountDTO fromEntity(Account account) {
        return new AccountDTO(account.getId(), account.getAccountType(), account.getUser().getId());
    }
}

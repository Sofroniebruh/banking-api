package org.example.accountservice.accounts.records;

import java.time.LocalDateTime;
import java.util.UUID;
import org.example.accountservice.accounts.AccountCurrency;

public record AccountReturnDTO(
        UUID accountId,
        String userId,
        double balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        AccountCurrency currency
) {
    public static AccountReturnDTO from(
            UUID accountId,
            String userId,
            double balance,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            AccountCurrency currency) {
        return new AccountReturnDTO(accountId, userId, balance, createdAt, updatedAt, currency);
    }
}
package org.example.accountservice.accounts.records;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountReturnDTO(
        UUID accountId,
        String userId,
        double balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String currency
) {
    public static AccountReturnDTO from(
            UUID accountId,
            String userId,
            double balance,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String currency) {
        return new AccountReturnDTO(accountId, userId, balance, createdAt, updatedAt, currency);
    }
}
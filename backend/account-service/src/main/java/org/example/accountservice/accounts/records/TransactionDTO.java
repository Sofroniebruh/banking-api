package org.example.accountservice.accounts.records;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        String status,
        LocalDateTime createdAt
) {
}

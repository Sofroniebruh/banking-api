package org.example.accountservice.accounts.records;

import java.sql.Timestamp;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        String status,
        Timestamp createdAt
) {
}

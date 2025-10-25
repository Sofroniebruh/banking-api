package org.example.transactionsservice.transactions.records;

import org.example.transactionsservice.transactions.Transaction;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        String status,
        LocalDateTime createdAt
) {
    public static TransactionDTO from(Transaction transaction) {
        return new TransactionDTO(transaction.getId(), transaction.getStatus().name(), transaction.getCreatedAt());
    }
}

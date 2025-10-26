package org.example.transactionsservice.transactions.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTransactionDTO(
        @NotNull(message = "Account id is required") UUID id,
        @NotNull(message = "Amount is required") double amount,
        @NotBlank(message = "Transaction status is required") String status,
        @NotBlank(message = "Transaction description is required") String description) {

}

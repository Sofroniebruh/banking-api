package org.example.transactionsservice.transactions.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record CreateTransactionDTO(
        @NotBlank(message = "Account id is required") String id,
        @NotNull(message = "Amount is required") double amount,
        @NotBlank(message = "Transaction status is required") String status,
        @NotBlank(message = "Transaction description is required") String description) {

}

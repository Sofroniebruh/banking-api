package org.example.transactionsservice.transactions;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.transactionsservice.transactions.records.CreateTransactionDTO;
import org.example.transactionsservice.transactions.records.PaginatedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<PaginatedResponse<Transaction>> getAllTransactionsForAccount(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PaginatedResponse<Transaction> paginatedTransactions = transactionService.getPaginatedTransactionsForAccount(pageable, id);

        return ResponseEntity.ok().body(paginatedTransactions);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Transaction>> createTransaction(@Valid @RequestBody CreateTransactionDTO transaction) {
        return transactionService.saveTransaction(transaction)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}

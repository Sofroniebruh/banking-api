package org.example.transactionsservice.transactions;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.transactionsservice.transactions.records.CreateTransactionDTO;
import org.example.transactionsservice.transactions.records.PaginatedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final Counter transactionErrorCounter;

    public TransactionService(
            TransactionRepository transactionRepository,
            MeterRegistry registry) {
        this.transactionRepository = transactionRepository;
        this.transactionErrorCounter = Counter.builder("errors.transactions")
                .description("Errors in transactions service")
                .register(registry);
    }

    public List<Transaction> getTransactionsByAccountId(UUID accountId) {
        Optional<List<Transaction>> transactions = transactionRepository.findAllByAccountId(accountId);

        return transactions.orElseGet(ArrayList::new);
    }

    public PaginatedResponse<Transaction> getPaginatedTransactionsForAccount(Pageable pageable, UUID accountId) {
        Page<Transaction> transactionsPage = transactionRepository.findAllByAccountId(accountId, pageable);

        return new PaginatedResponse<>(
                transactionsPage.getContent(),
                transactionsPage.getNumber(),
                transactionsPage.getSize(),
                transactionsPage.getTotalElements(),
                transactionsPage.getTotalPages(),
                transactionsPage.isLast()
        );
    }

    public Transaction saveTransaction(CreateTransactionDTO transactionDto) {
        try {
            Transaction transaction = new Transaction();

            transaction.setAmount(BigDecimal.valueOf(transactionDto.amount()));
            transaction.setStatus(TransactionStatus.valueOf(transactionDto.status().toUpperCase()));
            transaction.setDescription(transactionDto.description());
            transaction.setAccountId(transactionDto.id());

            return transactionRepository.save(transaction);
        } catch (IllegalArgumentException e) {
            logger.error("Error while passing string to enum: {}", e.getMessage());
            transactionErrorCounter.increment();

            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            transactionErrorCounter.increment();

            throw e;
        }
    }
}

package org.example.transactionsservice.transactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.transactionsservice.configs.RabbitConfig;
import org.example.transactionsservice.configs.RedisConfig;
import org.example.transactionsservice.configs.exceptions.BankAccountNotFoundException;
import org.example.transactionsservice.redis.RedisService;
import org.example.transactionsservice.transactions.enums.TransactionCurrency;
import org.example.transactionsservice.transactions.enums.TransactionStatus;
import org.example.transactionsservice.transactions.records.CreateTransactionDTO;
import org.example.transactionsservice.transactions.records.PaginatedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final RedisService redisService;
    private final RabbitTemplate rabbitTemplate;
    private final Counter transactionErrorCounter;
    private final Counter rabbitCommunicationErrorCounter;

    public TransactionService(
            TransactionRepository transactionRepository,
            RedisService redisService,
            MeterRegistry registry,
            RabbitTemplate rabbitTemplate) {
        this.transactionRepository = transactionRepository;
        this.redisService = redisService;
        this.transactionErrorCounter = Counter.builder("errors.transactions")
                .description("Errors in transactions service")
                .register(registry);
        this.rabbitCommunicationErrorCounter = Counter.builder("errors.rabbit.communication")
                .description("Errors in rabbit communication")
                .register(registry);
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<Transaction> getTransactionsByAccountId(UUID accountId) {
        Optional<List<Transaction>> transactions = transactionRepository.findTop5ByAccountId(accountId);

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

    @Async("rabbitTransactionAsyncExecutor")
    protected CompletableFuture<Boolean> communicateAccountBalance(String accountId, BigDecimal amount, String currency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> data = Map.of(
                        "accountId", accountId,
                        "amount", amount,
                        "currency", currency
                );
                Object response = rabbitTemplate.convertSendAndReceive(
                        RabbitConfig.TRANSACTIONS_EXCHANGE,
                        RabbitConfig.TRANSACTIONS_UPDATE_ROUTING_KEY,
                        data
                );

                if (response == null)
                {
                    return false;
                }

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> responseMap = mapper.convertValue(
                        response,
                        new TypeReference<>() {});

                return validateAccountBalanceResponse(responseMap, accountId);
            } catch (Exception e) {
                rabbitCommunicationErrorCounter.increment();
                logger.error("Error communicating with account: {}", accountId, e);

                return false;
            }
        });
    }

    @Transactional
    public CompletableFuture<Transaction> saveTransaction(CreateTransactionDTO transactionDto) {
        try {
            String id = transactionDto.id().replace("\"", "");
            Object accountBalance = redisService.getHashValue(RedisConfig.ACCOUNT_KEY_PREFIX + id, "balance");

            if (accountBalance == null) {
                transactionErrorCounter.increment();

                throw new BankAccountNotFoundException(String.format("Account with id: %s not found", id));
            }

            logger.info("Account balance: {}", accountBalance);

            Transaction transaction = new Transaction();

            transaction.setAmount(BigDecimal.valueOf(transactionDto.amount()));
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setDescription(transactionDto.description());
            transaction.setAccountId(UUID.fromString(id));
            transaction.setCurrency(TransactionCurrency.valueOf(transactionDto.currency().toUpperCase()));
            transaction.setCreatedAt(LocalDateTime.now());

            Transaction savedTransaction = transactionRepository.save(transaction);

            return communicateAccountBalance(
                    id,
                    BigDecimal.valueOf(transactionDto.amount()),
                    savedTransaction.getCurrency().name())
                    .thenApply(isSuccessful -> updateTransactionStatus(isSuccessful, savedTransaction));
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

    private Boolean validateAccountBalanceResponse(Map<String, Object> response, String accountId) {
        if (response.get("success") == null || response.get("success").equals("")) {
            return false;
        }
        if (response.get("accountId") == null || response.get("accountId").equals("")) {
            return false;
        }

        return !response.get("success").equals("false") && response.get("accountId").equals(accountId);
    }

    private Transaction updateTransactionStatus(boolean isSuccessful, Transaction transaction) {
        if (isSuccessful) {
            transaction.setStatus(TransactionStatus.DONE);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
        }

        return transactionRepository.save(transaction);
    }
}

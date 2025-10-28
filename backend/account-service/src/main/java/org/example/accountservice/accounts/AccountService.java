package org.example.accountservice.accounts;

import io.micrometer.core.instrument.MeterRegistry;
import org.example.accountservice.accounts.records.*;
import org.example.accountservice.configs.exceptions.*;
import org.example.accountservice.redis.AccountRedisService;
import org.example.accountservice.redis.RedisService;
import org.example.accountservice.services.AsyncRabbitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.Counter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;

@Service
public class AccountService {
    private final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final AccountRedisService accountRedisService;
    private final AsyncRabbitService asyncRabbitService;
    // My metrics
    private final Counter accountErrorCounter;
    private final Counter accountCreatedCounter;
    private final Counter accountDeletedCounter;
    private final RedisService redisService;
    private final Executor asyncExecutor;

    public AccountService(
            AccountRepository accountRepository,
            MeterRegistry registry,
            AccountRedisService accountRedisService,
            AsyncRabbitService asyncRabbitService, RedisService redisService, Executor asyncExecutor) {
        this.accountRepository = accountRepository;
        this.accountRedisService = accountRedisService;
        this.asyncRabbitService = asyncRabbitService;
        this.accountErrorCounter = io.micrometer.core.instrument.Counter.builder("account-service.account.errors.counter")
                .description("Error counter for account service")
                .register(registry);
        this.accountCreatedCounter = io.micrometer.core.instrument.Counter.builder("account-service.account.created.counter")
                .description("Counter for created accounts in account service")
                .register(registry);
        this.accountDeletedCounter = io.micrometer.core.instrument.Counter.builder("account-service.account.deleted.counter")
                .description("Counter for deleted accounts in account service")
                .register(registry);
        this.redisService = redisService;
        this.asyncExecutor = asyncExecutor;
    }

    @Transactional
    public Account createAccount(AccountDTO accountDTO) {
        try {
            Optional<Account> optionalAccount = accountRepository.findAccountByUserId(accountDTO.userId());

            if (optionalAccount.isPresent()) {
                throw new AccountAlreadyCreatedException(String.format("Account with user id %s is already created", accountDTO.userId()));
            }

            Account account = new Account();

            account.setBalance(BigDecimal.ZERO);
            account.setUserId(accountDTO.userId());
            account.setCurrency(accountDTO.currency());
            account.setCreatedAt(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());

            Account savedAccount = accountRepository.save(account);
            accountCreatedCounter.increment();

            accountRedisService.addAccountToRedis(savedAccount);

            return savedAccount;
        } catch (AccountAlreadyCreatedException e) {
            logger.error(e.getMessage());
            accountErrorCounter.increment();

            throw e;
        } catch (RedisOperationException | RedisConnectionException e) {
            logger.error(e.getMessage());
            accountErrorCounter.increment();

            throw new InternalAccountException("Internal Server Error");
        } catch (Exception e) {
            logger.error("Unexpected error creating account: {}", e.getMessage(), e);
            accountErrorCounter.increment();

            throw new InternalAccountException("Internal Server Error");
        }
    }

    @Transactional
    public Account updateAccount(UUID id, UpdateAccountDTO newData) {
        try {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new BankAccountNotFoundException(String.format("Account with id %s not found", id)));

            account.setCurrency(newData.currency());

            Account updatedAccount = accountRepository.save(account);

            accountRedisService.updateAccountFromRedis(updatedAccount);

            return updatedAccount;
        } catch (BankAccountNotFoundException e) {
            logger.error("Account with id {} not found", id, e);
            accountErrorCounter.increment();

            throw e;
        }
    }

    public CompletableFuture<List<TransactionDTO>> getTransactionsAsync(UUID accountId) {
        return asyncRabbitService.getTransactionsAsync(accountId)
                .thenApply(transactionMaps -> transactionMaps.stream()
                            .map(this::mapToTransactionDTO)
                            .toList())
                .exceptionally(ex -> {
                    logger.error("Failed to fetch transactions asynchronously for account {}: {}", 
                        accountId, ex.getMessage(), ex);
                    accountErrorCounter.increment();
                    
                    throw new TransactionsMessageFailedResponseException(
                        "Failed to fetch transactions: " + ex.getMessage(), ex);
                });
    }

    public CompletableFuture<AccountTransactionsDTO> getAccountByIdAsync(UUID id) {
        try {
            AccountReturnDTO account = accountRedisService.getAccountById(id.toString());

            if (account == null) {
                Account accountDb = accountRepository.findById(id).orElseThrow(() ->
                        new BankAccountNotFoundException("Account with id " + id + " not found"));
                account = AccountReturnDTO.from(
                        accountDb.getId(),
                        accountDb.getUserId(),
                        accountDb.getBalance().doubleValue(),
                        accountDb.getCreatedAt(),
                        accountDb.getUpdatedAt(),
                        accountDb.getCurrency());
            }

            AccountReturnDTO finalAccount = account;
            return getTransactionsAsync(id)
                    .thenApply(transactions -> AccountTransactionsDTO.from(finalAccount, transactions))
                    .exceptionally( ex -> {
                        logger.error("Failed to fetch transactions asynchronously: {}", ex.getMessage(), ex);

                        return AccountTransactionsDTO.from(finalAccount, Collections.emptyList());
                    });

        } catch (BankAccountNotFoundException e) {
            logger.error(e.getMessage());
            accountErrorCounter.increment();

            throw e;
        }
    }

    private TransactionDTO mapToTransactionDTO(Map<String, Object> transaction) {
        try {
            Object idValue = transaction.get("id");
            Object statusValue = transaction.get("status");
            Object createdAtValue = transaction.get("createdAt");

            if (idValue == null || statusValue == null || createdAtValue == null) {
                throw new IllegalArgumentException("Missing required transaction fields");
            }

            UUID id = idValue instanceof UUID ? (UUID) idValue : UUID.fromString(idValue.toString());
            String status = statusValue.toString();
            LocalDateTime createdAt = createdAtValue instanceof LocalDateTime ? 
                (LocalDateTime) createdAtValue : LocalDateTime.parse(createdAtValue.toString());

            return new TransactionDTO(id, status, createdAt);
        } catch (Exception e) {
            logger.error("Failed to map transaction: {}", transaction, e);

            throw new IllegalArgumentException("Invalid transaction data: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<Account> deleteAccountById(UUID id) {
        return asyncRabbitService.deleteTransactions(id)
                .thenCompose(isSuccess -> {
                    if (!isSuccess) {
                        accountErrorCounter.increment();
                        return CompletableFuture.failedFuture(
                                new TransactionRemovalFailedException("Failed to delete transactions")
                        );
                    }

                    return CompletableFuture.supplyAsync(() -> deleteAccountByIdFromDb(id), asyncExecutor);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to delete account: {}", ex.getMessage(), ex);
                    accountErrorCounter.increment();

                    throw new RuntimeException("Failed to delete account: " + ex.getMessage(), ex);
                });
    }

    @Transactional
    protected Account deleteAccountByIdFromDb(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BankAccountNotFoundException(String.format("Account with id %s not found", id)));

        accountRepository.delete(account);
        accountRedisService.deleteAccount(account.getId());
        accountDeletedCounter.increment();

        return account;
    }
}

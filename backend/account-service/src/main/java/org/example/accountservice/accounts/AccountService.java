package org.example.accountservice.accounts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.accountservice.accounts.records.AccountDTO;
import org.example.accountservice.accounts.records.AccountTransactionsDTO;
import org.example.accountservice.accounts.records.TransactionDTO;
import org.example.accountservice.accounts.records.UpdateAccountDTO;
import org.example.accountservice.configs.RabbitConfig;
import org.example.accountservice.configs.exceptions.AccountAlreadyCreatedException;
import org.example.accountservice.configs.exceptions.BankAccountNotFoundException;
import org.example.accountservice.configs.exceptions.TransactionsMessageFailedResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.Counter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {
    private final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final RabbitTemplate rabbitTemplate;
    // My metrics
    private final Counter accountErrorCounter;
    private final Counter accountCreatedCounter;
    private final Counter accountDeletedCounter;

    public AccountService(
            AccountRepository accountRepository,
            RabbitTemplate rabbitTemplate,
            MeterRegistry registry) {
        this.accountRepository = accountRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.accountErrorCounter = io.micrometer.core.instrument.Counter.builder("account-service.account.errors.counter")
                .description("Error counter for account service")
                .register(registry);
        this.accountCreatedCounter = io.micrometer.core.instrument.Counter.builder("account-service.account.created.counter")
                .description("Counter for created accounts in account service")
                .register(registry);
        this.accountDeletedCounter = io.micrometer.core.instrument.Counter.builder("account-service.account.deleted.counter")
                .description("Counter for deleted accounts in account service")
                .register(registry);
    }

    public AccountTransactionsDTO getAccountById(UUID id) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new BankAccountNotFoundException(String.format("Account with id %s not found", id)));

            Object response = rabbitTemplate.convertSendAndReceive(
                    RabbitConfig.TRANSACTIONS_EXCHANGE,
                    RabbitConfig.TRANSACTIONS_ROUTING_KEY,
                    id
            );

            if (response == null) {
                throw new TransactionsMessageFailedResponseException("Response was not received, timeout");
            }

            List<Map<String, Object>> transactionMaps = objectMapper.convertValue(
                    response,
                    new TypeReference<>() {});

            try {
                List<TransactionDTO> transactions = transactionMaps
                        .stream()
                        .map(transaction -> new TransactionDTO((UUID) transaction.get("id"), (String) transaction.get("status"), (LocalDateTime) transaction.get("createdAt")))
                        .toList();

                return AccountTransactionsDTO.from(account, transactions);

            } catch (ClassCastException e) {
                logger.error(e.getMessage());
                accountErrorCounter.increment();

                throw new TransactionsMessageFailedResponseException("Consumer returned different types than expected");
            } catch (NullPointerException e) {
                logger.error(e.getMessage());
                accountErrorCounter.increment();

                throw new TransactionsMessageFailedResponseException("Consumer returned null values");
            }

        } catch (TransactionsMessageFailedResponseException | BankAccountNotFoundException e) {
            logger.error(e.getMessage());
            accountErrorCounter.increment();

            throw e;
        }
    }

    @Transactional
    public Account createAccount(AccountDTO accountDTO) {
        try {
            Optional<Account> createdAccount = accountRepository.findByUserId(accountDTO.userId());

            if (createdAccount.isPresent()) {
                throw new AccountAlreadyCreatedException(String.format("Account with user id %s is already created", accountDTO.userId()));
            }

            Account account = new Account();

            account.setBalance(BigDecimal.ZERO);
            account.setUserId(accountDTO.userId());
            account.setCurrency(accountDTO.currency());

            Account savedAccount = accountRepository.save(account);
            accountCreatedCounter.increment();

            return savedAccount;
        } catch (AccountAlreadyCreatedException e) {
            logger.error(e.getMessage());
            accountErrorCounter.increment();

            throw e;
        }
    }

    @Transactional
    public Account updateAccount(UUID id, UpdateAccountDTO newData) {
        try {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new BankAccountNotFoundException(String.format("Account with id %s not found", id)));

            account.setCurrency(newData.currency());

            return accountRepository.save(account);
        } catch (BankAccountNotFoundException e) {
            logger.error("Account with id {} not found", id, e);
            accountErrorCounter.increment();

            throw e;
        }
    }

//    public Account deleteAccountById(UUID id) {
//
//    }
}

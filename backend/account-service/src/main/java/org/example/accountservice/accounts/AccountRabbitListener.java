package org.example.accountservice.accounts;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.example.accountservice.configs.RabbitConfig;
import org.example.accountservice.redis.AccountRedisService;
import org.example.accountservice.services.CurrencyConversionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class AccountRabbitListener {
    private final AccountRepository accountRepository;
    private final AccountRedisService accountRedisService;
    private final CurrencyConversionService currencyConversionService;
    private final Counter messagesProcessedCounter;
    private final Counter messagesErrorCounter;

    public AccountRabbitListener(
            AccountRepository accountRepository,
            CurrencyConversionService currencyConversionService,
            AccountRedisService accountRedisService,
            MeterRegistry registry) {
        this.accountRepository = accountRepository;
        this.currencyConversionService = currencyConversionService;
        this.accountRedisService = accountRedisService;
        this.messagesProcessedCounter = Counter.builder("transactions.processed.rabbit")
                .description("Success rabbit messages to get transactions for account")
                .register(registry);
        this.messagesErrorCounter = Counter.builder("errors.transactions.processed.rabbit")
                .description("Errors while processing rabbit messages to get transactions for account")
                .register(registry);
    }

    @RabbitListener(queues = RabbitConfig.ACCOUNT_UPDATE_QUEUE)
    public Map<String, Object> handleTransactionRequest(Map<String, Object> transactionData) {
        try {
            if (!validateIncomingMessage(transactionData)) {
                return Map.of("success", false);
            }

            String accountIdClean = transactionData.get("accountId").toString().replace("\"", "");
            String currencyString = transactionData.get("currency").toString();
            String amountString = transactionData.get("amount").toString();

            log.info("Received amount!!! {}", amountString);

            UUID accountId = UUID.fromString(accountIdClean);
            BigDecimal amount = new BigDecimal(amountString);
            AccountCurrency currency = AccountCurrency.valueOf(currencyString.toUpperCase());

            if (!handleReceivedTransactionData(accountId, amount, currency)) {
                return Map.of("success", false, "accountId", accountId.toString());
            }

            messagesProcessedCounter.increment();
            log.info("Successfully processed transaction request for account: {}", accountId);

            return Map.of("success", true, "accountId", accountId.toString());
        } catch (Exception e) {
            messagesErrorCounter.increment();
            log.error("Error processing transaction request", e);

            throw e;
        }
    }

    @Transactional
    public boolean handleReceivedTransactionData(UUID accountId, BigDecimal amount, AccountCurrency currency) {
        Optional<Account> optionalAccount = accountRepository.findById(accountId);

        if (optionalAccount.isEmpty()) {
            return false;
        }

        log.info("Received amount {} for account currency {}", amount, currency);

        Account account = optionalAccount.get();
        AccountCurrency accountCurrency = account.getCurrency();

        BigDecimal convertedAmount = currencyConversionService.convert(amount, currency, accountCurrency);
        log.info("Converted amount {} to {}", convertedAmount, currency);
        account.setBalance(account.getBalance().add(convertedAmount));

        log.info("Successfully processed account: {}", account);

        Account updatedAccount = accountRepository.save(account);

        accountRedisService.updateAccountFromRedis(updatedAccount);

        return true;
    }

    private boolean validateIncomingMessage(Map<String, Object> transactionData) {
        if (
                transactionData.get("accountId") == null ||
                        transactionData.get("amount") == null ||
                        transactionData.get("currency") == null
        ) {
            return false;
        }
        
        try {
            UUID.fromString(transactionData.get("accountId").toString().replace("\"", ""));
            new BigDecimal(transactionData.get("amount").toString());
            AccountCurrency.valueOf(transactionData.get("currency").toString().toUpperCase());

            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Error during casting: {}", e.getMessage());

            return false;
        }
    }
}

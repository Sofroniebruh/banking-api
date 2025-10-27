package org.example.transactionsservice.transactions;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionsservice.configs.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TransactionRabbitListener {
    private final TransactionService transactionService;
    private final Counter messagesProcessedCounter;
    private final Counter messagesErrorCounter;

    public TransactionRabbitListener(
            TransactionService transactionService,
            MeterRegistry registry) {
        this.transactionService = transactionService;
        this.messagesProcessedCounter = Counter.builder("transactions.processed.rabbit")
                .description("Success rabbit messages to get transactions for account")
                .register(registry);
        this.messagesErrorCounter = Counter.builder("errors.transactions.processed.rabbit")
                .description("Errors while processing rabbit messages to get transactions for account")
                .register(registry);
    }

    @RabbitListener(queues = RabbitConfig.TRANSACTIONS_QUEUE)
    public List<Map<String, Object>> handleTransactionRequest(String id) {
        try {
            id = id.replace("\"", "");
            UUID accountId = UUID.fromString(id);

            List<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId);

            List<Map<String, Object>> transactionDTOs = transactions
                    .stream()
                    .map(transaction -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", transaction.getId());
                        map.put("status", transaction.getStatus());
                        map.put("createdAt", transaction.getCreatedAt());

                        return map;
                    })
                    .toList();

            messagesProcessedCounter.increment();
            log.info("Successfully processed transaction request for account: {}", accountId);
            log.info("Transaction dtos: {}", transactionDTOs);

            return transactionDTOs;
        } catch (Exception e) {
            messagesErrorCounter.increment();
            log.error("Error processing transaction request for account: {}", id, e);

            throw e;
        }
    }
}

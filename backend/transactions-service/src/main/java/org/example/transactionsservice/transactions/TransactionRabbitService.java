package org.example.transactionsservice.transactions;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.transactionsservice.configs.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionRabbitService {
    private final Logger logger = LoggerFactory.getLogger(TransactionRabbitService.class);
    private final TransactionService transactionService;
    private final Counter messagesProcessedCounter;
    private final Counter messagesErrorCounter;

    public TransactionRabbitService(
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
            logger.info("Successfully processed transaction request for account: {}", accountId);

            return transactionDTOs;
        } catch (Exception e) {
            messagesErrorCounter.increment();
            logger.error("Error processing transaction request for account: {}", id, e);

            throw e;
        }
    }
}

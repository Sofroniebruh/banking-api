package org.example.transactionsservice.rabbit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionsservice.configs.RabbitConfig;
import org.example.transactionsservice.transactions.Transaction;
import org.example.transactionsservice.transactions.TransactionRepository;
import org.example.transactionsservice.transactions.TransactionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TransactionRepository transactionRepository;

    public TransactionRabbitListener(
            TransactionService transactionService,
            MeterRegistry registry, TransactionRepository transactionRepository) {
        this.transactionService = transactionService;
        this.messagesProcessedCounter = Counter.builder("transactions.processed.rabbit")
                .description("Success rabbit messages to get transactions for account")
                .register(registry);
        this.messagesErrorCounter = Counter.builder("errors.transactions.processed.rabbit")
                .description("Errors while processing rabbit messages to get transactions for account")
                .register(registry);
        this.transactionRepository = transactionRepository;
    }

    @RabbitListener(queues = RabbitConfig.TRANSACTIONS_QUEUE)
    @Transactional
    public Object handleTransactionRequest(
            String id,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        try {
            id = id.replace("\"", "");
            UUID accountId = UUID.fromString(id);

            if (RabbitConfig.TRANSACTIONS_ROUTING_KEY.equals(routingKey)) {
                return getTransactionsForAccount(accountId);
            } else if (RabbitConfig.TRANSACTIONS_DELETE_ROUTING_KEY.equals(routingKey)) {
                return deleteTransactionsByAccountId(accountId);
            }

            return null;
        } catch (Exception e) {
            messagesErrorCounter.increment();
            log.error("Error processing transaction request for account: {}", id, e);

            throw e;
        }
    }

    private Map<String, Object> deleteTransactionsByAccountId(UUID id) {
        transactionRepository.deleteAllByAccountId(id);

        return Map.of("success", true);
    }

    private List<Map<String, Object>> getTransactionsForAccount(UUID id) {
        List<Transaction> transactions = transactionService.getTransactionsByAccountId(id);

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

        return transactionDTOs;
    }
}

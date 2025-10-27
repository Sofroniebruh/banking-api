package org.example.accountservice.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.example.accountservice.configs.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AsyncRabbitService {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Counter successCounter;
    private final Counter timeoutCounter;
    private final Counter errorCounter;
    
    public AsyncRabbitService(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = new ObjectMapper();
        this.successCounter = Counter.builder("rabbit.async.success")
                .description("Successful async RabbitMQ calls")
                .register(meterRegistry);
        this.timeoutCounter = Counter.builder("rabbit.async.timeout")
                .description("Timeout async RabbitMQ calls")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("rabbit.async.error")
                .description("Failed async RabbitMQ calls")
                .register(meterRegistry);
    }
    
    @Async("rabbitAsyncExecutor")
    public CompletableFuture<List<Map<String, Object>>> sendAndReceiveAsync(
            String exchange, 
            String routingKey, 
            Object message) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Object response = rabbitTemplate.convertSendAndReceive(exchange, routingKey, message);

                if (response == null) {
                    timeoutCounter.increment();
                    log.warn("Received null response from RabbitMQ for routing key: {}", routingKey);

                    throw new RuntimeException("No response received from transactions service");
                }
                
                successCounter.increment();
                
                return objectMapper.convertValue(
                    response, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                errorCounter.increment();
                
                if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                    timeoutCounter.increment();
                    log.warn("RabbitMQ call timed out for routing key: {}", routingKey);

                    throw new RuntimeException("Timeout waiting for transactions service response", e);
                } else {
                    log.error("RabbitMQ call failed for routing key: {}", routingKey, e);

                    throw new RuntimeException("Failed to send RabbitMQ message: " + e.getMessage(), e);
                }
            }
        });
    }
    
    public CompletableFuture<List<Map<String, Object>>> getTransactionsAsync(UUID accountId) {
        return sendAndReceiveAsync(
                RabbitConfig.TRANSACTIONS_EXCHANGE,
                RabbitConfig.TRANSACTIONS_ROUTING_KEY,
                accountId
        );
    }
}
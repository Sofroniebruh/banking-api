package org.example.accountservice.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    private final String FACTORY_USERNAME;
    private final String FACTORY_PASSWORD;
    private final String FACTORY_HOST;
    private final String FACTORY_PORT;
    public static final String TRANSACTIONS_EXCHANGE = "transactions.exchange";
    public static final String TRANSACTIONS_QUEUE = "transactions.queue";
    public static final String TRANSACTIONS_ROUTING_KEY = "transactions.routing.key";
    private final Logger logger = LoggerFactory.getLogger(RabbitConfig.class);
    private final Counter transactionsErrorCounter;

    public RabbitConfig(
            @Value("${RABBIT_USERNAME}") String FACTORY_USERNAME,
            @Value("${RABBIT_PASSWORD}") String FACTORY_PASSWORD,
            @Value("${RABBIT_HOST}") String FACTORY_HOST,
            @Value("${RABBIT_PORT}") String FACTORY_PORT,
            MeterRegistry meterRegistry) {
        this.transactionsErrorCounter = Counter.builder("errors.account.transactions.rabbit")
                .description("Errors while sending rabbit messages to get transactions for account")
                .register(meterRegistry);
        this.FACTORY_USERNAME = FACTORY_USERNAME;
        this.FACTORY_PASSWORD = FACTORY_PASSWORD;
        this.FACTORY_HOST = FACTORY_HOST;
        this.FACTORY_PORT = FACTORY_PORT;
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(FACTORY_HOST);
        factory.setPort(Integer.parseInt(FACTORY_PORT));
        factory.setUsername(FACTORY_USERNAME);
        factory.setPassword(FACTORY_PASSWORD);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);

        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setCreateMessageIds(true);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setMessageConverter(messageConverter);

        template.setReplyTimeout(10000);

        template.setConfirmCallback(((correlationData, ack, cause) -> {
            if (ack) {
                logger.info("Confirmed by broker: {}", correlationData);
            } else {
                if (correlationData != null) {
                    logger.error("Message with id: {} rejected by broker: {}", correlationData.getId(), cause);
                } else {
                    logger.error("Message rejected by broker: {}", cause);
                }

                transactionsErrorCounter.increment();
            }
        }));

        return template;
    }

    @Bean
    public DirectExchange transactionsExchange() {
        return new DirectExchange(TRANSACTIONS_EXCHANGE);
    }

    @Bean
    public Queue transactionsQueue() {
        return new Queue(TRANSACTIONS_QUEUE, false);
    }

    @Bean
    public Binding transactionsBinding() {
        return BindingBuilder
                .bind(transactionsQueue())
                .to(transactionsExchange())
                .with(TRANSACTIONS_ROUTING_KEY);
    }
}

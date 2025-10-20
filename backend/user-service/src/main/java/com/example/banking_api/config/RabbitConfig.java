package com.example.banking_api.config;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    private final String FACTORY_USERNAME;
    private final String FACTORY_PASSWORD;
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_ROUTING_KEY = "email.routing.key";
    private final Logger logger = LoggerFactory.getLogger(RabbitConfig.class);
    private final Counter emailCounter;

    public RabbitConfig(
            @Value("${RABBIT_USERNAME}") String FACTORY_USERNAME,
            @Value("${RABBIT_PASSWORD}") String FACTORY_PASSWORD,
            MeterRegistry meterRegistry) {
        this.emailCounter = Counter.builder("errors.email.rabbit")
                .description("Errors while sending rabbit messages to send an email")
                .register(meterRegistry);
        this.FACTORY_USERNAME = FACTORY_USERNAME;
        this.FACTORY_PASSWORD = FACTORY_PASSWORD;
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory("rabbitmq-bank-api");
        factory.setUsername(FACTORY_USERNAME);
        factory.setPassword(FACTORY_PASSWORD);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);

        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setMessageConverter(new Jackson2JsonMessageConverter());

        template.setConfirmCallback(((correlationData, ack, cause) -> {
            if (ack) {
                logger.info("Confirmed by broker: {}", correlationData);
            } else {
                logger.error("Message with id: {} rejected by broker: {}", correlationData.getId(), cause);
                emailCounter.increment();
            }
        }));

        return template;
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, false);
    }

    @Bean
    public Binding emailSentBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(emailExchange())
                .with(EMAIL_ROUTING_KEY);
    }
}

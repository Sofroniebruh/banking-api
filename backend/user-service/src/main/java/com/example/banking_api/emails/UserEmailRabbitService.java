package com.example.banking_api.emails;

import com.example.banking_api.config.RabbitConfig;
import com.example.banking_api.config.exceptions.EmailServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserEmailRabbitService {
    private final RabbitTemplate rabbitTemplate;

    public EmailDTO sendEmailAndReceive() {
        try {
            String link = "test_link";
            ObjectMapper objectMapper = new ObjectMapper();

            Object response = rabbitTemplate.convertSendAndReceive(
                    RabbitConfig.EMAIL_EXCHANGE,
                    RabbitConfig.EMAIL_ROUTING_KEY,
                    link
            );

            if (response == null) {
                throw new EmailServiceException("Response was not received, timeout");
            }

            EmailDTO emailDTO = objectMapper.convertValue(response, EmailDTO.class);

            if (emailDTO.getError() != null) {
                throw new EmailServiceException(emailDTO.getError());
            }

            System.out.println("DTO: " + emailDTO);

            return emailDTO;
        } catch (Exception e) {
            throw new EmailServiceException("Failed to send email: " + e.getMessage());
        }
    }
}

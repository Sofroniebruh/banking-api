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

    public EmailResponseDTO sendEmailAndReceive(String link, String email) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            EmailDTO emailDTO = new EmailDTO();

            emailDTO.setEmail(email);
            emailDTO.setMessageBody(link);
            emailDTO.setSubject("Reset your password");

            Object response = rabbitTemplate.convertSendAndReceive(
                    RabbitConfig.EMAIL_EXCHANGE,
                    RabbitConfig.EMAIL_ROUTING_KEY,
                    emailDTO
            );

            if (response == null) {
                throw new EmailServiceException("Response was not received, timeout");
            }

            EmailResponseDTO emailResponseDTO = objectMapper.convertValue(response, EmailResponseDTO.class);

            if (emailResponseDTO.getError() != null) {
                throw new EmailServiceException(emailResponseDTO.getError());
            }

            System.out.println("DTO: " + emailResponseDTO);

            return emailResponseDTO;
        } catch (Exception e) {
            throw new EmailServiceException("Failed to send email: " + e.getMessage());
        }
    }
}

package com.example.banking_api.emails;

import lombok.Data;

@Data
public class EmailDTO {
    private String email;
    private String messageBody;
    private String subject;
}

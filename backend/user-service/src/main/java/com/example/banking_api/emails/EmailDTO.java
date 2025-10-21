package com.example.banking_api.emails;

import lombok.Data;

@Data
public class EmailDTO {
    private boolean success;
    private int statusCode;
    private String data;
    private String error;
}

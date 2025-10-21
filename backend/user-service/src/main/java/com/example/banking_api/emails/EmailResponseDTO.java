package com.example.banking_api.emails;

import lombok.Data;

@Data
public class EmailResponseDTO
{
    private boolean success;
    private int statusCode;
    private String data;
    private String error;
}

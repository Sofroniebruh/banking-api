package com.example.banking_api.accounts.exceptions;

public class InitialDepositRequiredException extends RuntimeException {
    public InitialDepositRequiredException(String message) {
        super(message);
    }
}

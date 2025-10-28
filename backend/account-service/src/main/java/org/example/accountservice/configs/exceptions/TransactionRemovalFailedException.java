package org.example.accountservice.configs.exceptions;

public class TransactionRemovalFailedException extends RuntimeException {
    public TransactionRemovalFailedException(String message) {
        super(message);
    }
}

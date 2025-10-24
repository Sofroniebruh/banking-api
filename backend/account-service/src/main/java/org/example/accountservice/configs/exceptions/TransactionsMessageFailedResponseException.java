package org.example.accountservice.configs.exceptions;

public class TransactionsMessageFailedResponseException extends RuntimeException {
    public TransactionsMessageFailedResponseException(String message) {
        super(message);
    }
}

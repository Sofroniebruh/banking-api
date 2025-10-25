package org.example.accountservice.configs.exceptions;

public class AccountAlreadyCreatedException extends RuntimeException {
    public AccountAlreadyCreatedException(String message) {
        super(message);
    }
}

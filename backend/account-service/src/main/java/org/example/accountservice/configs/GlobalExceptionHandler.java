package org.example.accountservice.configs;

import org.example.accountservice.configs.exceptions.AccountAlreadyCreatedException;
import org.example.accountservice.configs.exceptions.BankAccountNotFoundException;
import org.example.accountservice.configs.exceptions.InternalAccountException;
import org.example.accountservice.configs.exceptions.TransactionsMessageFailedResponseException;
import org.example.accountservice.configs.records.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BankAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> bankAccountNotFound(BankAccountNotFoundException e) {
        ErrorResponse response = ErrorResponse.from(e.getMessage(), Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccountAlreadyCreatedException.class)
    public ResponseEntity<ErrorResponse> accountAlreadyCreated(AccountAlreadyCreatedException e) {
        ErrorResponse response = ErrorResponse.from(e.getMessage(), Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(TransactionsMessageFailedResponseException.class)
    public ResponseEntity<ErrorResponse> transactionsMessageFailed(TransactionsMessageFailedResponseException e) {
        ErrorResponse response = ErrorResponse.from(e.getMessage(), Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationError(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(InternalAccountException.class)
    public ResponseEntity<ErrorResponse> internalAccountException(InternalAccountException e) {
        ErrorResponse response = ErrorResponse.from(e.getMessage(), Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

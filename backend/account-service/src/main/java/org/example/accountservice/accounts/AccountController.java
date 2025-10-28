package org.example.accountservice.accounts;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.accountservice.accounts.records.AccountDTO;
import org.example.accountservice.accounts.records.AccountTransactionsDTO;
import org.example.accountservice.accounts.records.UpdateAccountDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<AccountTransactionsDTO>> getAccount(@PathVariable UUID id) {
        return accountService.getAccountByIdAsync(id)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody AccountDTO accountDTO) {
        Account account = accountService.createAccount(accountDTO);

        return ResponseEntity.ok(account);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAccountDTO newData) {
        Account newAccount = accountService.updateAccount(id, newData);

        return ResponseEntity.ok(newAccount);
    }

    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Account>> deleteAccountById(@PathVariable("id") UUID id) {
        return accountService.deleteAccountById(id)
                .thenApply(ResponseEntity::ok);
    }
}

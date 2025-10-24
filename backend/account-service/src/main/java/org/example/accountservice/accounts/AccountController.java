package org.example.accountservice.accounts;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.accountservice.accounts.records.AccountDTO;
import org.example.accountservice.accounts.records.AccountTransactionsDTO;
import org.example.accountservice.accounts.records.UpdateAccountDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/{id}")
    public ResponseEntity<AccountTransactionsDTO> findById(@PathVariable("id") UUID id) {
        AccountTransactionsDTO account = accountService.getAccountById(id);

        return ResponseEntity.ok(account);
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

//    @DeleteMapping("/{id}")
//    public ResponseEntity<Account> deleteAccountById(@PathVariable("id") UUID id) {
//        Account deletedAccount = accountService.deleteAccountById(id);
//
//        return ResponseEntity.ok(deletedAccount);
//    }
}

package com.example.banking_api.accounts;

import com.example.banking_api.accounts.records.AccountDTO;
import com.example.banking_api.accounts.records.CreateAccountDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {
    public AccountService accountService;

    @PostMapping()
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody CreateAccountDTO createAccountDTO) {
        AccountDTO newAccount = accountService.createAccount(createAccountDTO);

        return new ResponseEntity<>(newAccount, HttpStatus.CREATED);
    }
}

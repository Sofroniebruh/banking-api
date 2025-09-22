package com.example.banking_api.users;

import com.example.banking_api.accounts.AccountService;
import com.example.banking_api.accounts.records.AccountDTO;
import com.example.banking_api.common.PaginatedResponse;
import com.example.banking_api.users.records.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    public UserService userService;
    public AccountService accountService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        UserDTO userDTO = userService.getUserById(id);

        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/{id}/accounts")
    public ResponseEntity<PaginatedResponse<AccountDTO>> getAccountsByUserId
            (@PathVariable UUID id,
             @RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PaginatedResponse<AccountDTO> accounts = accountService.getAccountsByUserId(id, pageable);

        return ResponseEntity.ok(accounts);
    }
}

package com.example.banking_api.users.records;

import com.example.banking_api.accounts.Account;
import com.example.banking_api.users.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserDTO fromEntity(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(), user.getUpdatedAt());
    }
}

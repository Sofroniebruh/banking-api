package org.example.authservice.users.records;

import org.example.authservice.users.Role;
import org.example.authservice.users.User;

import java.util.List;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        String email,
        List<Role> roles,
        String accessToken,
        String refreshToken
) {
    public static UserDTO fromEntity(User user, String accessToken, String refreshToken) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRoles(), accessToken, refreshToken);
    }
}

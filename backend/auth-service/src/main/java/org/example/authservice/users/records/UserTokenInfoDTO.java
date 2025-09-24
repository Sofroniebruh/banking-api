package org.example.authservice.users.records;

import org.example.authservice.users.Role;

import java.util.List;
import java.util.UUID;

public record UserTokenInfoDTO(
        String email,
        UUID id,
        List<Role> roles
) {

}

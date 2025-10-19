package com.example.banking_api.users.records;

public record DeletedUser(
        UserDTO deletedUser
) {
    public static DeletedUser fromEntity(UserDTO userDTO) {
        return new DeletedUser(userDTO);
    }
}

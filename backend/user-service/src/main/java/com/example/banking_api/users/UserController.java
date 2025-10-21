package com.example.banking_api.users;

import com.example.banking_api.users.records.DeletedUser;
import com.example.banking_api.users.records.UpdateUserDTO;
import com.example.banking_api.users.records.UserDTO;
import com.example.banking_api.users.records.UserEmailDTO;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        UserDTO userDTO = userService.getUserById(id);

        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody UpdateUserDTO userDTO) {
        UserDTO updatedUser = userService.updateUserById(id, userDTO);

        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/password-reset-initialization")
    public ResponseEntity<?> passwordResetRequest(@RequestBody UserEmailDTO userEmailDTO) {
        if (userEmailDTO.email() == null || userEmailDTO.email().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email cannot be empty"));
        }

        return userService.requestEmailSending(userEmailDTO.email().trim());
    }

//    @PutMapping("/password-reset")
//    public ResponseEntity<UserDTO> passwordUpdate(
//            @RequestBody UpdateUserDTO userDTO,
//            @RequestParam String token) {
//
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeletedUser> deleteUser(@PathVariable UUID id) {
        DeletedUser deletedUser = userService.deleteUserById(id);

        return ResponseEntity.ok(deletedUser);
    }
}

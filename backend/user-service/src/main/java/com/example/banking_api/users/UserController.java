package com.example.banking_api.users;

import com.example.banking_api.users.records.DeletedUser;
import com.example.banking_api.users.records.UpdateUserDTO;
import com.example.banking_api.users.records.UserDTO;
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

    @PutMapping("/{id}/password-reset")
    public ResponseEntity<?> passwordResetRequest(@PathVariable UUID id) {
        userService.requestEmailSending();

        return ResponseEntity.ok().body(Map.of("message", "Password Reset Request"));
    }

//    @PutMapping("/{id}")
//    public ResponseEntity<UserDTO> passwordUpdate(
//            @PathVariable UUID id,
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

package org.example.authservice.users;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.authservice.users.records.AuthUserDTO;
import org.example.authservice.users.records.CreateUserDTO;
import org.example.authservice.users.records.UserDTO;
import org.example.authservice.users.records.UserTokenInfoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @PostMapping("/registration")
    public ResponseEntity<UserDTO> registerUser(@RequestBody @Valid CreateUserDTO userDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody @Valid AuthUserDTO userDTO) {
        return ResponseEntity.ok(userService.login(userDTO));
    }

    @PostMapping("/validate")
    public ResponseEntity<UserTokenInfoDTO> validateToken(
            @CookieValue(value = "access_token") String accessToken) {
        return ResponseEntity.ok(userService.validateToken(accessToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserDTO> refreshToken(
            @CookieValue(value = "refresh_token") String refreshToken) {
        return ResponseEntity.ok(userService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        userService.logout(response);

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully",
                "timestamp", Instant.now()
        ));
    }
}

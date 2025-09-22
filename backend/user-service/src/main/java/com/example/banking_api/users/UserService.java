package com.example.banking_api.users;

import com.example.banking_api.users.exceptions.UserNotFoundException;
import com.example.banking_api.users.records.CreateUserDTO;
import com.example.banking_api.users.records.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;
    public UserRepository userRepository;

    public UserDTO getUserById(UUID id) {
        Optional<User> optionalUser = userRepository.findUserById(id);

        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }

        User user = optionalUser.get();

        return UserDTO.fromEntity(user);
    }

    public UserDTO saveUser(CreateUserDTO createUserDTO) {
        User newUser = new User();
        newUser.setName(createUserDTO.username());
        newUser.setEmail(createUserDTO.email());
        newUser.setPassword(passwordEncoder.encode(createUserDTO.password()));

        User savedUser = userRepository.save(newUser);

        return UserDTO.fromEntity(savedUser);
    }
}

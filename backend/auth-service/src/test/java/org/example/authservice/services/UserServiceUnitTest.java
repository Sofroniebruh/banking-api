package org.example.authservice.services;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.authservice.jwt_validators.JwtService;
import org.example.authservice.users.Role;
import org.example.authservice.users.User;
import org.example.authservice.users.UserRepository;
import org.example.authservice.users.UserService;
import org.example.authservice.users.exceptions.TokenGeneratorException;
import org.example.authservice.users.exceptions.UserException;
import org.example.authservice.users.records.AuthUserDTO;
import org.example.authservice.users.records.CreateUserDTO;
import org.example.authservice.users.records.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserDetailsService userDetailsService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserDetails userDetails;

    private UserService userService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        userService = new UserService(
                passwordEncoder,
                userDetailsService,
                authenticationManager,
                userRepository,
                jwtService,
                meterRegistry,
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        );
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        CreateUserDTO userDTO = mockCreateUserDTO();

        User savedUser = createTestUser(userDTO.name(), userDTO.email());

        when(userRepository.findUserByEmail(userDTO.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(userDTO.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername(userDTO.email())).thenReturn(userDetails);
        when(jwtService.generateToken(anyMap(), eq(userDetails), any(Duration.class)))
                .thenReturn("mock-access-token")
                .thenReturn("mock-refresh-token");

        UserDTO createdUser = userService.createUser(userDTO);

        assertNotNull(createdUser);
        assertEquals(userDTO.email(), createdUser.email());
        assertEquals(userDTO.name(), createdUser.name());
        assertEquals(savedUser.getId(), createdUser.id());
        assertEquals("mock-access-token", createdUser.accessToken());
        assertEquals("mock-refresh-token", createdUser.refreshToken());

        verify(userRepository).findUserByEmail(userDTO.email());
        verify(passwordEncoder).encode(userDTO.password());
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtService, times(2)).generateToken(anyMap(), eq(userDetails), any(Duration.class));

        assertEquals(1.0, meterRegistry.counter("users.created.total").count());
        assertEquals(0.0, meterRegistry.counter("users.error.total").count());
    }

    @Test
    @DisplayName("Should throw a User Exception if user exists")
    void shouldThrowUserExceptionIfUserExists() {
        CreateUserDTO userDTO = mockCreateUserDTO();
        User savedUser = createTestUser(userDTO.name(), userDTO.email());

        when(userRepository.findUserByEmail(userDTO.email())).thenReturn(Optional.of(savedUser));

        assertThrows(UserException.class, () -> userService.createUser(userDTO));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(userDTO.password());

        assertEquals(1.0, meterRegistry.counter("users.error.total").count());
        assertEquals(0.0, meterRegistry.counter("users.created.total").count());
    }

    @Test
    @DisplayName("Should throw a Token Exception if token generation failed")
    void shouldThrowTokenExceptionIfTokenGenerationFailed() {
        CreateUserDTO userDTO = mockCreateUserDTO();
        User savedUser = createTestUser(userDTO.name(), userDTO.email());

        when(userRepository.findUserByEmail(userDTO.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(userDTO.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername(userDTO.email()))
                .thenThrow(UsernameNotFoundException.class);

        assertThrows(TokenGeneratorException.class, () -> userService.createUser(userDTO));

        verify(userRepository, times(1)).findUserByEmail(userDTO.email());
        verify(userRepository, times(1)).save(any(User.class));

        assertEquals(1.0, meterRegistry.counter("tokens.error.total").count());
        assertEquals(0.0, meterRegistry.counter("users.created.total").count());
    }

    @Test
    @DisplayName("Successfully log in user")
    void shouldLogInSuccessfully() {
        AuthUserDTO authUserDTO = mockAuthUserDTO();
        User savedUser = createTestUser("John Doe", authUserDTO.email());

        when(userRepository.findUserByEmail(authUserDTO.email())).thenReturn(Optional.of(savedUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userDetailsService.loadUserByUsername(authUserDTO.email())).thenReturn(userDetails);
        when(jwtService.generateToken(anyMap(), eq(userDetails), any(Duration.class)))
                .thenReturn("mock-access-token")
                .thenReturn("mock-refresh-token");

        UserDTO loggedInUser = userService.login(authUserDTO);

        assertNotNull(loggedInUser);
        assertEquals(authUserDTO.email(), loggedInUser.email());
        assertEquals("mock-access-token", loggedInUser.accessToken());
        assertEquals("mock-refresh-token", loggedInUser.refreshToken());

        verify(userRepository, times(1)).findUserByEmail(authUserDTO.email());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(2)).generateToken(anyMap(), eq(userDetails), any(Duration.class));

        assertEquals(0.0, meterRegistry.counter("users.error.total").count());
        assertEquals(0.0, meterRegistry.counter("tokens.error.total").count());
    }

    @Test
    @DisplayName("Should throw a Bad Credentials Exception if credentials are incorrect")
    void shouldThrowBadCredentialsExceptionIfCredentialsAreIncorrect() {
        AuthUserDTO authUserDTO = mockAuthUserDTO();
        User savedUser = createTestUser("John Doe", authUserDTO.email());

        when(userRepository.findUserByEmail(authUserDTO.email())).thenReturn(Optional.of(savedUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(BadCredentialsException.class);

        assertThrows(BadCredentialsException.class, () -> userService.login(authUserDTO));

        verify(userRepository, times(1)).findUserByEmail(authUserDTO.email());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(anyMap(), eq(userDetails), any(Duration.class));
        
        assertEquals(1.0, meterRegistry.counter("users.error.total").count());
    }

    @Test
    @DisplayName("Should throw UserException if the user does not exist")
    void shouldThrowUserExceptionIfUserDoesNotExist() {
        AuthUserDTO authUserDTO = mockAuthUserDTO();

        when(userRepository.findUserByEmail(authUserDTO.email())).thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> userService.login(authUserDTO));

        verify(userRepository, times(1)).findUserByEmail(authUserDTO.email());
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertEquals(1.0, meterRegistry.counter("users.error.total").count());
    }

    private User createTestUser(String name, String email)
    {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRoles(List.of(Role.USER));

        return user;
    }

    private CreateUserDTO mockCreateUserDTO() {
        return new CreateUserDTO(
                "John Doe",
                "qwerty@gmail.com",
                "qwerty"
        );
    }

    private AuthUserDTO mockAuthUserDTO() {
        return new AuthUserDTO(
                "qwerty@gmail.com",
                "qwerty"
        );
    }
}

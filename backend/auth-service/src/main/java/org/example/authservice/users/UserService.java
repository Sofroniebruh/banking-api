package org.example.authservice.users;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.authservice.jwt_validators.JwtService;
import org.example.authservice.users.exceptions.InternalErrorException;
import org.example.authservice.users.exceptions.InvalidTokenException;
import org.example.authservice.users.exceptions.UserException;
import org.example.authservice.users.records.AuthUserDTO;
import org.example.authservice.users.records.CreateUserDTO;
import org.example.authservice.users.records.UserDTO;
import org.example.authservice.users.records.UserTokenInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final int ACCESS_TOKEN_TTL;
    private final int REFRESH_TOKEN_TTL;
    private final AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    public JwtService jwtService;

    public UserService(
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            @Value("${ACCESS_TOKEN_TTL}") int ACCESS_TOKEN_TTL,
            @Value("${REFRESH_TOKEN_TTL}") int REFRESH_TOKEN_TTL
    ) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.ACCESS_TOKEN_TTL = ACCESS_TOKEN_TTL;
        this.REFRESH_TOKEN_TTL = REFRESH_TOKEN_TTL;
    }

    @Transactional
    public UserDTO createUser(CreateUserDTO userDTO) {
        try {
            Optional<User> existingUser = userRepository.findByEmail(userDTO.email());

            if (existingUser.isPresent()) {
                throw new UserException(String.format("User with email %s already exists", userDTO.email()));
            }

            User user = new User();
            user.setName(userDTO.name());
            user.setEmail(userDTO.email());
            user.setPassword(passwordEncoder.encode(userDTO.password()));
            user.setRoles(List.of(Role.USER));

            Map<String, String> tokens = generateTokens(user);
            String accessToken = tokens.get("access_token");
            String refreshToken = tokens.get("refresh_token");

            return UserDTO.fromEntity(userRepository.save(user), accessToken, refreshToken);
        } catch (Exception e) {
            logger.error("Error creating user: ", e);
            throw new InternalErrorException("Internal Server Error");
        }
    }

    @Transactional
    public UserDTO login(AuthUserDTO userDTO) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userDTO.email(),
                            userDTO.password()
                    )
            );

            User user = userRepository.findByEmail(userDTO.email())
                    .orElseThrow(() -> new UserException(String.format("User with email %s not found", userDTO.email())));
            Map<String, String> tokens = generateTokens(user);
            String accessToken = tokens.get("access_token");
            String refreshToken = tokens.get("refresh_token");

            return UserDTO.fromEntity(user, accessToken, refreshToken);
        } catch (Exception e) {
            logger.error("Error logging in: ", e);
            throw new InternalErrorException("Internal Server Error");
        }
    }

    public UserTokenInfoDTO validateToken(String token) {
        try {
            String email = jwtService.extractUserEmail(token);

            if (email == null || email.trim().isEmpty()) {
                throw new InvalidTokenException("Invalid token");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserException(String.format("User with email %s not found", email)));
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new InvalidTokenException("Invalid token");
            }

            List<Role> roles = user.getRoles();
            UUID userId = user.getId();

            return new UserTokenInfoDTO(email, userId, roles);
        } catch (Exception e) {
            logger.error("Error validating token: ", e);
            throw new InternalErrorException("Internal Server Error");
        }
    }

    private Map<String, String> generateTokens(User user) {
        Map<String, Object> claims = new HashMap<>();
        Map<String, String> tokens = new HashMap<>();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        claims.put("roles", user.getRoles());
        claims.put("id", user.getId());

        String accessToken = jwtService.generateToken(claims, userDetails, ACCESS_TOKEN_TTL);
        String refreshToken = jwtService.generateToken(claims, userDetails, REFRESH_TOKEN_TTL);

        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken);

        return tokens;
    }

    public UserDTO refresh(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(jwtService.extractUserEmail(token));

        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        String email = jwtService.extractUserEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(String.format("User with email %s not found", email)));

        String accessToken = generateTokens(user).get("access_token");
        String refreshToken = generateTokens(user).get("refresh_token");

        return UserDTO.fromEntity(user, accessToken, refreshToken);
    }
}

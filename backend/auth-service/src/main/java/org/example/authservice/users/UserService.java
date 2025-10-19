package org.example.authservice.users;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authservice.jwt_validators.JwtService;
import org.example.authservice.config.exceptions.InternalErrorException;
import org.example.authservice.config.exceptions.InvalidTokenException;
import org.example.authservice.config.exceptions.TokenGeneratorException;
import org.example.authservice.config.exceptions.UserException;
import org.example.authservice.users.records.AuthUserDTO;
import org.example.authservice.users.records.CreateUserDTO;
import org.example.authservice.users.records.UserDTO;
import org.example.authservice.users.records.UserTokenInfoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final Duration ACCESS_TOKEN_TTL;
    private final Duration REFRESH_TOKEN_TTL;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    // My metrics for prometheus
    private final Counter userCreatedCounter;
    private final Counter userErrorCounter;
    private final Counter tokenErrorCounter;
    private final Counter internalErrorCounter;

    public UserService(
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            MeterRegistry registry,
            @Value("${ACCESS_TOKEN_TTL}") Duration ACCESS_TOKEN_TTL,
            @Value("${REFRESH_TOKEN_TTL}") Duration REFRESH_TOKEN_TTL
    ) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.ACCESS_TOKEN_TTL = ACCESS_TOKEN_TTL;
        this.REFRESH_TOKEN_TTL = REFRESH_TOKEN_TTL;
        this.userCreatedCounter = Counter.builder("users.created.total")
                .description("Total users created successfully")
                .register(registry);
        this.userErrorCounter = Counter.builder("users.error.total")
                .description("Total errors while creating users")
                .register(registry);
        this.tokenErrorCounter = Counter.builder("tokens.error.total")
                .description("Total errors while validating tokens")
                .register(registry);
        this.internalErrorCounter = Counter.builder("auth-service.internal-error.total")
                .description("Total internal errors encountered")
                .register(registry);
    }

    @Transactional
    public UserDTO createUser(CreateUserDTO userDTO) {
        try {
            Optional<User> existingUser = userRepository.findUserByEmail(userDTO.email());

            if (existingUser.isPresent()) {
                throw new UserException(String.format("User with email %s already exists", userDTO.email()));
            }

            User user = new User();
            user.setName(userDTO.name());
            user.setEmail(userDTO.email());
            user.setPassword(passwordEncoder.encode(userDTO.password()));
            user.setRoles(List.of(Role.USER));

            User savedUser = userRepository.save(user);

            Map<String, String> tokens = generateTokens(savedUser);

            String accessToken = tokens.get("access_token");
            String refreshToken = tokens.get("refresh_token");

            userCreatedCounter.increment();

            return UserDTO.fromEntity(savedUser, accessToken, refreshToken);
        } catch (UserException ex) {
            logger.error("User creation failed: ", ex);
            userErrorCounter.increment();

            throw ex;
        } catch (TokenGeneratorException ex) {
            logger.error("Token creation failed: ", ex);
            tokenErrorCounter.increment();

            throw ex;
        } catch (Exception ex) {
            logger.error("Error creating user: ", ex);
            userErrorCounter.increment();

            throw new InternalErrorException("[auth-service]: Internal Server Error");
        }
    }

    @Transactional
    public UserDTO login(AuthUserDTO userDTO) {
        try {
            User user = userRepository.findUserByEmail(userDTO.email())
                    .orElseThrow(() -> new UserException(String.format("User with email %s not found", userDTO.email())));

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userDTO.email(),
                            userDTO.password()
                    )
            );

            Map<String, String> tokens = generateTokens(user);

            String accessToken = tokens.get("access_token");
            String refreshToken = tokens.get("refresh_token");

            return UserDTO.fromEntity(user, accessToken, refreshToken);
        } catch (BadCredentialsException | UserException ex) {
            logger.error("User login failed: ", ex);
            userErrorCounter.increment();

            throw ex;
        } catch (TokenGeneratorException ex) {
            logger.error("Token generation failed during login: ", ex);
            tokenErrorCounter.increment();

            throw ex;
        } catch (Exception ex) {
            logger.error("Error logging in: ", ex);
            internalErrorCounter.increment();

            throw new InternalErrorException("[auth-service]: Internal Server Error");
        }
    }

    public UserTokenInfoDTO validateToken(String token) {
        try {
            String email = jwtService.extractUserEmail(token);

            if (email == null || email.trim().isEmpty()) {
                throw new InvalidTokenException("Invalid token");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new InvalidTokenException("Invalid token");
            }

            UUID userId = jwtService.extractUserId(token);
            List<Role> roles = jwtService.extractUserRoles(token)
                    .stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toList());

            return new UserTokenInfoDTO(email, userId, roles);
        } catch (InvalidTokenException ex) {
            logger.error("Token validation failed: ", ex);
            tokenErrorCounter.increment();

            throw ex;
        } catch (UserException ex) {
            logger.error("User not found during token validation: ", ex);
            userErrorCounter.increment();

            throw ex;
        } catch (ExpiredJwtException ex) {
            logger.error("Token expired during token validation: ", ex);
            tokenErrorCounter.increment();

            throw ex;
        } catch (MalformedJwtException ex) {
            logger.error("Malformed token during token validation: ", ex);
            tokenErrorCounter.increment();

            throw ex;
        } catch (Exception ex) {
            logger.error("Error validating token: ", ex);
            internalErrorCounter.increment();

            throw new InternalErrorException("[auth-service]: Internal Server Error");
        }
    }

    public UserDTO refresh(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new InvalidTokenException("Invalid refresh token");
            }

            String email = jwtService.extractUserEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new InvalidTokenException("Refresh token is invalid or expired");
            }

            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new UserException(String.format("User with email %s not found", email)));
            Map<String, String> tokens = generateTokens(user);
            String accessToken = tokens.get("access_token");
            String refreshToken = tokens.get("refresh_token");

            return UserDTO.fromEntity(user, accessToken, refreshToken);
        } catch (InvalidTokenException | MalformedJwtException ex) {
            logger.error("Token refresh failed: ", ex);
            tokenErrorCounter.increment();

            throw ex;
        } catch (UsernameNotFoundException | UserException ex) {
            logger.error("User not found during token refresh: ", ex);
            userErrorCounter.increment();

            throw ex;
        } catch (TokenGeneratorException ex) {
            logger.error("Token generation failed during refresh: ", ex);
            tokenErrorCounter.increment();

            throw ex;
        } catch (Exception ex) {
            logger.error("Error refreshing token: ", ex);
            internalErrorCounter.increment();

            throw new InternalErrorException("[auth-service]: Internal Server Error");
        }
    }

    public void logout(HttpServletResponse response) {
        Cookie accessCookie = createCookie("access_token", null, 0, true, "/", true);
        Cookie refreshCookie = createCookie("refresh_token", null, 0, true, "/", true);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    private Map<String, String> generateTokens(User user) {
        try {
            Map<String, Object> claims = new HashMap<>();
            Map<String, String> tokens = new HashMap<>();

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            claims.put("roles", user.getRoles().stream().map(Role::name).collect(Collectors.toList()));
            claims.put("id", user.getId().toString());

            String accessToken = jwtService.generateToken(claims, userDetails, ACCESS_TOKEN_TTL);
            String refreshToken = jwtService.generateToken(claims, userDetails, REFRESH_TOKEN_TTL);

            tokens.put("access_token", accessToken);
            tokens.put("refresh_token", refreshToken);

            return tokens;
        } catch (UsernameNotFoundException ex) {
            logger.error("User not found during token generation: ", ex);
            userErrorCounter.increment();

            throw new TokenGeneratorException("Failed to generate tokens: user not found");
        } catch (Exception ex) {
            logger.error("Error generating tokens: ", ex);
            internalErrorCounter.increment();

            throw new TokenGeneratorException("Failed to generate tokens: " + ex.getMessage());
        }
    }

    private Cookie createCookie(String name, String value, int maxAge, boolean httpOnly, String path, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);

        return cookie;
    }
}

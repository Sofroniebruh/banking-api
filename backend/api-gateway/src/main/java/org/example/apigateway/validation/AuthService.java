package org.example.apigateway.validation;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.example.apigateway.config.exceptions.AuthenticationServiceUnavailable;
import org.example.apigateway.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthService {
    private final RestClient restClient;
    private final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public AuthService(@Qualifier("authRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "authService", fallbackMethod = "fallback")
    public User validateTokenWithAuthService(String token) {
        try {
            return restClient
                    .post()
                    .uri("/api/v1/auth/validate")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(User.class);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new AuthenticationServiceUnavailable("Auth service unavailable");
        }
    }

    private User fallback(String token, Exception ex) {
        logger.error(ex.getMessage());
        throw new AuthenticationServiceUnavailable("Authentication service is currently unavailable");
    }
}

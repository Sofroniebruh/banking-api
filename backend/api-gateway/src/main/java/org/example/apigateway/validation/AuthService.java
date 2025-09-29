package org.example.apigateway.validation;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.example.apigateway.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Service
public class AuthService {
    @Autowired
    private WebClient authWebClient;

    @CircuitBreaker(name = "authService", fallbackMethod = "fallback")
    public User validateTokenWithAuthService(String token) {
        try {
            return authWebClient
                    .post()
                    .uri("/api/v1/auth/validate")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(User.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();

        } catch (Exception e) {
            return null;
        }
    }

    private User fallback() {
        return null;
    }
}

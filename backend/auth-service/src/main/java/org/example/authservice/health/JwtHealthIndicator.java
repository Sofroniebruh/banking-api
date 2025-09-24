package org.example.authservice.health;

import org.example.authservice.jwt_validators.JwtService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class JwtHealthIndicator implements HealthIndicator {
    private final JwtService jwtService;

    public JwtHealthIndicator(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Health health() {
        try {
            if (jwtService != null) {
                return Health.up()
                        .withDetail("status", "JWT Service is operational")
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "JWT Service is not available")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
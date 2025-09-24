package org.example.authservice.health;

import org.example.authservice.users.UserRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class UserServiceHealthIndicator implements HealthIndicator {
    private final UserRepository userRepository;

    public UserServiceHealthIndicator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Health health() {
        try {
            long userCount = userRepository.count();
            
            return Health.up()
                    .withDetail("status", "User service is operational")
                    .withDetail("total_users", userCount)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Cannot connect to user database")
                    .withDetail("exception", e.getMessage())
                    .build();
        }
    }
}
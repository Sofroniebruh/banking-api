package org.example.apigateway.validation;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.example.apigateway.config.exceptions.AuthenticationServiceUnavailable;
import org.example.apigateway.config.exceptions.AuthServiceClientException;
import org.example.apigateway.users.User;
import org.example.apigateway.validation.records.UserTokenInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthService {
    private final RestClient restClient;
    private final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final String INTERNAL_SERVICE_SECRET;
    private final String BASE_AUTH_SERVICE_URL;

    public AuthService(@Qualifier("downstreamRestClient") RestClient restClient,
                       @Value("${INTERNAL_SERVICE_SECRET}") String internalServiceSecret,
                       @Value("${BASE_AUTH_SERVICE_URL}") String baseAuthServiceUrl) {
        this.restClient = restClient;
        this.INTERNAL_SERVICE_SECRET = internalServiceSecret;
        this.BASE_AUTH_SERVICE_URL = baseAuthServiceUrl;
    }

    @CircuitBreaker(name = "authService", fallbackMethod = "fallback")
    public User validateTokenWithAuthService(String token) {
        try {
            logger.warn("URL used" + BASE_AUTH_SERVICE_URL + "/api/v1/auth/validate");
            UserTokenInfoDTO tokenInfo = restClient
                    .post()
                    .uri(BASE_AUTH_SERVICE_URL + "/api/v1/auth/validate")
                    .cookie("access_token", token)
                    .header("X-Internal-Request", INTERNAL_SERVICE_SECRET)
                    .retrieve()
                    .body(UserTokenInfoDTO.class);

            logger.warn("Token info: " + tokenInfo);

            if (tokenInfo == null) {
                return null;
            }
            
            User user = new User();
            user.setId(tokenInfo.id());
            user.setEmail(tokenInfo.email());
            user.setRoles(tokenInfo.roles());
            
            return user;
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            logger.error("Auth service returned client error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
            throw new AuthServiceClientException(
                "Auth service returned client error", 
                ex.getStatusCode(), 
                ex.getResponseBodyAsString()
            );
        } catch (org.springframework.web.client.HttpServerErrorException ex) {
            logger.error("Auth service returned server error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
            throw new AuthServiceClientException(
                "Auth service returned server error", 
                ex.getStatusCode(), 
                ex.getResponseBodyAsString()
            );
        } catch (Exception e) {
            logger.error("Auth service error: " + e.getMessage());
            throw new AuthenticationServiceUnavailable("Auth service unavailable");
        }
    }

    private User fallback(String token, Exception ex) {
        logger.error(ex.getMessage());
        throw new AuthenticationServiceUnavailable("Authentication service is currently unavailable");
    }
}

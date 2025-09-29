package org.example.apigateway.validation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.example.apigateway.users.Role;
import org.example.apigateway.users.User;
import org.example.apigateway.validation.records.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.ContentCachingRequestWrapper;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.time.Duration;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    private WebClient authServiceWebClient;
    @Autowired
    private AuthService authService;

    @Value("${INTERNAL_SERVICE_SECRET}")
    private String INTERNAL_SERVICE_SECRET;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (isAuthEndpoint(requestURI)) {
            ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
            handleAuthEndpoint(cachedRequest, response, requestURI);

            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);

            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);

            return;
        }

        final String token = authHeader.substring(7);

        try {
            User user = authService.validateTokenWithAuthService(token);

            if (user == null) {
                unauthorizedResponse(response);

                return;
            }

            setSecurityContext(user);
            setCustomHeaders(request, user);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Authentication error", e);
            unauthorizedResponse(response);
        }
    }

    private boolean isAuthEndpoint(String uri) {
        return uri.contains("/api/v1/auth/login") ||
                uri.contains("/api/v1/auth/register");
    }

    private void handleAuthEndpoint(ContentCachingRequestWrapper request,
                                    HttpServletResponse response,
                                    String requestURI) throws IOException {
        try {
            String requestBody = request.getContentAsString();
            String authPath = requestURI.contains("/login") ?
                    "/api/v1/auth/login" : "/api/v1/auth/register";

            AuthResponse authResponse = authServiceWebClient
                    .post()
                    .uri(authPath)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(AuthResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();

            if (authResponse != null && authResponse.user() != null) {
                setSecurityContext(authResponse.user());

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");

                String responseBody = String.format("""
                        {
                            "token": "%s",
                            "user": {
                                "id": "%s",
                                "email": "%s",
                                "roles": [%s]
                            }
                        }
                        """,
                        authResponse.token(),
                        authResponse.user().getId(),
                        authResponse.user().getEmail(),
                        authResponse.user().getRoles().stream()
                                .map(role -> "\"" + role.name() + "\"")
                                .collect(Collectors.joining(", "))
                );

                response.getWriter().write(responseBody);
                response.getWriter().flush();
            } else {
                unauthorizedResponse(response);
            }

        } catch (Exception e) {
            logger.error("Error handling auth endpoint", e);
            unauthorizedResponse(response);
        }
    }

    private void setSecurityContext(User user) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setCustomHeaders(HttpServletRequest request, User user) {
        request.setAttribute("X-User-ID", user.getId().toString());
        request.setAttribute("X-User-ROLES", user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.joining(", ")));
        request.setAttribute("X-User-EMAIL", user.getEmail());
        request.setAttribute("X-Service-TOKEN", INTERNAL_SERVICE_SECRET);
    }

    private void unauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        String body = """
                {
                    "error": "Invalid credentials or token"
                }
                """;
        response.getWriter().write(body);
        response.getWriter().flush();
    }
}
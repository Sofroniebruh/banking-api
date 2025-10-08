package org.example.apigateway.validation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.example.apigateway.config.HeaderMapRequestWrapper;
import org.example.apigateway.layers.GatewayService;
import org.example.apigateway.users.User;
import org.example.apigateway.validation.records.UserDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Duration;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final RestClient restClient;
    private final AuthService authService;
    private final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final GatewayService gatewayService;
    @Value("${ACCESS_TOKEN_TTL}")
    private Duration ACCESS_TOKEN_TTL;
    @Value("${REFRESH_TOKEN_TTL}")
    private Duration REFRESH_TOKEN_TTL;
    @Value("${BASE_AUTH_SERVICE_URL}")
    private String BASE_AUTH_SERVICE_URL;
    @Value("${INTERNAL_SERVICE_SECRET}")
    private String INTERNAL_SERVICE_SECRET;

    public JwtAuthFilter(@Qualifier("downstreamRestClient") RestClient restClient,
                         AuthService authService,
                         GatewayService gatewayService) {
        this.restClient = restClient;
        this.authService = authService;
        this.gatewayService = gatewayService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        HeaderMapRequestWrapper wrapper = new HeaderMapRequestWrapper(request);
        wrapper.addHeader("X-Internal-Request", INTERNAL_SERVICE_SECRET);

        if (isAuthEndpoint(requestURI)) {
            ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
            handleAuthEndpoint(cachedRequest, response, requestURI);

            return;
        }

        if (isActuatorEndpoint(requestURI)) {
            logger.warn("Endpoint {} is actuator", requestURI);
            filterChain.doFilter(wrapper, response);

            return;
        }

        final Cookie[] cookies = request.getCookies();

        if (cookies == null || getAuthTokenFromCookie(cookies) == null) {
            filterChain.doFilter(request, response);

            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);

            return;
        }

        final String token = getAuthTokenFromCookie(cookies);

        try {
            User user = authService.validateTokenWithAuthService(token);

            if (user == null) {
                logger.warn("Invalid token");
                unauthorizedResponse(response);

                return;
            }

            setSecurityContext(user);
            HeaderMapRequestWrapper wrappedRequest = setCustomHeaders(wrapper, user);
            filterChain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            internalError(response, e);
        }
    }

    private boolean isAuthEndpoint(String uri) {
        return uri.contains("/api/v1/auth/login") ||
                uri.contains("/api/v1/auth/registration");
    }

    private boolean isActuatorEndpoint(String uri) {
        return uri.startsWith("/actuator");
    }

    private String getAuthTokenFromCookie(Cookie[] cookies) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("access_token")) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void handleAuthEndpoint(ContentCachingRequestWrapper request,
                                    HttpServletResponse response,
                                    String requestURI) throws IOException {
        try {
            String requestBody = new String(request.getContentAsByteArray());
            if (requestBody.isEmpty()) {
                requestBody = request.getReader().lines()
                        .collect(Collectors.joining(System.lineSeparator()));
            }
            
            String authPath = requestURI.contains("/login") ?
                    "/api/v1/auth/login" : "/api/v1/auth/registration";

            logger.info("Forwarding to: " + BASE_AUTH_SERVICE_URL + authPath);
            logger.info("Request body: " + requestBody);

            try {
                UserDTO authResponse = restClient
                        .post()
                        .uri(BASE_AUTH_SERVICE_URL + authPath)
                        .header("Content-Type", "application/json")
                        .header("X-Internal-Request", INTERNAL_SERVICE_SECRET)
                        .body(requestBody)
                        .retrieve()
                        .body(UserDTO.class);

                logger.info("Response body: " + authResponse);

                if (authResponse != null && authResponse.id() != null) {
                    User user = new User();

                    user.setId(authResponse.id());
                    user.setName(authResponse.name());
                    user.setEmail(authResponse.email());
                    user.setRoles(authResponse.roles());

                    setSecurityContext(user);

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");

                    String responseBody = String.format("""
                            {
                                "user": {
                                    "id": "%s",
                                    "email": "%s",
                                    "roles": [%s]
                                }
                            }
                            """,
                            authResponse.id(),
                            authResponse.email(),
                            authResponse.roles().stream()
                                    .map(role -> "\"" + role + "\"")
                                    .collect(Collectors.joining(", "))
                    );

                    Cookie refreshTokenCookie = gatewayService.createCookie(
                            "refresh_token",
                            authResponse.refreshToken(),
                            (int) REFRESH_TOKEN_TTL.toSeconds(),
                            true,
                            "/",
                            true
                    );
                    Cookie accessTokenCookie = gatewayService.createCookie(
                            "access_token",
                            authResponse.accessToken(),
                            (int) ACCESS_TOKEN_TTL.toSeconds(),
                            true,
                            "/",
                            true
                    );

                    response.addCookie(refreshTokenCookie);
                    response.addCookie(accessTokenCookie);

                    response.getWriter().write(responseBody);
                    response.getWriter().flush();
                } else {
                    unauthorizedResponse(response);
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                logger.error("Auth service returned client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                response.setStatus(e.getStatusCode().value());
                response.setContentType("application/json");
                response.getWriter().write(e.getResponseBodyAsString());
                response.getWriter().flush();
            }

        } catch (Exception e) {
            internalError(response, e);
        }
    }

    private void setSecurityContext(User user) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private HeaderMapRequestWrapper setCustomHeaders(HeaderMapRequestWrapper wrapper, User user) {
        Cookie[] cookies = wrapper.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                wrapper.addHeader(cookie.getName(), cookie.getValue());
            }
        }

        wrapper.addHeader("X-User-ID", user.getId().toString());
        wrapper.addHeader("X-User-Roles", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")));
        wrapper.addHeader("X-User-Email", user.getEmail());

        return wrapper;
    }

    private void internalError(HttpServletResponse response, Exception e) throws IOException {
        logger.error("Error handling auth endpoint" + e.getCause() + " - " + e.getStackTrace());
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Authentication service error: " + e.getMessage() + "\"}");
        response.getWriter().flush();
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
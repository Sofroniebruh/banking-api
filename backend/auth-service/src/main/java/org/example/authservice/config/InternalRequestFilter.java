package org.example.authservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalRequestFilter extends OncePerRequestFilter {
    @Value("${INTERNAL_SERVICE_SECRET}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();

        if (requestPath.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String internalHeader = request.getHeader("X-Internal-Request");

        if (internalHeader == null || !internalHeader.equals(internalSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Access denied\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
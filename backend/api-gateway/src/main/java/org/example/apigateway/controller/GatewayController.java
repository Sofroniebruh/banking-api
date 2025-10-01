package org.example.apigateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequiredArgsConstructor
public class GatewayController {

    @Qualifier("downstreamRestClient")
    private final RestClient downstreamRestClient;

    @RequestMapping("/api/v1/users/**")
    public ResponseEntity<String> proxyToUserService(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        return proxyToService(request, body, "http://user-service:8082");
    }

    @RequestMapping("/api/v1/accounts/**")
    public ResponseEntity<String> proxyToAccountService(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        return proxyToService(request, body, "http://account-service:8083");
    }

    private ResponseEntity<String> proxyToService(HttpServletRequest request, String body, String serviceUrl) {
        var requestSpec = downstreamRestClient
                .method(org.springframework.http.HttpMethod.valueOf(request.getMethod()))
                .uri(serviceUrl + request.getRequestURI());
        
        if (body != null && !body.isEmpty()) {
            requestSpec.body(body);
        }
        
        return requestSpec.retrieve().toEntity(String.class);
    }
}
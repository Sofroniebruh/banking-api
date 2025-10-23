package org.example.apigateway.layers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
public class GatewayController {
    private final ObjectMapper objectMapper;
    private final GatewayService gatewayService;
    private final Duration ACCESS_TOKEN_TTL;
    private final Duration REFRESH_TOKEN_TTL;

    public GatewayController(ObjectMapper objectMapper,
                             GatewayService gatewayService,
                             @Value("${ACCESS_TOKEN_TTL}") Duration ACCESS_TOKEN_TTL,
                             @Value("${REFRESH_TOKEN_TTL}") Duration REFRESH_TOKEN_TTL) {
        this.objectMapper = objectMapper;
        this.gatewayService = gatewayService;
        this.ACCESS_TOKEN_TTL = ACCESS_TOKEN_TTL;
        this.REFRESH_TOKEN_TTL = REFRESH_TOKEN_TTL;
    }

    @RequestMapping("/api/v1/auth/**")
    public ResponseEntity<String> proxyToAuthService(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody(required = false) Object body) throws JsonProcessingException {
        ResponseEntity<String> serviceResponse = gatewayService.proxyToService(request, body, "http://auth-service:8081");
        String responseBody = serviceResponse.getBody();

        if (responseBody != null) {
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {});

            if (responseMap.containsKey("accessToken") && responseMap.containsKey("refreshToken")) {
                String accessToken = responseMap.get("accessToken").toString();
                String refreshToken = responseMap.get("refreshToken").toString();

                Cookie accessTokenCookie = gatewayService.createCookie(
                        "access_token",
                        accessToken,
                        (int) ACCESS_TOKEN_TTL.toSeconds(),
                        true,
                        "/",
                        true);
                Cookie refreshTokenCookie = gatewayService.createCookie(
                        "refresh_token",
                        refreshToken,
                        (int) REFRESH_TOKEN_TTL.toSeconds(),
                        true,
                        "/",
                        true);

                response.addCookie(accessTokenCookie);
                response.addCookie(refreshTokenCookie);
            }
        }

        return ResponseEntity
                .status(serviceResponse.getStatusCode())
                .headers(serviceResponse.getHeaders())
                .body(responseBody);
    }

    @RequestMapping("/api/v1/users/**")
    public ResponseEntity<String> proxyToUserService(
            HttpServletRequest request,
            @RequestBody(required = false) Object body) {
        return gatewayService.proxyToService(request, body, "http://user-service:8082");
    }

    @RequestMapping("/actuator/auth-service/**")
    public ResponseEntity<String> proxyToAuthServiceActuator(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        return gatewayService.proxyToServiceWithPathRewrite(request, body, "http://auth-service:8081", "/actuator/auth-service");
    }

    @RequestMapping("/actuator/user-service/**")
    public ResponseEntity<String> proxyToUserServiceActuator(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        return gatewayService.proxyToServiceWithPathRewrite(request, body, "http://user-service:8082", "/actuator/user-service");
    }

//    @RequestMapping("/api/v1/accounts/**")
//    public ResponseEntity<Object> proxyToAccountService(
//            HttpServletRequest request,
//            @RequestBody(required = false) String body) throws JsonProcessingException {
//        ResponseEntity<Object> response =  proxyToService(request, body, "http://account-service:8083");
//
//        Object responseBody = response.getBody();
//
//        if (responseBody != null) {
//            String responseJson = objectMapper.writeValueAsString(responseBody);
//            Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
//
//
//        }
//
//    }
}
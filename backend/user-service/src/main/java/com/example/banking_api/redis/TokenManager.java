package com.example.banking_api.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TokenManager {
    private final RedisService redisService;
    
    private static final String ACTIVE_TOKEN_PREFIX = "reset_tokens:active:";
    private static final String USED_TOKEN_PREFIX = "reset_tokens:used:";
    
    public void storeActiveToken(String email, String token, long timeout, TimeUnit unit) {
        String hashedEmail = hashEmail(email);
        String key = ACTIVE_TOKEN_PREFIX + hashedEmail;
        redisService.setValueWithExpiry(key, token, timeout, unit);
    }
    
    public String getActiveToken(String email) {
        String hashedEmail = hashEmail(email);
        String key = ACTIVE_TOKEN_PREFIX + hashedEmail;
        return (String) redisService.getValue(key);
    }
    
    public void markTokenAsUsed(String email, String token, long timeout, TimeUnit unit) {
        String hashedEmail = hashEmail(email);
        String activeKey = ACTIVE_TOKEN_PREFIX + hashedEmail;
        String usedKey = USED_TOKEN_PREFIX + hashedEmail;
        
        redisService.deleteKey(activeKey);
        redisService.setValueWithExpiry(usedKey, token, timeout, unit);
    }
    
    public boolean isTokenUsed(String email) {
        String hashedEmail = hashEmail(email);
        String key = USED_TOKEN_PREFIX + hashedEmail;
        return redisService.hasKey(key);
    }
    
    public void deleteActiveToken(String email) {
        String hashedEmail = hashEmail(email);
        String key = ACTIVE_TOKEN_PREFIX + hashedEmail;
        redisService.deleteKey(key);
    }
    
    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
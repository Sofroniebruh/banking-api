package org.example.transactionsservice.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionsservice.configs.exceptions.RedisConnectionException;
import org.example.transactionsservice.configs.exceptions.RedisOperationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public Object getHashValue(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while getting hash value for key: {}, field: {}", key, field, e);

            throw new RedisConnectionException("Failed to connect to Redis while getting hash value");
        } catch (Exception e) {
            log.error("Redis operation failed while getting hash value for key: {}, field: {}", key, field, e);

            throw new RedisOperationException("Failed to get hash value from Redis");
        }
    }
}

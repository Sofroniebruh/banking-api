package org.example.accountservice.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountservice.configs.exceptions.RedisConnectionException;
import org.example.accountservice.configs.exceptions.RedisOperationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting value for key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while setting value");
        } catch (Exception e) {
            log.error("Redis operation failed while setting value for key: {}", key, e);

            throw new RedisOperationException("Failed to set value in Redis");
        }
    }

    public void setHash(String key, Map<String, Object> hashMap) {
        try {
            redisTemplate.opsForHash().putAll(key, hashMap);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting hash for key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while setting hash");
        } catch (Exception e) {
            log.error("Redis operation failed while setting hash for key: {}", key, e);

            throw new RedisOperationException("Failed to set hash in Redis");
        }
    }

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

    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting value for key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while setting value");
        } catch (Exception e) {
            log.error("Redis operation failed while setting value for key: {}", key, e);
            throw new RedisOperationException("Failed to set value in Redis");
        }
    }

    public void delValue(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting value for key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while setting value");
        } catch (Exception e) {
            log.error("Redis operation failed while setting value for key: {}", key, e);
            throw new RedisOperationException("Failed to set value in Redis");
        }
    }
}

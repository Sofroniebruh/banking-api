package com.example.banking_api.redis;

import com.example.banking_api.redis.exceptions.RedisConnectionException;
import com.example.banking_api.redis.exceptions.RedisOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting value for key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while setting value", e);
        } catch (Exception e) {
            log.error("Redis operation failed while setting value for key: {}", key, e);
            throw new RedisOperationException("Failed to set value in Redis", e);
        }
    }

    public void setValueWithExpiry(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting value with expiry for key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while setting value with expiry", e);
        } catch (Exception e) {
            log.error("Redis operation failed while setting value with expiry for key: {}", key, e);

            throw new RedisOperationException("Failed to set value with expiry in Redis", e);
        }
    }

    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while getting value for key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while getting value", e);
        } catch (Exception e) {
            log.error("Redis operation failed while getting value for key: {}", key, e);

            throw new RedisOperationException("Failed to get value from Redis", e);
        }
    }

    public void deleteKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while deleting key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while deleting key", e);
        } catch (Exception e) {
            log.error("Redis operation failed while deleting key: {}", key, e);

            throw new RedisOperationException("Failed to delete key from Redis", e);
        }
    }

    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while checking key existence: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while checking key existence", e);
        } catch (Exception e) {
            log.error("Redis operation failed while checking key existence: {}", key, e);

            throw new RedisOperationException("Failed to check key existence in Redis", e);
        }
    }

    public void setExpire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting expiry for key: {}", key, e);

            throw new RedisConnectionException("Failed to connect to Redis while setting expiry", e);
        } catch (Exception e) {
            log.error("Redis operation failed while setting expiry for key: {}", key, e);

            throw new RedisOperationException("Failed to set expiry in Redis", e);
        }
    }
}

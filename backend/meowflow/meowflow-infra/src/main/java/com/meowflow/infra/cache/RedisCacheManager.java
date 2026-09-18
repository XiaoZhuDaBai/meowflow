package com.meowflow.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存管理器
 * 提供通用的缓存操作能力
 *
 * <p>本类不再使用 {@code @Component} 注册，改为在 {@code RedisConfig} 中通过
 * {@code @Bean} 显式注册。这样不依赖业务模块的 ComponentScan 范围（避免类似
 * template 模块只扫 template/common 时找不到 bean 的问题）。
 */
@Slf4j
@RequiredArgsConstructor
public class RedisCacheManager {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CACHE_PREFIX = "meowflow:cache:";
    private static final String LOCK_PREFIX = "meowflow:lock:";
    
    /**
     * 默认过期时间：5分钟
     */
    private static final Duration DEFAULT_EXPIRE = Duration.ofMinutes(5);
    
    /**
     * 短过期时间：1分钟（高频更新数据）
     */
    private static final Duration SHORT_EXPIRE = Duration.ofMinutes(1);
    
    /**
     * 长过期时间：1小时（低频变更数据）
     */
    private static final Duration LONG_EXPIRE = Duration.ofHours(1);

    /**
     * 存入缓存
     */
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_EXPIRE);
    }

    /**
     * 存入缓存（指定过期时间）
     */
    public <T> void put(String key, T value, Duration expire) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(CACHE_PREFIX + key, json, expire);
            log.debug("Cached key: {}, expire: {}", key, expire);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize value for key: {}", key, e);
        }
    }

    /**
     * 获取缓存
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, type));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize value for key: {}", key, e);
        }
        return Optional.empty();
    }

    /**
     * 获取缓存（泛型支持）
     */
    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, typeRef));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize value for key: {}", key, e);
        }
        return Optional.empty();
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(CACHE_PREFIX + key);
        log.debug("Deleted key: {}", key);
    }

    /**
     * 删除缓存（支持通配符）
     */
    public void deleteByPattern(String pattern) {
        var keys = redisTemplate.keys(CACHE_PREFIX + pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Deleted {} keys matching pattern: {}", keys.size(), pattern);
        }
    }

    /**
     * 缓存是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CACHE_PREFIX + key));
    }

    /**
     * 设置过期时间
     */
    public void expire(String key, Duration duration) {
        redisTemplate.expire(CACHE_PREFIX + key, duration.toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 获取剩余过期时间
     */
    public long getExpire(String key) {
        Long ttl = redisTemplate.getExpire(CACHE_PREFIX + key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }

    /**
     * 尝试获取锁
     */
    public boolean tryLock(String key, Duration timeout) {
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(LOCK_PREFIX + key, "locked", timeout);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        redisTemplate.delete(LOCK_PREFIX + key);
    }

    /**
     * 获取或加载（缓存穿透防护）
     */
    public <T> T getOrLoad(String key, java.util.function.Supplier<T> loader, Duration expire) {
        Optional<T> cached = get(key, new com.fasterxml.jackson.core.type.TypeReference<T>() {});
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // 尝试获取锁防止缓存击穿
        if (tryLock(key, Duration.ofSeconds(10))) {
            try {
                // 双重检查
                cached = get(key, new com.fasterxml.jackson.core.type.TypeReference<T>() {});
                if (cached.isPresent()) {
                    return cached.get();
                }
                
                // 加载数据
                T value = loader.get();
                if (value != null) {
                    put(key, value, expire);
                }
                return value;
            } finally {
                unlock(key);
            }
        } else {
            // 未获取到锁，直接加载（可能重复计算，但避免阻塞）
            return loader.get();
        }
    }

    // ==================== 快捷方法 ====================

    /**
     * 短期缓存（1分钟）
     */
    public <T> void putShort(String key, T value) {
        put(key, value, SHORT_EXPIRE);
    }

    /**
     * 长期缓存（1小时）
     */
    public <T> void putLong(String key, T value) {
        put(key, value, LONG_EXPIRE);
    }

    /**
     * 永久缓存（需要手动删除）
     */
    public <T> void putPermanent(String key, T value) {
        put(key, value, Duration.ZERO);
    }
}

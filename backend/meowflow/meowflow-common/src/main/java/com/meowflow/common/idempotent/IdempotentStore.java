package com.meowflow.common.idempotent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 幂等存储接口（Redis 实现）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentStore {

    private static final String IDEMPOTENT_PREFIX = "idempotent:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取幂等锁
     *
     * @param key   幂等键
     * @param value 幂等值
     * @param ttl   过期时间（秒）
     * @return true 如果成功获取锁
     */
    public boolean tryLock(String key, String value, int ttl) {
        String redisKey = IDEMPOTENT_PREFIX + key;
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, value, ttl, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放幂等锁
     *
     * @param key   幂等键
     * @param value 幂等值
     */
    public void unlock(String key, String value) {
        String redisKey = IDEMPOTENT_PREFIX + key;
        String storedValue = stringRedisTemplate.opsForValue().get(redisKey);
        if (value.equals(storedValue)) {
            stringRedisTemplate.delete(redisKey);
        }
    }

    /**
     * 检查幂等键是否存在
     *
     * @param key 幂等键
     * @return true 如果存在
     */
    public boolean exists(String key) {
        String redisKey = IDEMPOTENT_PREFIX + key;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey));
    }

    /**
     * 获取幂等键的值
     *
     * @param key 幂等键
     * @return 值，如果不存在返回 null
     */
    public String get(String key) {
        String redisKey = IDEMPOTENT_PREFIX + key;
        return stringRedisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 设置幂等键
     *
     * @param key        幂等键
     * @param value      幂等值
     * @param expireSeconds 过期时间（秒）
     */
    public void set(String key, String value, int expireSeconds) {
        String redisKey = IDEMPOTENT_PREFIX + key;
        stringRedisTemplate.opsForValue().set(redisKey, value, expireSeconds, TimeUnit.SECONDS);
    }
}

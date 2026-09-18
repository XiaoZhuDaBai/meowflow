package com.meowflow.infra.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模型健康状态存储（带 Redis 缓存）
 * 借鉴 ragent 的 ModelHealthStore 设计
 */
@Slf4j
@Component
public class ModelHealthStore {

    private static final String HEALTH_KEY_PREFIX = "meowflow:model:health:";
    private static final String FAILURE_KEY_PREFIX = "meowflow:model:failure:";
    private static final Duration HEALTH_EXPIRE = Duration.ofMinutes(5);
    private static final Duration FAILURE_EXPIRE = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;
    
    // 本地缓存：避免频繁访问 Redis
    private final Map<String, HealthState> localCache = new ConcurrentHashMap<>();
    
    // 连续失败计数器
    private final Map<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();
    
    // 最后成功时间
    private final Map<String, AtomicLong> lastSuccessTime = new ConcurrentHashMap<>();

    public ModelHealthStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查是否允许调用该模型
     */
    public boolean allowCall(String modelId) {
        HealthState state = getHealthState(modelId);
        if (state == HealthState.UNHEALTHY) {
            return false;
        }
        if (state == HealthState.RECOVERING) {
            // 恢复期中，每3次允许1次探测
            return Math.random() < 0.33;
        }
        return true;
    }

    /**
     * 标记调用成功
     */
    public void markSuccess(String modelId) {
        localCache.put(modelId, HealthState.HEALTHY);
        redisTemplate.opsForValue().set(
            HEALTH_KEY_PREFIX + modelId,
            HealthState.HEALTHY.name(),
            HEALTH_EXPIRE
        );
        
        // 重置失败计数
        AtomicInteger counter = failureCounters.get(modelId);
        if (counter != null) {
            counter.set(0);
        }
        
        lastSuccessTime.put(modelId, new AtomicLong(System.currentTimeMillis()));
        
        // 清除失败记录
        redisTemplate.delete(FAILURE_KEY_PREFIX + modelId);
        
        log.debug("Model {} marked healthy", modelId);
    }

    /**
     * 标记调用失败
     */
    public void markFailure(String modelId) {
        AtomicInteger counter = failureCounters.computeIfAbsent(
            modelId, k -> new AtomicInteger(0)
        );
        int failures = counter.incrementAndGet();
        
        // 累计失败 3 次，标记为不健康
        if (failures >= 3) {
            localCache.put(modelId, HealthState.UNHEALTHY);
            redisTemplate.opsForValue().set(
                HEALTH_KEY_PREFIX + modelId,
                HealthState.UNHEALTHY.name(),
                HEALTH_EXPIRE
            );
            redisTemplate.opsForValue().set(
                FAILURE_KEY_PREFIX + modelId,
                String.valueOf(failures),
                FAILURE_EXPIRE
            );
            log.warn("Model {} marked unhealthy after {} failures", modelId, failures);
        }
    }

    /**
     * 获取健康状态
     */
    public HealthState getHealthState(String modelId) {
        // 先查本地缓存
        HealthState cached = localCache.get(modelId);
        if (cached != null && cached != HealthState.UNKNOWN) {
            return cached;
        }
        
        // 再查 Redis
        try {
            String state = redisTemplate.opsForValue().get(HEALTH_KEY_PREFIX + modelId);
            if (state != null) {
                HealthState healthState = HealthState.valueOf(state);
                localCache.put(modelId, healthState);
                return healthState;
            }
        } catch (Exception e) {
            log.debug("Failed to get health state from Redis: {}", e.getMessage());
        }
        
        return HealthState.UNKNOWN;
    }

    /**
     * 获取失败次数
     */
    public int getFailureCount(String modelId) {
        AtomicInteger counter = failureCounters.get(modelId);
        if (counter != null) {
            return counter.get();
        }
        
        try {
            String failures = redisTemplate.opsForValue().get(FAILURE_KEY_PREFIX + modelId);
            if (failures != null) {
                return Integer.parseInt(failures);
            }
        } catch (Exception e) {
            log.debug("Failed to get failure count from Redis: {}", e.getMessage());
        }
        
        return 0;
    }

    /**
     * 手动恢复模型健康状态
     */
    public void recover(String modelId) {
        localCache.remove(modelId);
        failureCounters.remove(modelId);
        redisTemplate.delete(HEALTH_KEY_PREFIX + modelId);
        redisTemplate.delete(FAILURE_KEY_PREFIX + modelId);
        log.info("Model {} manually recovered", modelId);
    }

    /**
     * 获取所有模型健康状态
     */
    public Map<String, HealthState> getAllHealthStates() {
        return Map.copyOf(localCache);
    }

    /**
     * 健康状态枚举
     */
    public enum HealthState {
        /** 未知状态 */
        UNKNOWN,
        /** 健康 */
        HEALTHY,
        /** 不健康（连续失败） */
        UNHEALTHY,
        /** 恢复中（试探性允许） */
        RECOVERING
    }
}

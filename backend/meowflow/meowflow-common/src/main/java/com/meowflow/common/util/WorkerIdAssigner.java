package com.meowflow.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * WorkerId 分配器
 *
 * <p>使用 Redis 分配雪花 ID 生成器的 workerId，每次启动时从 Redis 获取可用 workerId，重启后自动释放。
 * 若 Redis 不可用，则使用本地主机名哈希作为 fallback，确保应用可以启动。
 *
 * <p>读写全部走 {@link StringRedisTemplate}，避免与通用 {@code RedisTemplate<String, Object>}
 * 的 Jackson 序列化器产生格式不一致问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerIdAssigner implements ApplicationRunner {

    private static final String WORKER_ID_KEY = "snowflake:worker:current";
    private static final String WORKER_ID_SET_KEY = "snowflake:worker:allocated";
    private static final int MAX_WORKER_ID = 1023;

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${snowflake.worker-id:0}")
    private int configuredWorkerId;

    @Value("${snowflake.datacenter-id:0}")
    private int configuredDatacenterId;

    private static volatile Long assignedWorkerId;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (configuredWorkerId > 0) {
            assignedWorkerId = (long) configuredWorkerId;
            log.info("使用配置的 workerId: {}", assignedWorkerId);
            return;
        }

        try {
            assignWorkerId();
        } catch (Exception e) {
            log.warn("Redis 不可用，使用本地 fallback 策略分配 workerId", e);
            assignFallbackWorkerId();
        }
    }

    /**
     * 分配 workerId（依赖 Redis）
     */
    private void assignWorkerId() {
        String currentValue = stringRedisTemplate.opsForValue().get(WORKER_ID_KEY);

        if (currentValue != null && !currentValue.isEmpty()) {
            String trimmed = currentValue.trim();
            // 兼容旧 key 中可能存在的 JSON 引号包裹 (例如 "1")
            if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            try {
                assignedWorkerId = Long.parseLong(trimmed);
                log.info("复用之前的 workerId: {}", assignedWorkerId);
                return;
            } catch (NumberFormatException e) {
                log.warn("redis 中已有 workerId 值无法解析: '{}', 将重新分配", currentValue);
            }
        }

        Long incremented = stringRedisTemplate.opsForValue().increment(WORKER_ID_SET_KEY);
        long base = incremented != null ? incremented : System.currentTimeMillis();
        long workerId = base % (MAX_WORKER_ID + 1);
        stringRedisTemplate.opsForValue().set(WORKER_ID_KEY, String.valueOf(workerId));
        assignedWorkerId = workerId;

        log.info("分配新的 workerId: {}, datacenterId: {}", workerId, configuredDatacenterId);
    }

    /**
     * Fallback 策略：基于主机名哈希分配 workerId
     *
     * <p>当 Redis 不可用时使用此策略，确保应用可以启动。
     * 注意：多实例部署时可能产生 workerId 冲突，仅作为降级方案。
     */
    private void assignFallbackWorkerId() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String hostName = localHost.getHostName();
            int hash = hashCode(hostName);
            long workerId = Math.abs(hash % (MAX_WORKER_ID + 1));
            assignedWorkerId = workerId;
            log.warn("Redis 不可用，使用主机名 '{}' 哈希分配 fallback workerId: {}，多实例部署可能存在冲突风险", hostName, workerId);
        } catch (Exception e) {
            // 最后 fallback：使用随机数
            long workerId = (long) (Math.random() * (MAX_WORKER_ID + 1));
            assignedWorkerId = workerId;
            log.warn("无法获取主机名，使用随机 workerId: {}，多实例部署存在 ID 冲突风险", workerId);
        }
    }

    /**
     * 计算字符串的哈希码（与 String.hashCode 一致）
     */
    private int hashCode(String str) {
        int h = 0;
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            h = 31 * h + (b & 0xff);
        }
        return h;
    }

    /**
     * 获取分配到的 workerId
     *
     * @return workerId
     */
    public static long getWorkerId() {
        if (assignedWorkerId == null) {
            throw new IllegalStateException("WorkerId 尚未分配，请确保 WorkerIdAssigner 已初始化");
        }
        return assignedWorkerId;
    }

    /**
     * 尝试获取 workerId（可返回默认值）
     *
     * @param defaultValue 默认值
     * @return workerId
     */
    public static long getWorkerIdOrDefault(long defaultValue) {
        return assignedWorkerId != null ? assignedWorkerId : defaultValue;
    }
}

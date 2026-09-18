package com.meowflow.template.counter;

import com.meowflow.template.cache.TemplateCacheKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 模板使用计数（use_count）的 Redis 计数器。
 *
 * <p>写路径：{@link #increment(Long)} 调用 INCR（O(1)），无 DB 锁竞争。
 * <p>读路径：{@link #peek(Long)} 返回当前累计值（不消费）。
 * <p>刷路径：{@link #getAndReset(Long)} 由 {@code UseCountFlushTask} 周期调用，原子地取走并清零。
 *
 * <p>注意：
 * <ul>
 *   <li>key 不设 TTL，避免冷数据被自动清理丢失计数。重启 / 刷失败兜底由运维补偿。</li>
 *   <li>内置模板（{@code builtin:} 前缀）不走本组件，避免污染 Redis 计数空间。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UseCountCounter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 模板被使用一次，计数 +1。
     */
    public long increment(Long templateId) {
        try {
            Long v = stringRedisTemplate.opsForValue().increment(TemplateCacheKeys.useCountKey(templateId), 1L);
            return v == null ? 0L : v;
        } catch (Exception e) {
            // Redis 不可用时降级：返回 0（不计），调用方走 DB fallback 由 UseCountFlushTask 异步补偿即可。
            // 这里选择 fail-soft：宁可少计一次，不能阻塞用户操作。
            log.warn("Failed to increment useCount in redis for templateId={}", templateId, e);
            return 0L;
        }
    }

    /**
     * 仅查询当前值，不消费。
     */
    public Long peek(Long templateId) {
        try {
            String v = stringRedisTemplate.opsForValue().get(TemplateCacheKeys.useCountKey(templateId));
            return v == null ? null : Long.parseLong(v);
        } catch (Exception e) {
            log.warn("Failed to peek useCount in redis for templateId={}", templateId, e);
            return null;
        }
    }

    /**
     * 原子地读取并清零，返回消费掉的值；如果 key 不存在返回 null。
     */
    public Long getAndReset(Long templateId) {
        try {
            return getAndDelete(TemplateCacheKeys.useCountKey(templateId));
        } catch (Exception e) {
            log.warn("Failed to getAndReset useCount for templateId={}", templateId, e);
            return null;
        }
    }

    private Long getAndDelete(String fullKey) {
        String v = stringRedisTemplate.opsForValue().get(fullKey);
        if (v == null) return null;
        stringRedisTemplate.delete(fullKey);
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}

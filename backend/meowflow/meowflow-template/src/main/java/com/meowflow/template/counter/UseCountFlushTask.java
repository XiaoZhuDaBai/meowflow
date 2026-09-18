package com.meowflow.template.counter;

import com.meowflow.infra.cache.RedisCacheManager;
import com.meowflow.template.cache.TemplateCacheKeys;
import com.meowflow.template.cache.TemplateCacheSupport;
import com.meowflow.template.entity.TemplateUseCountDelta;
import com.meowflow.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * use_count 定时 flush 任务。
 *
 * <p>每 30 秒从 Redis 把计数器取走，批量加到 DB。
 * <p>多实例部署下通过分布式锁保证只有 1 个节点真正执行。
 * <p>flush 成功后失效 search 缓存与受影响模板的 detail 缓存。
 *
 * <p>失败语义：
 * <ul>
 *   <li>未拿到锁：静默返回。</li>
 *   <li>DB 更新失败：把已消费的 delta 重新 push 回 Redis（按原值），下一周期重试。</li>
 *   <li>Redis 不可用：直接返回，DB 数据暂时落后于真实使用量。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UseCountFlushTask {

    private static final String FLUSH_LOCK_KEY = "tpl:usecnt:flush";
    private static final Duration FLUSH_LOCK_TTL = Duration.ofSeconds(25);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisCacheManager cacheManager;
    private final TemplateRepository templateRepository;
    private final TemplateCacheSupport cacheSupport;

    @Scheduled(fixedDelay = 30_000L, initialDelay = 30_000L)
    public void flush() {
        // 多实例保护：只有抢到锁的节点执行
        boolean locked;
        try {
            locked = cacheManager.tryLock(FLUSH_LOCK_KEY, FLUSH_LOCK_TTL);
        } catch (Exception e) {
            log.warn("Failed to acquire flush lock, skip", e);
            return;
        }
        if (!locked) return;

        try {
            Set<String> keys;
            try {
                keys = stringRedisTemplate.keys("meowflow:cache:" + TemplateCacheKeys.USE_COUNT_PATTERN);
            } catch (Exception e) {
                log.warn("Failed to SCAN use_count keys", e);
                return;
            }
            if (keys == null || keys.isEmpty()) return;

            List<TemplateUseCountDelta> deltas = new ArrayList<>();
            for (String fullKey : keys) {
                Long id = parseId(fullKey);
                if (id == null) continue;
                String v = stringRedisTemplate.opsForValue().get(fullKey);
                if (v == null) continue;
                stringRedisTemplate.delete(fullKey); // 原子：先取值再删除，失败也仅丢失这一轮
                try {
                    long delta = Long.parseLong(v);
                    if (delta > 0) deltas.add(new TemplateUseCountDelta(id, delta));
                } catch (NumberFormatException ignored) {
                    // ignore malformed value
                }
            }

            if (deltas.isEmpty()) return;

            try {
                templateRepository.batchIncrementUseCount(deltas);
                log.info("Flushed use_count: {} templates, total +{}",
                        deltas.size(),
                        deltas.stream().mapToLong(TemplateUseCountDelta::delta).sum());
                // 失效缓存
                List<Long> ids = deltas.stream().map(TemplateUseCountDelta::id).toList();
                cacheSupport.evictAfterUseCountFlush(ids);
            } catch (Exception e) {
                // DB 失败：把已消费的 delta 重新 push 回 Redis，下一周期重试
                log.error("Failed to batch increment use_count, restore counters to redis", e);
                restore(deltas);
            }
        } finally {
            try {
                cacheManager.unlock(FLUSH_LOCK_KEY);
            } catch (Exception e) {
                log.warn("Failed to release flush lock", e);
            }
        }
    }

    /**
     * 手动 flush 接口（管理后台或运维脚本调用）。
     */
    public int flushNow() {
        Set<String> keys;
        try {
            keys = stringRedisTemplate.keys("meowflow:cache:" + TemplateCacheKeys.USE_COUNT_PATTERN);
        } catch (Exception e) {
            log.warn("Failed to SCAN use_count keys in manual flush", e);
            return 0;
        }
        if (keys == null || keys.isEmpty()) return 0;
        List<TemplateUseCountDelta> deltas = new ArrayList<>();
        for (String fullKey : keys) {
            Long id = parseId(fullKey);
            if (id == null) continue;
            String v = stringRedisTemplate.opsForValue().get(fullKey);
            if (v == null) continue;
            stringRedisTemplate.delete(fullKey);
            try {
                long delta = Long.parseLong(v);
                if (delta > 0) deltas.add(new TemplateUseCountDelta(id, delta));
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        if (deltas.isEmpty()) return 0;
        try {
            templateRepository.batchIncrementUseCount(deltas);
            List<Long> ids = deltas.stream().map(TemplateUseCountDelta::id).toList();
            cacheSupport.evictAfterUseCountFlush(ids);
            return deltas.size();
        } catch (Exception e) {
            log.error("Failed to batch increment use_count (manual)", e);
            restore(deltas);
            throw e;
        }
    }

    private void restore(List<TemplateUseCountDelta> deltas) {
        for (TemplateUseCountDelta d : deltas) {
            if (!d.isValid()) continue;
            try {
                stringRedisTemplate.opsForValue().increment(TemplateCacheKeys.useCountKey(d.id()), d.delta());
            } catch (Exception e) {
                log.error("Failed to restore use_count for templateId={}", d.id(), e);
            }
        }
    }

    private Long parseId(String fullKey) {
        // fullKey 形如 "meowflow:cache:tpl:usecnt:v1:12345"
        int idx = fullKey.lastIndexOf(':');
        if (idx < 0) return null;
        try {
            return Long.parseLong(fullKey.substring(idx + 1));
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}

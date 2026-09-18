package com.meowflow.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis 通用服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    // ==================== 基础操作 ====================

    /**
     * 设置值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置值并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 设置值并指定过期时间（秒）
     */
    public void setEx(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 获取值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取字符串值
     */
    public String getStr(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 获取值，如果不存在则设置
     */
    public <T> T getOrSet(String key, Supplier<T> supplier, long timeout, TimeUnit unit) {
        T value = get(key);
        if (value == null) {
            value = supplier.get();
            if (value != null) {
                set(key, value, timeout, unit);
            }
        }
        return value;
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除键
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断键是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // ==================== 哈希操作 ====================

    /**
     * 设置哈希字段
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取哈希字段
     */
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 删除哈希字段
     */
    public Long hDel(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * 判断哈希字段是否存在
     */
    public Boolean hExists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * 获取哈希所有字段和值
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    // ==================== 列表操作 ====================

    /**
     * 左添加列表
     */
    public Long lPush(String key, Object... values) {
        return redisTemplate.opsForList().leftPushAll(key, values);
    }

    /**
     * 右添加列表
     */
    public Long rPush(String key, Object... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * 左弹出列表
     */
    @SuppressWarnings("unchecked")
    public <T> T lPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 右弹出列表
     */
    @SuppressWarnings("unchecked")
    public <T> T rPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取列表长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ==================== 集合操作 ====================

    /**
     * 添加集合成员
     */
    public Long sAdd(String key, Object... members) {
        return redisTemplate.opsForSet().add(key, members);
    }

    /**
     * 移除集合成员
     */
    public Long sRem(String key, Object... members) {
        return redisTemplate.opsForSet().remove(key, members);
    }

    /**
     * 判断是否是集合成员
     */
    public Boolean sIsMember(String key, Object member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * 获取集合所有成员
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    // ==================== 有序集合操作 ====================

    /**
     * 添加有序集合成员
     */
    public Boolean zAdd(String key, Object member, double score) {
        return redisTemplate.opsForZSet().add(key, member, score);
    }

    /**
     * 移除有序集合成员
     */
    public Long zRem(String key, Object... members) {
        return redisTemplate.opsForZSet().remove(key, members);
    }

    /**
     * 获取成员分数
     */
    public Double zScore(String key, Object member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    // ==================== 分布式锁 ====================

    private static final String LOCK_PREFIX = "lock:";

    /**
     * 获取锁（简单实现）
     */
    public Boolean tryLock(String key, long expireSeconds) {
        return stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + key, "1", expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        delete(LOCK_PREFIX + key);
    }

    /**
     * 尝试获取锁（带重试）
     */
    public Boolean tryLockWithRetry(String key, long expireSeconds, int maxRetries, long retryIntervalMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (Boolean.TRUE.equals(tryLock(key, expireSeconds))) {
                return true;
            }
            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    // ==================== 计数器 ====================

    /**
     * 增加计数
     */
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 增加计数（指定步长）
     */
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 减少计数
     */
    public Long decrement(String key) {
        return stringRedisTemplate.opsForValue().decrement(key);
    }

    /**
     * 减少计数（指定步长）
     */
    public Long decrement(String key, long delta) {
        return stringRedisTemplate.opsForValue().decrement(key, delta);
    }

    // ==================== 限流 ====================

    /**
     * 滑动窗口限流
     *
     * @param key          限流键
     * @param maxRequests 最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @return 是否允许通过
     */
    public boolean slideWindowRateLimit(String key, int maxRequests, long windowSeconds) {
        String luaScript = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            
            -- 删除过期数据
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)
            
            -- 获取当前请求数
            local current = redis.call('ZCARD', key)
            
            if current < limit then
                redis.call('ZADD', key, now, now .. ':' .. math.random())
                redis.call('EXPIRE', key, window)
                return 1
            else
                return 0
            end
            """;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script,
                Collections.singletonList(key),
                maxRequests, windowSeconds, System.currentTimeMillis());

        return result != null && result == 1L;
    }

    // ==================== Stream 操作 ====================
    // Spring Data Redis 3.2 (Spring Boot 3.2) compatible Stream API
    // Stream ID: "*-*" (millisTimestamp-sequenceNumber)

    private static final org.springframework.data.redis.connection.stream.StreamReadOptions STREAM_READ_OPTIONS =
            org.springframework.data.redis.connection.stream.StreamReadOptions.empty();

    /**
     * 添加元素到 Stream。
     *
     * @param key   Stream key
     * @param map   field-value map
     * @return 新增元素的 Stream ID
     */
    public String streamAdd(String key, Map<String, String> map) {
        org.springframework.data.redis.connection.stream.MapRecord<String, String, String> record =
                org.springframework.data.redis.connection.stream.MapRecord.create(key, map);
        return stringRedisTemplate.opsForStream().add(record).getValue();
    }

    /**
     * 读取 Stream 消息（同步读取，不使用 blocking）。
     *
     * @param key      Stream key
     * @param cursor   游标（null 表示从头）
     * @param count    每次最多读取数量
     * @return Stream 消息列表
     */
    public List<Map<String, String>> streamRead(String key, String cursor, int count) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            org.springframework.data.redis.connection.stream.StreamReadOptions options =
                    org.springframework.data.redis.connection.stream.StreamReadOptions.empty().count(count);
            org.springframework.data.redis.connection.stream.StreamOffset<String> offset =
                    org.springframework.data.redis.connection.stream.StreamOffset.fromStart(key);
            java.util.List records = stringRedisTemplate.opsForStream().read(options, offset);
            if (records == null) return result;
            for (Object raw : records) {
                org.springframework.data.redis.connection.stream.MapRecord msg =
                        (org.springframework.data.redis.connection.stream.MapRecord) raw;
                Map<String, String> fields = new HashMap<>();
                java.util.Map msgMap = (java.util.Map) msg.getValue();
                if (msgMap != null) {
                    for (java.util.Map.Entry e : ((java.util.Map<?, ?>) msgMap).entrySet()) {
                        Object v = e.getValue();
                        fields.put(String.valueOf(e.getKey()), v != null ? String.valueOf(v) : "");
                    }
                }
                fields.put("_id", msg.getId().getValue());
                result.add(fields);
            }
        } catch (Exception e) {
            log.warn("streamRead failed: key={}, {}", key, e.getMessage());
        }
        return result;
    }

    /**
     * 按 Stream ID 范围读取 Stream（使用 XRANGE 语义）。
     *
     * @param key      Stream key
     * @param startId  开始 ID（包含），"-" 表示从头，"0-0" 等价于 "-"
     * @param endId    结束 ID（包含），"+" 表示到最新
     * @return Stream 消息列表，每条消息包含 _id 字段
     */
    public List<Map<String, String>> streamRange(String key, String startId, String endId) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            // 转换前端游标格式
            String realStartId = startId;
            if ("0-0".equals(startId) || "-".equals(startId) || startId == null || startId.isEmpty()) {
                realStartId = "-";
            }
            String realEndId = "+".equals(endId) || endId == null || endId.isEmpty() ? "+" : endId;
            
            // 使用 XRANGE 命令读取指定范围
            Range<String> range = Range.closed(realStartId, realEndId);
            
            java.util.List records = stringRedisTemplate.opsForStream().range(
                    key,
                    range,
                    org.springframework.data.redis.connection.Limit.limit().count(10000));
            
            if (records == null || records.isEmpty()) return result;
            
            for (Object raw : records) {
                org.springframework.data.redis.connection.stream.MapRecord msg =
                        (org.springframework.data.redis.connection.stream.MapRecord) raw;
                Map<String, String> fields = new HashMap<>();
                java.util.Map msgMap = (java.util.Map) msg.getValue();
                if (msgMap != null) {
                    for (java.util.Map.Entry e : ((java.util.Map<?, ?>) msgMap).entrySet()) {
                        Object v = e.getValue();
                        fields.put(String.valueOf(e.getKey()), v != null ? String.valueOf(v) : "");
                    }
                }
                // 将 Redis Stream ID 作为 _id 字段返回
                fields.put("_id", msg.getId().getValue());
                result.add(fields);
            }
        } catch (Exception e) {
            log.warn("streamRange failed: key={}, start={}, end={}, {}",
                    key, startId, endId, e.getMessage());
        }
        return result;
    }

    /**
     * 获取 Stream 长度。
     */
    public Long streamSize(String key) {
        return stringRedisTemplate.opsForStream().size(key);
    }

    /**
     * 删除 Stream 中的消息。
     */
    public Long streamDelete(String key, String... ids) {
        return stringRedisTemplate.opsForStream().delete(key, ids);
    }
}

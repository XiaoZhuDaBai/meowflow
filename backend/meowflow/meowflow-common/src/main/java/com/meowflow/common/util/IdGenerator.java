package com.meowflow.common.util;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花 ID 生成器
 * <p>
 * 分布式唯一 ID 生成算法，支持 10 位 workerId（0-1023）
 */
@Slf4j
public class IdGenerator {

    private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private final Object lock = new Object();
    private final AtomicLong fallbackCounter = new AtomicLong(0);

    /**
     * 使用随机 workerId 初始化（仅用于本地测试）
     */
    public IdGenerator() {
        this.workerId = Math.abs(ThreadLocalRandom.current().nextLong()) % (MAX_WORKER_ID + 1);
        log.info("IdGenerator 初始化，使用随机 workerId: {}", workerId);
    }

    /**
     * 使用指定 workerId 初始化
     *
     * @param workerId 工作节点 ID（0-1023）
     */
    public IdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new BizException(ResultCode.PARAM_ERROR, "workerId 超出范围: " + workerId + ", 有效范围: 0-" + MAX_WORKER_ID);
        }
        this.workerId = workerId;
        log.info("IdGenerator 初始化，workerId: {}", workerId);
    }

    /**
     * 生成下一个 ID
     *
     * @return 唯一 ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨处理
        if (timestamp < lastTimestamp) {
            log.warn("时钟回拨检测到，回拨时间: {}ms，启用备用计数器", lastTimestamp - timestamp);
            return fallbackCounter.incrementAndGet();
        }

        // 同一毫秒内序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & ((1L << SEQUENCE_BITS) - 1);
            if (sequence == 0L) {
                // 序列号用尽，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成下一个 ID（字符串形式）
     *
     * @return 唯一 ID 字符串
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 等待下一毫秒
     */
    private long waitNextMillis(long lastTs) {
        long ts = System.currentTimeMillis();
        while (ts <= lastTs) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }

    /**
     * 获取当前 workerId
     */
    public long getWorkerId() {
        return workerId;
    }

    /**
     * 解析 ID 获取时间戳
     *
     * @param id 雪花 ID
     * @return 时间戳
     */
    public static long getTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }

    /**
     * 解析 ID 获取 workerId
     *
     * @param id 雪花 ID
     * @return workerId
     */
    public static long getWorkerId(long id) {
        return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * 解析 ID 获取序列号
     *
     * @param id 雪花 ID
     * @return 序列号
     */
    public static long getSequence(long id) {
        return id & ((1L << SEQUENCE_BITS) - 1);
    }
}
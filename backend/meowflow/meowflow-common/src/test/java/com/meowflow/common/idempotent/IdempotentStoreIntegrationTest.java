package com.meowflow.common.idempotent;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdempotentStore 真 Redis 集成测试。直接基于 {@link LettuceConnectionFactory} 构造
 * {@link StringRedisTemplate}，避开 meowflow-common 没有 @SpringBootApplication 的限制。
 *
 * <p>使用 db 15 与 localhost:6379，符合 application-isolation.yml 的约定。
 */
@DisplayName("IdempotentStore 集成测试 — 真 Redis")
class IdempotentStoreIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static IdempotentStore store;

    private static final String TEST_KEY = "itest:idem:key1";

    @BeforeAll
    static void setUpAll() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        cfg.setDatabase(15);
        connectionFactory = new LettuceConnectionFactory(cfg);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        store = new IdempotentStore(redisTemplate);
    }

    @AfterAll
    static void tearDownAll() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        redisTemplate.delete("idempotent:" + TEST_KEY);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete("idempotent:" + TEST_KEY);
    }

    @Test
    @DisplayName("tryLock - 同一 key 第二次调用返回 false")
    void tryLock_duplicate_returnsFalse() {
        boolean first = store.tryLock(TEST_KEY, "v1", 60);
        boolean second = store.tryLock(TEST_KEY, "v2", 60);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(store.get(TEST_KEY)).isEqualTo("v1");
    }

    @Test
    @DisplayName("tryLock - 100 并发竞争同一 key，仅一次成功")
    void tryLock_concurrent_onlyOneSucceeds() throws Exception {
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>(threads));

        try {
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                pool.submit(() -> {
                    try {
                        start.await();
                        boolean ok = store.tryLock(TEST_KEY, "thread-" + idx, 60);
                        if (ok) successCount.incrementAndGet();
                        results.add(ok);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get())
                .as("exactly one thread should win the lock")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("tryLock - 解锁后再次获取应成功")
    void tryLock_afterUnlock_succeeds() {
        assertThat(store.tryLock(TEST_KEY, "v1", 60)).isTrue();
        store.unlock(TEST_KEY, "v1");
        assertThat(store.exists(TEST_KEY)).isFalse();

        assertThat(store.tryLock(TEST_KEY, "v2", 60)).isTrue();
        assertThat(store.get(TEST_KEY)).isEqualTo("v2");
    }

    @Test
    @DisplayName("unlock - 仅当值匹配才删除（防止误删他人的锁）")
    void unlock_mismatch_noDelete() {
        store.tryLock(TEST_KEY, "owner", 60);
        store.unlock(TEST_KEY, "imposter");

        assertThat(store.exists(TEST_KEY)).isTrue();
        assertThat(store.get(TEST_KEY)).isEqualTo("owner");
    }
}
package com.meowflow.infra.config;

import com.meowflow.infra.router.ModelHealthStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.*;

/**
 * AI 基础设施线程池配置
 * 借鉴 ragent 的 ThreadPoolExecutorConfig 设计
 */
@Configuration
@EnableAsync
public class AIThreadPoolConfig {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 记忆加载线程池
     */
    @Bean("memoryLoadExecutor")
    public Executor memoryLoadExecutor() {
        return new ThreadPoolExecutor(
            Math.max(1, CPU_COUNT >> 1),  // 核心线程数
            CPU_COUNT,                      // 最大线程数
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactoryBuilder()
                .setNamePrefix("memory_load_")
                .setDaemon(false)
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 模型健康检查线程池
     */
    @Bean("healthCheckExecutor")
    public ScheduledExecutorService healthCheckExecutor() {
        return Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
            .setNamePrefix("health_check_")
            .setDaemon(true)
            .build());
    }

    /**
     * 流式响应处理线程池
     */
    @Bean("streamProcessExecutor")
    public Executor streamProcessExecutor() {
        return new ThreadPoolExecutor(
            CPU_COUNT << 1,
            CPU_COUNT << 2,
            30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactoryBuilder()
                .setNamePrefix("stream_proc_")
                .setDaemon(false)
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 意图识别线程池
     */
    @Bean("intentResolveExecutor")
    public Executor intentResolveExecutor() {
        return new ThreadPoolExecutor(
            CPU_COUNT,
            CPU_COUNT << 1,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactoryBuilder()
                .setNamePrefix("intent_resolve_")
                .setDaemon(false)
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 模型健康状态存储
     */
    @Bean
    @ConditionalOnMissingBean(ModelHealthStore.class)
    public ModelHealthStore modelHealthStore(StringRedisTemplate redisTemplate) {
        return new ModelHealthStore(redisTemplate);
    }

    /**
     * 线程工厂构建器
     */
    public static class ThreadFactoryBuilder {
        private String prefix = "pool";
        private boolean daemon = false;

        public ThreadFactoryBuilder setNamePrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ThreadFactoryBuilder setDaemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public ThreadFactory build() {
            return r -> {
                Thread t = new Thread(r);
                t.setName(prefix + System.nanoTime());
                t.setDaemon(daemon);
                return t;
            };
        }
    }
}

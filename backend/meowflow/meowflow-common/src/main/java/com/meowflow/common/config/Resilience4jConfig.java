package com.meowflow.common.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j 通用配置（限流）。
 *
 * <p>注：{@code CircuitBreakerConfig} 的统一 Bean 已迁移至
 * {@link UnifiedCircuitBreakerConfig#defaultCircuitBreakerConfig()}，
 * 这里不再单独暴露，避免与通用配置冲突。</p>
 */
@ConditionalOnClass(RateLimiterConfig.class)
@Configuration
public class Resilience4jConfig {

    @Bean
    public RateLimiterConfig apiRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }
}
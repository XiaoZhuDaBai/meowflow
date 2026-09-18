package com.meowflow.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * 统一熔断器配置
 *
 * <p>由原 meowflow-infra/chat/AICircuitBreakerConfig 抽离而来，提供：
 * <ul>
 *   <li>通用熔断器注册中心 {@code circuitBreakerRegistry}</li>
 *   <li>AI 专用注册中心 {@code aiCircuitBreakerRegistry}</li>
 * </ul>
 *
 * <p><strong>使用约定</strong>：
 * <ul>
 *   <li>{@code aiCircuitBreakerRegistry}：AI 服务调用专用，所有 AI 组件（ChatModelRouter、AICircuitBreakerRegistryHolder）必须使用此 Registry</li>
 *   <li>{@code circuitBreakerRegistry}：通用熔断器，预留给 HTTP/DB 限流等场景，业务组件请勿直接使用</li>
 * </ul>
 */
@Slf4j
@Configuration
@ConditionalOnClass(CircuitBreakerRegistry.class)
public class UnifiedCircuitBreakerConfig {

    /**
     * 通用熔断器注册中心配置
     */
    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }

    /**
     * 通用熔断器 Registry
     *
     * <p>预留给 HTTP/DB 限流等场景使用，业务组件请勿直接使用。
     */
    @Bean
    @ConditionalOnProperty(name = "meowflow.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreakerRegistry circuitBreakerRegistry(
            @Qualifier("defaultCircuitBreakerConfig") CircuitBreakerConfig defaultCircuitBreakerConfig) {
        log.info("初始化统一熔断器 Registry");
        return CircuitBreakerRegistry.of(defaultCircuitBreakerConfig);
    }

    /**
     * AI 专用熔断器 Registry
     *
     * <p>AI 调用场景需要更严格的熔断保护：
     * <ul>
     *   <li>更短的等待时间（防止长时调用堆积）</li>
     *   <li>更高的失败率阈值（防止误熔断）</li>
     * </ul>
     *
     * <p>本 Bean 标记为 {@code @Primary}，当 AI 组件未显式指定 Qualifier 时优先使用。
     */
    @Bean(name = "aiCircuitBreakerRegistry")
    @Primary
    public CircuitBreakerRegistry aiCircuitBreakerRegistry() {
        log.info("初始化 AI 专用熔断器 Registry");
        CircuitBreakerConfig aiConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        return CircuitBreakerRegistry.of(aiConfig);
    }
}

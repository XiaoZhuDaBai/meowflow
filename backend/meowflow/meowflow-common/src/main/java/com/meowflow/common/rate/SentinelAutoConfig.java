package com.meowflow.common.rate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Sentinel 切面自动配置
 *
 * <p>只有当类路径存在 Sentinel 相关类时才生效，避免在不使用 Sentinel 的服务中加载切面。</p>
 *
 * <p>可通过配置 {@code meowflow.sentinel.enabled=false} 关闭。</p>
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "com.alibaba.csp.sentinel.SphU")
@ConditionalOnProperty(name = "meowflow.sentinel.enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
public class SentinelAutoConfig {

    @Bean
    public SentinelRateAspect sentinelRateAspect() {
        log.info("初始化 Sentinel 限流切面");
        return new SentinelRateAspect();
    }

    @Bean
    public SentinelCircuitBreakerAspect sentinelCircuitBreakerAspect() {
        log.info("初始化 Sentinel 熔断切面");
        return new SentinelCircuitBreakerAspect();
    }
}
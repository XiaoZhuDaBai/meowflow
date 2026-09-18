package com.meowflow.common.rate;

import java.lang.annotation.*;

/**
 * Sentinel 限流注解
 *
 * <p>使用 Sentinel 的滑动窗口算法进行单机/分布式限流。</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * {@code
 * @RateLimiter(value = "workflow:execute", qps = 100)
 * public void executeWorkflow(WorkflowRequest request) {
 *     // ...
 * }
 * }
 * </pre>
 *
 * <p>限流规则可通过 Sentinel Dashboard 或 Nacos 动态配置。</p>
 *
 * @see com.meowflow.common.rate.SentinelRateAspect
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /**
     * 限流资源名称（唯一标识）
     */
    String value();

    /**
     * 每秒允许的最大请求数（QPS）
     * 默认 100
     */
    int qps() default 100;

    /**
     * 限流类型
     * 默认单机限流（单机有效）
     * 分布式限流需配合 Redis 或 Sentinel Token Server
     */
    LimitType limitType() default LimitType.SINGLE;

    /**
     * 达到限流时的提示信息
     */
    String message() default "请求过于频繁，请稍后重试";

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 单机限流：基于滑动窗口计数，每个实例独立统计
         */
        SINGLE,
        /**
         * 集群限流：基于 Redis 或 Sentinel Token Server，全局统计
         */
        CLUSTER
    }
}
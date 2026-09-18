package com.meowflow.common.idempotent;

import java.lang.annotation.*;

/**
 * 幂等注解
 * <p>
 * 用于标记接口或方法为幂等的，防止重复提交。
 * 支持基于 Redis 的分布式锁实现。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键的前缀，默认使用方法名
     */
    String prefix() default "";

    /**
     * 幂等过期时间（秒），默认 300 秒
     */
    int expireSeconds() default 300;

    /**
     * 失败时的提示消息
     */
    String message() default "请求过于频繁，请稍后重试";

    /**
     * 是否启用
     */
    boolean enabled() default true;

    /**
     * 获取幂等键的 SpEL 表达式
     * <p>
     * 例如：#userId 或 #request.orderId
     */
    String key() default "";

    /**
     * 限流策略
     */
    RateLimitStrategy rateLimitStrategy() default RateLimitStrategy.NONE;

    /**
     * 限流策略枚举
     */
    enum RateLimitStrategy {
        /**
         * 不限流
         */
        NONE,
        /**
         * 滑动窗口限流
         */
        SLIDE_WINDOW,
        /**
         * 令牌桶限流
         */
        TOKEN_BUCKET
    }
}

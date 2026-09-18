package com.meowflow.common.rate;

import java.lang.annotation.*;

/**
 * Sentinel 熔断注解
 *
 * <p>基于 Sentinel 的熔断降级机制。当失败率超过阈值时自动熔断，</p>
 * <p>经过熔断时长后进入探测恢复期（HALF_OPEN），少量请求通过验证。</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * {@code
 * @CircuitBreaker(value = "external:payment",
 *                failureRateThreshold = 50,
 *                slowCallRateThreshold = 80,
 *                slowCallDurationMs = 3000)
 * public PaymentResult pay(OrderRequest request) {
 *     // 调用外部支付服务
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CircuitBreaker {

    /**
     * 熔断资源名称
     */
    String value();

    /**
     * 失败率阈值（百分比），达到该值触发熔断，默认 50%
     */
    double failureRateThreshold() default 50.0;

    /**
     * 慢调用比例阈值（百分比），默认 100（不启用）
     */
    double slowCallRateThreshold() default 100.0;

    /**
     * 慢调用时长阈值（毫秒），超过此值视为慢调用
     */
    int slowCallDurationMs() default 3000;

    /**
     * 滑动窗口大小，默认 10
     */
    int slidingWindowSize() default 10;

    /**
     * 最少调用次数（达不到该值不会熔断），默认 5
     */
    int minimumNumberOfCalls() default 5;

    /**
     * 熔断持续时间（秒），默认 30
     */
    int waitDurationInOpenStateSeconds() default 30;

    /**
     * 熔断时的降级提示
     */
    String message() default "服务暂不可用，请稍后重试";

    /**
     * 自定义降级方法名（在同一类中，签名需一致）
     */
    String fallback() default "";
}
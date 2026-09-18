package com.meowflow.common.exception;

import com.meowflow.common.result.Result;
import com.meowflow.common.result.ResultCode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Resilience4j 熔断/限流异常处理器。
 *
 * <p>独立拆分到本类是为了让未引入 resilience4j 的下游模块（如 monitor）也能正常启动
 * —— Spring 扫描 @ControllerAdvice 时若发现其方法签名包含运行时缺失的类，会导致
 * Bean 创建失败。本类通过 @ConditionalOnClass 保护：仅当 classpath 同时存在
 * resilience4j-circuitbreaker 与 resilience4j-ratelimiter 时才会注册。
 */
@Slf4j
@ConditionalOnClass({CallNotPermittedException.class, RequestNotPermitted.class})
@RestControllerAdvice
public class Resilience4jExceptionHandler {

    @ExceptionHandler(RequestNotPermitted.class)
    public Result<Void> handleRateLimit(RequestNotPermitted e) {
        log.warn("Resilience4j 限流触发: {}", e.getMessage());
        return Result.error(ResultCode.RATE_LIMITED);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public Result<Void> handleCircuitOpen(CallNotPermittedException e) {
        log.warn("Resilience4j 熔断器开启: {}", e.getMessage());
        return Result.error(ResultCode.CIRCUIT_BREAKER_OPEN);
    }
}
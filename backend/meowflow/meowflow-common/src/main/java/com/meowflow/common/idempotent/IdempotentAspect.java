package com.meowflow.common.idempotent;

import com.meowflow.common.context.TraceContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 幂等切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final IdempotentStore idempotentStore;

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.meowflow.common.idempotent.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        if (!idempotent.enabled()) {
            return joinPoint.proceed();
        }

        String idempotentKey = buildIdempotentKey(joinPoint, idempotent);
        String idempotentValue = buildIdempotentValue();

        boolean acquired;
        try {
            acquired = idempotentStore.tryLock(idempotentKey, idempotentValue, idempotent.expireSeconds());
        } catch (Exception e) {
            log.warn("幂等锁获取失败，降级处理: key={}, error={}", idempotentKey, e.getMessage());
            return joinPoint.proceed();
        }

        if (!acquired) {
            log.warn("请求重复: key={}, message={}", idempotentKey, idempotent.message());
            throw new BizException(ResultCode.REQUEST_REPEAT);
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (Boolean.TRUE) {
                try {
                    idempotentStore.unlock(idempotentKey, idempotentValue);
                } catch (Exception e) {
                    log.warn("幂等锁释放失败: key={}, error={}", idempotentKey, e.getMessage());
                }
            }
        }
    }

    /**
     * 构建幂等键
     */
    private String buildIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String key = idempotent.key();

        if (key.isEmpty()) {
            String methodName = joinPoint.getSignature().toShortString();
            String traceId = TraceContextHolder.getTraceId();
            return methodName + ":" + traceId;
        }

        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = NAME_DISCOVERER.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                ((StandardEvaluationContext) context).setVariable(parameterNames[i], args[i]);
            }
        }

        Expression expression = PARSER.parseExpression(key);
        Object value = expression.getValue(context);

        String prefix = idempotent.prefix();
        if (prefix.isEmpty()) {
            prefix = joinPoint.getSignature().getName();
        }

        return prefix + ":" + value;
    }

    /**
     * 构建幂等值
     */
    private String buildIdempotentValue() {
        String traceId = TraceContextHolder.getTraceId();
        String spanId = TraceContextHolder.getSpanId();
        long timestamp = System.currentTimeMillis();
        return String.format("%s:%s:%d", traceId != null ? traceId : "no-trace",
                spanId != null ? spanId : "no-span", timestamp);
    }
}

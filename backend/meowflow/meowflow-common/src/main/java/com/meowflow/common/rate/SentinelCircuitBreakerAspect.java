package com.meowflow.common.rate;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sentinel 熔断 AOP 切面
 *
 * <p>拦截 {@link CircuitBreaker} 注解的方法，配置熔断规则。</p>
 *
 * <p>熔断策略：</p>
 * <ul>
 *   <li>基于异常比例：异常数 / 总调用数 >= failureRateThreshold 触发熔断</li>
 *   <li>基于慢调用比例：慢调用数 / 总调用数 >= slowCallRateThreshold 触发熔断</li>
 * </ul>
 */
@Slf4j
@Aspect
@Order(2)
public class SentinelCircuitBreakerAspect {

    private static final ConcurrentMap<String, Boolean> REGISTERED = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("SentinelCircuitBreakerAspect initialized");
    }

    @Around("@annotation(com.meowflow.common.rate.CircuitBreaker)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CircuitBreaker annotation = method.getAnnotation(CircuitBreaker.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        String resource = annotation.value();
        registerRuleIfAbsent(resource, annotation);

        try (Entry entry = SphU.entry(resource, EntryType.OUT, 1)) {
            return joinPoint.proceed();
        } catch (BlockException e) {
            log.warn("熔断触发 resource={} message={}", resource, annotation.message());
            Object fallbackResult = invokeFallback(joinPoint, annotation, method);
            if (fallbackResult != null) {
                return fallbackResult;
            }
            throw new CircuitBreakerOpenException(annotation.message());
        }
    }

    /**
     * 注册熔断规则
     */
    private void registerRuleIfAbsent(String resource, CircuitBreaker annotation) {
        if (REGISTERED.containsKey(resource)) {
            return;
        }
        synchronized (REGISTERED) {
            if (REGISTERED.containsKey(resource)) {
                return;
            }
            DegradeRule rule = new DegradeRule(resource);
            // 异常比例熔断
            rule.setGrade(com.alibaba.csp.sentinel.slots.block.RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
            rule.setCount(annotation.failureRateThreshold() / 100.0);
            rule.setSlowRatioThreshold(annotation.slowCallRateThreshold() / 100.0);
            rule.setMinRequestAmount(annotation.minimumNumberOfCalls());
            rule.setStatIntervalMs((int) annotation.slidingWindowSize() * 1000);
            rule.setTimeWindow(annotation.waitDurationInOpenStateSeconds());

            List<DegradeRule> rules = new ArrayList<>(DegradeRuleManager.getRules());
            rules.add(rule);
            DegradeRuleManager.loadRules(rules);
            REGISTERED.put(resource, Boolean.TRUE);
            log.info("注册 Sentinel 熔断规则 resource={} failureRate={}% timeWindow={}s",
                    resource, annotation.failureRateThreshold(), annotation.waitDurationInOpenStateSeconds());
        }
    }

    /**
     * 调用降级方法
     */
    private Object invokeFallback(ProceedingJoinPoint joinPoint, CircuitBreaker annotation, Method method) {
        if (annotation.fallback() == null || annotation.fallback().isEmpty()) {
            return null;
        }
        try {
            Object target = joinPoint.getTarget();
            Method fallbackMethod = target.getClass().getMethod(annotation.fallback(), method.getParameterTypes());
            return fallbackMethod.invoke(target, joinPoint.getArgs());
        } catch (Exception e) {
            log.warn("调用降级方法失败 resource={} fallback={}", annotation.value(), annotation.fallback(), e);
            return null;
        }
    }

    /**
     * 熔断触发异常
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
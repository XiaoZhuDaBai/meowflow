package com.meowflow.common.rate;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
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
 * Sentinel 限流 AOP 切面
 *
 * <p>拦截 {@link RateLimiter} 注解的方法，根据注解参数动态注册流控规则，</p>
 * <p>通过 Sentinel 的 {@link SphU#entry(String)} 进行限流控制。</p>
 *
 * <p>优先级：</p>
 * <ul>
 *   <li>1 = 限流切面（先执行）</li>
 *   <li>2 = 熔断切面（后执行）</li>
 * </ul>
 */
@Slf4j
@Aspect
@Order(1)
public class SentinelRateAspect {

    /**
     * 已注册的限流资源（避免重复注册）
     */
    private static final ConcurrentMap<String, Boolean> REGISTERED = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("SentinelRateAspect initialized");
    }

    @Around("@annotation(com.meowflow.common.rate.RateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimiter annotation = method.getAnnotation(RateLimiter.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        String resource = annotation.value();
        registerRuleIfAbsent(resource, annotation);

        try (Entry entry = SphU.entry(resource, EntryType.OUT)) {
            return joinPoint.proceed();
        } catch (BlockException e) {
            log.warn("限流触发 resource={} message={}", resource, annotation.message());
            throw new RateLimitExceededException(annotation.message());
        }
    }

    /**
     * 注册限流规则（首次调用时注册）
     */
    private void registerRuleIfAbsent(String resource, RateLimiter annotation) {
        if (REGISTERED.containsKey(resource)) {
            return;
        }
        synchronized (REGISTERED) {
            if (REGISTERED.containsKey(resource)) {
                return;
            }
            FlowRule rule = new FlowRule(resource);
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            rule.setCount(annotation.qps());
            rule.setLimitApp("default");
            rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
            rule.setStrategy(RuleConstant.STRATEGY_DIRECT);
            List<FlowRule> rules = new ArrayList<>(FlowRuleManager.getRules());
            rules.add(rule);
            FlowRuleManager.loadRules(rules);
            REGISTERED.put(resource, Boolean.TRUE);
            log.info("注册 Sentinel 限流规则 resource={} qps={}", resource, annotation.qps());
        }
    }

    /**
     * 限流触发异常
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
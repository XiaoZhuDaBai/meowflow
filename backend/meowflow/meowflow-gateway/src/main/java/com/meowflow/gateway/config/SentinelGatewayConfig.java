package com.meowflow.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关限流配置
 *
 * <p>职责：</p>
 * <ul>
 *   <li>注册 Sentinel Gateway Filter（注入到 Gateway 过滤器链）</li>
 *   <li>定义默认流控规则（按 Route ID 维度）</li>
 *   <li>自定义限流响应（统一 JSON 格式）</li>
 * </ul>
 */
@Slf4j
@Configuration
public class SentinelGatewayConfig {

    private final BlockRequestHandler blockHandler = this::buildBlockResponse;

    @PostConstruct
    public void initRules() {
        try {
            // 禁用 Sentinel 日志（避免容器权限问题）
            System.setProperty("csp.sentinel.log.use.pid", "false");
            System.setProperty("csp.sentinel.log.output.type", "console");
            
            Set<GatewayFlowRule> rules = new HashSet<>();

            rules.add(buildRule("meowflow-user", 200, 1));
            rules.add(buildRule("meowflow-workflow", 200, 1));
            rules.add(buildRule("meowflow-executor", 100, 1));
            rules.add(buildRule("meowflow-template", 100, 1));
            rules.add(buildRule("meowflow-monitor", 50, 1));
            rules.add(buildRule("meowflow-infra", 200, 1));

            GatewayRuleManager.loadRules(rules);
            log.info("Sentinel Gateway 规则加载完成，共 {} 条规则", rules.size());

            // 设置自定义限流响应处理器
            GatewayCallbackManager.setBlockHandler(blockHandler);
        } catch (Exception e) {
            log.error("Sentinel 初始化失败，将跳过限流配置", e);
        }
    }

    private GatewayFlowRule buildRule(String routeId, int qps, int intervalSec) {
        GatewayFlowRule rule = new GatewayFlowRule(routeId);
        rule.setResourceMode(0);
        rule.setGrade(1);
        rule.setCount(qps);
        rule.setIntervalSec(intervalSec);
        rule.setControlBehavior(0);
        rule.setBurst(50);
        return rule;
    }

    /**
     * 自定义限流响应
     */
    private Mono<ServerResponse> buildBlockResponse(ServerWebExchange exchange, Throwable t) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", 2001);
        body.put("message", "请求过于频繁，请稍后重试");
        body.put("data", null);
        body.put("path", exchange.getRequest().getPath().value());
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}
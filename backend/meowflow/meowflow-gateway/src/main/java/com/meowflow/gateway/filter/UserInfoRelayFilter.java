package com.meowflow.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * 用户身份信息透传过滤器
 *
 * <p>将 Sa-Token 校验后的用户信息放入请求头，透传给下游服务。</p>
 *
 * <p>全局过滤器接收到的是网关原始路径（如 /user/api/v1/auth/...）。</p>
 */
@Component
public class UserInfoRelayFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_LOGIN_TOKEN = "X-Login-Token";

    private static final Set<String> SKIP_PATHS = Set.of(
            "/user/api/v1/auth/login",
            "/user/api/v1/auth/refresh",
            "/user/api/v1/auth/register",
            "/user/api/v1/auth/email-code",
            "/user/api/v1/auth/password/reset",
            "/user/api/v1/auth/logout",
            "/user/api/v1/captcha/",
            "/workflow/api/webhook/",
            "/workflow/api/plugin/trigger/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (shouldSkip(path)) {
            return chain.filter(exchange);
        }

        try {
            if (StpUtil.isLogin()) {
                Object loginId = StpUtil.getLoginIdDefaultNull();
                String token = StpUtil.getTokenValue();

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header(HEADER_USER_ID, loginId != null ? loginId.toString() : "")
                        .header(HEADER_LOGIN_TOKEN, token != null ? token : "")
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
        } catch (Exception ignored) {
            // 未登录或 Sa-Token 未配置：不透传
        }

        return chain.filter(exchange);
    }

    boolean shouldSkip(String path) {
        return SKIP_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

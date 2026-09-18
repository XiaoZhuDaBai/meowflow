package com.meowflow.gateway.filter;

import com.meowflow.gateway.util.ReactiveMdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 请求日志过滤器
 *
 * <p>记录：</p>
 * <ul>
 *   <li>请求方法、路径、来源 IP</li>
 *   <li>响应状态码、耗时</li>
 *   <li>User-Agent、Trace ID</li>
 * </ul>
 */
@Slf4j
@Component
public class LogGlobalFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTR = "GATEWAY_START_TIME";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(START_TIME_ATTR, startTime);

        String traceId = request.getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
        String method = request.getMethod().name();
        URI uri = request.getURI();
        String remoteAddr = getClientIp(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");

        log.info(">>> 请求开始 method={} uri={} remote={} ua={} traceId={}",
                method, uri.getPath(), remoteAddr, userAgent, traceId);

        return chain.filter(exchange).then().doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;
            if (statusCode >= 500) {
                log.error("<<< 请求结束 method={} uri={} status={} duration={}ms traceId={}",
                        method, uri.getPath(), statusCode, duration, traceId);
            } else {
                log.info("<<< 请求结束 method={} uri={} status={} duration={}ms traceId={}",
                        method, uri.getPath(), statusCode, duration, traceId);
            }
            ReactiveMdcUtils.mdcCleaner().run();
        });
    }

    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeaders().getFirst("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
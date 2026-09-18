package com.meowflow.gateway.filter;

import com.meowflow.gateway.util.ReactiveMdcUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Trace ID 透传过滤器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>从上游请求头读取 X-Trace-Id（如果有）</li>
 *   <li>否则生成新的 Trace ID（UUID）</li>
 *   <li>放入 Reactor Context、请求/响应头</li>
 *   <li>通过 ReactiveMdcUtils 在响应式链路中正确传递 MDC</li>
 * </ul>
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        final String finalTraceId = traceId;
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange)
                .contextWrite(ctx -> ReactiveMdcUtils.putTraceId(ctx, finalTraceId))
                .doOnEach(signal -> {
                    if (signal.isOnNext()) {
                        mutatedExchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId);
                    }
                })
                .doFinally(signalType -> ReactiveMdcUtils.mdcCleaner().run());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
package com.meowflow.infra.chat;

import com.meowflow.common.context.CancellationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 模型统一网关。
 *
 * <p>对外暴露的单一 AI 调用入口，聚合了路由、健康检查、熔断、重试逻辑。
 * 对比旧的 {@code infra.client.ChatClient} + {@code infra.client.ChatClientAnthropic} 双体系，
 * 此网关统一由 {@link ChatModelRouter} 提供底层能力，工作流节点只需注入此接口。</p>
 *
 * <p>使用方式：
 * <pre>
 * ChatModelGateway gateway; // injected
 * ChatResponse resp = gateway.complete(request);
 * Flux&lt;String&gt; stream = gateway.stream(request);
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelGateway {

    private final ChatModelRouter router;

    /**
     * 同步对话（带重试 + 熔断）。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse complete(ChatRequest request) {
        return router.routeWithCircuitBreaker(request);
    }

    /**
     * 同步对话（带重试 + 熔断 + 取消支持）。
     *
     * @param request 聊天请求
     * @param cancellationToken 取消令牌（可选，为 null 时等价于 {@link #complete(ChatRequest)}）
     * @return 聊天响应
     */
    public ChatResponse complete(ChatRequest request,
                                CancellationToken cancellationToken) {
        if (cancellationToken != null && cancellationToken.isCancelled()) {
            log.debug("Chat request cancelled before call: model={}", request.getModel());
            throw new CancellationException("LLM call cancelled before start: "
                    + cancellationToken.getCancelReason());
        }
        try {
            return router.routeWithCircuitBreaker(request);
        } finally {
            // LLM HTTP 请求本身无法中止，只能在下一次调用时检查取消
            if (cancellationToken != null && cancellationToken.isCancelled()) {
                log.debug("LLM call completed but token was cancelled during call: model={}",
                        request.getModel());
            }
        }
    }

    /**
     * 流式对话。
     *
     * @param request 聊天请求
     * @return 响应内容流（每个元素为一个 chunk）
     */
    public Flux<String> stream(ChatRequest request) {
        return router.routeAndStream(request);
    }

    /**
     * 检查模型是否可用。
     *
     * @param model 模型名称
     * @return 是否可用
     */
    public boolean isHealthy(String model) {
        return router.isModelSupported(model);
    }

    /**
     * 获取支持的模型列表。
     */
    public List<String> getSupportedModels() {
        return router.getSupportedModels();
    }

    /**
     * 取消异常（用于传递取消信号）。
     */
    public static class CancellationException extends RuntimeException {
        public CancellationException(String message) {
            super(message);
        }
    }
}

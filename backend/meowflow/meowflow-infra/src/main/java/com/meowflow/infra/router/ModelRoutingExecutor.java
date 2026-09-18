package com.meowflow.infra.router;

import com.meowflow.infra.client.ChatClient;
import com.meowflow.infra.client.ChatClient.ChatOptions;
import com.meowflow.infra.client.ChatClient.ChatResponse;
import com.meowflow.infra.client.ChatClient.Message;
import com.meowflow.infra.client.ChatClient.StreamCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 模型路由执行器 - 支持多模型候选和故障转移
 * 借鉴 ragent 的 ModelRoutingExecutor 设计
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRoutingExecutor {

    private final ModelHealthStore healthStore;
    
    // 首包探测超时（毫秒）
    private static final long FIRST_PACKET_TIMEOUT_MS = 5000;

    /**
     * 执行带故障转移的对话请求
     */
    public ChatResponse executeWithFallback(
            List<ModelTarget> candidates,
            Function<ModelTarget, ChatClient> clientResolver,
            List<Message> messages,
            ChatOptions options) {
        
        Exception lastError = null;
        
        for (ModelTarget target : candidates) {
            // 健康检查
            if (!healthStore.allowCall(target.modelId())) {
                log.debug("Model {} skipped due to health check", target.modelId());
                continue;
            }
            
            try {
                ChatClient client = clientResolver.apply(target);
                long start = System.currentTimeMillis();
                
                ChatResponse response = client.chat(messages, options);
                healthStore.markSuccess(target.modelId());
                
                log.info("Model {} responded successfully, latency={}ms", target.modelId(), response.latencyMs());
                return new ChatResponse(
                    response.content(),
                    target.modelId(),
                    response.promptTokens(),
                    response.completionTokens(),
                    response.totalTokens(),
                    System.currentTimeMillis() - start,
                    response.finishReason()
                );
            } catch (Exception e) {
                healthStore.markFailure(target.modelId());
                lastError = e;
                log.warn("Model {} failed: {}, trying next...", target.modelId(), e.getMessage());
            }
        }
        
        throw new RuntimeException("All model candidates failed", lastError);
    }

    /**
     * 执行带首包探测的流式对话
     */
    public void executeStreamWithProbe(
            List<ModelTarget> candidates,
            Function<ModelTarget, ChatClient> clientResolver,
            List<Message> messages,
            ChatOptions options,
            StreamCallback callback) {
        
        for (ModelTarget target : candidates) {
            if (!healthStore.allowCall(target.modelId())) {
                log.debug("Model {} skipped due to health check", target.modelId());
                continue;
            }
            
            try {
                ChatClient client = clientResolver.apply(target);
                
                // 创建探测桥接器
                ProbeStreamBridge bridge = new ProbeStreamBridge(callback);
                
                // 启动流式调用
                client.streamChat(messages, options, bridge);
                
                // 等待首包探测结果
                ProbeStreamBridge.ProbeResult result = awaitFirstPacket(bridge, callback, target);
                
                if (result.isSuccess()) {
                    healthStore.markSuccess(target.modelId());
                    return;
                } else {
                    healthStore.markFailure(target.modelId());
                    log.warn("Model {} first packet probe failed, trying next...", target.modelId());
                }
            } catch (Exception e) {
                healthStore.markFailure(target.modelId());
                log.warn("Model {} stream failed: {}, trying next...", target.modelId(), e.getMessage());
            }
        }
        
        callback.onError(new RuntimeException("All model candidates failed"));
    }

    /**
     * 等待首包探测
     */
    private ProbeStreamBridge.ProbeResult awaitFirstPacket(
            ProbeStreamBridge bridge,
            StreamCallback callback,
            ModelTarget target) {
        
        long deadline = System.currentTimeMillis() + FIRST_PACKET_TIMEOUT_MS;
        
        while (System.currentTimeMillis() < deadline) {
            ProbeStreamBridge.ProbeResult result = bridge.getProbeResult();
            if (result != null) {
                return result;
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return new ProbeStreamBridge.ProbeResult(false, "Timeout waiting for first packet");
    }

    /**
     * 探测流式桥接器 - 用于首包探测
     */
    @Slf4j
    public static class ProbeStreamBridge implements StreamCallback {
        
        private final StreamCallback delegate;
        private volatile ProbeResult probeResult;
        private final long startTime;
        private final StringBuilder content = new StringBuilder();
        private volatile boolean firstPacketReceived;

        public ProbeStreamBridge(StreamCallback delegate) {
            this.delegate = delegate;
            this.startTime = System.currentTimeMillis();
            this.firstPacketReceived = false;
        }

        @Override
        public void onChunk(String content, int index) {
            if (!firstPacketReceived) {
                firstPacketReceived = true;
                long latency = System.currentTimeMillis() - startTime;
                probeResult = new ProbeResult(true, null, latency);
                log.info("First packet received from model, latency={}ms", latency);
            }
            this.content.append(content);
            delegate.onChunk(content, index);
        }

        @Override
        public void onComplete(ChatResponse response) {
            if (!firstPacketReceived) {
                firstPacketReceived = true;
                probeResult = new ProbeResult(true, "completed_without_chunk", 0);
            }
            delegate.onComplete(new ChatResponse(
                content.toString(),
                response.model(),
                response.promptTokens(),
                response.completionTokens(),
                response.totalTokens(),
                System.currentTimeMillis() - startTime,
                response.finishReason()
            ));
        }

        @Override
        public void onError(Throwable error) {
            if (!firstPacketReceived) {
                firstPacketReceived = true;
                probeResult = new ProbeResult(false, error.getMessage());
            }
            delegate.onError(error);
        }

        public ProbeResult getProbeResult() {
            return probeResult;
        }

        /**
         * 探测结果
         */
        public record ProbeResult(
            boolean success,
            String message,
            long latencyMs
        ) {
            public ProbeResult(boolean success, String message) {
                this(success, message, 0);
            }
            
            public boolean isSuccess() {
                return success;
            }
        }
    }

    /**
     * 模型目标
     */
    public record ModelTarget(
        String modelId,
        String provider,
        int priority,
        double weight
    ) {
        public ModelTarget(String modelId, String provider) {
            this(modelId, provider, 100, 1.0);
        }
    }
}

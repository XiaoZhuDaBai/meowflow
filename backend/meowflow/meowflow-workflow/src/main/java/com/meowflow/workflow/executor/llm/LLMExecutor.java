package com.meowflow.workflow.executor.llm;

import com.meowflow.common.context.CancellationToken;
import com.meowflow.common.exception.NodeException;
import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatRequest;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.infra.chat.Message;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import com.meowflow.workflow.event.RunEvent;
import com.meowflow.workflow.event.RunEventSink;
import com.meowflow.workflow.config.WorkflowProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LLM 节点执行器。
 * <p>
 * 通过 {@link ChatModelRouter} 将请求路由到对应的 AI 提供商（OpenAI / Claude / 百度等），
 * 支持温度参数、最大 Token、系统提示词等配置。
 * 支持流式输出和容错重试。
 */
@Slf4j
@Component
public class LLMExecutor extends AbstractNodeExecutor {

    private final ChatModelGateway gateway;
    private final WorkflowProperties workflowProperties;
    private RunEventSink runEventSink;

    public LLMExecutor(ChatModelGateway gateway, WorkflowProperties workflowProperties) {
        this.gateway = gateway;
        this.workflowProperties = workflowProperties;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.LLM;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String prompt = resolveToString(input.get("prompt"), context);
        if (prompt == null || prompt.isEmpty()) {
            throw new NodeException(node.getId(), node.getType().getCode(), "Prompt is required for LLM node");
        }

        Object modelValue = input.get("model");
        if (modelValue == null) {
            modelValue = input.get("modelId");
        }
        String model = resolveToString(modelValue != null ? modelValue : "gpt-4o", context);
        Double temperature = parseDouble(input.get("temperature"), 0.7);
        Integer maxTokens = parseInteger(input.get("maxTokens"), null);
        Boolean enableStream = parseBoolean(input.get("enableStream"), false);

        int maxRetries = workflowProperties.getEngine().getRetryMaxAttempts();
        if (maxRetries <= 0) maxRetries = 3;

        List<Message> messages = buildMessages(prompt, input, context);
        log.info("LLM call: model={}, promptLength={}, stream={}, retries={}",
                model, prompt.length(), enableStream, maxRetries);

        ChatResponse response;
        try {
            if (Boolean.TRUE.equals(enableStream)) {
                response = executeStream(node, model, messages, temperature, maxTokens, context, maxRetries);
            } else {
                response = executeSync(model, messages, temperature, maxTokens, context, maxRetries);
            }
        } catch (Exception e) {
            log.error("LLM call failed: model={}, error={}", model, e.getMessage(), e);
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "LLM call failed: " + e.getMessage(), e);
        }

        Map<String, Object> output = new HashMap<>();
        output.put("model", response.getModel());
        output.put("response", response.getContent());
        output.put("finishReason", response.getFinishReason());
        output.put("promptTokens", response.getPromptTokens());
        output.put("completionTokens", response.getCompletionTokens());
        output.put("totalTokens", response.getTotalTokens());
        output.put("latencyMs", response.getLatencyMs());

        log.info("LLM call success: model={}, tokens={}, latency={}ms",
                response.getModel(), response.getTotalTokens(), response.getLatencyMs());

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output)
                .withCostToken(response.getTotalTokens());
    }

    private ChatResponse executeSync(String model, List<Message> messages,
                                               Double temperature, Integer maxTokens,
                                               ExecutionContext context, int maxRetries) {
        ChatRequest request = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        Exception lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return gateway.complete(request, context.getCancellationToken());
            } catch (ChatModelGateway.CancellationException ce) {
                throw ce; // 不要重试取消
            } catch (Exception e) {
                lastError = e;
                log.warn("LLM attempt {}/{} failed: {}", attempt + 1, maxRetries + 1, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("LLM call interrupted", ie);
                    }
                }
            }
        }
        throw new RuntimeException("LLM call failed after " + (maxRetries + 1) + " attempts",
                lastError);
    }

    private ChatResponse executeStream(NodeDefinition node, String model, List<Message> messages,
                                                 Double temperature, Integer maxTokens,
                                                 ExecutionContext context, int maxRetries) {
        ChatRequest request = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        StringBuilder content = new StringBuilder();
        AtomicInteger totalTokens = new AtomicInteger(0);
        long[] latency = {System.currentTimeMillis()};

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            final int currentAttempt = attempt;
            try {
                Flux<String> stream = gateway.stream(request);
                stream.subscribe(
                        chunk -> {
                            content.append(chunk);
                            notifyStreamChunk(context, node, chunk);
                        },
                        error -> log.warn("Stream error (attempt {}): {}", currentAttempt, error.getMessage()),
                        () -> { }
                );
                break; // stream started successfully
            } catch (Exception e) {
                log.warn("Stream attempt {}/{} failed: {}", attempt + 1, maxRetries + 1, e.getMessage());
                if (attempt >= maxRetries) {
                    // 回退到同步调用
                    log.info("Stream failed, falling back to sync call");
                    return executeSync(model, messages, temperature, maxTokens, context, maxRetries);
                }
                try { Thread.sleep(1000L * (attempt + 1)); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("LLM stream interrupted", ie);
                }
            }
        }

        latency[0] = System.currentTimeMillis() - latency[0];
        return ChatResponse.builder()
                .content(content.toString())
                .model(model)
                .finishReason("stop")
                .promptTokens(0)
                .completionTokens(totalTokens.get())
                .latencyMs(latency[0])
                .build();
    }

    private void notifyStreamChunk(ExecutionContext context,
                                   com.meowflow.workflow.definition.NodeDefinition node,
                                   String chunk) {
        log.debug("Stream chunk: {}", chunk);
        if (runEventSink != null && node != null) {
            try {
                runEventSink.append(RunEvent.nodeStreamDelta(
                        context.getExecutionId(), context.getWorkflowId(), node.getId(), chunk));
            } catch (Exception e) {
                log.warn("Failed to emit node stream delta: {}", e.getMessage());
            }
        }
    }

    @Autowired(required = false)
    public void setRunEventSink(RunEventSink runEventSink) {
        this.runEventSink = runEventSink;
    }

    private List<Message> buildMessages(String prompt, Map<String, Object> input, ExecutionContext context) {
        String systemPrompt = resolveToString(input.get("systemPrompt"), context);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            return List.of(Message.system(systemPrompt), Message.user(prompt));
        }
        return List.of(Message.user(prompt));
    }

    protected String resolveToString(Object value, ExecutionContext context) {
        if (value == null) return null;
        if (value instanceof String str) {
            return (context != null) ? (String) context.resolveExpression(str) : str;
        }
        return value.toString();
    }

    protected Double parseDouble(Object value, Double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (Exception e) { return defaultValue; }
    }

    protected Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return defaultValue; }
    }

    protected Boolean parseBoolean(Object value, Boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}

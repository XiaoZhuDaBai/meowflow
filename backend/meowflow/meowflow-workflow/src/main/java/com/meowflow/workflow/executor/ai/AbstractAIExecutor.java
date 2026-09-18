package com.meowflow.workflow.executor.ai;

import com.meowflow.common.util.JsonUtils;
import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatRequest;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.infra.chat.Message;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.executor.AbstractNodeExecutor;

import java.util.List;
import java.util.Map;

/**
 * AI 类节点公共执行逻辑。
 */
public abstract class AbstractAIExecutor extends AbstractNodeExecutor {

    protected final ChatModelGateway gateway;

    protected AbstractAIExecutor(ChatModelGateway gateway) {
        this.gateway = gateway;
    }

    protected ChatResponse complete(ExecutionContext context,
                                    Map<String, Object> input,
                                    String userPrompt,
                                    String systemPrompt) {
        List<Message> messages = systemPrompt != null && !systemPrompt.isBlank()
                ? List.of(Message.system(systemPrompt), Message.user(userPrompt))
                : List.of(Message.user(userPrompt));
        return complete(context, input, messages);
    }

    protected ChatResponse complete(ExecutionContext context,
                                    Map<String, Object> input,
                                    List<Message> messages) {
        return complete(context, input, messages, null);
    }

    protected ChatResponse complete(ExecutionContext context,
                                    Map<String, Object> input,
                                    List<Message> messages,
                                    Map<String, Object> extraParams) {
        Object modelValue = input.get("model");
        if (modelValue == null) {
            modelValue = input.get("modelId");
        }
        String model = resolveString(modelValue != null ? modelValue : "gpt-4o", context);
        Double temperature = parseDouble(input.get("temperature"), 0.3);
        Integer maxTokens = parseInt(input.get("maxTokens"), 2048);

        return gateway.complete(ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .extraParams(extraParams)
                .build(), context.getCancellationToken());
    }

    protected String resolveString(Object value, ExecutionContext context) {
        if (value == null) return null;
        Object resolved = value instanceof String
                ? context.resolveExpression((String) value)
                : value;
        return resolved != null ? resolved.toString() : null;
    }

    protected Map<String, Object> extractJsonObject(String content) {
        if (content == null) return Map.of();
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) return Map.of();
        try {
            Map<String, Object> parsed = JsonUtils.fromJsonToMap(content.substring(start, end + 1));
            return parsed != null ? parsed : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    protected String cleanAnswer(String value) {
        if (value == null) return "";
        String s = value.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.trim();
    }

    protected Double parseDouble(Object value, Double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected Integer parseInt(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

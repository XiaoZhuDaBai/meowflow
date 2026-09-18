package com.meowflow.infra.client;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude API ChatClient 实现
 */
@Slf4j
public class ChatClientAnthropic implements ChatClient {

    private static final String API_VERSION = "2023-06-01";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public ChatClientAnthropic(String baseUrl, String apiKey, String model) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model != null ? model : "claude-3-5-sonnet-20240620";
    }

    @Override
    public ChatResponse chat(List<Message> messages, ChatOptions options) {
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", API_VERSION);

            Map<String, Object> requestBody = buildRequestBody(messages, options);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/v1/messages";

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody(), start);
            }
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Anthropic API 调用失败");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Anthropic chat error", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Anthropic API 调用异常: " + e.getMessage());
        }
    }

    @Override
    public void streamChat(List<Message> messages, ChatOptions options, StreamCallback callback) {
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", API_VERSION);

            Map<String, Object> requestBody = buildRequestBody(messages, options);
            requestBody.put("stream", true);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/v1/messages";

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                parseStreamResponse(response.getBody(), start, callback);
            }
        } catch (Exception e) {
            log.error("Anthropic stream chat error", e);
            callback.onError(e);
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", API_VERSION);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("model", model, "max_tokens", 1), headers);
            restTemplate.exchange(baseUrl + "/v1/messages", HttpMethod.POST, entity, Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Anthropic service unavailable: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildRequestBody(List<Message> messages, ChatOptions options) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);

        StringBuilder content = new StringBuilder();
        for (Message msg : messages) {
            if ("system".equals(msg.role())) {
                body.put("system", msg.content());
            } else {
                if (content.length() > 0) content.append("\n");
                content.append(msg.role()).append(": ").append(msg.content());
            }
        }
        body.put("messages", List.of(Map.of("role", "user", "content", content.toString())));

        if (options != null) {
            if (options.temperature() != null) body.put("temperature", options.temperature());
            if (options.maxTokens() != null) body.put("max_tokens", options.maxTokens());
            else body.put("max_tokens", 2048);
        } else {
            body.put("max_tokens", 2048);
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse parseResponse(Map body, long start) {
        String content = "";
        List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) body.get("content");
        if (contentBlocks != null && !contentBlocks.isEmpty()) {
            Map<String, Object> firstBlock = contentBlocks.get(0);
            if ("text".equals(firstBlock.get("type"))) {
                content = (String) firstBlock.get("text");
            }
        }

        Map<String, Object> usage = (Map<String, Object>) body.getOrDefault("usage", java.util.Collections.emptyMap());
        int promptTokens = 0;
        int completionTokens = 0;
        if (usage != null) {
            Object inputTokens = usage.get("input_tokens");
            Object outputTokens = usage.get("output_tokens");
            if (inputTokens instanceof Number) {
                promptTokens = ((Number) inputTokens).intValue();
            }
            if (outputTokens instanceof Number) {
                completionTokens = ((Number) outputTokens).intValue();
            }
        }

        return new ChatResponse(
                content,
                model,
                promptTokens,
                completionTokens,
                promptTokens + completionTokens,
                System.currentTimeMillis() - start,
                (String) body.get("stop_reason")
        );
    }

    private void parseStreamResponse(String response, long start, StreamCallback callback) {
        StringBuilder content = new StringBuilder();
        int index = 0;

        for (String line : response.split("\n")) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                try {
                    Map<String, Object> chunk = com.meowflow.common.util.JsonUtils.fromJsonToMap(data);
                    String eventType = (String) chunk.get("type");
                    if ("content_block_delta".equals(eventType)) {
                        Map<String, Object> delta = (Map<String, Object>) chunk.get("delta");
                        if (delta != null && "text_delta".equals(delta.get("type"))) {
                            String part = (String) delta.get("text");
                            content.append(part);
                            callback.onChunk(part, index++);
                        }
                    } else if ("message_stop".equals(eventType)) {
                        break;
                    }
                } catch (Exception e) {
                    log.debug("Parse Anthropic stream chunk error: {}", e.getMessage());
                }
            }
        }

        callback.onComplete(new ChatResponse(
                content.toString(), model, 0, 0, 0,
                System.currentTimeMillis() - start, "stop"));
    }
}

package com.meowflow.infra.client;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 ChatClient 实现
 */
@Slf4j
public class ChatClientOpenAI implements ChatClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public ChatClientOpenAI(String baseUrl, String apiKey, String model) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public ChatResponse chat(List<Message> messages, ChatOptions options) {
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = buildRequestBody(messages, options);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/v1/chat/completions";

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody(), start);
            }
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI API 调用失败");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI chat error", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI API 调用异常: " + e.getMessage());
        }
    }

    @Override
    public void streamChat(List<Message> messages, ChatOptions options, StreamCallback callback) {
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = buildRequestBody(messages, options);
            requestBody.put("stream", true);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/v1/chat/completions";

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                parseStreamResponse(response.getBody(), start, callback);
            }
        } catch (Exception e) {
            log.error("OpenAI stream chat error", e);
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
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(baseUrl + "/v1/models", HttpMethod.GET, entity, Map.class);
            return true;
        } catch (Exception e) {
            log.warn("OpenAI service unavailable: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildRequestBody(List<Message> messages, ChatOptions options) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);

        List<Map<String, String>> msgs = messages.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();
        body.put("messages", msgs);

        if (options != null) {
            if (options.temperature() != null) body.put("temperature", options.temperature());
            if (options.maxTokens() != null) body.put("max_tokens", options.maxTokens());
            if (options.topP() != null) body.put("top_p", options.topP());
            if (options.stop() != null) body.put("stop", options.stop());
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse parseResponse(Map body, long start) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");

        Map<String, Object> usage = (Map<String, Object>) body.get("usage");
        int promptTokens = usage != null ? (Integer) usage.getOrDefault("prompt_tokens", 0) : 0;
        int completionTokens = usage != null ? (Integer) usage.getOrDefault("completion_tokens", 0) : 0;
        int totalTokens = usage != null ? (Integer) usage.getOrDefault("total_tokens", 0) : 0;

        return new ChatResponse(
                (String) message.get("content"),
                (String) body.get("model"),
                promptTokens,
                completionTokens,
                totalTokens,
                System.currentTimeMillis() - start,
                (String) choice.get("finish_reason")
        );
    }

    private void parseStreamResponse(String response, long start, StreamCallback callback) {
        String[] lines = response.split("\n");
        StringBuilder content = new StringBuilder();
        int index = 0;

        for (String line : lines) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) break;

                try {
                    Map<String, Object> chunk = JsonUtils.fromJsonToMap(data);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                        if (delta != null && delta.get("content") != null) {
                            String part = (String) delta.get("content");
                            content.append(part);
                            callback.onChunk(part, index++);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Parse stream chunk error: {}", e.getMessage());
                }
            }
        }

        callback.onComplete(new ChatResponse(
                content.toString(), model, 0, 0, 0,
                System.currentTimeMillis() - start, "stop"));
    }
}

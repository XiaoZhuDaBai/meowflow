package com.meowflow.infra.client;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 百度文心一言 ChatClient 实现
 */
@Slf4j
public class ChatClientBaidu implements ChatClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String secretKey;
    private final String model;
    private String accessToken;

    public ChatClientBaidu(String apiKey, String secretKey, String model) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.model = model != null ? model : "ernie-4.0-8k-latest";
    }

    @Override
    public ChatResponse chat(List<Message> messages, ChatOptions options) {
        long start = System.currentTimeMillis();
        try {
            ensureAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = buildRequestBody(messages, options);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=" + accessToken;

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody(), start);
            }
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "百度文心 API 调用失败");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Baidu chat error", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "百度文心 API 调用异常: " + e.getMessage());
        }
    }

    @Override
    public void streamChat(List<Message> messages, ChatOptions options, StreamCallback callback) {
        long start = System.currentTimeMillis();
        try {
            ensureAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = buildRequestBody(messages, options);
            requestBody.put("stream", true);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=" + accessToken;

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                parseStreamResponse(response.getBody(), start, callback);
            }
        } catch (Exception e) {
            log.error("Baidu stream chat error", e);
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
            ensureAccessToken();
            return true;
        } catch (Exception e) {
            log.warn("Baidu service unavailable: {}", e.getMessage());
            return false;
        }
    }

    private synchronized void ensureAccessToken() {
        if (accessToken != null) return;

        try {
            String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=" + apiKey + "&client_secret=" + secretKey;
            ResponseEntity<Map> response = restTemplate.getForEntity(tokenUrl, Map.class);
            if (response.getBody() != null) {
                accessToken = (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "获取百度 access_token 失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(List<Message> messages, ChatOptions options) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);

        List<Map<String, String>> msgs = messages.stream()
                .map(m -> {
                    Map<String, String> msg = new HashMap<>();
                    msg.put("role", m.role());
                    msg.put("content", m.content());
                    return msg;
                })
                .toList();
        body.put("messages", msgs);

        if (options != null) {
            if (options.temperature() != null) body.put("temperature", options.temperature());
            if (options.maxTokens() != null) body.put("max_tokens", options.maxTokens());
        }
        return body;
    }

    private ChatResponse parseResponse(Map body, long start) {
        String content = (String) body.get("result");

        Map<String, Object> usage = (Map<String, Object>) body.get("usage");
        int promptTokens = usage != null ? (Integer) usage.getOrDefault("prompt_tokens", 0) : 0;
        int completionTokens = usage != null ? (Integer) usage.getOrDefault("completion_tokens", 0) : 0;

        return new ChatResponse(
                content, model, promptTokens, completionTokens,
                promptTokens + completionTokens,
                System.currentTimeMillis() - start, (String) body.get("finish_reason"));
    }

    private void parseStreamResponse(String response, long start, StreamCallback callback) {
        StringBuilder content = new StringBuilder();
        int index = 0;

        for (String line : response.split("\n")) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                try {
                    Map<String, Object> chunk = com.meowflow.common.util.JsonUtils.fromJsonToMap(data);
                    String part = (String) chunk.get("result");
                    if (part != null && !part.isEmpty()) {
                        content.append(part);
                        callback.onChunk(part, index++);
                    }
                } catch (Exception e) {
                    log.debug("Parse Baidu stream chunk error: {}", e.getMessage());
                }
            }
        }

        callback.onComplete(new ChatResponse(
                content.toString(), model, 0, 0, 0,
                System.currentTimeMillis() - start, "stop"));
    }
}

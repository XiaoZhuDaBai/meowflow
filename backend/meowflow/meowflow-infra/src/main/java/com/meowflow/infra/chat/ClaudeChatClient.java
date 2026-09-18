package com.meowflow.infra.chat;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "meowflow.ai.anthropic", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ClaudeChatClient implements ChatClient {

    private final String apiKey;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final WebClient webClient;
    @Autowired(required = false)
    private AICircuitBreakerRegistryHolder circuitBreakerHolder;

    private final List<String> supportedModels = List.of(
            "claude-3-5-sonnet-latest",
            "claude-3-5-sonnet-20240620",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229"
    );

    public ClaudeChatClient() {
        this("", "https://api.anthropic.com");
    }

    @Autowired
    public ClaudeChatClient(
            @Value("${meowflow.ai.anthropic.api-key:}") String apiKey,
            @Value("${meowflow.ai.anthropic.base-url:https://api.anthropic.com}") String baseUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.restTemplate = new RestTemplate();
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader("x-api-key", this.apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        if (!isAvailable()) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Claude API key is not configured");
        }
        if (circuitBreakerHolder != null && circuitBreakerHolder.forProvider("anthropic") != null) {
            try {
                return circuitBreakerHolder.forProvider("anthropic").executeSupplier(() -> invokeClaudeApi(request));
            } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
                return fallbackResponse(request);
            }
        }
        return invokeClaudeApi(request);
    }

    private ChatResponse invokeClaudeApi(ChatRequest request) {
        try {
            long start = System.currentTimeMillis();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildAnthropicRequest(request), headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(messagesUrl(), entity, Map.class);
            if (response == null) {
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Empty response from Claude API");
            }
            return parseAnthropicResponse(response, System.currentTimeMillis() - start);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Claude chat error", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Claude API call failed: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> streamComplete(ChatRequest request) {
        if (!isAvailable()) {
            return Flux.error(new BizException(ResultCode.SERVICE_UNAVAILABLE, "Claude API key is not configured"));
        }
        Map<String, Object> body = buildAnthropicRequest(request);
        body.put("stream", true);
        return webClient.post()
                .uri(messagesPath())
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .onErrorMap(e -> new BizException(ResultCode.SERVICE_UNAVAILABLE, "Claude streaming failed: " + e.getMessage()));
    }

    @Override
    public List<String> getSupportedModels() {
        return supportedModels;
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    private ChatResponse fallbackResponse(ChatRequest request) {
        return ChatResponse.success(
                "Claude service is temporarily unavailable. Please try again later.",
                request.getModel(),
                "fallback",
                0,
                0,
                0L
        );
    }

    private Map<String, Object> buildAnthropicRequest(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1024);
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }

        List<Map<String, String>> messages = new ArrayList<>();
        List<String> systemParts = new ArrayList<>();
        for (Message message : request.getMessages()) {
            if ("system".equalsIgnoreCase(message.getRole())) {
                systemParts.add(message.getContent());
                continue;
            }
            messages.add(Map.of("role", message.getRole(), "content", message.getContent()));
        }
        body.put("messages", messages);
        String systemPrompt = request.getExtraParams() != null && request.getExtraParams().get("system") != null
                ? request.getExtraParams().get("system").toString()
                : String.join("\n", systemParts);
        if (!systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse parseAnthropicResponse(Map<String, Object> response, long latencyMs) {
        String content = null;
        Object contentObj = response.get("content");
        if (contentObj instanceof List<?> contentList && !contentList.isEmpty()
                && contentList.get(0) instanceof Map<?, ?> block && block.get("text") != null) {
            content = block.get("text").toString();
        }
        String model = response.get("model") != null ? response.get("model").toString() : null;
        String finishReason = response.get("stop_reason") != null ? response.get("stop_reason").toString() : null;
        Integer promptTokens = null;
        Integer completionTokens = null;
        if (response.get("usage") instanceof Map<?, ?> usage) {
            if (usage.get("input_tokens") instanceof Number n) promptTokens = n.intValue();
            if (usage.get("output_tokens") instanceof Number n) completionTokens = n.intValue();
        }
        return ChatResponse.success(content, model, finishReason, promptTokens, completionTokens, latencyMs);
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "https://api.anthropic.com" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String messagesUrl() {
        return baseUrl + messagesPath();
    }

    private String messagesPath() {
        return baseUrl.endsWith("/v1") ? "/messages" : "/v1/messages";
    }
}

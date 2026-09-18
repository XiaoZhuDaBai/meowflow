package com.meowflow.infra.chat;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OpenAI Chat Client with circuit breaker integration.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "meowflow.ai.openai.enabled", havingValue = "true", matchIfMissing = false)
public class OpenAIChatClient implements ChatClient {

    private final String apiKey;
    private final String baseUrl;
    private final List<String> supportedModels;
    private final RestTemplate restTemplate;
    private final WebClient webClient;

    @Autowired(required = false)
    private AICircuitBreakerRegistryHolder circuitBreakerHolder;

    public OpenAIChatClient(
            @Value("${meowflow.ai.openai.api-key:}") String apiKey,
            @Value("${meowflow.ai.openai.base-url:https://api.openai.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.supportedModels = Arrays.asList("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo");
        this.restTemplate = new RestTemplate();
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        if (!isAvailable()) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI API key is not configured");
        }

        if (circuitBreakerHolder != null && circuitBreakerHolder.forProvider("openai") != null) {
            AtomicReference<ChatResponse> responseRef = new AtomicReference<>();
            try {
                circuitBreakerHolder.forProvider("openai").executeSupplier(() -> {
                    responseRef.set(invokeApi(request));
                    return null;
                });
            } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
                log.warn("OpenAI circuit breaker is OPEN, returning fallback response");
                return fallbackResponse(request);
            } catch (Exception e) {
                if (e instanceof BizException) {
                    throw (BizException) e;
                }
                log.error("OpenAI chat error", e);
                return fallbackResponse(request);
            }
            if (responseRef.get() != null) {
                return responseRef.get();
            }
        }

        return invokeApi(request);
    }

    private ChatResponse invokeApi(ChatRequest request) {
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = buildRequestBody(request);

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(requestBody, headers);
            String url = chatCompletionsUrl();

            var response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody(), request.getModel(), start);
            }
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI API call failed");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI chat error", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI API error: " + e.getMessage());
        }
    }

    private ChatResponse fallbackResponse(ChatRequest request) {
        log.info("Fallback response for OpenAI request: model={}", request.getModel());
        return ChatResponse.success(
                "OpenAI service is temporarily unavailable. Please try again later.",
                request.getModel(),
                "fallback",
                0,
                0,
                0L
        );
    }

    @Override
    public Flux<String> streamComplete(ChatRequest request) {
        if (!isAvailable()) {
            return Flux.error(new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI API key is not configured"));
        }

        Map<String, Object> requestBody = buildRequestBody(request);
        requestBody.put("stream", true);

        return webClient.post()
                .uri(chatCompletionsPath())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .filter(data -> !"[DONE]".equals(data))
                .flatMap(this::parseStreamChunk)
                .onErrorResume(e -> {
                    log.error("OpenAI stream error", e);
                    return Flux.error(new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI stream error: " + e.getMessage()));
                });
    }

    private Mono<String> parseStreamChunk(String data) {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> chunk = parseJson(data);
                if (chunk == null) return "";

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                if (choices == null || choices.isEmpty()) return "";

                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                if (delta == null) return "";

                Object content = delta.get("content");
                return content != null ? content.toString() : "";
            } catch (Exception e) {
                log.debug("Parse stream chunk error: {}", e.getMessage());
                return "";
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.debug("JSON parse error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> getSupportedModels() {
        return supportedModels;
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "https://api.openai.com" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String chatCompletionsUrl() {
        return baseUrl + chatCompletionsPath();
    }

    private String chatCompletionsPath() {
        return baseUrl.endsWith("/v1") ? "/chat/completions" : "/v1/chat/completions";
    }

    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());

        List<Map<String, String>> messages = request.getMessages().stream()
                .map(m -> {
                    Map<String, String> msg = new HashMap<>();
                    msg.put("role", m.getRole());
                    msg.put("content", m.getContent());
                    if (m.getName() != null) msg.put("name", m.getName());
                    if (m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                        msg.put("tool_calls", com.meowflow.common.util.JsonUtils.toJson(m.getToolCalls()));
                    }
                    return msg;
                })
                .toList();
        body.put("messages", messages);

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getExtraParams() != null) {
            body.putAll(request.getExtraParams());
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse parseResponse(Map body, String model, long start) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Empty response from OpenAI");
        }

        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        List<Map<String, Object>> toolCalls = parseToolCalls(message);

        Map<String, Object> usage = (Map<String, Object>) body.get("usage");
        int promptTokens = usage != null ? ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue() : 0;
        int completionTokens = usage != null ? ((Number) usage.getOrDefault("completion_tokens", 0)).intValue() : 0;

        return ChatResponse.builder()
                .content(message != null ? (String) message.get("content") : "")
                .model((String) body.getOrDefault("model", model))
                .finishReason((String) choice.get("finish_reason"))
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .latencyMs(System.currentTimeMillis() - start)
                .toolCalls(toolCalls)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseToolCalls(Map<String, Object> message) {
        if (message == null) return java.util.List.of();
        Object raw = message.get("tool_calls");
        if (!(raw instanceof List<?> list)) return java.util.List.of();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }
}

package com.meowflow.infra.embedding;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 Embedding 客户端，用于前端配置的动态模型。
 */
@Slf4j
public class OpenAICompatibleEmbeddingClient implements EmbeddingClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public OpenAICompatibleEmbeddingClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "text-embedding-3-small" : model.trim();
    }

    @Override
    public EmbeddingResult embed(String text) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> response = exchange(Map.of("model", model, "input", text));
            return parseOne(text, response, System.currentTimeMillis() - start);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding request failed", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Embedding 调用失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EmbeddingResult> embedBatch(List<String> texts) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> response = exchange(Map.of("model", model, "input", texts));
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data == null) throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Embedding 返回数据为空");
            List<EmbeddingResult> results = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                results.add(parseOne(texts.get(i), data.get(i), System.currentTimeMillis() - start));
            }
            return results;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Batch embedding request failed", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "批量 Embedding 调用失败: " + e.getMessage());
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public int getDimension() {
        String lower = model.toLowerCase();
        if (lower.contains("large")) return 3072;
        return 1536;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchange(Map<String, Object> body) {
        if (apiKey.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "Embedding 模型未配置 API Key");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        Map<String, Object> response = restTemplate.postForObject(
                embeddingsUrl(), new HttpEntity<>(body, headers), Map.class);
        if (response == null) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Embedding 接口返回为空");
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private EmbeddingResult parseOne(String text, Map<String, Object> response, long latencyMs) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        if (data == null || data.isEmpty()) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Embedding 返回数据为空");
        }
        List<Number> embedding = (List<Number>) data.get(0).get("embedding");
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) vector[i] = embedding.get(i).floatValue();
        int tokens = 0;
        if (response.get("usage") instanceof Map<?, ?> usage && usage.get("prompt_tokens") instanceof Number n) {
            tokens = n.intValue();
        }
        return new EmbeddingResult(text, vector, tokens, latencyMs);
    }

    private String embeddingsUrl() {
        if (baseUrl.endsWith("/embeddings")) return baseUrl;
        return baseUrl.endsWith("/v1") ? baseUrl + "/embeddings" : baseUrl + "/v1/embeddings";
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "https://api.openai.com" : value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }
}

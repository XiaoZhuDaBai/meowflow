package com.meowflow.infra.embedding;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 阿里通义 Embedding 客户端配置
 */
@Slf4j
@Configuration
public class EmbeddingClientAliConfig {

    @Value("${embedding.ali.base-url:https://dashscope.aliyuncs.com}")
    private String baseUrl;

    @Value("${embedding.ali.api-key:}")
    private String apiKey;

    @Value("${embedding.ali.model:text-embedding-ada-002}")
    private String model;

    @Bean
    public EmbeddingClient aliEmbeddingClient() {
        return new AliEmbeddingClient(baseUrl, apiKey, model);
    }

    static class AliEmbeddingClient implements EmbeddingClient {
        private final RestTemplate restTemplate;
        private final String baseUrl;
        private final String apiKey;
        private final String model;

        public AliEmbeddingClient(String baseUrl, String apiKey, String model) {
            this.restTemplate = new RestTemplate();
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.apiKey = apiKey;
            this.model = model != null ? model : "text-embedding-ada-002";
        }

        @Override
        public EmbeddingResult embed(String text) {
            long start = System.currentTimeMillis();
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("input", text);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                String url = baseUrl + "/v1/embeddings";

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return parseResponse(text, response.getBody(), start);
                }
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI Embedding API 调用失败");
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("OpenAI embed error", e);
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI Embedding API 调用异常: " + e.getMessage());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<EmbeddingResult> embedBatch(List<String> texts) {
            long start = System.currentTimeMillis();
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("input", texts);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                String url = baseUrl + "/v1/embeddings";

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<EmbeddingResult> results = new ArrayList<>();
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                    for (int i = 0; i < data.size(); i++) {
                        Map<String, Object> item = data.get(i);
                        List<Number> embedding = (List<Number>) item.get("embedding");
                        float[] vector = new float[embedding.size()];
                        for (int j = 0; j < embedding.size(); j++) {
                            vector[j] = embedding.get(j).floatValue();
                        }
                        Map<String, Object> usage = (Map<String, Object>) response.getBody().get("usage");
                        int tokens = usage != null ? ((Number) usage.get("prompt_tokens")).intValue() / texts.size() : 0;
                        results.add(new EmbeddingResult(texts.get(i), vector, tokens, System.currentTimeMillis() - start));
                    }
                    return results;
                }
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "OpenAI Embedding API 调用失败");
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("OpenAI embed batch error", e);
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "批量 Embedding API 调用异常: " + e.getMessage());
            }
        }

        @Override
        public String getModelName() {
            return model;
        }

        @Override
        public int getDimension() {
            return switch (model) {
                case "text-embedding-ada-002", "text-embedding-3-small" -> 1536;
                case "text-embedding-3-large" -> 3072;
                default -> 1536;
            };
        }

        @SuppressWarnings("unchecked")
        private EmbeddingResult parseResponse(String text, Map body, long start) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            Map<String, Object> item = data.get(0);
            List<Number> embedding = (List<Number>) item.get("embedding");
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }

            Map<String, Object> usage = (Map<String, Object>) body.get("usage");
            int tokens = usage != null ? ((Number) usage.get("prompt_tokens")).intValue() : 0;

            return new EmbeddingResult(text, vector, tokens, System.currentTimeMillis() - start);
        }
    }
}

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
 * OpenAI 兼容的 Embedding 客户端实现
 */
@Slf4j
@Configuration
public class EmbeddingClientOpenAI {

    @Value("${embedding.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${embedding.openai.api-key:}")
    private String apiKey;

    @Value("${embedding.openai.model:text-embedding-v2}")
    private String model;

    @Bean
    public EmbeddingClient embeddingClient() {
        return new OpenAIEmbeddingClient(baseUrl, apiKey, model);
    }

    static class OpenAIEmbeddingClient implements EmbeddingClient {
        private final RestTemplate restTemplate;
        private final String baseUrl;
        private final String apiKey;
        private final String model;

        public OpenAIEmbeddingClient(String baseUrl, String apiKey, String model) {
            this.restTemplate = new RestTemplate();
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.apiKey = apiKey;
            this.model = model != null ? model : "text-embedding-v2";
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
                String url = baseUrl + "/services/embeddings/embedding-creation";

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return parseResponse(text, response.getBody(), start);
                }
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "阿里 Embedding API 调用失败");
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("Ali embed error", e);
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "阿里 Embedding API 调用异常: " + e.getMessage());
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
                String url = baseUrl + "/services/embeddings/embedding-creation";

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<EmbeddingResult> results = new ArrayList<>();
                    List<Map<String, Object>> output = (List<Map<String, Object>>) response.getBody().get("output");
                    for (int i = 0; i < output.size(); i++) {
                        Map<String, Object> item = output.get(i);
                        List<Number> embeddingData = (List<Number>) item.get("embedding");
                        float[] vector = new float[embeddingData.size()];
                        for (int j = 0; j < embeddingData.size(); j++) {
                            vector[j] = embeddingData.get(j).floatValue();
                        }
                        results.add(new EmbeddingResult(texts.get(i), vector, 0, System.currentTimeMillis() - start));
                    }
                    return results;
                }
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "阿里 Embedding API 调用失败");
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("Ali embed batch error", e);
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "批量 Embedding API 调用异常: " + e.getMessage());
            }
        }

        @Override
        public String getModelName() {
            return model;
        }

        @Override
        public int getDimension() {
            return 1536;
        }

        @SuppressWarnings("unchecked")
        private EmbeddingResult parseResponse(String text, Map body, long start) {
            List<Map<String, Object>> output = (List<Map<String, Object>>) body.get("output");
            if (output == null || output.isEmpty()) {
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "Embedding 结果为空");
            }
            List<Number> embeddingData = (List<Number>) output.get(0).get("embedding");
            float[] vector = new float[embeddingData.size()];
            for (int i = 0; i < embeddingData.size(); i++) {
                vector[i] = embeddingData.get(i).floatValue();
            }
            return new EmbeddingResult(text, vector, 0, System.currentTimeMillis() - start);
        }
    }
}

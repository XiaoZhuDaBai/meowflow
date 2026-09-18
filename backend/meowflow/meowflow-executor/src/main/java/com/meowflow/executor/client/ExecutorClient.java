package com.meowflow.executor.client;

import com.meowflow.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 执行器 REST 客户端
 */
@Slf4j
public class ExecutorClient {

    private final RestTemplate restTemplate;
    private final ExecutorNodeConfig config;

    public ExecutorClient(ExecutorNodeConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    public <T> T submitTask(String taskId, String taskType, Map<String, Object> payload) {
        try {
            String url = config.getUrl() + "/api/executor/tasks";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(
                    "taskId", taskId,
                    "taskType", taskType,
                    "payload", payload
            ), headers);

            ResponseEntity<Result> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Result.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                T result = (T) response.getBody().getData();
                return result;
            }
            throw new RuntimeException("Task submission failed: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Submit task error: {}", taskId, e);
            throw new RuntimeException("Task submission error: " + e.getMessage(), e);
        }
    }

    public void cancelTask(String taskId) {
        try {
            String url = config.getUrl() + "/api/executor/tasks/" + taskId + "/cancel";
            restTemplate.delete(url);
            log.info("Cancelled task: {} on executor: {}", taskId, config.getExecutorId());
        } catch (Exception e) {
            log.error("Cancel task error: {}", taskId, e);
            throw new RuntimeException("Cancel task error: " + e.getMessage(), e);
        }
    }

    public Result<?> getTaskStatus(String taskId) {
        try {
            String url = config.getUrl() + "/api/executor/tasks/" + taskId;
            return restTemplate.getForObject(url, Result.class);
        } catch (Exception e) {
            log.error("Get task status error: {}", taskId, e);
            throw new RuntimeException("Get task status error: " + e.getMessage(), e);
        }
    }

    public Result<?> getExecutorStatus() {
        try {
            String url = config.getUrl() + "/api/executor/status";
            return restTemplate.getForObject(url, Result.class);
        } catch (Exception e) {
            log.error("Get executor status error", e);
            throw new RuntimeException("Get executor status error: " + e.getMessage(), e);
        }
    }

    public boolean isHealthy() {
        try {
            String url = config.getUrl() + "/api/executor/health";
            ResponseEntity<Result> response = restTemplate.getForEntity(url, Result.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Executor health check failed: {}", config.getExecutorId());
            return false;
        }
    }

    public ExecutorNodeConfig getConfig() {
        return config;
    }
}

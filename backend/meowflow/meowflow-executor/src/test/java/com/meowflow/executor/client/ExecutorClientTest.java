package com.meowflow.executor.client;

import com.meowflow.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExecutorClient 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExecutorClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ExecutorClient client;
    private ExecutorNodeConfig config;

    @BeforeEach
    void setUp() {
        config = new ExecutorNodeConfig();
        config.setExecutorId("executor-1");
        config.setHost("localhost");
        config.setPort(8080);

        client = new ExecutorClient(config);
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
    }

    @Test
    void submitTask_shouldSubmitSuccessfully() {
        // Given
        Map<String, Object> payload = Map.of("data", "value");
        Result<String> result = Result.success("task-result");
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Result.class)
        )).thenReturn(ResponseEntity.ok(result));

        // When
        String taskResult = client.submitTask("task-1", "HTTP", payload);

        // Then
        assertThat(taskResult).isEqualTo("task-result");
        verify(restTemplate).exchange(
                eq("http://localhost:8080/api/executor/tasks"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Result.class)
        );
    }

    @Test
    void submitTask_whenNon2xxResponse_shouldThrowException() {
        // Given
        Map<String, Object> payload = Map.of("data", "value");
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Result.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // When/Then
        assertThatThrownBy(() -> client.submitTask("task-1", "HTTP", payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Task submission failed");
    }

    @Test
    void submitTask_whenRestTemplateThrows_shouldWrapException() {
        // Given
        Map<String, Object> payload = Map.of("data", "value");
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Result.class)
        )).thenThrow(new RuntimeException("Network error"));

        // When/Then
        assertThatThrownBy(() -> client.submitTask("task-1", "HTTP", payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Task submission error");
    }

    @Test
    void cancelTask_shouldDeleteTask() {
        // When
        client.cancelTask("task-123");

        // Then
        verify(restTemplate).delete("http://localhost:8080/api/executor/tasks/task-123/cancel");
    }

    @Test
    void cancelTask_whenRestTemplateThrows_shouldWrapException() {
        // Given
        doThrow(new RuntimeException("Network error"))
                .when(restTemplate).delete(anyString());

        // When/Then
        assertThatThrownBy(() -> client.cancelTask("task-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cancel task error");
    }

    @Test
    void getTaskStatus_shouldReturnStatus() {
        // Given
        Result<Map<String, Object>> result = Result.success(Map.of("status", "running"));
        
        when(restTemplate.getForObject(anyString(), eq(Result.class)))
                .thenReturn(result);

        // When
        Result<?> status = client.getTaskStatus("task-123");

        // Then
        assertThat(status).isNotNull();
        assertThat(status.isSuccess()).isTrue();
        verify(restTemplate).getForObject("http://localhost:8080/api/executor/tasks/task-123", Result.class);
    }

    @Test
    void getTaskStatus_whenRestTemplateThrows_shouldWrapException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(Result.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When/Then
        assertThatThrownBy(() -> client.getTaskStatus("task-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Get task status error");
    }

    @Test
    void getExecutorStatus_shouldReturnExecutorStatus() {
        // Given
        Result<Map<String, Object>> result = Result.success(Map.of("health", "ok"));
        
        when(restTemplate.getForObject(anyString(), eq(Result.class)))
                .thenReturn(result);

        // When
        Result<?> status = client.getExecutorStatus();

        // Then
        assertThat(status).isNotNull();
        verify(restTemplate).getForObject("http://localhost:8080/api/executor/status", Result.class);
    }

    @Test
    void getExecutorStatus_whenRestTemplateThrows_shouldWrapException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(Result.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When/Then
        assertThatThrownBy(() -> client.getExecutorStatus())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Get executor status error");
    }

    @Test
    void isHealthy_whenHealthEndpointReturns200_shouldReturnTrue() {
        // Given
        Result<String> result = Result.success("OK");
        
        when(restTemplate.getForEntity(anyString(), eq(Result.class)))
                .thenReturn(ResponseEntity.ok(result));

        // When
        boolean healthy = client.isHealthy();

        // Then
        assertThat(healthy).isTrue();
    }

    @Test
    void isHealthy_whenHealthEndpointFails_shouldReturnFalse() {
        // Given
        when(restTemplate.getForEntity(anyString(), eq(Result.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());

        // When
        boolean healthy = client.isHealthy();

        // Then
        assertThat(healthy).isFalse();
    }

    @Test
    void isHealthy_whenNetworkError_shouldReturnFalse() {
        // Given
        when(restTemplate.getForEntity(anyString(), eq(Result.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // When
        boolean healthy = client.isHealthy();

        // Then
        assertThat(healthy).isFalse();
    }

    @Test
    void getConfig_shouldReturnConfig() {
        // When
        ExecutorNodeConfig returnedConfig = client.getConfig();

        // Then
        assertThat(returnedConfig).isSameAs(config);
        assertThat(returnedConfig.getExecutorId()).isEqualTo("executor-1");
        assertThat(returnedConfig.getUrl()).isEqualTo("http://localhost:8080");
    }
}

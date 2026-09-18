package com.meowflow.executor.service;

import com.meowflow.common.client.ExecutionLogCreateRequest;
import com.meowflow.common.client.ExecutionLogResponse;
import com.meowflow.common.client.MonitorFeignClient;
import com.meowflow.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MonitorService 鍗曞厓娴嬭瘯
 */
@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private MonitorFeignClient monitorClient;

    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorService();
        ReflectionTestUtils.setField(monitorService, "monitorClient", monitorClient);
        ReflectionTestUtils.setField(monitorService, "monitorEnabled", true);
    }

    @Test
    void logStart_whenMonitorEnabled_shouldCallClient() {
        // Given
        ExecutionLogCreateRequest request = new ExecutionLogCreateRequest();
        request.setWorkflowId("wf-1");
        request.setNodeId("node-1");

        ExecutionLogResponse response = new ExecutionLogResponse();
        response.setId(123L);

        when(monitorClient.logStart(any())).thenReturn(Result.success(response));

        // When
        ExecutionLogResponse result = monitorService.logStart("log-1", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(123L);
        verify(monitorClient).logStart(request);
    }

    @Test
    void logStart_whenMonitorDisabled_shouldReturnNull() {
        // Given
        ReflectionTestUtils.setField(monitorService, "monitorEnabled", false);
        ExecutionLogCreateRequest request = new ExecutionLogCreateRequest();

        // When
        ExecutionLogResponse result = monitorService.logStart("log-1", request);

        // Then
        assertThat(result).isNull();
        verify(monitorClient, never()).logStart(any());
    }

    @Test
    void logStart_whenClientUnavailable_shouldReturnNull() {
        // Given
        ReflectionTestUtils.setField(monitorService, "monitorClient", null);
        ExecutionLogCreateRequest request = new ExecutionLogCreateRequest();

        // When
        ExecutionLogResponse result = monitorService.logStart("log-1", request);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void logStart_whenClientThrows_shouldHandleGracefully() {
        // Given
        ExecutionLogCreateRequest request = new ExecutionLogCreateRequest();
        when(monitorClient.logStart(any())).thenThrow(new RuntimeException("Network error"));

        // When
        ExecutionLogResponse result = monitorService.logStart("log-1", request);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void logComplete_whenMonitorEnabled_shouldCallClient() {
        // Given
        ExecutionLogResponse response = new ExecutionLogResponse();
        response.setId(123L);

        when(monitorClient.logComplete(anyString(), anyString())).thenReturn(Result.success(response));

        // When
        ExecutionLogResponse result = monitorService.logComplete("log-123", "output data");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(123L);
        verify(monitorClient).logComplete("log-123", "output data");
    }

    @Test
    void logComplete_whenLogIdNull_shouldReturnNull() {
        // When
        ExecutionLogResponse result = monitorService.logComplete(null, "output");

        // Then
        assertThat(result).isNull();
        verify(monitorClient, never()).logComplete(anyString(), anyString());
    }

    @Test
    void logError_whenMonitorEnabled_shouldCallClient() {
        // Given
        ExecutionLogResponse response = new ExecutionLogResponse();
        response.setId(123L);

        when(monitorClient.logError(anyString(), anyString())).thenReturn(Result.success(response));

        // When
        ExecutionLogResponse result = monitorService.logError("log-123", "error message");

        // Then
        assertThat(result).isNotNull();
        verify(monitorClient).logError("log-123", "error message");
    }

    @Test
    void logError_whenClientThrows_shouldHandleGracefully() {
        // Given
        when(monitorClient.logError(anyString(), anyString()))
                .thenThrow(new RuntimeException("Network error"));

        // When
        ExecutionLogResponse result = monitorService.logError("log-123", "error");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void recordWorkflowExecution_whenMonitorEnabled_shouldCallClient() {
        // When
        monitorService.recordWorkflowExecution("wf-1", "Test Workflow", 1, "manual", true, 1000L);

        // Then
        verify(monitorClient).recordWorkflowExecution("wf-1", "Test Workflow", 1, "manual", true, 1000L);
    }

    @Test
    void recordWorkflowExecution_whenMonitorDisabled_shouldNotCallClient() {
        // Given
        ReflectionTestUtils.setField(monitorService, "monitorEnabled", false);

        // When
        monitorService.recordWorkflowExecution("wf-1", "Test", 1, "manual", true, 1000L);

        // Then
        verify(monitorClient, never()).recordWorkflowExecution(anyString(), anyString(), anyInt(), anyString(), anyBoolean(), anyLong());
    }

    @Test
    void recordWorkflowExecution_whenClientThrows_shouldHandleGracefully() {
        // Given
        doThrow(new RuntimeException("Network error"))
                .when(monitorClient).recordWorkflowExecution(anyString(), anyString(), anyInt(), anyString(), anyBoolean(), anyLong());

        // When/Then - should not throw
        monitorService.recordWorkflowExecution("wf-1", "Test", 1, "manual", true, 1000L);
    }

    @Test
    void recordNodeExecution_whenMonitorEnabled_shouldCallClient() {
        // When
        monitorService.recordNodeExecution("wf-1", "node-1", "HTTP", true, 500L);

        // Then
        verify(monitorClient).recordNodeExecution("wf-1", "node-1", "HTTP", true, 500L);
    }

    @Test
    void recordNodeExecution_whenClientUnavailable_shouldNotThrow() {
        // Given
        ReflectionTestUtils.setField(monitorService, "monitorClient", null);

        // When/Then - should not throw
        monitorService.recordNodeExecution("wf-1", "node-1", "HTTP", true, 500L);
    }

    @Test
    void buildStartRequest_shouldCreateRequestWithAllFields() {
        // When
        ExecutionLogCreateRequest request = monitorService.buildStartRequest(
                "wf-1",
                "Test Workflow",
                "node-1",
                "HTTP Node",
                "HTTP",
                "{\"url\": \"https://api.example.com\"}"
        );

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getWorkflowId()).isEqualTo("wf-1");
        assertThat(request.getWorkflowName()).isEqualTo("Test Workflow");
        assertThat(request.getNodeId()).isEqualTo("node-1");
        assertThat(request.getNodeName()).isEqualTo("HTTP Node");
        assertThat(request.getNodeType()).isEqualTo("HTTP");
        assertThat(request.getInputData()).isEqualTo("{\"url\": \"https://api.example.com\"}");
        assertThat(request.getStartTime()).isNotNull();
    }
}

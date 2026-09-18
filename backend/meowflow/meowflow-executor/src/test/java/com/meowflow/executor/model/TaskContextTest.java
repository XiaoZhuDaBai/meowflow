package com.meowflow.executor.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskContext 鍗曞厓娴嬭瘯
 */
class TaskContextTest {

    @Test
    void constructor_shouldInitializeEmptyCollections() {
        // When
        TaskContext context = new TaskContext();

        // Then
        assertThat(context.getVariables()).isNotNull().isEmpty();
        assertThat(context.getSharedData()).isNotNull().isEmpty();
        assertThat(context.getHeaders()).isNotNull().isEmpty();
        assertThat(context.getStartTime()).isNotNull();
    }

    @Test
    void of_shouldCreateContextWithWorkflowInfo() {
        // When
        TaskContext context = TaskContext.of("exec-123", "wf-456");

        // Then
        assertThat(context.getExecutionId()).isEqualTo("exec-123");
        assertThat(context.getWorkflowId()).isEqualTo("wf-456");
        assertThat(context.getStartTime()).isNotNull();
    }

    @Test
    void setVariable_shouldStoreValue() {
        // Given
        TaskContext context = new TaskContext();

        // When
        context.setVariable("key1", "value1");
        context.setVariable("key2", 42);

        // Then
        assertThat(context.getVariable("key1")).isEqualTo("value1");
        assertThat(context.getVariable("key2")).isEqualTo(42);
    }

    @Test
    void getVariable_shouldReturnNullForMissingKey() {
        // Given
        TaskContext context = new TaskContext();

        // When
        Object value = context.getVariable("non-existent");

        // Then
        assertThat(value).isNull();
    }

    @Test
    void setShared_shouldStoreSharedData() {
        // Given
        TaskContext context = new TaskContext();

        // When
        context.setShared("shared-key", "shared-value");

        // Then
        assertThat(context.getShared("shared-key")).isEqualTo("shared-value");
    }

    @Test
    void getAllVariables_shouldMergeBothMaps() {
        // Given
        TaskContext context = new TaskContext();
        context.setVariable("var1", "value1");
        context.setVariable("var2", "value2");
        context.setShared("shared1", "sharedValue1");

        // When
        Map<String, Object> all = context.getAllVariables();

        // Then
        assertThat(all).hasSize(3);
        assertThat(all).containsEntry("var1", "value1");
        assertThat(all).containsEntry("var2", "value2");
        assertThat(all).containsEntry("shared1", "sharedValue1");
    }

    @Test
    void getElapsedMs_shouldCalculateTimeDifference() throws Exception {
        // Given
        TaskContext context = new TaskContext();
        Thread.sleep(100);

        // When
        long elapsed = context.getElapsedMs();

        // Then
        assertThat(elapsed).isGreaterThanOrEqualTo(100);
    }

    @Test
    void getElapsedMs_whenStartTimeNull_shouldReturnZero() {
        // Given
        TaskContext context = new TaskContext();
        context.setStartTime(null);

        // When
        long elapsed = context.getElapsedMs();

        // Then
        assertThat(elapsed).isEqualTo(0);
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Given
        TaskContext context = new TaskContext();

        // When
        context.setExecutionId("exec-1");
        context.setWorkflowId("wf-1");
        context.setNodeId("node-1");
        context.setTaskId("task-1");
        context.setCreatedBy("user-123");
        context.setCallbackUrl("http://callback.com");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        context.setHeaders(headers);

        // Then
        assertThat(context.getExecutionId()).isEqualTo("exec-1");
        assertThat(context.getWorkflowId()).isEqualTo("wf-1");
        assertThat(context.getNodeId()).isEqualTo("node-1");
        assertThat(context.getTaskId()).isEqualTo("task-1");
        assertThat(context.getCreatedBy()).isEqualTo("user-123");
        assertThat(context.getCallbackUrl()).isEqualTo("http://callback.com");
        assertThat(context.getHeaders()).containsEntry("Authorization", "Bearer token");
    }

    @Test
    void variables_shouldBeIndependent() {
        // Given
        TaskContext context = new TaskContext();
        context.setVariable("key", "var-value");
        context.setShared("key", "shared-value");

        // When
        Object varValue = context.getVariable("key");
        Object sharedValue = context.getShared("key");

        // Then
        assertThat(varValue).isEqualTo("var-value");
        assertThat(sharedValue).isEqualTo("shared-value");
    }

    @Test
    void getAllVariables_sharedDataShouldOverrideVariables() {
        // Given
        TaskContext context = new TaskContext();
        context.setVariable("key", "var-value");
        context.setShared("key", "shared-value");

        // When
        Map<String, Object> all = context.getAllVariables();

        // Then
        assertThat(all.get("key")).isEqualTo("shared-value");
    }
}

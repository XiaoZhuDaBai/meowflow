package com.meowflow.executor.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskResult 单元测试
 */
class TaskResultTest {

    @Test
    void success_shouldCreateSuccessResult() {
        // When
        TaskResult result = TaskResult.success("task-001", "Success data", 100L);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("Success data");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void failure_shouldCreateFailureResult() {
        // When
        TaskResult result = TaskResult.failure("task-001", "Error message", 50L);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Error message");
        assertThat(result.getData()).isNull();
    }

    @Test
    void timeout_shouldCreateTimeoutResult() {
        // When
        TaskResult result = TaskResult.timeout("task-001", 30000L);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TIMEOUT);
        assertThat(result.getErrorMessage()).isEqualTo("Task execution timeout");
    }

    @Test
    void cancelled_shouldCreateCancelledResult() {
        // When
        TaskResult result = TaskResult.cancelled("task-001");

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Given
        TaskResult result = new TaskResult();

        // When
        result.setSuccess(true);
        result.setData("test data");
        result.setErrorMessage("error");
        result.setCostMs(1000L);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("test data");
        assertThat(result.getErrorMessage()).isEqualTo("error");
        assertThat(result.getCostMs()).isEqualTo(1000L);
    }

    @Test
    void withExecutor_shouldSetExecutorAndReturnSelf() {
        // Given
        TaskResult result = TaskResult.success("task-001", "data", 100L);

        // When
        TaskResult updated = result.withExecutor("executor-1");

        // Then
        assertThat(updated).isSameAs(result);
        assertThat(result.getExecutorNodeId()).isEqualTo("executor-1");
    }

    @Test
    void withMetadata_shouldSetMetadataAndReturnSelf() {
        // Given
        TaskResult result = TaskResult.success("task-001", "data", 100L);

        // When
        TaskResult updated = result.withMetadata("key1", "value1");

        // Then
        assertThat(updated).isSameAs(result);
        assertThat(result.getMetadata()).containsEntry("key1", "value1");
    }

    @Test
    void withMetadata_shouldInitializeMapIfNull() {
        // Given
        TaskResult result = new TaskResult();

        // When
        result.withMetadata("key", "value");

        // Then
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).containsEntry("key", "value");
    }
}

package com.meowflow.workflow.engine;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutionResult 单元测试
 */
class ExecutionResultTest {

    @Test
    void success_shouldCreateSuccessResult() {
        // Given
        Map<String, Object> input = Map.of("key", "value");
        Map<String, Object> output = Map.of("result", "done");

        // When
        ExecutionResult result = ExecutionResult.success(
                1L, 100L, "v1.0", input, output, 5000L);

        // Then
        assertThat(result.getExecutionId()).isEqualTo(1L);
        assertThat(result.getWorkflowId()).isEqualTo(100L);
        assertThat(result.getVersion()).isEqualTo("v1.0");
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getInput()).isEqualTo(input);
        assertThat(result.getOutput()).isEqualTo(output);
        assertThat(result.getCostMs()).isEqualTo(5000L);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailed()).isFalse();
        assertThat(result.isCancelled()).isFalse();
    }

    @Test
    void failed_shouldCreateFailedResult() {
        // Given
        Map<String, Object> input = Map.of("key", "value");

        // When
        ExecutionResult result = ExecutionResult.failed(
                1L, 100L, "v1.0", input, "Error", 3000L);

        // Then
        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getErrorMessage()).isEqualTo("Error");
        assertThat(result.isFailed()).isTrue();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void cancelled_shouldCreateCancelledResult() {
        // When
        ExecutionResult result = ExecutionResult.cancelled(
                1L, 100L, "v1.0", new HashMap<>(), 1500L);

        // Then
        assertThat(result.getStatus()).isEqualTo("cancelled");
        assertThat(result.isCancelled()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailed()).isFalse();
    }

    @Test
    void builder_shouldSetAllFields() {
        // Given
        Map<String, Object> input = Map.of("a", 1);
        Map<String, Object> output = Map.of("b", 2);

        // When
        ExecutionResult result = ExecutionResult.builder()
                .executionId(1L)
                .workflowId(100L)
                .version("v2.0")
                .status("success")
                .input(input)
                .output(output)
                .errorMessage(null)
                .costMs(2000L)
                .costToken(100)
                .costAmount(0.05)
                .nodeResults(new HashMap<>())
                .build();

        // Then
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getCostToken()).isEqualTo(100);
        assertThat(result.getCostAmount()).isEqualTo(0.05);
        assertThat(result.getNodeResults()).isNotNull();
    }

    @Test
    void settersAndGetters_shouldWork() {
        // Given
        ExecutionResult result = new ExecutionResult();

        // When
        result.setExecutionId(42L);
        result.setWorkflowId(100L);
        result.setVersion("v1.0");
        result.setStatus("success");

        // Then
        assertThat(result.getExecutionId()).isEqualTo(42L);
        assertThat(result.getWorkflowId()).isEqualTo(100L);
        assertThat(result.getVersion()).isEqualTo("v1.0");
        assertThat(result.getStatus()).isEqualTo("success");
    }

    @Test
    void isSuccess_withDifferentStatus_returnsCorrectly() {
        // Given
        ExecutionResult result = new ExecutionResult();

        // When - set status
        result.setStatus("success");
        // Then
        assertThat(result.isSuccess()).isTrue();

        // When - change status
        result.setStatus("failed");
        // Then
        assertThat(result.isFailed()).isTrue();

        // When - change again
        result.setStatus("cancelled");
        // Then
        assertThat(result.isCancelled()).isTrue();
    }

    @Test
    void isMethods_withUnknownStatus_returnsFalse() {
        // Given
        ExecutionResult result = ExecutionResult.builder().status("unknown").build();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailed()).isFalse();
        assertThat(result.isCancelled()).isFalse();
    }

    @Test
    void noArgsConstructor_shouldCreateEmpty() {
        // When
        ExecutionResult result = new ExecutionResult();

        // Then
        assertThat(result.getExecutionId()).isNull();
        assertThat(result.getStatus()).isNull();
        assertThat(result.getInput()).isNull();
        assertThat(result.getOutput()).isNull();
        assertThat(result.getNodeResults()).isNull();
    }
}

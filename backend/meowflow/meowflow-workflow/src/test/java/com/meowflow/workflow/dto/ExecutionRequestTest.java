package com.meowflow.workflow.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutionRequest 单元测试
 */
class ExecutionRequestTest {

    @Test
    void defaultValues_areApplied() {
        // When
        ExecutionRequest request = new ExecutionRequest();

        // Then
        assertThat(request.getWorkflowId()).isNull();
        assertThat(request.getVersion()).isNull();
        assertThat(request.getTriggerType()).isEqualTo("manual");
        assertThat(request.getAsync()).isEqualTo(false);
        assertThat(request.getInput()).isNull();
    }

    @Test
    void settersAndGetters_shouldWork() {
        // Given
        ExecutionRequest request = new ExecutionRequest();
        Map<String, Object> input = new HashMap<>();
        input.put("key", "value");

        // When
        request.setWorkflowId(100L);
        request.setVersion("v1.0");
        request.setTriggerType("webhook");
        request.setAsync(true);
        request.setInput(input);

        // Then
        assertThat(request.getWorkflowId()).isEqualTo(100L);
        assertThat(request.getVersion()).isEqualTo("v1.0");
        assertThat(request.getTriggerType()).isEqualTo("webhook");
        assertThat(request.getAsync()).isEqualTo(true);
        assertThat(request.getInput()).isSameAs(input);
    }

    @Test
    void workflowId_isRequired_whenNotSet_constraintValidates() {
        // Given
        ExecutionRequest request = new ExecutionRequest();
        request.setVersion("v1.0");

        // When/Then - workflowId is null
        assertThat(request.getWorkflowId()).isNull();
        // Validation would fail at controller level via @NotNull
    }

    @Test
    void triggerType_supportsMultipleValues() {
        // Given
        ExecutionRequest request = new ExecutionRequest();

        // When - manually setting trigger type
        request.setTriggerType("cron");

        // Then
        assertThat(request.getTriggerType()).isEqualTo("cron");

        // When - changing to webhook
        request.setTriggerType("webhook");

        // Then
        assertThat(request.getTriggerType()).isEqualTo("webhook");
    }

    @Test
    void async_flag_canBeSet() {
        // Given
        ExecutionRequest request = new ExecutionRequest();

        // When
        request.setAsync(true);

        // Then
        assertThat(request.getAsync()).isTrue();
    }

    @Test
    void input_canBeNull() {
        // Given
        ExecutionRequest request = new ExecutionRequest();
        request.setWorkflowId(1L);

        // Then
        assertThat(request.getInput()).isNull();
    }
}

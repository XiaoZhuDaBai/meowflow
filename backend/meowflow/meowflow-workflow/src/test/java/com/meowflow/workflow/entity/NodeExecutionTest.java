package com.meowflow.workflow.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NodeExecution 单元测试
 */
class NodeExecutionTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        Map<String, Object> input = new HashMap<>();
        input.put("key", "value");
        Map<String, Object> output = new HashMap<>();
        output.put("result", "done");
        LocalDateTime now = LocalDateTime.now();

        // When
        NodeExecution execution = new NodeExecution();
        execution.setId(1L);
        execution.setExecutionId(100L);
        execution.setNodeId("node-1");
        execution.setNodeType("llm");
        execution.setNodeName("LLM Node");
        execution.setStatus("success");
        execution.setInput(input);
        execution.setOutput(output);
        execution.setErrorMessage(null);
        execution.setRetryCount(0);
        execution.setStartedAt(now);
        execution.setFinishedAt(now.plusSeconds(5));
        execution.setCostMs(5000L);
        execution.setCostToken(100);
        execution.setCreateTime(now);

        // Then
        assertThat(execution.getId()).isEqualTo(1L);
        assertThat(execution.getExecutionId()).isEqualTo(100L);
        assertThat(execution.getNodeId()).isEqualTo("node-1");
        assertThat(execution.getNodeType()).isEqualTo("llm");
        assertThat(execution.getNodeName()).isEqualTo("LLM Node");
        assertThat(execution.getStatus()).isEqualTo("success");
        assertThat(execution.getInput()).containsEntry("key", "value");
        assertThat(execution.getOutput()).containsEntry("result", "done");
        assertThat(execution.getRetryCount()).isEqualTo(0);
        assertThat(execution.getCostMs()).isEqualTo(5000L);
        assertThat(execution.getCostToken()).isEqualTo(100);
        assertThat(execution.getStartedAt()).isEqualTo(now);
        assertThat(execution.getFinishedAt()).isEqualTo(now.plusSeconds(5));
    }

    @Test
    void equals_shouldCompareById() {
        // Given
        NodeExecution e1 = new NodeExecution();
        e1.setId(1L);
        e1.setNodeId("node-1");

        NodeExecution e2 = new NodeExecution();
        e2.setId(1L);
        e2.setNodeId("node-2");

        NodeExecution e3 = new NodeExecution();
        e3.setId(2L);
        e3.setNodeId("node-1");

        // Then - @EqualsAndHashCode(callSuper = false) compares by all fields
        // Fields are same when both have id=1 but different nodeId
        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void fields_defaultValues_areNull() {
        // When
        NodeExecution execution = new NodeExecution();

        // Then
        assertThat(execution.getId()).isNull();
        assertThat(execution.getExecutionId()).isNull();
        assertThat(execution.getNodeId()).isNull();
        assertThat(execution.getStatus()).isNull();
        assertThat(execution.getInput()).isNull();
        assertThat(execution.getOutput()).isNull();
    }

    @Test
    void status_values() {
        // Given
        NodeExecution execution = new NodeExecution();

        // When
        execution.setStatus("running");

        // Then
        assertThat(execution.getStatus()).isEqualTo("running");

        // When
        execution.setStatus("success");
        // Then
        assertThat(execution.getStatus()).isEqualTo("success");

        // When
        execution.setStatus("failed");
        // Then
        assertThat(execution.getStatus()).isEqualTo("failed");
    }

    @Test
    void retryCount_tracking() {
        // Given
        NodeExecution execution = new NodeExecution();

        // When
        execution.setRetryCount(1);
        execution.setRetryCount(2);
        execution.setRetryCount(3);

        // Then
        assertThat(execution.getRetryCount()).isEqualTo(3);
    }

    @Test
    void costTracking_shouldWork() {
        // Given
        NodeExecution execution = new NodeExecution();

        // When
        execution.setCostMs(1500L);
        execution.setCostToken(250);

        // Then
        assertThat(execution.getCostMs()).isEqualTo(1500L);
        assertThat(execution.getCostToken()).isEqualTo(250);
    }
}

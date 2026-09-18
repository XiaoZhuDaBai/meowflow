package com.meowflow.executor.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutorTaskEntity 鍗曞厓娴嬭瘯
 */
class ExecutorTaskEntityTest {

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Given
        ExecutorTaskEntity entity = new ExecutorTaskEntity();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> input = new HashMap<>();
        input.put("url", "https://api.example.com");
        Map<String, Object> output = new HashMap<>();
        output.put("status", "success");

        // When
        entity.setId(1L);
        entity.setExecutionId(100L);
        entity.setNodeId("node-123");
        entity.setStatus("completed");
        entity.setPriority(80);
        entity.setInput(input);
        entity.setOutput(output);
        entity.setErrorMessage("Test error");
        entity.setStartTime(now);
        entity.setEndTime(now.plusSeconds(5));
        entity.setCostMs(5000L);
        entity.setCreateTime(now);

        // Then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getExecutionId()).isEqualTo(100L);
        assertThat(entity.getNodeId()).isEqualTo("node-123");
        assertThat(entity.getStatus()).isEqualTo("completed");
        assertThat(entity.getPriority()).isEqualTo(80);
        assertThat(entity.getInput()).containsEntry("url", "https://api.example.com");
        assertThat(entity.getOutput()).containsEntry("status", "success");
        assertThat(entity.getErrorMessage()).isEqualTo("Test error");
        assertThat(entity.getStartTime()).isEqualTo(now);
        assertThat(entity.getEndTime()).isEqualTo(now.plusSeconds(5));
        assertThat(entity.getCostMs()).isEqualTo(5000L);
        assertThat(entity.getCreateTime()).isEqualTo(now);
    }

    @Test
    void equals_shouldCompareById() {
        // Given
        ExecutorTaskEntity entity1 = new ExecutorTaskEntity();
        entity1.setId(1L);
        entity1.setNodeId("node-1");

        ExecutorTaskEntity entity2 = new ExecutorTaskEntity();
        entity2.setId(1L);
        entity2.setNodeId("node-2");

        ExecutorTaskEntity entity3 = new ExecutorTaskEntity();
        entity3.setId(2L);
        entity3.setNodeId("node-1");

        // Then
        assertThat(entity1).isEqualTo(entity2); // Same ID
        assertThat(entity1).isNotEqualTo(entity3); // Different ID
    }

    @Test
    void hashCode_shouldBeConsistent() {
        // Given
        ExecutorTaskEntity entity1 = new ExecutorTaskEntity();
        entity1.setId(1L);
        entity1.setExecutionId(100L);

        ExecutorTaskEntity entity2 = new ExecutorTaskEntity();
        entity2.setId(1L);
        entity2.setExecutionId(200L);

        // Then
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }
}

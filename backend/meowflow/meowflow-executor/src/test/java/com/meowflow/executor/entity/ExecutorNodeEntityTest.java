package com.meowflow.executor.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutorNodeEntity 鍗曞厓娴嬭瘯
 */
class ExecutorNodeEntityTest {

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Given
        ExecutorNodeEntity entity = new ExecutorNodeEntity();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> tags = new HashMap<>();
        tags.put("env", "prod");

        // When
        entity.setId(1L);
        entity.setNodeId("node-123");
        entity.setName("Test Node");
        entity.setHost("192.168.1.100");
        entity.setPort(8080);
        entity.setStatus("healthy");
        entity.setLastHeartbeat(now);
        entity.setCpuCount(8);
        entity.setMemoryTotal(16000000L);
        entity.setMemoryUsed(8000000L);
        entity.setActiveTasks(5);
        entity.setCompletedTasks(100L);
        entity.setFailedTasks(2L);
        entity.setTags(tags);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        // Then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getNodeId()).isEqualTo("node-123");
        assertThat(entity.getName()).isEqualTo("Test Node");
        assertThat(entity.getHost()).isEqualTo("192.168.1.100");
        assertThat(entity.getPort()).isEqualTo(8080);
        assertThat(entity.getStatus()).isEqualTo("healthy");
        assertThat(entity.getLastHeartbeat()).isEqualTo(now);
        assertThat(entity.getCpuCount()).isEqualTo(8);
        assertThat(entity.getMemoryTotal()).isEqualTo(16000000L);
        assertThat(entity.getMemoryUsed()).isEqualTo(8000000L);
        assertThat(entity.getActiveTasks()).isEqualTo(5);
        assertThat(entity.getCompletedTasks()).isEqualTo(100L);
        assertThat(entity.getFailedTasks()).isEqualTo(2L);
        assertThat(entity.getTags()).containsEntry("env", "prod");
        assertThat(entity.getCreateTime()).isEqualTo(now);
        assertThat(entity.getUpdateTime()).isEqualTo(now);
    }

    @Test
    void equals_shouldCompareById() {
        // Given
        ExecutorNodeEntity entity1 = new ExecutorNodeEntity();
        entity1.setId(1L);
        entity1.setNodeId("node-1");

        ExecutorNodeEntity entity2 = new ExecutorNodeEntity();
        entity2.setId(1L);
        entity2.setNodeId("node-2");

        ExecutorNodeEntity entity3 = new ExecutorNodeEntity();
        entity3.setId(2L);
        entity3.setNodeId("node-1");

        // Then
        assertThat(entity1).isEqualTo(entity2); // Same ID
        assertThat(entity1).isNotEqualTo(entity3); // Different ID
    }

    @Test
    void hashCode_shouldBeConsistent() {
        // Given
        ExecutorNodeEntity entity1 = new ExecutorNodeEntity();
        entity1.setId(1L);
        entity1.setNodeId("node-1");

        ExecutorNodeEntity entity2 = new ExecutorNodeEntity();
        entity2.setId(1L);
        entity2.setNodeId("node-2");

        // Then
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }
}

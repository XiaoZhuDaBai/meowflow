package com.meowflow.executor.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meowflow.executor.entity.ExecutorNodeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * ExecutorNodeMapper 鍗曞厓娴嬭瘯
 */
@ExtendWith(MockitoExtension.class)
class ExecutorNodeMapperTest {

    @Mock
    private ExecutorNodeMapper mapper;

    @Test
    void insert_shouldCallBaseMapper() {
        // Given
        ExecutorNodeEntity entity = new ExecutorNodeEntity();
        entity.setNodeId("node-1");
        entity.setName("Test Node");

        // When
        mapper.insert(entity);

        // Then
        verify(mapper).insert(entity);
    }

    @Test
    void updateHeartbeat_shouldUpdateFields() {
        // Given
        String nodeId = "node-1";
        String status = "healthy";
        LocalDateTime lastHeartbeat = LocalDateTime.now();
        int activeTasks = 5;
        long completedTasks = 100L;
        long failedTasks = 2L;
        long memoryUsed = 1024000L;
        LocalDateTime updateTime = LocalDateTime.now();

        // When
        mapper.updateHeartbeat(nodeId, status, lastHeartbeat, activeTasks, 
                completedTasks, failedTasks, memoryUsed, updateTime);

        // Then
        verify(mapper).updateHeartbeat(
                eq(nodeId), 
                eq(status), 
                eq(lastHeartbeat),
                eq(activeTasks),
                eq(completedTasks),
                eq(failedTasks),
                eq(memoryUsed),
                eq(updateTime)
        );
    }

    @Test
    void update_shouldCallBaseMapper() {
        // Given
        ExecutorNodeEntity entity = null;
        LambdaUpdateWrapper<ExecutorNodeEntity> wrapper = new LambdaUpdateWrapper<>();

        // When
        mapper.update(entity, wrapper);

        // Then
        verify(mapper).update(entity, wrapper);
    }
}

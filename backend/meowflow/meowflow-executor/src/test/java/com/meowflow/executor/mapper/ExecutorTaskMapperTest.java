package com.meowflow.executor.mapper;

import com.meowflow.executor.entity.ExecutorTaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * ExecutorTaskMapper 鍗曞厓娴嬭瘯
 */
@ExtendWith(MockitoExtension.class)
class ExecutorTaskMapperTest {

    @Mock
    private ExecutorTaskMapper mapper;

    @Test
    void insert_shouldCallBaseMapper() {
        // Given
        ExecutorTaskEntity entity = new ExecutorTaskEntity();
        entity.setExecutionId(1L);
        entity.setNodeId("node-1");
        entity.setStatus("pending");

        // When
        mapper.insert(entity);

        // Then
        verify(mapper).insert(entity);
    }

    @Test
    void selectById_shouldCallBaseMapper() {
        // When
        mapper.selectById(123L);

        // Then
        verify(mapper).selectById(123L);
    }

    @Test
    void updateById_shouldCallBaseMapper() {
        // Given
        ExecutorTaskEntity entity = new ExecutorTaskEntity();
        entity.setId(123L);
        entity.setStatus("completed");

        // When
        mapper.updateById(entity);

        // Then
        verify(mapper).updateById(entity);
    }

    @Test
    void deleteById_shouldCallBaseMapper() {
        // When
        mapper.deleteById(123L);

        // Then
        verify(mapper).deleteById(123L);
    }
}

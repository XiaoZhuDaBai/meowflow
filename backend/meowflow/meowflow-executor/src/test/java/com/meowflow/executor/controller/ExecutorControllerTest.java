package com.meowflow.executor.controller;

import com.meowflow.common.result.Result;
import com.meowflow.executor.model.Task;
import com.meowflow.executor.model.TaskStatus;
import com.meowflow.executor.model.TaskType;
import com.meowflow.executor.registry.ExecutorNode;
import com.meowflow.executor.service.ExecutorNodeRegistry;
import com.meowflow.executor.service.TaskDispatchService;
import com.meowflow.executor.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * ExecutorController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExecutorControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private ExecutorNodeRegistry nodeRegistry;

    @Mock
    private TaskDispatchService dispatchService;

    @InjectMocks
    private ExecutorController controller;

    private Task testTask;
    private ExecutorNode testNode;

    @BeforeEach
    void setUp() {
        testTask = Task.of("task-1", "Test Task", TaskType.HTTP_REQUEST);
        testTask.setStatus(TaskStatus.RUNNING);

        testNode = ExecutorNode.of("node-1", "Test Node", "localhost", 8080);
    }

    @Test
    void createTask_shouldReturnSuccess() {
        // Given
        ExecutorController.CreateTaskRequest request = new ExecutorController.CreateTaskRequest();
        request.setName("Test Task");
        request.setType(TaskType.HTTP_REQUEST);
        request.setParams(Map.of("url", "https://api.example.com"));
        when(taskService.createTask(anyString(), any(TaskType.class), any()))
                .thenReturn(testTask);

        // When
        Result<Task> result = controller.createTask(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        verify(taskService).createTask("Test Task", TaskType.HTTP_REQUEST, Map.of("url", "https://api.example.com"));
    }

    @Test
    void getTask_whenExists_shouldReturnTask() {
        // Given
        lenient().when(taskService.getTask("task-1")).thenReturn(testTask);

        // When
        Result<Task> result = controller.getTask("task-1");

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(testTask);
    }

    @Test
    void getTask_whenNotExists_shouldReturnError() {
        // Given
        when(taskService.getTask("non-existent")).thenReturn(null);

        // When
        Result<Task> result = controller.getTask("non-existent");

        // Then
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void listTasks_shouldReturnList() {
        // Given
        Task task2 = Task.of("task-2", "Task 2", TaskType.SCRIPT);
        when(taskService.getAllTasks()).thenReturn(List.of(testTask, task2));

        // When
        Result<List<Task>> result = controller.listTasks(null);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
    }

    @Test
    void listTasks_withStatus_shouldReturnFilteredList() {
        // Given
        when(taskService.getTasksByStatus(TaskStatus.RUNNING))
                .thenReturn(List.of(testTask));

        // When
        Result<List<Task>> result = controller.listTasks(TaskStatus.RUNNING);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getStatus()).isEqualTo(TaskStatus.RUNNING);
    }

    @Test
    void submitTask_shouldReturnSubmittedTask() {
        // Given
        lenient().when(taskService.getTask("task-1")).thenReturn(testTask);
        when(taskService.submitTask(testTask)).thenReturn(testTask);

        // When
        Result<Task> result = controller.submitTask("task-1");

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(taskService).submitTask(testTask);
    }

    @Test
    void cancelTask_shouldCancelSuccessfully() {
        // Given
        lenient().when(taskService.getTask("task-1")).thenReturn(testTask);

        // When
        Result<Void> result = controller.cancelTask("task-1");

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(taskService).cancelTask("task-1");
    }

    @Test
    void deleteTask_shouldDeleteSuccessfully() {
        // Given
        lenient().when(taskService.getTask("task-1")).thenReturn(testTask);

        // When
        Result<Void> result = controller.deleteTask("task-1");

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(taskService).deleteTask("task-1");
    }

    @Test
    void registerNode_shouldReturnRegisteredNode() {
        // Given
        ExecutorController.RegisterNodeRequest request = new ExecutorController.RegisterNodeRequest();
        request.setName("Test Node");
        request.setHost("localhost");
        request.setPort(8080);
        request.setMaxConcurrentTasks(10);
        when(nodeRegistry.register(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(testNode);

        // When
        Result<ExecutorNode> result = controller.registerNode(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(testNode);
        verify(nodeRegistry).register("Test Node", "localhost", 8080, 10);
    }

    @Test
    void unregisterNode_shouldUnregisterSuccessfully() {
        // When
        Result<Void> result = controller.unregisterNode("node-1");

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(nodeRegistry).unregister("node-1");
    }

    @Test
    void heartbeat_shouldUpdateHeartbeat() {
        // Given
        when(nodeRegistry.heartbeat("node-1")).thenReturn(testNode);

        // When
        Result<ExecutorNode> result = controller.heartbeat("node-1");

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(testNode);
    }

    @Test
    void listNodes_shouldReturnNodeList() {
        // Given
        ExecutorNode node2 = ExecutorNode.of("node-2", "Node 2", "localhost", 8081);
        when(nodeRegistry.getAllNodes()).thenReturn(List.of(testNode, node2));

        // When
        Result<List<ExecutorNode>> result = controller.listNodes();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
    }

    @Test
    void listHealthyNodes_shouldReturnHealthyNodesList() {
        // Given
        when(nodeRegistry.getHealthyNodes()).thenReturn(List.of(testNode));

        // When
        Result<List<ExecutorNode>> result = controller.listHealthyNodes();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void getStatus_shouldReturnExecutorStatus() {
        // Given
        when(taskService.getTasksByStatus(TaskStatus.RUNNING)).thenReturn(List.of(testTask));
        when(taskService.getTasksByStatus(TaskStatus.PENDING)).thenReturn(List.of());
        when(nodeRegistry.getTotalNodes()).thenReturn(5);
        when(nodeRegistry.getHealthyNodesCount()).thenReturn(4);

        // When
        Result<Map<String, Object>> result = controller.getStatus();

        // Then
        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> data = result.getData();
        assertThat(data).containsEntry("totalNodes", 5);
        assertThat(data).containsEntry("healthyNodes", 4);
    }

    @Test
    void health_shouldReturnHealthyStatus() {
        // When
        Result<Boolean> result = controller.health();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
    }
}



package com.meowflow.executor.service;

import com.meowflow.common.util.IdGeneratorFactory;
import com.meowflow.executor.model.*;
import com.meowflow.executor.registry.TimeoutScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TaskService 鍗曞厓娴嬭瘯
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskDispatchService dispatchService;

    @Mock
    private TimeoutScanner timeoutScanner;

    @Mock
    private IdGeneratorFactory idGenerator;

    @Mock
    private Executor taskExecutor;

    @Mock
    private MonitorService monitorService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(dispatchService, timeoutScanner, idGenerator, taskExecutor);
        
        when(idGenerator.nextIdStr()).thenReturn("task-123", "task-456", "task-789");
    }

    @Test
    void createTask_shouldCreateTaskWithDefaultValues() {
        // Given
        Map<String, Object> params = Map.of("key", "value");

        // When
        Task task = taskService.createTask("Test Task", TaskType.HTTP_REQUEST, params);

        // Then
        assertThat(task).isNotNull();
        assertThat(task.getTaskId()).isEqualTo("task-123");
        assertThat(task.getName()).isEqualTo("Test Task");
        assertThat(task.getType()).isEqualTo(TaskType.HTTP_REQUEST);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getMaxRetries()).isEqualTo(3);
        assertThat(task.getTimeoutMs()).isEqualTo(30000L);
        assertThat(task.getPriority()).isEqualTo(50);
    }

    @Test
    void getTask_shouldReturnTaskById() {
        // Given
        Map<String, Object> params = new HashMap<>();
        Task created = taskService.createTask("Test", TaskType.HTTP_REQUEST, params);

        // When
        Task retrieved = taskService.getTask(created.getTaskId());

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getTaskId()).isEqualTo(created.getTaskId());
    }

    @Test
    void getTasksByStatus_shouldFilterByStatus() {
        // Given
        taskService.createTask("Task 1", TaskType.HTTP_REQUEST, Map.of());
        Task task2 = taskService.createTask("Task 2", TaskType.HTTP_REQUEST, Map.of());
        task2.markRunning();
        taskService.updateTaskStatus(task2.getTaskId(), TaskStatus.RUNNING);

        // When
        List<Task> pending = taskService.getTasksByStatus(TaskStatus.PENDING);
        List<Task> running = taskService.getTasksByStatus(TaskStatus.RUNNING);

        // Then
        assertThat(pending).hasSize(1);
        assertThat(running).hasSize(1);
    }

    @Test
    void submitTask_shouldDispatchSuccessfully() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        when(dispatchService.dispatch(task)).thenReturn(task);

        // When
        Task result = taskService.submitTask(task);

        // Then
        assertThat(result).isNotNull();
        verify(dispatchService).dispatch(task);
    }

    @Test
    void submitTask_shouldEnqueueWhenDispatchFails() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        when(dispatchService.dispatch(task)).thenReturn(null);

        // When
        Task result = taskService.submitTask(task);

        // Then
        assertThat(result).isNotNull();
        verify(dispatchService).dispatch(task);
        
        TaskQueue queue = taskService.getQueue("queue-http_request");
        assertThat(queue).isNotNull();
        assertThat(queue.size()).isGreaterThan(0);
    }

    @Test
    void startTask_shouldMarkTaskAsRunningAndRegisterTimeout() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        task.setTimeoutMs(5000L);

        // When
        taskService.startTask(task);

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(retrieved.getStartTime()).isNotNull();
        verify(timeoutScanner).registerTask(eq(task.getTaskId()), eq(5000L), any());
    }

    @Test
    void completeTask_shouldMarkSuccessAndUnregisterTimeout() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        task.setExecutorNodeId("node-1");
        taskService.startTask(task);

        // When
        taskService.completeTask(task.getTaskId(), "success result");

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved.getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(retrieved.getResult()).isEqualTo("success result");
        assertThat(retrieved.getEndTime()).isNotNull();
        verify(timeoutScanner).unregisterTask(task.getTaskId());
        verify(dispatchService).notifyTaskCompleted("node-1");
    }

    @Test
    void failTask_shouldMarkFailedAndRetryIfPossible() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        task.setMaxRetries(3);
        task.setExecutorNodeId("node-1");
        taskService.startTask(task);
        
        when(dispatchService.dispatch(any())).thenReturn(task);

        // When
        taskService.failTask(task.getTaskId(), "error message");

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved.getStatus()).isEqualTo(TaskStatus.RETRYING);
        assertThat(retrieved.getErrorMessage()).isEqualTo("error message");
        assertThat(retrieved.getRetryCount()).isEqualTo(1);
        verify(timeoutScanner).unregisterTask(task.getTaskId());
        verify(dispatchService).notifyTaskFailed("node-1");
        verify(dispatchService).dispatch(retrieved);
    }

    @Test
    void failTask_shouldNotRetryWhenMaxRetriesReached() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        task.setMaxRetries(0);
        taskService.startTask(task);

        // When
        taskService.failTask(task.getTaskId(), "error");

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved.getStatus()).isEqualTo(TaskStatus.FAILED);
        verify(dispatchService, never()).dispatch(retrieved);
    }

    @Test
    void cancelTask_shouldMarkCancelled() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        task.setExecutorNodeId("node-1");
        taskService.startTask(task);

        // When
        taskService.cancelTask(task.getTaskId());

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        verify(timeoutScanner).unregisterTask(task.getTaskId());
    }

    @Test
    void timeoutTask_shouldMarkTimeout() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        task.setExecutorNodeId("node-1");
        taskService.startTask(task);

        // When
        taskService.timeoutTask(task.getTaskId());

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved.getStatus()).isEqualTo(TaskStatus.TIMEOUT);
        verify(dispatchService).notifyTaskFailed("node-1");
    }

    @Test
    void executeAsync_shouldExecuteSuccessfully() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        TaskService.TaskExecutor executor = t -> "result";
        
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        // When
        taskService.executeAsync(task, executor);

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved.getStatus()).isEqualTo(TaskStatus.SUCCESS);
    }

    @Test
    void executeAsync_shouldHandleException() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        TaskService.TaskExecutor executor = t -> {
            throw new RuntimeException("Execution failed");
        };
        
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        // When
        taskService.executeAsync(task, executor);

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved.getStatus()).isEqualTo(TaskStatus.RETRYING);
        assertThat(retrieved.getErrorMessage()).contains("Execution failed");
    }

    @Test
    void queueOperations_shouldWorkCorrectly() {
        // Given
        String queueId = "queue-http_request";

        // When
        TaskQueue queue = taskService.getQueue(queueId);

        // Then - queue should be created lazily when task is enqueued
        assertThat(queue).isNull();

        // Enqueue a task
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        taskService.enqueueTask(task);

        // Now queue should exist
        queue = taskService.getQueue(queueId);
        assertThat(queue).isNotNull();
        assertThat(queue.size()).isEqualTo(1);

        // Poll task
        Task polled = taskService.pollTask(queueId);
        assertThat(polled).isNotNull();
        assertThat(polled.getTaskId()).isEqualTo(task.getTaskId());
    }

    @Test
    void pauseAndResumeQueue_shouldWork() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());
        taskService.enqueueTask(task);
        String queueId = "queue-http_request";

        // When - pause
        taskService.pauseQueue(queueId);
        TaskQueue queue = taskService.getQueue(queueId);

        // Then
        assertThat(queue.isPaused()).isTrue();

        // When - resume
        taskService.resumeQueue(queueId);

        // Then
        assertThat(queue.isPaused()).isFalse();
    }

    @Test
    void deleteTask_shouldRemoveTask() {
        // Given
        Task task = taskService.createTask("Test", TaskType.HTTP_REQUEST, Map.of());

        // When
        taskService.deleteTask(task.getTaskId());

        // Then
        Task retrieved = taskService.getTask(task.getTaskId());
        assertThat(retrieved).isNull();
    }

    @Test
    void getAllTasks_shouldReturnAllTasks() {
        // Given
        taskService.createTask("Task 1", TaskType.HTTP_REQUEST, Map.of());
        taskService.createTask("Task 2", TaskType.SCRIPT, Map.of());
        taskService.createTask("Task 3", TaskType.HTTP_REQUEST, Map.of());

        // When
        List<Task> allTasks = taskService.getAllTasks();

        // Then
        assertThat(allTasks).hasSize(3);
    }

    @Test
    void getTasksByExecutor_shouldFilterCorrectly() {
        // Given
        Task task1 = taskService.createTask("Task 1", TaskType.HTTP_REQUEST, Map.of());
        task1.setExecutorNodeId("node-1");
        taskService.updateTaskStatus(task1.getTaskId(), task1.getStatus());

        Task task2 = taskService.createTask("Task 2", TaskType.HTTP_REQUEST, Map.of());
        task2.setExecutorNodeId("node-2");
        taskService.updateTaskStatus(task2.getTaskId(), task2.getStatus());

        Task task3 = taskService.createTask("Task 3", TaskType.HTTP_REQUEST, Map.of());
        task3.setExecutorNodeId("node-1");
        taskService.updateTaskStatus(task3.getTaskId(), task3.getStatus());

        // When
        List<Task> node1Tasks = taskService.getTasksByExecutor("node-1");

        // Then
        assertThat(node1Tasks).hasSize(2);
        assertThat(node1Tasks).allMatch(t -> "node-1".equals(t.getExecutorNodeId()));
    }

    @Test
    void getAllQueues_shouldReturnAllQueues() {
        // Given
        taskService.createTask("Task 1", TaskType.HTTP_REQUEST, Map.of());
        taskService.enqueueTask(taskService.getTask("task-123"));
        
        taskService.createTask("Task 2", TaskType.SCRIPT, Map.of());
        taskService.enqueueTask(taskService.getTask("task-456"));

        // When
        List<TaskQueue> queues = taskService.getAllQueues();

        // Then
        assertThat(queues).hasSize(2);
    }
}




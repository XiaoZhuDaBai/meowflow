package com.meowflow.executor.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    void of_shouldCreateTaskWithDefaults() {
        Task task = Task.of("task-123", "Test Task", TaskType.AI_CHAT);

        assertNotNull(task);
        assertEquals("task-123", task.getTaskId());
        assertEquals("Test Task", task.getName());
        assertEquals(TaskType.AI_CHAT, task.getType());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNotNull(task.getCreateTime());
        assertEquals(0, task.getRetryCount());
        assertEquals(50, task.getPriority());
    }

    @Test
    void markRunning_shouldSetRunningStatus() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);
        assertEquals(TaskStatus.PENDING, task.getStatus());

        task.markRunning();

        assertEquals(TaskStatus.RUNNING, task.getStatus());
        assertNotNull(task.getStartTime());
    }

    @Test
    void markSuccess_shouldSetSuccessStatusAndCalculateCost() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);
        task.markRunning();

        task.markSuccess("{\"result\": \"success\"}");

        assertEquals(TaskStatus.SUCCESS, task.getStatus());
        assertEquals("{\"result\": \"success\"}", task.getResult());
        assertNotNull(task.getEndTime());
        assertNotNull(task.getCostMs());
        assertTrue(task.getCostMs() >= 0);
    }

    @Test
    void markFailed_shouldSetFailedStatusAndErrorMessage() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);
        task.markRunning();

        task.markFailed("Network error");

        assertEquals(TaskStatus.FAILED, task.getStatus());
        assertEquals("Network error", task.getErrorMessage());
        assertNotNull(task.getEndTime());
        assertNotNull(task.getCostMs());
    }

    @Test
    void markCancelled_shouldSetCancelledStatus() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);

        task.markCancelled();

        assertEquals(TaskStatus.CANCELLED, task.getStatus());
        assertNotNull(task.getEndTime());
    }

    @Test
    void canRetry_shouldReturnTrueWhenRetriesAvailable() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);
        task.setMaxRetries(3);
        task.setRetryCount(1);

        assertTrue(task.canRetry());

        task.setRetryCount(2);
        assertTrue(task.canRetry());
    }

    @Test
    void canRetry_shouldReturnFalseWhenMaxRetriesReached() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);
        task.setMaxRetries(3);
        task.setRetryCount(3);

        assertFalse(task.canRetry());
    }

    @Test
    void canRetry_shouldReturnFalseWhenMaxRetriesNotSet() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);

        assertFalse(task.canRetry());
    }

    @Test
    void incrementRetry_shouldIncrementCountAndSetRetryingStatus() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);
        task.setMaxRetries(3);
        assertEquals(0, task.getRetryCount());

        task.incrementRetry();
        assertEquals(1, task.getRetryCount());
        assertEquals(TaskStatus.RETRYING, task.getStatus());

        task.incrementRetry();
        assertEquals(2, task.getRetryCount());
    }

    @Test
    void taskLifecycle_shouldFollowCorrectTransitions() {
        Task task = Task.of("task-123", "Test Task", TaskType.AI_CHAT);

        assertEquals(TaskStatus.PENDING, task.getStatus());

        task.markRunning();
        assertEquals(TaskStatus.RUNNING, task.getStatus());

        task.markSuccess("done");
        assertEquals(TaskStatus.SUCCESS, task.getStatus());
    }

    @Test
    void taskProperties_shouldBeSettable() {
        Task task = Task.of("task-123", "Test Task", TaskType.HTTP_REQUEST);
        task.setWorkflowId("wf-1");
        task.setNodeId("node-1");
        task.setPriority(80);
        task.setTimeoutMs(30000L);
        task.setParams(Map.of("key", "value"));

        assertEquals("wf-1", task.getWorkflowId());
        assertEquals("node-1", task.getNodeId());
        assertEquals(80, task.getPriority());
        assertEquals(30000L, task.getTimeoutMs());
        assertEquals("value", task.getParams().get("key"));
    }
}

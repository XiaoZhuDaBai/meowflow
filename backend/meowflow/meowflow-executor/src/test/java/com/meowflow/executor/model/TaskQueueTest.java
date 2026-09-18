package com.meowflow.executor.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskQueue 鍗曞厓娴嬭瘯
 */
class TaskQueueTest {

    @Test
    void constructor_shouldInitializeQueues() {
        // When
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        // Then
        assertThat(queue.getQueueId()).isEqualTo("queue-1");
        assertThat(queue.getName()).isEqualTo("Test Queue");
        assertThat(queue.getTaskType()).isEqualTo(TaskType.HTTP_REQUEST);
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.isPaused()).isFalse();
        assertThat(queue.getPendingCount().get()).isEqualTo(0);
        assertThat(queue.getRunningCount().get()).isEqualTo(0);
        assertThat(queue.getCompletedCount().get()).isEqualTo(0);
        assertThat(queue.getFailedCount().get()).isEqualTo(0);
    }

    @Test
    void offer_shouldAddTaskToQueue() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);
        Task task = Task.of("task-1", "Test Task", TaskType.HTTP_REQUEST);
        task.setPriority(50);

        // When
        boolean result = queue.offer(task);

        // Then
        assertThat(result).isTrue();
        assertThat(queue.size()).isEqualTo(1);
        assertThat(queue.getPendingCount().get()).isEqualTo(1);
    }

    @Test
    void offer_whenPaused_shouldReturnFalse() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);
        queue.pause();
        Task task = Task.of("task-1", "Test Task", TaskType.HTTP_REQUEST);

        // When
        boolean result = queue.offer(task);

        // Then
        assertThat(result).isFalse();
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void poll_shouldReturnTaskInPriorityOrder() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        Task lowPriorityTask = Task.of("task-1", "Low", TaskType.HTTP_REQUEST);
        lowPriorityTask.setPriority(10);

        Task normalPriorityTask = Task.of("task-2", "Normal", TaskType.HTTP_REQUEST);
        normalPriorityTask.setPriority(50);

        Task highPriorityTask = Task.of("task-3", "High", TaskType.HTTP_REQUEST);
        highPriorityTask.setPriority(90);

        queue.offer(normalPriorityTask);
        queue.offer(lowPriorityTask);
        queue.offer(highPriorityTask);

        // When
        Task first = queue.poll();
        Task second = queue.poll();
        Task third = queue.poll();

        // Then
        assertThat(first.getTaskId()).isEqualTo("task-3"); // High priority first
        assertThat(second.getTaskId()).isEqualTo("task-2"); // Normal priority second
        assertThat(third.getTaskId()).isEqualTo("task-1"); // Low priority last
    }

    @Test
    void poll_whenEmpty_shouldReturnNull() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        // When
        Task task = queue.poll();

        // Then
        assertThat(task).isNull();
    }

    @Test
    void remove_shouldRemoveTaskFromQueue() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);
        Task task = Task.of("task-1", "Test", TaskType.HTTP_REQUEST);
        task.setPriority(50);
        queue.offer(task);

        // When
        Task removed = queue.remove("task-1");

        // Then
        assertThat(removed).isNotNull();
        assertThat(removed.getTaskId()).isEqualTo("task-1");
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void remove_nonExistentTask_shouldReturnNull() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        // When
        Task removed = queue.remove("non-existent");

        // Then
        assertThat(removed).isNull();
    }

    @Test
    void taskStarted_shouldUpdateCounters() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);
        Task task = Task.of("task-1", "Test", TaskType.HTTP_REQUEST);
        task.setPriority(50);
        queue.offer(task);

        // When
        queue.taskStarted();

        // Then
        assertThat(queue.getPendingCount().get()).isEqualTo(0);
        assertThat(queue.getRunningCount().get()).isEqualTo(1);
    }

    @Test
    void taskCompleted_shouldUpdateCounters() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);
        queue.taskStarted();

        // When
        queue.taskCompleted();

        // Then
        assertThat(queue.getRunningCount().get()).isEqualTo(0);
        assertThat(queue.getCompletedCount().get()).isEqualTo(1);
    }

    @Test
    void taskFailed_shouldUpdateCounters() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);
        queue.taskStarted();

        // When
        queue.taskFailed();

        // Then
        assertThat(queue.getRunningCount().get()).isEqualTo(0);
        assertThat(queue.getFailedCount().get()).isEqualTo(1);
    }

    @Test
    void size_shouldReturnTotalTaskCount() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        Task task1 = Task.of("task-1", "T1", TaskType.HTTP_REQUEST);
        task1.setPriority(90);
        Task task2 = Task.of("task-2", "T2", TaskType.HTTP_REQUEST);
        task2.setPriority(50);
        Task task3 = Task.of("task-3", "T3", TaskType.HTTP_REQUEST);
        task3.setPriority(10);

        queue.offer(task1);
        queue.offer(task2);
        queue.offer(task3);

        // When
        int size = queue.size();

        // Then
        assertThat(size).isEqualTo(3);
    }

    @Test
    void isEmpty_shouldReturnTrueWhenNoTasks() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        // Then
        assertThat(queue.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_shouldReturnFalseWhenTasksExist() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);
        Task task = Task.of("task-1", "Test", TaskType.HTTP_REQUEST);
        task.setPriority(50);
        queue.offer(task);

        // Then
        assertThat(queue.isEmpty()).isFalse();
    }

    @Test
    void pause_shouldSetPausedFlag() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        // When
        queue.pause();

        // Then
        assertThat(queue.isPaused()).isTrue();
    }

    @Test
    void resume_shouldClearPausedFlag() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);
        queue.pause();

        // When
        queue.resume();

        // Then
        assertThat(queue.isPaused()).isFalse();
    }

    @Test
    void priorityLevels_shouldSeparateTasksCorrectly() {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        // Create tasks with different priorities
        Task highTask = Task.of("high", "High", TaskType.HTTP_REQUEST);
        highTask.setPriority(85); // >= 80 = HIGH

        Task normalTask = Task.of("normal", "Normal", TaskType.HTTP_REQUEST);
        normalTask.setPriority(50); // 21-79 = NORMAL

        Task lowTask = Task.of("low", "Low", TaskType.HTTP_REQUEST);
        lowTask.setPriority(15); // <= 20 = LOW

        // When - add in mixed order
        queue.offer(normalTask);
        queue.offer(lowTask);
        queue.offer(highTask);

        // Then - should come out in priority order
        assertThat(queue.poll().getTaskId()).isEqualTo("high");
        assertThat(queue.poll().getTaskId()).isEqualTo("normal");
        assertThat(queue.poll().getTaskId()).isEqualTo("low");
    }

    @Test
    void pollWithTimeout_shouldWaitForTask() throws Exception {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        // Add task in another thread after delay
        new Thread(() -> {
            try {
                Thread.sleep(100);
                Task task = Task.of("task-1", "Test", TaskType.HTTP_REQUEST);
                task.setPriority(50);
                queue.offer(task);
            } catch (InterruptedException ignored) {
            }
        }).start();

        // When
        Task task = queue.poll(500);

        // Then
        assertThat(task).isNotNull();
        assertThat(task.getTaskId()).isEqualTo("task-1");
    }

    @Test
    void pollWithTimeout_whenNoTask_shouldReturnNull() throws Exception {
        // Given
        TaskQueue queue = new TaskQueue("queue-1", "Test Queue", TaskType.HTTP_REQUEST);

        // When
        Task task = queue.poll(100);

        // Then
        assertThat(task).isNull();
    }
}

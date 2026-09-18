package com.meowflow.executor.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务队列
 */
@Data
public class TaskQueue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String queueId;
    private String name;
    private TaskType taskType;
    private PriorityBlockingQueue<Task> highPriorityQueue;
    private PriorityBlockingQueue<Task> normalPriorityQueue;
    private PriorityBlockingQueue<Task> lowPriorityQueue;
    private Map<String, Task> taskMap;
    private AtomicLong pendingCount;
    private AtomicLong runningCount;
    private AtomicLong completedCount;
    private AtomicLong failedCount;
    private LocalDateTime createTime;
    private boolean paused;

    public TaskQueue(String queueId, String name, TaskType taskType) {
        this.queueId = queueId;
        this.name = name;
        this.taskType = taskType;
        this.highPriorityQueue = new PriorityBlockingQueue<>(100,
                (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
        this.normalPriorityQueue = new PriorityBlockingQueue<>(1000,
                (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
        this.lowPriorityQueue = new PriorityBlockingQueue<>(500,
                (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
        this.taskMap = new ConcurrentHashMap<>();
        this.pendingCount = new AtomicLong(0);
        this.runningCount = new AtomicLong(0);
        this.completedCount = new AtomicLong(0);
        this.failedCount = new AtomicLong(0);
        this.createTime = LocalDateTime.now();
        this.paused = false;
    }

    public boolean offer(Task task) {
        if (paused) return false;

        taskMap.put(task.getTaskId(), task);
        pendingCount.incrementAndGet();

        return switch (getPriorityLevel(task.getPriority())) {
            case HIGH -> highPriorityQueue.offer(task);
            case LOW -> lowPriorityQueue.offer(task);
            default -> normalPriorityQueue.offer(task);
        };
    }

    public Task poll() {
        Task task = highPriorityQueue.poll();
        if (task == null) {
            task = normalPriorityQueue.poll();
        }
        if (task == null) {
            task = lowPriorityQueue.poll();
        }
        return task;
    }

    public Task poll(long timeoutMs) throws InterruptedException {
        Task task = highPriorityQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (task == null) {
            task = normalPriorityQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        if (task == null) {
            task = lowPriorityQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        return task;
    }

    public Task remove(String taskId) {
        Task task = taskMap.remove(taskId);
        if (task != null) {
            highPriorityQueue.remove(task);
            normalPriorityQueue.remove(task);
            lowPriorityQueue.remove(task);
        }
        return task;
    }

    public void taskStarted() {
        pendingCount.decrementAndGet();
        runningCount.incrementAndGet();
    }

    public void taskCompleted() {
        runningCount.decrementAndGet();
        completedCount.incrementAndGet();
    }

    public void taskFailed() {
        runningCount.decrementAndGet();
        failedCount.incrementAndGet();
    }

    public int size() {
        return highPriorityQueue.size() + normalPriorityQueue.size() + lowPriorityQueue.size();
    }

    public boolean isEmpty() {
        return highPriorityQueue.isEmpty() && normalPriorityQueue.isEmpty() && lowPriorityQueue.isEmpty();
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
    }

    private PriorityLevel getPriorityLevel(Integer priority) {
        if (priority == null) return PriorityLevel.NORMAL;
        if (priority >= 80) return PriorityLevel.HIGH;
        if (priority <= 20) return PriorityLevel.LOW;
        return PriorityLevel.NORMAL;
    }

    private enum PriorityLevel {
        HIGH, NORMAL, LOW
    }
}

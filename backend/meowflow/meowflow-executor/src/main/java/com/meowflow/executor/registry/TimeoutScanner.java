package com.meowflow.executor.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 任务超时扫描器
 */
@Slf4j
@Component
public class TimeoutScanner {

    private final Map<String, TaskTimeoutInfo> taskTimeoutMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile TimeoutCallback callback;

    public TimeoutScanner() {
        scheduler.scheduleAtFixedRate(this::scanTimeouts, 1, 1, TimeUnit.SECONDS);
    }

    public void registerTask(String taskId, long timeoutMs, LocalDateTime startTime) {
        taskTimeoutMap.put(taskId, new TaskTimeoutInfo(taskId, timeoutMs, startTime));
        log.debug("Registered task timeout: {} ({}ms)", taskId, timeoutMs);
    }

    public void unregisterTask(String taskId) {
        taskTimeoutMap.remove(taskId);
    }

    public void setCallback(TimeoutCallback callback) {
        this.callback = callback;
    }

    private void scanTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        taskTimeoutMap.forEach((taskId, info) -> {
            if (info.isTimeout(now)) {
                log.warn("Task {} timeout detected", taskId);
                taskTimeoutMap.remove(taskId);
                if (callback != null) {
                    try {
                        callback.onTimeout(taskId);
                    } catch (Exception e) {
                        log.error("Timeout callback error for task {}", taskId, e);
                    }
                }
            }
        });
    }

    public int getPendingTaskCount() {
        return taskTimeoutMap.size();
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public interface TimeoutCallback {
        void onTimeout(String taskId);
    }

    private static class TaskTimeoutInfo {
        final String taskId;
        final long timeoutMs;
        final LocalDateTime startTime;
        final LocalDateTime deadline;

        TaskTimeoutInfo(String taskId, long timeoutMs, LocalDateTime startTime) {
            this.taskId = taskId;
            this.timeoutMs = timeoutMs;
            this.startTime = startTime;
            this.deadline = startTime.plusNanos(timeoutMs * 1_000_000);
        }

        boolean isTimeout(LocalDateTime now) {
            return now.isAfter(deadline);
        }
    }
}

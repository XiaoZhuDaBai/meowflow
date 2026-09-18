package com.meowflow.executor.service;

import com.meowflow.common.util.IdGeneratorFactory;
import com.meowflow.executor.model.*;
import com.meowflow.executor.registry.TimeoutScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 任务服务
 */
@Slf4j
@Service
public class TaskService {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final Map<String, TaskQueue> queues = new ConcurrentHashMap<>();
    private final TaskDispatchService dispatchService;
    private final TimeoutScanner timeoutScanner;
    private final IdGeneratorFactory idGenerator;
    private final Executor taskExecutor;

    @Autowired(required = false)
    private MonitorService monitorService;

    // 用于存储日志ID映射
    private final Map<String, String> taskLogIdMap = new ConcurrentHashMap<>();

    public TaskService(
            TaskDispatchService dispatchService,
            TimeoutScanner timeoutScanner,
            IdGeneratorFactory idGenerator,
            @Qualifier("workflowExecutorPool") Executor taskExecutor) {
        this.dispatchService = dispatchService;
        this.timeoutScanner = timeoutScanner;
        this.idGenerator = idGenerator;
        this.taskExecutor = taskExecutor;
    }

    public Task createTask(String name, TaskType type, Map<String, Object> params) {
        Task task = Task.of(idGenerator.nextIdStr(), name, type);
        task.setParams(params);
        task.setMaxRetries(3);
        task.setTimeoutMs(30000L);
        task.setPriority(50);
        tasks.put(task.getTaskId(), task);
        log.info("Created task: {} ({})", task.getTaskId(), type);
        return task;
    }

    public Task getTask(String taskId) {
        return tasks.get(taskId);
    }

    public List<Task> getAllTasks() {
        return tasks.values().stream().toList();
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(t -> t.getStatus() == status)
                .toList();
    }

    public List<Task> getTasksByExecutor(String executorNodeId) {
        return tasks.values().stream()
                .filter(t -> executorNodeId.equals(t.getExecutorNodeId()))
                .toList();
    }

    public Task submitTask(Task task) {
        Task dispatched = dispatchService.dispatch(task);
        if (dispatched == null) {
            enqueueTask(task);
        }
        return dispatched != null ? dispatched : task;
    }

    public void enqueueTask(Task task) {
        String queueId = getQueueId(task.getType());
        TaskQueue queue = queues.computeIfAbsent(queueId,
                k -> new TaskQueue(k, task.getType().name(), task.getType()));
        queue.offer(task);
        log.info("Enqueued task: {} to queue: {}", task.getTaskId(), queueId);
    }

    public Task pollTask(String queueId) {
        TaskQueue queue = queues.get(queueId);
        if (queue == null) return null;
        return queue.poll();
    }

    public void startTask(Task task) {
        task.markRunning();
        tasks.put(task.getTaskId(), task);

        if (task.getTimeoutMs() != null && task.getTimeoutMs() > 0) {
            timeoutScanner.registerTask(task.getTaskId(), task.getTimeoutMs(), LocalDateTime.now());
        }

        // 记录监控日志
        if (monitorService != null && task.getWorkflowId() != null) {
            try {
                var request = monitorService.buildStartRequest(
                        task.getWorkflowId(),
                        task.getWorkflowName(),
                        task.getNodeId(),
                        task.getName(),
                        task.getType() != null ? task.getType().name() : null,
                        task.getParams() != null ? task.getParams().toString() : null
                );
                var response = monitorService.logStart(task.getTaskId(), request);
                if (response != null && response.getId() != null) {
                    taskLogIdMap.put(task.getTaskId(), String.valueOf(response.getId()));
                }
            } catch (Exception e) {
                log.warn("Failed to log task start to monitor: {}", e.getMessage());
            }
        }

        log.info("Started task: {}", task.getTaskId());
    }

    public void completeTask(String taskId, Object result) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.markSuccess(result != null ? result.toString() : null);
            tasks.put(taskId, task);

            timeoutScanner.unregisterTask(taskId);

            if (task.getExecutorNodeId() != null) {
                dispatchService.notifyTaskCompleted(task.getExecutorNodeId());
            }

            // 记录监控
            if (monitorService != null) {
                try {
                    String logId = taskLogIdMap.remove(taskId);
                    String outputData = result != null ? result.toString() : null;
                    monitorService.logComplete(logId, outputData);

                    // 记录节点执行指标
                    if (task.getWorkflowId() != null) {
                        monitorService.recordNodeExecution(
                                task.getWorkflowId(),
                                task.getNodeId(),
                                task.getType() != null ? task.getType().name() : null,
                                true,
                                task.getCostMs() != null ? task.getCostMs() : 0L
                        );
                    }
                } catch (Exception e) {
                    log.warn("Failed to log task complete to monitor: {}", e.getMessage());
                }
            }

            log.info("Completed task: {} (cost: {}ms)", taskId, task.getCostMs());
        }
    }

    public void failTask(String taskId, String errorMessage) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.markFailed(errorMessage);
            tasks.put(taskId, task);

            timeoutScanner.unregisterTask(taskId);

            if (task.getExecutorNodeId() != null) {
                dispatchService.notifyTaskFailed(task.getExecutorNodeId());
            }

            // 记录监控
            if (monitorService != null) {
                try {
                    String logId = taskLogIdMap.remove(taskId);
                    monitorService.logError(logId, errorMessage);

                    // 记录节点执行指标
                    if (task.getWorkflowId() != null) {
                        monitorService.recordNodeExecution(
                                task.getWorkflowId(),
                                task.getNodeId(),
                                task.getType() != null ? task.getType().name() : null,
                                false,
                                task.getCostMs() != null ? task.getCostMs() : 0L
                        );
                    }
                } catch (Exception e) {
                    log.warn("Failed to log task failure to monitor: {}", e.getMessage());
                }
            }

            if (task.canRetry()) {
                task.incrementRetry();
                submitTask(task);
                log.info("Retrying task: {} (attempt {})", taskId, task.getRetryCount());
            } else {
                log.error("Task failed permanently: {} - {}", taskId, errorMessage);
            }
        }
    }

    public void cancelTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.markCancelled();
            tasks.put(taskId, task);

            timeoutScanner.unregisterTask(taskId);

            if (task.getExecutorNodeId() != null) {
                dispatchService.notifyTaskFailed(task.getExecutorNodeId());
            }

            log.info("Cancelled task: {}", taskId);
        }
    }

    public void timeoutTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.markTimeout();
            tasks.put(taskId, task);

            if (task.getExecutorNodeId() != null) {
                dispatchService.notifyTaskFailed(task.getExecutorNodeId());
            }

            log.warn("Task timeout: {}", taskId);
        }
    }

    public Task updateTaskStatus(String taskId, TaskStatus status) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
            tasks.put(taskId, task);
        }
        return task;
    }

    public void executeAsync(Task task, TaskExecutor executor) {
        taskExecutor.execute(() -> {
            try {
                startTask(task);
                Object result = executor.execute(task);
                completeTask(task.getTaskId(), result);
            } catch (Exception e) {
                failTask(task.getTaskId(), e.getMessage());
            }
        });
    }

    public TaskQueue getQueue(String queueId) {
        return queues.get(queueId);
    }

    public List<TaskQueue> getAllQueues() {
        return queues.values().stream().toList();
    }

    public void pauseQueue(String queueId) {
        TaskQueue queue = queues.get(queueId);
        if (queue != null) {
            queue.pause();
        }
    }

    public void resumeQueue(String queueId) {
        TaskQueue queue = queues.get(queueId);
        if (queue != null) {
            queue.resume();
        }
    }

    public void deleteTask(String taskId) {
        tasks.remove(taskId);
    }

    private String getQueueId(TaskType type) {
        return "queue-" + type.name().toLowerCase();
    }

    @FunctionalInterface
    public interface TaskExecutor {
        Object execute(Task task) throws Exception;
    }
}

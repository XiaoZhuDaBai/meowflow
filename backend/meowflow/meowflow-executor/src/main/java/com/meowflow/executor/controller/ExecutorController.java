package com.meowflow.executor.controller;

import com.meowflow.common.result.Result;
import com.meowflow.executor.model.*;
import com.meowflow.executor.registry.ExecutorNode;
import com.meowflow.executor.service.ExecutorNodeRegistry;
import com.meowflow.executor.service.TaskDispatchService;
import com.meowflow.executor.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 执行器管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/executor")
@Tag(name = "执行器管理", description = "执行器节点与任务管理接口")
public class ExecutorController {

    private final ExecutorNodeRegistry nodeRegistry;
    private final TaskService taskService;
    private final TaskDispatchService dispatchService;

    public ExecutorController(
            ExecutorNodeRegistry nodeRegistry,
            TaskService taskService,
            TaskDispatchService dispatchService) {
        this.nodeRegistry = nodeRegistry;
        this.taskService = taskService;
        this.dispatchService = dispatchService;
    }

    @PostMapping("/nodes/register")
    @Operation(summary = "注册执行节点", description = "注册一个新的执行节点")
    public Result<ExecutorNode> registerNode(@RequestBody RegisterNodeRequest request) {
        ExecutorNode node = nodeRegistry.register(
                request.getName(),
                request.getHost(),
                request.getPort(),
                request.getMaxConcurrentTasks());
        return Result.success(node);
    }

    @DeleteMapping("/nodes/{nodeId}")
    @Operation(summary = "注销执行节点", description = "注销指定的执行节点")
    public Result<Void> unregisterNode(@PathVariable String nodeId) {
        nodeRegistry.unregister(nodeId);
        return Result.success();
    }

    @PostMapping("/nodes/{nodeId}/heartbeat")
    @Operation(summary = "节点心跳", description = "执行节点发送心跳")
    public Result<ExecutorNode> heartbeat(@PathVariable String nodeId) {
        ExecutorNode node = nodeRegistry.heartbeat(nodeId);
        return Result.success(node);
    }

    @GetMapping("/nodes")
    @Operation(summary = "列出执行节点", description = "列出所有执行节点")
    public Result<List<ExecutorNode>> listNodes() {
        return Result.success(nodeRegistry.getAllNodes());
    }

    @GetMapping("/nodes/healthy")
    @Operation(summary = "列出健康节点", description = "列出所有健康的执行节点")
    public Result<List<ExecutorNode>> listHealthyNodes() {
        return Result.success(nodeRegistry.getHealthyNodes());
    }

    @GetMapping("/nodes/{nodeId}")
    @Operation(summary = "获取执行节点", description = "获取指定节点的信息")
    public Result<ExecutorNode> getNode(@PathVariable String nodeId) {
        return Result.success(nodeRegistry.getNode(nodeId));
    }

    @PutMapping("/nodes/{nodeId}/capabilities")
    @Operation(summary = "更新节点能力", description = "更新执行节点的能力列表")
    public Result<Void> updateCapabilities(
            @PathVariable String nodeId,
            @RequestBody List<String> capabilities) {
        nodeRegistry.updateNodeCapabilities(nodeId, capabilities);
        return Result.success();
    }

    @PostMapping("/tasks")
    @Operation(summary = "创建任务", description = "创建一个新任务")
    public Result<Task> createTask(@RequestBody CreateTaskRequest request) {
        Task task = taskService.createTask(request.getName(),
                request.getType(), request.getParams());
        return Result.success(task);
    }

    @PostMapping("/tasks/{taskId}/submit")
    @Operation(summary = "提交任务", description = "提交任务进行分发执行")
    public Result<Task> submitTask(@PathVariable String taskId) {
        Task task = taskService.getTask(taskId);
        if (task == null) {
            return Result.error(404, "Task not found");
        }
        Task submitted = taskService.submitTask(task);
        return Result.success(submitted);
    }

    @PostMapping("/tasks/{taskId}/start")
    @Operation(summary = "开始任务", description = "开始执行指定的任务")
    public Result<Void> startTask(@PathVariable String taskId) {
        Task task = taskService.getTask(taskId);
        if (task == null) {
            return Result.error(404, "Task not found");
        }
        taskService.startTask(task);
        return Result.success();
    }

    @PostMapping("/tasks/{taskId}/complete")
    @Operation(summary = "完成任务", description = "标记任务为完成状态")
    public Result<Void> completeTask(
            @PathVariable String taskId,
            @RequestBody CompleteTaskRequest request) {
        taskService.completeTask(taskId, request.getResult());
        return Result.success();
    }

    @PostMapping("/tasks/{taskId}/fail")
    @Operation(summary = "失败任务", description = "标记任务为失败状态")
    public Result<Void> failTask(
            @PathVariable String taskId,
            @RequestBody FailTaskRequest request) {
        taskService.failTask(taskId, request.getErrorMessage());
        return Result.success();
    }

    @PostMapping("/tasks/{taskId}/cancel")
    @Operation(summary = "取消任务", description = "取消指定的任务")
    public Result<Void> cancelTask(@PathVariable String taskId) {
        taskService.cancelTask(taskId);
        return Result.success();
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "获取任务", description = "获取指定任务的信息")
    public Result<Task> getTask(@PathVariable String taskId) {
        Task task = taskService.getTask(taskId);
        return task == null ? Result.error(404, "Task not found") : Result.success(task);
    }

    @GetMapping("/tasks")
    @Operation(summary = "列出任务", description = "列出所有任务")
    public Result<List<Task>> listTasks(
            @Parameter(description = "按状态筛选") @RequestParam(required = false) TaskStatus status) {
        if (status != null) {
            return Result.success(taskService.getTasksByStatus(status));
        }
        return Result.success(taskService.getAllTasks());
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "删除任务", description = "删除指定的任务")
    public Result<Void> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return Result.success();
    }

    @GetMapping("/queues")
    @Operation(summary = "列出任务队列", description = "列出所有任务队列")
    public Result<List<TaskQueue>> listQueues() {
        return Result.success(taskService.getAllQueues());
    }

    @PostMapping("/queues/{queueId}/pause")
    @Operation(summary = "暂停队列", description = "暂停指定的任务队列")
    public Result<Void> pauseQueue(@PathVariable String queueId) {
        taskService.pauseQueue(queueId);
        return Result.success();
    }

    @PostMapping("/queues/{queueId}/resume")
    @Operation(summary = "恢复队列", description = "恢复指定的任务队列")
    public Result<Void> resumeQueue(@PathVariable String queueId) {
        taskService.resumeQueue(queueId);
        return Result.success();
    }

    @GetMapping("/status")
    @Operation(summary = "获取执行器状态", description = "获取执行器的整体状态")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = Map.of(
                "totalNodes", nodeRegistry.getTotalNodes(),
                "healthyNodes", nodeRegistry.getHealthyNodesCount(),
                "totalTasks", taskService.getAllTasks().size(),
                "pendingTasks", taskService.getTasksByStatus(TaskStatus.PENDING).size(),
                "runningTasks", taskService.getTasksByStatus(TaskStatus.RUNNING).size(),
                "completedTasks", taskService.getTasksByStatus(TaskStatus.SUCCESS).size(),
                "failedTasks", taskService.getTasksByStatus(TaskStatus.FAILED).size());
        return Result.success(status);
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "执行器健康检查接口")
    public Result<Boolean> health() {
        return Result.success(true);
    }

    @Data
    public static class RegisterNodeRequest {
        @Parameter(description = "节点名称")
        private String name;

        @Parameter(description = "主机地址")
        private String host;

        @Parameter(description = "端口")
        private int port;

        @Parameter(description = "最大并发任务数")
        private int maxConcurrentTasks = 10;
    }

    @Data
    public static class CreateTaskRequest {
        @Parameter(description = "任务名称")
        private String name;

        @Parameter(description = "任务类型")
        private TaskType type;

        @Parameter(description = "任务参数")
        private Map<String, Object> params;
    }

    @Data
    public static class CompleteTaskRequest {
        @Parameter(description = "执行结果")
        private Object result;
    }

    @Data
    public static class FailTaskRequest {
        @Parameter(description = "错误信息")
        private String errorMessage;
    }
}




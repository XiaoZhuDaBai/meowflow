package com.meowflow.common.client;

import com.meowflow.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Monitor 服务 Feign 客户端
 * 供其他服务调用监控功能
 */
@FeignClient(name = "meowflow-monitor", path = "/api/monitor")
public interface MonitorFeignClient {

    // ==================== 执行日志相关 ====================

    /**
     * 记录执行开始
     */
    @PostMapping("/log/start")
    Result<ExecutionLogResponse> logStart(@RequestBody ExecutionLogCreateRequest request);

    /**
     * 记录执行完成
     */
    @PostMapping("/log/{logId}/complete")
    Result<ExecutionLogResponse> logComplete(
            @PathVariable("logId") String logId,
            @RequestBody(required = false) String outputData);

    /**
     * 记录执行错误
     */
    @PostMapping("/log/{logId}/error")
    Result<ExecutionLogResponse> logError(
            @PathVariable("logId") String logId,
            @RequestBody String errorMessage);

    /**
     * 记录重试
     */
    @PostMapping("/log/{logId}/retry")
    Result<ExecutionLogResponse> logRetry(
            @PathVariable("logId") String logId,
            @RequestParam("retryCount") Integer retryCount);

    // ==================== 指标相关 ====================

    /**
     * 记录工作流执行指标
     */
    @PostMapping("/metrics/workflow/execution")
    Result<Void> recordWorkflowExecution(
            @RequestParam("workflowId") String workflowId,
            @RequestParam("workflowName") String workflowName,
            @RequestParam("version") Integer version,
            @RequestParam("triggerType") String triggerType,
            @RequestParam("success") boolean success,
            @RequestParam("durationMs") long durationMs);

    /**
     * 记录节点执行指标
     */
    @PostMapping("/metrics/node/execution")
    Result<Void> recordNodeExecution(
            @RequestParam("workflowId") String workflowId,
            @RequestParam("nodeId") String nodeId,
            @RequestParam("nodeType") String nodeType,
            @RequestParam("success") boolean success,
            @RequestParam("durationMs") long durationMs);

    // ==================== 日志查询 ====================

    @GetMapping("/log/execution/{executionId}")
    Result<java.util.List<ExecutionLogResponse>> getExecutionLogs(
            @PathVariable("executionId") Long executionId);

    @GetMapping("/log/execution/{executionId}/node/{nodeId}")
    Result<java.util.List<ExecutionLogResponse>> getNodeLogs(
            @PathVariable("executionId") Long executionId,
            @PathVariable("nodeId") String nodeId);
}

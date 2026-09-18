package com.meowflow.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.monitor.dto.ExecutionLogCreateRequest;
import com.meowflow.monitor.dto.ExecutionLogDTO;
import com.meowflow.monitor.dto.ExecutionLogQuery;
import com.meowflow.monitor.entity.ExecutionLog;
import com.meowflow.monitor.service.ExecutionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "执行日志", description = "工作流执行日志相关接口")
@RestController
@RequestMapping("/api/monitor/log")
@RequiredArgsConstructor
public class ExecutionLogController {

    private final ExecutionLogService logService;

    @Operation(summary = "记录执行开始", description = "记录工作流节点执行开始")
    @PostMapping("/start")
    public Result<ExecutionLog> logStart(@RequestBody ExecutionLogCreateRequest request) {
        return Result.success(logService.logStart(request));
    }

    @Operation(summary = "记录执行完成", description = "记录工作流节点执行完成")
    @PostMapping("/{logId}/complete")
    public Result<ExecutionLog> logComplete(
            @Parameter(description = "日志ID") @PathVariable Long logId,
            @RequestBody(required = false) String outputData) {
        return Result.success(logService.logComplete(logId, outputData));
    }

    @Operation(summary = "记录执行错误", description = "记录工作流节点执行错误")
    @PostMapping("/{logId}/error")
    public Result<ExecutionLog> logError(
            @Parameter(description = "日志ID") @PathVariable Long logId,
            @RequestBody String errorMessage) {
        return Result.success(logService.logError(logId, errorMessage));
    }

    @Operation(summary = "查询执行日志", description = "根据条件查询执行日志")
    @PostMapping("/query")
    public Result<IPage<ExecutionLogDTO>> queryLogs(@RequestBody ExecutionLogQuery query) {
        return Result.success(logService.queryLogs(query));
    }

    @Operation(summary = "获取执行流程日志", description = "获取指定执行的所有节点日志")
    @GetMapping("/execution/{executionId}")
    public Result<List<ExecutionLogDTO>> getExecutionLogs(
            @Parameter(description = "执行ID") @PathVariable Long executionId) {
        return Result.success(logService.getExecutionLogs(executionId));
    }

    @Operation(summary = "获取节点日志", description = "获取指定执行中特定节点的日志")
    @GetMapping("/execution/{executionId}/node/{nodeId}")
    public Result<List<ExecutionLogDTO>> getNodeLogs(
            @Parameter(description = "执行ID") @PathVariable Long executionId,
            @Parameter(description = "节点ID") @PathVariable String nodeId) {
        return Result.success(logService.getNodeLogs(executionId, nodeId));
    }

    @Operation(summary = "获取错误日志", description = "获取所有错误日志")
    @GetMapping("/errors")
    public Result<IPage<ExecutionLogDTO>> getErrorLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(logService.getErrorLogs(pageNum, pageSize));
    }
}

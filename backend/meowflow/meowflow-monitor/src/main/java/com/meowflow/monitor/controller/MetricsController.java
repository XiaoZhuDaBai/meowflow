package com.meowflow.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.monitor.dto.MetricDTO;
import com.meowflow.monitor.dto.MetricsQuery;
import com.meowflow.monitor.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "监控指标", description = "系统指标和业务指标相关接口")
@RestController
@RequestMapping("/api/monitor/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @Operation(summary = "查询指标", description = "根据条件查询监控指标")
    @PostMapping("/query")
    public Result<IPage<MetricDTO>> queryMetrics(@RequestBody MetricsQuery query) {
        return Result.success(metricsService.queryMetrics(query));
    }

    @Operation(summary = "获取最新指标", description = "获取最近的监控指标")
    @GetMapping("/latest")
    public Result<List<MetricDTO>> getLatestMetrics(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "50") int limit) {
        return Result.success(metricsService.getLatestMetrics(limit));
    }

    @Operation(summary = "记录计数器", description = "记录一个计数器指标")
    @PostMapping("/counter")
    public Result<Void> recordCounter(
            @RequestParam String name,
            @RequestBody(required = false) Map<String, String> tags) {
        if (tags != null && !tags.isEmpty()) {
            metricsService.recordCounter(name, tags.keySet().toArray(new String[0]));
        } else {
            metricsService.recordCounter(name);
        }
        return Result.success();
    }

    @Operation(summary = "记录计时器", description = "记录一个计时器指标")
    @PostMapping("/timer")
    public Result<Void> recordTimer(
            @RequestParam String name,
            @RequestParam long durationMs,
            @RequestBody(required = false) Map<String, String> tags) {
        if (tags != null && !tags.isEmpty()) {
            metricsService.recordTimer(name, durationMs, tags.keySet().toArray(new String[0]));
        } else {
            metricsService.recordTimer(name, durationMs);
        }
        return Result.success();
    }

    @Operation(summary = "记录仪表盘", description = "记录一个仪表盘指标")
    @PostMapping("/gauge")
    public Result<Void> recordGauge(
            @RequestParam String name,
            @RequestParam double value,
            @RequestBody(required = false) Map<String, String> tags) {
        if (tags != null && !tags.isEmpty()) {
            metricsService.recordGauge(name, value, tags.keySet().toArray(new String[0]));
        } else {
            metricsService.recordGauge(name, value);
        }
        return Result.success();
    }

    @Operation(summary = "记录工作流执行", description = "记录工作流执行指标")
    @PostMapping("/workflow/execution")
    public Result<Void> recordWorkflowExecution(
            @RequestParam String workflowId,
            @RequestParam boolean success,
            @RequestParam long durationMs) {
        metricsService.recordWorkflowExecution(workflowId, success, durationMs);
        return Result.success();
    }

    @Operation(summary = "记录节点执行", description = "记录节点执行指标")
    @PostMapping("/node/execution")
    public Result<Void> recordNodeExecution(
            @RequestParam String workflowId,
            @RequestParam String nodeId,
            @RequestParam String nodeType,
            @RequestParam boolean success,
            @RequestParam long durationMs) {
        metricsService.recordNodeExecution(workflowId, nodeId, nodeType, success, durationMs);
        return Result.success();
    }

    @Operation(summary = "记录API调用", description = "记录API调用指标")
    @PostMapping("/api/call")
    public Result<Void> recordApiCall(
            @RequestParam String endpoint,
            @RequestParam int statusCode,
            @RequestParam long durationMs) {
        metricsService.recordApiCall(endpoint, statusCode, durationMs);
        return Result.success();
    }
}

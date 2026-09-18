package com.meowflow.workflow.controller;

import com.meowflow.common.client.ExecutionLogResponse;
import com.meowflow.common.client.MonitorFeignClient;
import com.meowflow.common.context.DebugManager;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.result.Result;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "执行管理")
@RestController
@RequestMapping("/api/execution")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;
    private final MonitorFeignClient monitorFeignClient;

    @Operation(summary = "执行工作流")
    @PostMapping
    public Result<ExecutionResponse> execute(@Valid @RequestBody ExecutionRequest request) {
        Long userId = getCurrentUserId();
        ExecutionResponse response = executionService.execute(request, userId);
        return Result.success(response);
    }

    @Operation(summary = "取消执行")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        executionService.cancel(id);
        return Result.success();
    }

    // ===================================================================
    // 调试模式 API (断点 / 单步 / 变量查看)
    // 参考 Dify Workflow / Coze Studio debug 包设计
    // ===================================================================

    @Operation(summary = "为执行实例设置断点（多个节点）")
    @PostMapping("/{id}/debug/breakpoints")
    public Result<Set<String>> setBreakpoints(
            @PathVariable Long id,
            @RequestBody Set<String> nodeIds) {
        DebugManager dm = ensureDebugManager(id);
        if (nodeIds == null) {
            dm.clearBreakpoints();
        } else {
            dm.clearBreakpoints();
            nodeIds.forEach(dm::addBreakpoint);
        }
        log.info("Set breakpoints for execution={}: {}", id, dm.getBreakpoints());
        return Result.success(dm.getBreakpoints());
    }

    @Operation(summary = "添加单个断点")
    @PostMapping("/{id}/debug/breakpoints/{nodeId}")
    public Result<Void> addBreakpoint(
            @PathVariable Long id,
            @PathVariable String nodeId) {
        ensureDebugManager(id).addBreakpoint(nodeId);
        return Result.success();
    }

    @Operation(summary = "移除单个断点")
    @DeleteMapping("/{id}/debug/breakpoints/{nodeId}")
    public Result<Void> removeBreakpoint(
            @PathVariable Long id,
            @PathVariable String nodeId) {
        ensureDebugManager(id).removeBreakpoint(nodeId);
        return Result.success();
    }

    @Operation(summary = "获取当前所有断点")
    @GetMapping("/{id}/debug/breakpoints")
    public Result<Set<String>> getBreakpoints(@PathVariable Long id) {
        DebugManager dm = DebugManager.get(id);
        return Result.success(dm == null ? Set.of() : dm.getBreakpoints());
    }

    @Operation(summary = "恢复执行（继续）")
    @PostMapping("/{id}/debug/resume")
    public Result<Boolean> resume(@PathVariable Long id) {
        DebugManager dm = DebugManager.get(id);
        if (dm == null) return Result.success(false);
        boolean ok = dm.resume();
        log.info("Resume execution={}, result={}", id, ok);
        return Result.success(ok);
    }

    @Operation(summary = "单步执行（执行下一节点后再次暂停）")
    @PostMapping("/{id}/debug/step")
    public Result<Boolean> step(@PathVariable Long id) {
        DebugManager dm = DebugManager.get(id);
        if (dm == null) return Result.success(false);
        boolean ok = dm.step();
        log.info("Step execution={}, result={}", id, ok);
        return Result.success(ok);
    }

    @Operation(summary = "停止调试（终止当前执行）")
    @PostMapping("/{id}/debug/stop")
    public Result<Boolean> stop(@PathVariable Long id) {
        DebugManager dm = DebugManager.get(id);
        if (dm != null) dm.stop();
        executionService.cancel(id);
        log.info("Stop debug execution={}", id);
        return Result.success(true);
    }

    @Operation(summary = "获取调试状态（前端轮询用）")
    @GetMapping("/{id}/debug/status")
    public Result<Map<String, Object>> debugStatus(@PathVariable Long id) {
        DebugManager dm = DebugManager.get(id);
        if (dm == null) {
            return Result.success(Map.of(
                    "executionId", id,
                    "state", "IDLE",
                    "pausedAtNodeId", "",
                    "pauseReason", "",
                    "breakpoints", Set.of(),
                    "nodeSnapshots", Map.of()
            ));
        }
        return Result.success(dm.snapshot());
    }

    @Operation(summary = "获取所有节点的执行快照（变量查看器）")
    @GetMapping("/{id}/debug/snapshots")
    public Result<Map<String, Map<String, Object>>> nodeSnapshots(@PathVariable Long id) {
        DebugManager dm = DebugManager.get(id);
        if (dm == null) return Result.success(Map.of());
        return Result.success(dm.getAllSnapshots());
    }

    @Operation(summary = "获取单个节点的执行快照")
    @GetMapping("/{id}/debug/snapshots/{nodeId}")
    public Result<Map<String, Object>> nodeSnapshot(
            @PathVariable Long id,
            @PathVariable String nodeId) {
        DebugManager dm = DebugManager.get(id);
        if (dm == null) return Result.success(Map.of());
        Map<String, Object> snap = dm.getSnapshot(nodeId);
        return Result.success(snap == null ? Map.of() : snap);
    }

    /** 确保 DebugManager 已挂载（自动从 ExecutionService 复用或新建） */
    private DebugManager ensureDebugManager(Long executionId) {
        DebugManager dm = DebugManager.get(executionId);
        if (dm == null) {
            dm = DebugManager.attach(executionId);
        }
        return dm;
    }

    @Operation(summary = "获取执行详情")
    @GetMapping("/{id}")
    public Result<ExecutionResponse> getById(@PathVariable Long id) {
        ExecutionResponse response = executionService.getById(id);
        return Result.success(response);
    }

    @Operation(summary = "分页查询执行记录")
    @GetMapping("/page")
    public Result<PageResponse<ExecutionResponse>> page(
            @ModelAttribute ExecutionService.ExecutionQueryRequest request) {
        PageResponse<ExecutionResponse> response = executionService.page(request);
        return Result.success(response);
    }

    @Operation(summary = "获取节点执行列表")
    @GetMapping("/{id}/nodes")
    public Result<List<NodeExecutionDto>> getNodeExecutions(@PathVariable Long id) {
        List<NodeExecutionDto> nodes = executionService.getNodeExecutions(id);
        return Result.success(nodes);
    }

    @Operation(summary = "获取执行日志")
    @GetMapping("/{id}/logs")
    public Result<List<ExecutionLogDto>> getLogs(
            @PathVariable Long id,
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String level) {
        try {
            List<ExecutionLogResponse> logs;
            if (nodeId != null && !nodeId.isEmpty()) {
                logs = monitorFeignClient.getNodeLogs(id, nodeId).getData();
            } else {
                logs = monitorFeignClient.getExecutionLogs(id).getData();
            }
            if (logs == null) {
                logs = List.of();
            }
            return Result.success(logs.stream().map(this::toLogDto).collect(Collectors.toList()));
        } catch (Exception e) {
            // Monitor 服务不可用时优雅降级：返回空列表而不是失败
            // 建议使用 start-all.ps1 启动完整服务以获取日志功能
            log.warn("Failed to fetch logs from monitor service for executionId={}: {}", id, e.getMessage());
            return Result.success(List.of());
        }
    }

    @Operation(summary = "获取工作流的所有执行记录")
    @GetMapping("/workflow/{workflowId}")
    public Result<PageResponse<ExecutionResponse>> listByWorkflow(
            @PathVariable Long workflowId,
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        ExecutionService.ExecutionQueryRequest request = new ExecutionService.ExecutionQueryRequest();
        request.setWorkflowId(workflowId);
        request.setCurrent(current);
        request.setSize(size);
        PageResponse<ExecutionResponse> page = executionService.page(request);
        return Result.success(page);
    }

    private Long getCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? userId : 1L;
    }

    private ExecutionLogDto toLogDto(ExecutionLogResponse log) {
        ExecutionLogDto dto = new ExecutionLogDto();
        dto.setId(log.getId());
        dto.setExecutionId(log.getExecutionId());
        dto.setNodeId(log.getNodeId());
        dto.setLevel(log.getStatus());
        dto.setMessage(log.getErrorMessage() != null ? log.getErrorMessage() : log.getOutputData());
        dto.setPayload(null);
        dto.setCreatedAt(log.getCreateTime() != null ? log.getCreateTime().toString() : null);
        return dto;
    }
}

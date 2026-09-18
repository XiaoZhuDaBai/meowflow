package com.meowflow.workflow.controller;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.result.Result;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.entity.WorkflowVersion;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import com.meowflow.workflow.service.WorkflowImportExportService;
import com.meowflow.workflow.service.WorkflowService;
import com.meowflow.workflow.trigger.TriggerManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "工作流管理")
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowImportExportService importExportService;
    private final TriggerManager triggerManager;
    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository versionRepository;

    @Operation(summary = "创建工作流")
    @PostMapping
    public Result<WorkflowResponse> create(@Valid @RequestBody WorkflowCreateRequest request) {
        Long userId = getCurrentUserId();
        WorkflowResponse response = workflowService.create(request, userId);
        return Result.success(response);
    }

    @Operation(summary = "更新工作流")
    @PutMapping("/{id}")
    public Result<WorkflowResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowUpdateRequest request) {
        Long userId = getCurrentUserId();
        WorkflowResponse response = workflowService.update(id, request, userId);
        return Result.success(response);
    }

    @Operation(summary = "删除工作流")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return Result.success();
    }

    @Operation(summary = "获取工作流详情")
    @GetMapping("/{id}")
    public Result<WorkflowResponse> getById(@PathVariable Long id) {
        WorkflowResponse response = workflowService.getById(id);
        return Result.success(response);
    }

    @Operation(summary = "分页查询工作流")
    @GetMapping("/page")
    public Result<PageResponse<WorkflowResponse>> page(
            @ModelAttribute WorkflowService.WorkflowQueryRequest request) {
        PageResponse<WorkflowResponse> response = workflowService.page(request);
        return Result.success(response);
    }

    @Operation(summary = "保存工作流版本")
    @PostMapping("/{id}/versions")
    public Result<WorkflowVersionResponse> saveVersion(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowDefinitionRequest request) {
        Long userId = getCurrentUserId();
        WorkflowVersionResponse response = workflowService.saveVersion(id, request, userId);
        return Result.success(response);
    }

    @Operation(summary = "发布工作流版本")
    @PostMapping("/{id}/versions/publish")
    public Result<WorkflowVersionResponse> publishVersion(
            @PathVariable Long id,
            @Valid @RequestBody PublishVersionRequest request) {
        Long userId = getCurrentUserId();
        WorkflowVersionResponse response = workflowService.publishVersion(id, request, userId);
        return Result.success(response);
    }

    @Operation(summary = "获取工作流版本列表")
    @GetMapping("/{id}/versions")
    public Result<List<WorkflowVersionResponse>> listVersions(@PathVariable Long id) {
        List<WorkflowVersionResponse> versions = workflowService.listVersions(id);
        return Result.success(versions);
    }

    // ==================== 新增端点 ====================

    @Operation(summary = "停止工作流（取消所有定时调度）")
    @PostMapping("/{id}/stop")
    @Transactional
    public Result<WorkflowResponse> stop(@PathVariable Long id) {
        WorkflowResponse wf = workflowService.getById(id);
        if (wf == null) {
            return Result.error(404, "工作流不存在");
        }
        // 取消所有调度
        triggerManager.cancelAllTasks(id);
        // 更新状态
        workflowService.updateStatus(id, "draft");
        return Result.success(workflowService.getById(id));
    }

    @Operation(summary = "复制工作流")
    @PostMapping("/{id}/copy")
    @Transactional
    public Result<WorkflowResponse> copy(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        WorkflowResponse original = workflowService.getById(id);
        if (original == null) {
            return Result.error(404, "工作流不存在");
        }
        // 导出后作为新工作流导入
        String exportJson = importExportService.export(id);
        WorkflowVersion version = importExportService.importFromJson(exportJson, true, userId);

        WorkflowUpdateRequest nameRequest = new WorkflowUpdateRequest();
        nameRequest.setName(original.getName() + " (副本)");
        workflowService.update(version.getWorkflowId(), nameRequest, userId);

        return Result.success(workflowService.getById(version.getWorkflowId()));
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/{id}/rollback/{version}")
    @Transactional
    public Result<WorkflowVersionResponse> rollback(
            @PathVariable Long id,
            @PathVariable String version) {
        Long userId = getCurrentUserId();
        var targetVersion = workflowService.getVersion(id, version);
        if (targetVersion == null) {
            return Result.error(404, "目标版本不存在");
        }

        PublishVersionRequest req = new PublishVersionRequest();
        req.setVersion(version);
        req.setChangelog("回滚到版本 " + version);

        // 如果当前状态是 running，先取消调度
        var current = workflowService.getById(id);
        if ("running".equals(current.getStatus())) {
            triggerManager.cancelAllTasks(id);
        }

        workflowService.publishVersion(id, req, userId);
        return Result.success(workflowService.listVersions(id).stream()
                .filter(v -> version.equals(v.getVersion()))
                .findFirst()
                .orElseThrow());
    }

    @Operation(summary = "导出工作流")
    @GetMapping("/{id}/export")
    public Result<String> export(@PathVariable Long id) {
        String json = importExportService.export(id);
        return Result.success(json);
    }

    @Operation(summary = "导出工作流（含版本历史）")
    @GetMapping("/{id}/export-with-history")
    public Result<String> exportWithHistory(@PathVariable Long id) {
        String json = importExportService.export(id, true);
        return Result.success(json);
    }

    @Operation(summary = "导入工作流（新建）")
    @PostMapping("/import")
    @Transactional
    public Result<WorkflowVersionResponse> importWorkflow(
            @RequestBody ImportRequest request) {
        Long userId = getCurrentUserId();
        WorkflowVersion version = importExportService.importFromJson(
                request.getJson(), true, userId);
        return Result.success(toVersionResponse(version));
    }

    @Operation(summary = "导入工作流（追加版本）")
    @PostMapping("/{id}/import-version")
    @Transactional
    public Result<WorkflowVersionResponse> importVersion(
            @PathVariable Long id,
            @RequestBody ImportRequest request) {
        Long userId = getCurrentUserId();
        WorkflowVersion version = importExportService.importFromJson(
                request.getJson(), false, userId);
        // 同步当前版本
        workflowService.updateCurrentVersion(id, version.getVersion());
        return Result.success(toVersionResponse(version));
    }

    // ==================== 私有方法 ====================

    private Long getCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? userId : 1L;
    }

    private WorkflowVersionResponse toVersionResponse(WorkflowVersion v) {
        WorkflowVersionResponse r = new WorkflowVersionResponse();
        r.setId(v.getId());
        r.setWorkflowId(v.getWorkflowId());
        r.setVersion(v.getVersion());
        r.setPublishStatus(v.getPublishStatus());
        r.setChangelog(v.getChangelog());
        r.setPublishedAt(v.getPublishedAt() != null ? v.getPublishedAt().toString() : null);
        r.setPublishedBy(v.getPublishedBy());
        r.setCreateTime(v.getCreateTime() != null ? v.getCreateTime().toString() : null);
        return r;
    }

    @lombok.Data
    public static class ImportRequest {
        private String json;
    }
}

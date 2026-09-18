package com.meowflow.workflow.controller;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.result.Result;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.service.WorkflowGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "工作流分组")
@RestController
@RequestMapping("/api/workflow/group")
@RequiredArgsConstructor
public class WorkflowGroupController {

    private final WorkflowGroupService groupService;

    @Operation(summary = "创建分组")
    @PostMapping
    public Result<WorkflowGroupService.GroupResponse> create(
            @Valid @RequestBody GroupCreateRequest request) {
        Long userId = getCurrentUserId();
        WorkflowGroupService.GroupResponse response = groupService.create(request, userId);
        return Result.success(response);
    }

    @Operation(summary = "更新分组")
    @PutMapping("/{id}")
    public Result<WorkflowGroupService.GroupResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody GroupUpdateRequest request) {
        Long userId = getCurrentUserId();
        WorkflowGroupService.GroupResponse response = groupService.update(id, request, userId);
        return Result.success(response);
    }

    @Operation(summary = "删除分组")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        groupService.delete(id, userId);
        return Result.success();
    }

    @Operation(summary = "获取分组详情")
    @GetMapping("/{id}")
    public Result<WorkflowGroupService.GroupResponse> getById(@PathVariable Long id) {
        WorkflowGroupService.GroupResponse response = groupService.getById(id);
        return Result.success(response);
    }

    @Operation(summary = "获取当前用户的分组列表")
    @GetMapping("/list")
    public Result<List<WorkflowGroupService.GroupResponse>> list() {
        Long userId = getCurrentUserId();
        List<WorkflowGroupService.GroupResponse> groups = groupService.listByUser(userId);
        return Result.success(groups);
    }

    private Long getCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? userId : 1L;
    }
}

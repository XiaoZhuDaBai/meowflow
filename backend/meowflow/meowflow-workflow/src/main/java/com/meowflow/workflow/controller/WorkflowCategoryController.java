package com.meowflow.workflow.controller;

import com.meowflow.common.result.Result;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.service.WorkflowCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "工作流分类")
@RestController
@RequestMapping("/api/workflow/category")
@RequiredArgsConstructor
public class WorkflowCategoryController {

    private final WorkflowCategoryService categoryService;

    @Operation(summary = "创建分类")
    @PostMapping
    public Result<WorkflowCategoryService.CategoryResponse> create(
            @Valid @RequestBody CategoryCreateRequest request) {
        WorkflowCategoryService.CategoryResponse response = categoryService.create(request);
        return Result.success(response);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<WorkflowCategoryService.CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        WorkflowCategoryService.CategoryResponse response = categoryService.update(id, request);
        return Result.success(response);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public Result<WorkflowCategoryService.CategoryResponse> getById(@PathVariable Long id) {
        WorkflowCategoryService.CategoryResponse response = categoryService.getById(id);
        return Result.success(response);
    }

    @Operation(summary = "获取分类列表")
    @GetMapping("/list")
    public Result<List<WorkflowCategoryService.CategoryResponse>> list() {
        List<WorkflowCategoryService.CategoryResponse> categories = categoryService.list();
        return Result.success(categories);
    }

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<WorkflowCategoryService.CategoryResponse>> tree() {
        List<WorkflowCategoryService.CategoryResponse> tree = categoryService.tree();
        return Result.success(tree);
    }
}

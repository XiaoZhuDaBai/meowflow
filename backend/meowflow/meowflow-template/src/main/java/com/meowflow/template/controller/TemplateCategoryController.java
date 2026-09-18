package com.meowflow.template.controller;

import com.meowflow.common.result.Result;
import com.meowflow.template.dto.*;
import com.meowflow.template.service.TemplateCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "模板分类管理", description = "模板分类和标签管理接口")
@RestController
@RequestMapping("/api/template/category")
@RequiredArgsConstructor
public class TemplateCategoryController {

    private final TemplateCategoryService categoryService;

    @Operation(summary = "创建分类", description = "创建新分类")
    @PostMapping
    public Result<TemplateCategoryDTO> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return Result.success(categoryService.createCategory(request));
    }

    @Operation(summary = "更新分类", description = "更新已有分类")
    @PutMapping("/{id}")
    public Result<TemplateCategoryDTO> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Valid @RequestBody CategoryCreateRequest request) {
        return Result.success(categoryService.updateCategory(id, request));
    }

    @Operation(summary = "删除分类", description = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    @Operation(summary = "获取分类详情", description = "根据ID获取分类详细信息")
    @GetMapping("/{id}")
    public Result<TemplateCategoryDTO> getCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        return Result.success(categoryService.getCategory(id));
    }

    @Operation(summary = "创建标签", description = "创建新标签")
    @PostMapping("/tag")
    public Result<TemplateTagDTO> createTag(@Valid @RequestBody TagCreateRequest request) {
        return Result.success(categoryService.createTag(request));
    }

    @Operation(summary = "更新标签", description = "更新已有标签")
    @PutMapping("/tag/{id}")
    public Result<TemplateTagDTO> updateTag(
            @Parameter(description = "标签ID") @PathVariable Long id,
            @Valid @RequestBody TagCreateRequest request) {
        return Result.success(categoryService.updateTag(id, request));
    }

    @Operation(summary = "删除标签", description = "删除标签")
    @DeleteMapping("/tag/{id}")
    public Result<Void> deleteTag(@Parameter(description = "标签ID") @PathVariable Long id) {
        categoryService.deleteTag(id);
        return Result.success();
    }
}


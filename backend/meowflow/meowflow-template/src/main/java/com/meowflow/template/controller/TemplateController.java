package com.meowflow.template.controller;

import com.meowflow.common.result.Result;
import com.meowflow.template.dto.*;
import com.meowflow.template.service.TemplateInteractionService;
import com.meowflow.template.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "模板管理", description = "模板 CRUD 相关接口")
@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateInteractionService interactionService;

    @Operation(summary = "创建模板", description = "创建新模板")
    @PostMapping
    public Result<TemplateDTO> createTemplate(@Valid @RequestBody TemplateCreateRequest request) {
        return Result.success(templateService.createTemplate(request));
    }

    @Operation(summary = "更新模板", description = "更新已有模板")
    @PutMapping("/{id}")
    public Result<TemplateDTO> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Valid @RequestBody TemplateUpdateRequest request) {
        return Result.success(templateService.updateTemplate(id, request));
    }

    @Operation(summary = "删除模板", description = "删除模板（软删除）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@Parameter(description = "模板ID") @PathVariable Long id) {
        templateService.deleteTemplate(id);
        return Result.success();
    }

    @Operation(summary = "获取模板详情", description = "根据ID获取模板详细信息")
    @GetMapping("/{id}")
    public Result<TemplateDTO> getTemplate(@Parameter(description = "模板ID") @PathVariable String id) {
        TemplateDTO dto = templateService.getTemplate(id);
        interactionService.enrich(dto, interactionService.currentUserId());
        return Result.success(dto);
    }

    @Operation(summary = "获取我的模板", description = "获取当前用户创建的模板列表")
    @GetMapping("/my")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<TemplateDTO>> getMyTemplates(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        var page = templateService.getMyTemplates(pageNum, pageSize);
        page.getRecords().forEach(dto -> interactionService.enrich(dto, interactionService.currentUserId()));
        return Result.success(page);
    }

    @Operation(summary = "使用模板", description = "使用模板创建工作流实例，使用次数+1")
    @PostMapping("/{id}/use")
    public Result<Void> useTemplate(@Parameter(description = "模板ID") @PathVariable String id) {
        templateService.useTemplate(id);
        return Result.success();
    }

    @Operation(summary = "复制模板", description = "复制模板为自己所有")
    @PostMapping("/{id}/copy")
    public Result<TemplateDTO> copyTemplate(@Parameter(description = "模板ID") @PathVariable String id) {
        TemplateDTO dto = templateService.copyTemplate(id);
        interactionService.enrich(dto, interactionService.currentUserId());
        return Result.success(dto);
    }

    @Operation(summary = "点赞模板")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable String id) {
        interactionService.like(id, interactionService.currentUserId());
        return Result.success();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable String id) {
        interactionService.unlike(id, interactionService.currentUserId());
        return Result.success();
    }

    @Operation(summary = "收藏模板")
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable String id) {
        interactionService.favorite(id, interactionService.currentUserId());
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable String id) {
        interactionService.unfavorite(id, interactionService.currentUserId());
        return Result.success();
    }

    @Operation(summary = "获取我收藏的模板")
    @GetMapping("/favorites")
    public Result<java.util.List<TemplateDTO>> favorites() {
        String userId = interactionService.currentUserId();
        java.util.List<TemplateDTO> result = interactionService.getFavoriteTemplateIds(userId).stream()
                .map(id -> {
                    try {
                        TemplateDTO dto = templateService.getTemplate(id);
                        interactionService.enrich(dto, userId);
                        return dto;
                    } catch (Exception e) {
                        log.warn("Skip missing favorite template id={}: {}", id, e.getMessage());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        return Result.success(result);
    }
}

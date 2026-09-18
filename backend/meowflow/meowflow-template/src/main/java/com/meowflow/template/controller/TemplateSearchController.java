package com.meowflow.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.template.dto.TemplateDTO;
import com.meowflow.template.dto.TemplateSearchRequest;
import com.meowflow.template.entity.TemplateCategory;
import com.meowflow.template.entity.TemplateTag;
import com.meowflow.template.service.TemplateInteractionService;
import com.meowflow.template.service.TemplateSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "模板搜索", description = "模板搜索和推荐相关接口")
@RestController
@RequestMapping("/api/template/search")
@RequiredArgsConstructor
public class TemplateSearchController {

    private final TemplateSearchService searchService;
    private final TemplateInteractionService interactionService;

    @Operation(summary = "搜索模板", description = "关键词、分类、标签搜索模板")
    @PostMapping
    public Result<IPage<TemplateDTO>> search(@RequestBody TemplateSearchRequest request) {
        IPage<TemplateDTO> page = searchService.search(request);
        enrich(page.getRecords());
        return Result.success(page);
    }

    @Operation(summary = "获取推荐模板", description = "获取精选推荐模板")
    @GetMapping("/featured")
    public Result<List<TemplateDTO>> getFeaturedTemplates() {
        return Result.success(enrich(searchService.getFeaturedTemplates()));
    }

    @Operation(summary = "获取热门模板", description = "获取使用次数最多的模板")
    @GetMapping("/popular")
    public Result<List<TemplateDTO>> getPopularTemplates(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        return Result.success(enrich(searchService.getPopularTemplates(limit)));
    }

    @Operation(summary = "获取最新模板", description = "获取最近创建的模板")
    @GetMapping("/latest")
    public Result<List<TemplateDTO>> getLatestTemplates(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        return Result.success(enrich(searchService.getLatestTemplates(limit)));
    }

    @Operation(summary = "获取全部分类", description = "获取模板分类树")
    @GetMapping("/categories")
    public Result<List<TemplateCategory>> getAllCategories() {
        return Result.success(searchService.getAllCategories());
    }

    @Operation(summary = "获取全部标签", description = "获取所有模板标签")
    @GetMapping("/tags")
    public Result<List<TemplateTag>> getAllTags() {
        return Result.success(searchService.getAllTags());
    }

    private List<TemplateDTO> enrich(List<TemplateDTO> templates) {
        String userId = interactionService.currentUserId();
        templates.forEach(template -> interactionService.enrich(template, userId));
        return templates;
    }

    @Operation(summary = "获取热门标签", description = "获取使用次数最多的标签")
    @GetMapping("/tags/hot")
    public Result<List<TemplateTag>> getHotTags(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        return Result.success(searchService.getHotTags(limit));
    }
}

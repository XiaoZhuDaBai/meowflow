package com.meowflow.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.template.dto.RatingRequest;
import com.meowflow.template.dto.RatingStatistics;
import com.meowflow.template.dto.TemplateRatingDTO;
import com.meowflow.template.dto.TemplateRatingRequest;
import com.meowflow.template.dto.TemplateReviewRequest;
import com.meowflow.template.entity.TemplateRating;
import com.meowflow.template.entity.TemplateReview;
import com.meowflow.template.enums.ReviewSortType;
import com.meowflow.template.service.TemplateRatingService;
import com.meowflow.template.service.TemplateReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "模板审核", description = "模板审核相关接口")
@RestController
@RequestMapping("/api/template/review")
@RequiredArgsConstructor
public class TemplateReviewController {

    private final TemplateReviewService reviewService;
    private final TemplateRatingService ratingService;

    @Operation(summary = "提交模板审核", description = "提交模板进行审核")
    @PostMapping("/submit/{templateId}")
    public Result<TemplateReview> submitForReview(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        return Result.success(reviewService.submitForReview(templateId));
    }

    @Operation(summary = "审核通过", description = "审核通过模板")
    @PostMapping("/approve")
    public Result<TemplateReview> approveTemplate(@Valid @RequestBody TemplateReviewRequest request) {
        return Result.success(reviewService.approveTemplate(request.getTemplateId(), request.getComment()));
    }

    @Operation(summary = "审核拒绝", description = "拒绝通过模板")
    @PostMapping("/reject")
    public Result<TemplateReview> rejectTemplate(@Valid @RequestBody TemplateReviewRequest request) {
        return Result.success(reviewService.rejectTemplate(request.getTemplateId(), request.getComment()));
    }

    @Operation(summary = "获取待审核列表", description = "获取待审核的模板列表")
    @GetMapping("/pending")
    public Result<IPage<TemplateReview>> getPendingReviews(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reviewService.getPendingReviews(pageNum, pageSize));
    }

    @Operation(summary = "获取我的审核历史", description = "获取当前审核员的审核历史")
    @GetMapping("/history")
    public Result<IPage<TemplateReview>> getMyReviewHistory(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reviewService.getMyReviewHistory(pageNum, pageSize));
    }

    @Operation(summary = "评价模板", description = "对模板进行评分和评论")
    @PostMapping("/rating")
    public Result<TemplateRating> rateTemplate(
            @Parameter(description = "模板ID") @RequestParam Long templateId,
            @Valid @RequestBody RatingRequest request) {
        return Result.success(ratingService.submitRating(templateId, request));
    }

    @Operation(summary = "获取模板评价", description = "获取模板的所有评价")
    @GetMapping("/{templateId}/ratings")
    public Result<IPage<TemplateRatingDTO>> getTemplateRatings(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "排序类型") @RequestParam(defaultValue = "RECENT") ReviewSortType sortType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        return Result.success(ratingService.getReviews(templateId, sortType, page, size));
    }

    @Operation(summary = "获取模板评分统计", description = "获取模板的评分统计信息")
    @GetMapping("/{templateId}/rating/stats")
    public Result<RatingStatistics> getAverageRating(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        return Result.success(ratingService.getStatistics(templateId));
    }
}

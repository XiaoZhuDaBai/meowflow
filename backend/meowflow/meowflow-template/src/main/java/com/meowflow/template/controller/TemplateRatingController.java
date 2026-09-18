package com.meowflow.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.result.Result;
import com.meowflow.template.dto.RatingRequest;
import com.meowflow.template.dto.RatingStatistics;
import com.meowflow.template.dto.TemplateRatingDTO;
import com.meowflow.template.entity.TemplateRating;
import com.meowflow.template.enums.ReviewSortType;
import com.meowflow.template.service.TemplateRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "模板评分", description = "模板评分和评论相关接口")
@RestController
@RequestMapping("/api/template/{templateId}/rating")
@RequiredArgsConstructor
public class TemplateRatingController {

    private final TemplateRatingService ratingService;

    @Operation(summary = "提交评分和评论", description = "对模板进行评分和评论")
    @PostMapping
    public Result<TemplateRating> submitRating(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Valid @RequestBody RatingRequest request) {
        return Result.success(ratingService.submitRating(templateId, request));
    }

    @Operation(summary = "获取评分统计", description = "获取模板的评分统计信息")
    @GetMapping("/statistics")
    public Result<RatingStatistics> getStatistics(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        return Result.success(ratingService.getStatistics(templateId));
    }

    @Operation(summary = "获取评论列表", description = "分页获取模板的评论列表")
    @GetMapping("/reviews")
    public Result<IPage<TemplateRatingDTO>> getReviews(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "排序类型") @RequestParam(defaultValue = "RECENT") ReviewSortType sortType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        return Result.success(ratingService.getReviews(templateId, sortType, page, size));
    }

    @Operation(summary = "标记评论有帮助", description = "标记某条评论为有帮助")
    @PostMapping("/{ratingId}/helpful")
    public Result<Void> markHelpful(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "评分ID") @PathVariable Long ratingId) {
        Long userId = getCurrentUserId();
        ratingService.markHelpful(ratingId, userId);
        return Result.success();
    }

    @Operation(summary = "删除评论", description = "删除自己的评论或管理员删除任意评论")
    @DeleteMapping("/{ratingId}")
    public Result<Void> deleteReview(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "评分ID") @PathVariable Long ratingId) {
        Long userId = getCurrentUserId();
        ratingService.deleteReview(ratingId, userId);
        return Result.success();
    }

    private Long getCurrentUserId() {
        UserContext userContext = UserContextHolder.get();
        return userContext != null && userContext.getUserId() != null ? userContext.getUserId() : 0L;
    }
}


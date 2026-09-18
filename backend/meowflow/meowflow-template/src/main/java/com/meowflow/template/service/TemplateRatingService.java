package com.meowflow.template.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.template.dto.RatingRequest;
import com.meowflow.template.dto.RatingStatistics;
import com.meowflow.template.dto.TemplateRatingDTO;
import com.meowflow.template.entity.Template;
import com.meowflow.template.entity.TemplateRating;
import com.meowflow.template.entity.TemplateRatingHelpful;
import com.meowflow.template.enums.ReviewSortType;
import com.meowflow.template.cache.TemplateCacheSupport;
import com.meowflow.template.repository.TemplateRatingHelpfulRepository;
import com.meowflow.template.repository.TemplateRatingRepository;
import com.meowflow.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateRatingService {

    private final TemplateRatingRepository ratingRepository;
    private final TemplateRatingHelpfulRepository helpfulRepository;
    private final TemplateRepository templateRepository;
    private final TemplateCacheSupport cacheSupport;

    @Transactional(rollbackFor = Exception.class)
    public TemplateRating submitRating(Long templateId, RatingRequest request) {
        if (request.getScore() == null || request.getScore() < 1 || request.getScore() > 5) {
            throw new BizException(ResultCode.PARAM_ERROR, "评分必须在1-5之间");
        }

        Template template = templateRepository.findById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        Long userId = getCurrentUserId();

        TemplateRating existingRating = ratingRepository.findByTemplateIdAndUserId(templateId, userId);

        TemplateRating rating;
        if (existingRating != null) {
            existingRating.setScore(request.getScore());
            existingRating.setContent(request.getContent());
            existingRating.setTags(request.getTags() != null ? String.join(",", request.getTags()) : null);
            existingRating.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false);
            existingRating.setUpdateBy(userId);
            ratingRepository.updateById(existingRating);
            rating = existingRating;
            log.info("Updated rating for template {} by user {}", templateId, userId);
        } else {
            rating = new TemplateRating();
            rating.setTemplateId(templateId);
            rating.setUserId(userId);
            rating.setScore(request.getScore());
            rating.setContent(request.getContent());
            rating.setTags(request.getTags() != null ? String.join(",", request.getTags()) : null);
            rating.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false);
            rating.setHelpfulCount(0);
            rating.setStatus("active");
            rating.setCreateBy(userId);
            ratingRepository.insert(rating);
            log.info("Created rating for template {} by user {}", templateId, userId);
        }

        updateTemplateRatingStats(templateId);

        // 评分变化：详情 + 搜索列表（按 rating / score 排序时）失效
        cacheSupport.evictDetail(String.valueOf(templateId));
        cacheSupport.evictSearch();

        return rating;
    }

    public RatingStatistics getStatistics(Long templateId) {
        Template template = templateRepository.findById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        RatingStatistics statistics = new RatingStatistics();

        Double avgScore = ratingRepository.selectAverageScore(templateId);
        statistics.setAverageScore(avgScore != null ? Math.round(avgScore * 10) / 10.0 : 0.0);

        Long totalCount = ratingRepository.countByTemplateId(templateId);
        statistics.setTotalCount(totalCount);

        List<Map<String, Object>> distribution = ratingRepository.selectScoreDistribution(templateId);
        Map<Integer, Long> scoreDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            scoreDistribution.put(i, 0L);
        }
        for (Map<String, Object> item : distribution) {
            Integer score = ((Number) item.get("score")).intValue();
            Long count = ((Number) item.get("cnt")).longValue();
            scoreDistribution.put(score, count);
        }
        statistics.setScoreDistribution(scoreDistribution);

        List<Map<String, Object>> topTagsResult = ratingRepository.selectTopTags(templateId);
        List<String> topTags = topTagsResult.stream()
                .map(m -> (String) m.get("tag"))
                .collect(Collectors.toList());
        statistics.setTopTags(topTags);

        return statistics;
    }

    public IPage<TemplateRatingDTO> getReviews(Long templateId, ReviewSortType sortType, int page, int size) {
        Template template = templateRepository.findById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        Page<TemplateRating> pageParam = new Page<>(page, size);

        String orderBy = switch (sortType) {
            case RECENT -> "create_time DESC";
            case HELPFUL -> "helpful_count DESC, create_time DESC";
            case HIGH_SCORE -> "score DESC, create_time DESC";
            case LOW_SCORE -> "score ASC, create_time DESC";
        };

        IPage<TemplateRating> ratingPage = ratingRepository.selectPage(pageParam, templateId, orderBy);
        return ratingPage.convert(this::convertToDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markHelpful(Long ratingId, Long userId) {
        TemplateRating rating = ratingRepository.selectById(ratingId);
        if (rating == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "评分不存在");
        }
        if (helpfulRepository.existsByRatingIdAndUserId(ratingId, userId)) {
            throw new BizException(ResultCode.BIZ_ERROR, "您已经标记过这条评论有帮助");
        }

        TemplateRatingHelpful helpful = new TemplateRatingHelpful();
        helpful.setRatingId(ratingId);
        helpful.setUserId(userId);
        helpful.setCreateBy(userId);
        helpfulRepository.insert(helpful);

        rating.setHelpfulCount(rating.getHelpfulCount() == null ? 1 : rating.getHelpfulCount() + 1);
        rating.setUpdateBy(userId);
        ratingRepository.updateById(rating);

        log.info("User {} marked rating {} as helpful", userId, ratingId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long ratingId, Long userId) {
        TemplateRating rating = ratingRepository.selectById(ratingId);
        if (rating == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "评分不存在");
        }
        if (!rating.getUserId().equals(userId) && !"admin".equals(getUserRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权删除此评论");
        }
        rating.setStatus("deleted");
        rating.setUpdateBy(userId);
        ratingRepository.updateById(rating);
        Long affectedTemplateId = rating.getTemplateId();
        updateTemplateRatingStats(affectedTemplateId);
        log.info("Deleted rating {}", ratingId);

        cacheSupport.evictDetail(String.valueOf(affectedTemplateId));
        cacheSupport.evictSearch();
    }

    private void updateTemplateRatingStats(Long templateId) {
        Double avgRating = ratingRepository.selectAverageScore(templateId);
        Long count = ratingRepository.countByTemplateId(templateId);
        Template template = templateRepository.findById(templateId);
        if (template != null) {
            template.setScore(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
            template.setReviewCount(count != null ? count : 0L);
            templateRepository.updateById(template);
        }
    }

    private TemplateRatingDTO convertToDTO(TemplateRating rating) {
        TemplateRatingDTO dto = new TemplateRatingDTO();
        dto.setId(rating.getId());
        dto.setScore(rating.getScore());
        dto.setContent(rating.getContent());
        dto.setHelpfulCount(rating.getHelpfulCount() != null ? rating.getHelpfulCount() : 0);
        dto.setIsAnonymous(rating.getIsAnonymous() != null ? rating.getIsAnonymous() : false);
        dto.setCreateTime(rating.getCreateTime());

        if (Boolean.TRUE.equals(rating.getIsAnonymous())) {
            dto.setUserId(null);
            dto.setUserName("匿名用户");
        } else {
            dto.setUserId(String.valueOf(rating.getUserId()));
            dto.setUserName(getUserDisplayName(rating.getUserId()));
        }

        if (StrUtil.isNotBlank(rating.getTags())) {
            dto.setTags(Arrays.asList(rating.getTags().split(",")));
        } else {
            dto.setTags(new ArrayList<>());
        }

        return dto;
    }

    private Long getCurrentUserId() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null && ctx.getUserId() != null ? ctx.getUserId() : 0L;
    }

    private String getUserRole() {
        return Optional.ofNullable(UserContextHolder.get())
                .map(ctx -> ctx.getRoles().isEmpty() ? "user" : ctx.getRoles().get(0))
                .orElse("user");
    }

    private String getUserDisplayName(Long userId) {
        if (userId == null) return "匿名用户";
        String uid = String.valueOf(userId);
        return "用户" + uid.substring(0, Math.min(6, uid.length()));
    }
}




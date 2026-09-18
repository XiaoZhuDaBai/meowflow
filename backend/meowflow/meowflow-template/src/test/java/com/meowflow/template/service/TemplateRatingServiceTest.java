package com.meowflow.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.template.cache.TemplateCacheSupport;
import com.meowflow.template.dto.RatingRequest;
import com.meowflow.template.dto.RatingStatistics;
import com.meowflow.template.dto.TemplateRatingDTO;
import com.meowflow.template.entity.Template;
import com.meowflow.template.entity.TemplateRating;
import com.meowflow.template.entity.TemplateRatingHelpful;
import com.meowflow.template.enums.ReviewSortType;
import com.meowflow.template.repository.TemplateRatingHelpfulRepository;
import com.meowflow.template.repository.TemplateRatingRepository;
import com.meowflow.template.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TemplateRatingService Unit Tests")
class TemplateRatingServiceTest {

    @Mock
    private TemplateRatingRepository ratingRepository;

    @Mock
    private TemplateRatingHelpfulRepository helpfulRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateCacheSupport cacheSupport;

    @InjectMocks
    private TemplateRatingService ratingService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(UserContext.builder()
                .userId(1001L)
                .username("testuser")
                .roles(List.of("user"))
                .build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("submitRating - 棣栨璇勫垎鎴愬姛")
    void submitRating_newRating_createsRating() {
        Template template = createTemplate(1001L);
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(ratingRepository.findByTemplateIdAndUserId(1001L, 1001L)).thenReturn(null);
        when(ratingRepository.insert(any(TemplateRating.class))).thenReturn(1);
        when(ratingRepository.selectAverageScore(1001L)).thenReturn(4.5);
        when(ratingRepository.countByTemplateId(1001L)).thenReturn(1L);
        when(templateRepository.updateById(any(Template.class))).thenReturn(1);

        RatingRequest request = new RatingRequest();
        request.setScore(5);
        request.setContent("Great template!");
        request.setTags(Arrays.asList("good", "useful"));

        TemplateRating result = ratingService.submitRating(1001L, request);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(5);
        verify(ratingRepository).insert(any(TemplateRating.class));
    }

    @Test
    @DisplayName("submitRating - 鏇存柊宸叉湁璇勫垎")
    void submitRating_existingRating_updatesRating() {
        Template template = createTemplate(1001L);
        TemplateRating existing = createRating(6001L, 1001L, 1001L, 3);

        when(templateRepository.findById(1001L)).thenReturn(template);
        when(ratingRepository.findByTemplateIdAndUserId(1001L, 1001L)).thenReturn(existing);
        when(ratingRepository.updateById(any(TemplateRating.class))).thenReturn(1);
        when(ratingRepository.selectAverageScore(1001L)).thenReturn(4.0);
        when(ratingRepository.countByTemplateId(1001L)).thenReturn(1L);
        when(templateRepository.updateById(any(Template.class))).thenReturn(1);

        RatingRequest request = new RatingRequest();
        request.setScore(4);

        TemplateRating result = ratingService.submitRating(1001L, request);

        assertThat(result.getScore()).isEqualTo(4);
        verify(ratingRepository, never()).insert(any());
        verify(ratingRepository).updateById(any(TemplateRating.class));
    }

    @Test
    @DisplayName("test")
    void submitRating_invalidScore_throwsBizException() {
        RatingRequest request = new RatingRequest();
        request.setScore(6);

        assertThatThrownBy(() -> ratingService.submitRating(1001L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("评分必须在1-5之间");
    }

    @Test
    @DisplayName("test")
    void submitRating_zeroScore_throwsBizException() {
        RatingRequest request = new RatingRequest();
        request.setScore(0);

        assertThatThrownBy(() -> ratingService.submitRating(1001L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("评分必须在1-5之间");
    }

    @Test
    @DisplayName("submitRating - 妯℃澘涓嶅瓨鍦ㄦ姏寮傚父")
    void submitRating_templateNotFound_throwsBizException() {
        when(templateRepository.findById(9999L)).thenReturn(null);

        RatingRequest request = new RatingRequest();
        request.setScore(5);

        assertThatThrownBy(() -> ratingService.submitRating(9999L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("getStatistics - 杩斿洖璇勫垎缁熻")
    void getStatistics_existsTemplate_returnsStatistics() {
        Template template = createTemplate(1001L);
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(ratingRepository.selectAverageScore(1001L)).thenReturn(4.5);
        when(ratingRepository.countByTemplateId(1001L)).thenReturn(10L);

        List<Map<String, Object>> distribution = new ArrayList<>();
        Map<String, Object> dist1 = new HashMap<>();
        dist1.put("score", 5);
        dist1.put("cnt", 5L);
        distribution.add(dist1);
        Map<String, Object> dist2 = new HashMap<>();
        dist2.put("score", 4);
        dist2.put("cnt", 3L);
        distribution.add(dist2);
        when(ratingRepository.selectScoreDistribution(1001L)).thenReturn(distribution);

        List<Map<String, Object>> topTags = new ArrayList<>();
        Map<String, Object> tag1 = new HashMap<>();
        tag1.put("tag", "good");
        topTags.add(tag1);
        when(ratingRepository.selectTopTags(1001L)).thenReturn(topTags);

        RatingStatistics result = ratingService.getStatistics(1001L);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(4.5);
        assertThat(result.getTotalCount()).isEqualTo(10L);
        assertThat(result.getScoreDistribution().get(5)).isEqualTo(5L);
        assertThat(result.getScoreDistribution().get(4)).isEqualTo(3L);
        assertThat(result.getTopTags()).contains("good");
    }

    @Test
    @DisplayName("test")
    void getStatistics_noRatings_returnsDefaults() {
        Template template = createTemplate(1001L);
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(ratingRepository.selectAverageScore(1001L)).thenReturn(null);
        when(ratingRepository.countByTemplateId(1001L)).thenReturn(0L);
        when(ratingRepository.selectScoreDistribution(1001L)).thenReturn(List.of());
        when(ratingRepository.selectTopTags(1001L)).thenReturn(List.of());

        RatingStatistics result = ratingService.getStatistics(1001L);

        assertThat(result.getAverageScore()).isEqualTo(0.0);
        assertThat(result.getTotalCount()).isEqualTo(0L);
        assertThat(result.getScoreDistribution().get(5)).isEqualTo(0L);
    }

    @Test
    @DisplayName("test")
    void getReviews_defaultSort_returnsPage() {
        Template template = createTemplate(1001L);
        TemplateRating rating = createRating(5001L, 1001L, 1001L, 5);
        when(templateRepository.findById(1001L)).thenReturn(template);

        Page<TemplateRating> page = new Page<>(1, 10);
        page.setRecords(List.of(rating));
        page.setTotal(1L);
        when(ratingRepository.selectPage(any(Page.class), eq(1001L), anyString())).thenReturn(page);

        IPage<TemplateRatingDTO> result = ratingService.getReviews(1001L, ReviewSortType.RECENT, 1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    @DisplayName("test")
    void getReviews_helpfulSort_usesCorrectOrder() {
        Template template = createTemplate(1001L);
        when(templateRepository.findById(1001L)).thenReturn(template);

        Page<TemplateRating> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0L);
        when(ratingRepository.selectPage(any(Page.class), eq(1001L), eq("helpful_count DESC, create_time DESC"))).thenReturn(page);

        ratingService.getReviews(1001L, ReviewSortType.HELPFUL, 1, 10);

        verify(ratingRepository).selectPage(any(Page.class), eq(1001L), eq("helpful_count DESC, create_time DESC"));
    }

    @Test
    @DisplayName("markHelpful - 鎴愬姛鏍囪鏈夌敤")
    void markHelpful_validRating_incrementsCount() {
        TemplateRating rating = createRating(5001L, 1001L, 999L, 5);
        when(ratingRepository.selectById(5001L)).thenReturn(rating);
        when(helpfulRepository.existsByRatingIdAndUserId(5001L, 1001L)).thenReturn(false);
        when(helpfulRepository.insert(any(TemplateRatingHelpful.class))).thenReturn(1);
        when(ratingRepository.updateById(any(TemplateRating.class))).thenReturn(1);

        ratingService.markHelpful(5001L, 1001L);

        verify(helpfulRepository).insert(any(TemplateRatingHelpful.class));
        verify(ratingRepository).updateById(any(TemplateRating.class));
    }

    @Test
    @DisplayName("test")
    void markHelpful_alreadyMarked_throwsBizException() {
        TemplateRating rating = createRating(5001L, 1001L, 999L, 5);
        when(ratingRepository.selectById(5001L)).thenReturn(rating);
        when(helpfulRepository.existsByRatingIdAndUserId(5001L, 1001L)).thenReturn(true);

        assertThatThrownBy(() -> ratingService.markHelpful(5001L, 1001L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("您已经标记过这条评论有帮助");
    }

    @Test
    @DisplayName("markHelpful - 璇勫垎涓嶅瓨鍦ㄦ姏寮傚父")
    void markHelpful_ratingNotFound_throwsBizException() {
        when(ratingRepository.selectById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> ratingService.markHelpful(9999L, 1001L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("test")
    void deleteReview_owner_deletesRating() {
        TemplateRating rating = createRating(5001L, 1001L, 1001L, 5);
        when(ratingRepository.selectById(5001L)).thenReturn(rating);
        when(ratingRepository.updateById(any(TemplateRating.class))).thenReturn(1);
        when(ratingRepository.selectAverageScore(1001L)).thenReturn(0.0);
        when(ratingRepository.countByTemplateId(1001L)).thenReturn(0L);
        when(templateRepository.updateById(any(Template.class))).thenReturn(1);

        ratingService.deleteReview(5001L, 1001L);

        assertThat(rating.getStatus()).isEqualTo("deleted");
        verify(ratingRepository).updateById(rating);
    }

    @Test
    @DisplayName("deleteReview - 闈炴墍鏈夎€呮姏寮傚父")
    void deleteReview_notOwner_throwsBizException() {
        TemplateRating rating = createRating(5001L, 1001L, 999L, 5);
        when(ratingRepository.selectById(5001L)).thenReturn(rating);

        assertThatThrownBy(() -> ratingService.deleteReview(5001L, 1001L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权删除此评论");
    }

    @Test
    @DisplayName("deleteReview - 璇勫垎涓嶅瓨鍦ㄦ姏寮傚父")
    void deleteReview_notFound_throwsBizException() {
        when(ratingRepository.selectById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> ratingService.deleteReview(9999L, 1001L))
                .isInstanceOf(BizException.class);
    }

    private Template createTemplate(Long id) {
        Template template = new Template();
        template.setId(id);
        template.setName("Test Template");
        template.setStatus("active");
        template.setReviewStatus("approved");
        return template;
    }

    private TemplateRating createRating(Long id, Long templateId, Long userId, int score) {
        TemplateRating rating = new TemplateRating();
        rating.setId(id);
        rating.setTemplateId(templateId);
        rating.setUserId(userId);
        rating.setScore(score);
        rating.setContent("Test review");
        rating.setStatus("active");
        rating.setHelpfulCount(0);
        rating.setIsAnonymous(false);
        rating.setCreateBy(userId);
        rating.setCreateTime(LocalDateTime.now());
        return rating;
    }
}




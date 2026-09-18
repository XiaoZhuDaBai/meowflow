package com.meowflow.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.util.IdGeneratorFactory;
import com.meowflow.template.entity.Template;
import com.meowflow.template.entity.TemplateReview;
import com.meowflow.template.repository.TemplateRepository;
import com.meowflow.template.repository.TemplateReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TemplateReviewService Unit Tests")
class TemplateReviewServiceTest {

    @Mock
    private TemplateReviewRepository reviewRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private IdGeneratorFactory idGenerator;

    @InjectMocks
    private TemplateReviewService reviewService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(UserContext.builder().userId(1001L).username("reviewer").build());
        when(idGenerator.nextIdStr()).thenReturn("review-001");
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("submitForReview - 鎴愬姛鎻愪氦瀹℃牳")
    void submitForReview_pendingTemplate_createsReview() {
        Template template = createTemplate(1001L, "pending");
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(reviewRepository.insert(any(TemplateReview.class))).thenReturn(1);

        TemplateReview result = reviewService.submitForReview(1001L);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo("submit");
        assertThat(result.getResult()).isEqualTo("pending");
        verify(reviewRepository).insert(any(TemplateReview.class));
    }

    @Test
    @DisplayName("submitForReview - 妯℃澘涓嶅瓨鍦ㄦ姏寮傚父")
    void submitForReview_templateNotFound_throwsBizException() {
        when(templateRepository.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.submitForReview(9999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("submitForReview - 闈?pending 鐘舵€佹姏寮傚父")
    void submitForReview_notPending_throwsBizException() {
        Template template = createTemplate(1001L, "approved");
        when(templateRepository.findById(1001L)).thenReturn(template);

        assertThatThrownBy(() -> reviewService.submitForReview(1001L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板当前状态不允许提交审核");
    }

    @Test
    @DisplayName("approveTemplate - 鎴愬姛閫氳繃瀹℃牳")
    void approveTemplate_pendingTemplate_approvesAndUpdates() {
        Template template = createTemplate(1001L, "pending");
        template.setCreateBy(1001L);
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(templateRepository.updateById(any(Template.class))).thenReturn(1);
        when(reviewRepository.insert(any(TemplateReview.class))).thenReturn(1);

        TemplateReview result = reviewService.approveTemplate(1001L, "Looks good!");

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo("approve");
        assertThat(result.getResult()).isEqualTo("approved");

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateRepository).updateById(captor.capture());
        assertThat(captor.getValue().getReviewStatus()).isEqualTo("approved");
        assertThat(captor.getValue().getReviewComment()).isEqualTo("Looks good!");
    }

    @Test
    @DisplayName("approveTemplate - 妯℃澘涓嶅瓨鍦ㄦ姏寮傚父")
    void approveTemplate_notFound_throwsBizException() {
        when(templateRepository.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.approveTemplate(9999L, "comment"))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("approveTemplate - 闈?pending 鐘舵€佹姏寮傚父")
    void approveTemplate_alreadyApproved_throwsBizException() {
        Template template = createTemplate(1001L, "approved");
        when(templateRepository.findById(1001L)).thenReturn(template);

        assertThatThrownBy(() -> reviewService.approveTemplate(1001L, "comment"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板当前状态不允许审核");
    }

    @Test
    @DisplayName("rejectTemplate - 鎴愬姛鎷掔粷瀹℃牳")
    void rejectTemplate_pendingTemplate_rejects() {
        Template template = createTemplate(1001L, "pending");
        template.setCreateBy(1001L);
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(templateRepository.updateById(any(Template.class))).thenReturn(1);
        when(reviewRepository.insert(any(TemplateReview.class))).thenReturn(1);

        TemplateReview result = reviewService.rejectTemplate(1001L, "comment");

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo("reject");
        assertThat(result.getResult()).isEqualTo("rejected");
        assertThat(result.getComment()).isEqualTo("comment");

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateRepository).updateById(captor.capture());
        assertThat(captor.getValue().getReviewStatus()).isEqualTo("rejected");
    }

    @Test
    @DisplayName("rejectTemplate - 妯℃澘涓嶅瓨鍦ㄦ姏寮傚父")
    void rejectTemplate_notFound_throwsBizException() {
        when(templateRepository.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.rejectTemplate(9999L, "comment"))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("test")
    void getPendingReviews_returnsPagedResults() {
        TemplateReview review = new TemplateReview();
        review.setId(1L);
        review.setAction("submit");

        Page<TemplateReview> page = new Page<>(1, 10);
        page.setRecords(List.of(review));
        page.setTotal(1L);
        when(reviewRepository.findByResult(any(Page.class), eq("pending"))).thenReturn(page);

        IPage<TemplateReview> result = reviewService.getPendingReviews(1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    @DisplayName("getMyReviewHistory - 杩斿洖瀹℃牳鍘嗗彶")
    void getMyReviewHistory_returnsPagedResults() {
        TemplateReview review = new TemplateReview();
        review.setId(1L);
        review.setReviewerId(1001L);

        Page<TemplateReview> page = new Page<>(1, 10);
        page.setRecords(List.of(review));
        page.setTotal(1L);
        when(reviewRepository.findByReviewerId(any(Page.class), eq(1001L))).thenReturn(page);

        IPage<TemplateReview> result = reviewService.getMyReviewHistory(1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    private Template createTemplate(Long id, String reviewStatus) {
        Template template = new Template();
        template.setId(id);
        template.setName("Test Template");
        template.setStatus("active");
        template.setReviewStatus(reviewStatus);
        template.setCreateBy(1001L);
        template.setCreateTime(LocalDateTime.now());
        return template;
    }
}







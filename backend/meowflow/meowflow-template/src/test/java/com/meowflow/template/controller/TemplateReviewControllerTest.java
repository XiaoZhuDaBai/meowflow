package com.meowflow.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.template.dto.RatingRequest;
import com.meowflow.template.dto.TemplateReviewRequest;
import com.meowflow.template.entity.TemplateRating;
import com.meowflow.template.entity.TemplateReview;
import com.meowflow.template.enums.ReviewSortType;
import com.meowflow.template.service.TemplateRatingService;
import com.meowflow.template.service.TemplateReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TemplateReviewController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("TemplateReviewController HTTP 层")
class TemplateReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TemplateReviewService reviewService;
    @MockBean
    private TemplateRatingService ratingService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /review/submit/{templateId}")
    void submit_invokesService() throws Exception {
        TemplateReview rv = new TemplateReview();
        rv.setId(1L);
        when(reviewService.submitForReview(anyLong())).thenReturn(rv);

        mockMvc.perform(post("/api/template/review/submit/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("POST /review/approve")
    void approve_invokesService() throws Exception {
        TemplateReviewRequest req = new TemplateReviewRequest();
        req.setTemplateId(1L);
        req.setComment("looks good");
        when(reviewService.approveTemplate(anyLong(), any())).thenReturn(new TemplateReview());

        mockMvc.perform(post("/api/template/review/approve")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /review/reject")
    void reject_invokesService() throws Exception {
        TemplateReviewRequest req = new TemplateReviewRequest();
        req.setTemplateId(1L);
        req.setComment("violates policy");
        when(reviewService.rejectTemplate(anyLong(), any())).thenReturn(new TemplateReview());

        mockMvc.perform(post("/api/template/review/reject")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /review/pending")
    void pending_invokesService() throws Exception {
        when(reviewService.getPendingReviews(anyInt(), anyInt()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/template/review/pending?pageNum=1&pageSize=10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /review/history")
    void history_invokesService() throws Exception {
        when(reviewService.getMyReviewHistory(anyInt(), anyInt()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/template/review/history?pageNum=1&pageSize=10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /review/rating")
    void rate_invokesService() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setScore(5);
        when(ratingService.submitRating(anyLong(), any())).thenReturn(new TemplateRating());

        mockMvc.perform(post("/api/template/review/rating?templateId=1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /review/{templateId}/ratings")
    void ratings_invokesService() throws Exception {
        when(ratingService.getReviews(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/template/review/1/ratings?sortType=RECENT&page=1&size=10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /review/{templateId}/rating/stats")
    void stats_invokesService() throws Exception {
        when(ratingService.getStatistics(anyLong())).thenReturn(new com.meowflow.template.dto.RatingStatistics());

        mockMvc.perform(get("/api/template/review/1/rating/stats"))
                .andExpect(status().isOk());
    }
}


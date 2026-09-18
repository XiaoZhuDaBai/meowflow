package com.meowflow.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.template.dto.RatingRequest;
import com.meowflow.template.dto.RatingStatistics;
import com.meowflow.template.entity.TemplateRating;
import com.meowflow.template.enums.ReviewSortType;
import com.meowflow.template.service.TemplateRatingService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TemplateRatingController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("TemplateRatingController HTTP 层")
class TemplateRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TemplateRatingService ratingService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/template/{templateId}/rating")
    void submitRating_invokesService() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setScore(5);
        req.setContent("Great!");
        TemplateRating rating = new TemplateRating();
        rating.setId(1L);
        when(ratingService.submitRating(anyLong(), any())).thenReturn(rating);

        mockMvc.perform(post("/api/template/1/rating")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/template/{templateId}/rating/statistics")
    void statistics_invokesService() throws Exception {
        RatingStatistics stats = new RatingStatistics();
        when(ratingService.getStatistics(anyLong())).thenReturn(stats);

        mockMvc.perform(get("/api/template/1/rating/statistics"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/template/{templateId}/rating/reviews")
    void reviews_invokesService() throws Exception {
        when(ratingService.getReviews(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/template/1/rating/reviews?sortType=RECENT&page=1&size=10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/template/{templateId}/rating/{ratingId}/helpful")
    void helpful_invokesService() throws Exception {
        mockMvc.perform(post("/api/template/1/rating/10/helpful"))
                .andExpect(status().isOk());
        verify(ratingService).markHelpful(anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("DELETE /api/template/{templateId}/rating/{ratingId}")
    void deleteReview_invokesService() throws Exception {
        mockMvc.perform(delete("/api/template/1/rating/10"))
                .andExpect(status().isOk());
        verify(ratingService).deleteReview(anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }
}


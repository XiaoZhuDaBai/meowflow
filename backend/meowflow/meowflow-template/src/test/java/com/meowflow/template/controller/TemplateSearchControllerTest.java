package com.meowflow.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.template.dto.TemplateSearchRequest;
import com.meowflow.template.entity.TemplateCategory;
import com.meowflow.template.entity.TemplateTag;
import com.meowflow.template.service.TemplateInteractionService;
import com.meowflow.template.service.TemplateSearchService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TemplateSearchController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("TemplateSearchController HTTP 层")
class TemplateSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TemplateSearchService searchService;
    @MockBean
    private TemplateInteractionService interactionService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/template/search")
    void search_invokesService() throws Exception {
        when(searchService.search(any()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        TemplateSearchRequest req = new TemplateSearchRequest();
        req.setKeyword("RAG");
        mockMvc.perform(post("/api/template/search")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/template/search/featured")
    void featured_invokesService() throws Exception {
        when(searchService.getFeaturedTemplates()).thenReturn(List.of(new com.meowflow.template.dto.TemplateDTO()));

        mockMvc.perform(get("/api/template/search/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/template/search/popular")
    void popular_invokesService() throws Exception {
        when(searchService.getPopularTemplates(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/template/search/popular?limit=5"))
                .andExpect(status().isOk());
        verify(searchService).getPopularTemplates(5);
    }

    @Test
    @DisplayName("GET /api/template/search/latest")
    void latest_invokesService() throws Exception {
        when(searchService.getLatestTemplates(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/template/search/latest?limit=5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/template/search/categories")
    void categories_invokesService() throws Exception {
        when(searchService.getAllCategories()).thenReturn(List.of(new TemplateCategory()));

        mockMvc.perform(get("/api/template/search/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/template/search/tags")
    void tags_invokesService() throws Exception {
        when(searchService.getAllTags()).thenReturn(List.of(new TemplateTag()));

        mockMvc.perform(get("/api/template/search/tags"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/template/search/tags/hot")
    void hotTags_invokesService() throws Exception {
        when(searchService.getHotTags(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/template/search/tags/hot?limit=10"))
                .andExpect(status().isOk());
        verify(searchService).getHotTags(10);
    }
}

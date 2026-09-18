package com.meowflow.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.dto.CategoryCreateRequest;
import com.meowflow.workflow.service.WorkflowCategoryService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkflowCategoryController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("WorkflowCategoryController HTTP 层")
class WorkflowCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private WorkflowCategoryService categoryService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/workflow/category")
    void create_invokesService() throws Exception {
        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName("营销");
        when(categoryService.create(any())).thenReturn(new WorkflowCategoryService.CategoryResponse());

        mockMvc.perform(post("/api/workflow/category")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/workflow/category/{id}")
    void update_invokesService() throws Exception {
        when(categoryService.update(any(), any())).thenReturn(new WorkflowCategoryService.CategoryResponse());

        mockMvc.perform(put("/api/workflow/category/1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new CategoryCreateRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/workflow/category/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/workflow/category/1"))
                .andExpect(status().isOk());
        verify(categoryService).delete(1L);
    }

    @Test
    @DisplayName("GET /api/workflow/category/{id}")
    void getById_invokesService() throws Exception {
        when(categoryService.getById(1L)).thenReturn(new WorkflowCategoryService.CategoryResponse());

        mockMvc.perform(get("/api/workflow/category/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/workflow/category/list")
    void list_invokesService() throws Exception {
        when(categoryService.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/workflow/category/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/workflow/category/tree")
    void tree_invokesService() throws Exception {
        when(categoryService.tree()).thenReturn(List.of());

        mockMvc.perform(get("/api/workflow/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}

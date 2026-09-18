package com.meowflow.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.template.dto.CategoryCreateRequest;
import com.meowflow.template.dto.TagCreateRequest;
import com.meowflow.template.dto.TemplateCategoryDTO;
import com.meowflow.template.dto.TemplateTagDTO;
import com.meowflow.template.service.TemplateCategoryService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TemplateCategoryController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("TemplateCategoryController HTTP 层")
class TemplateCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TemplateCategoryService categoryService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /category")
    void createCategory_invokesService() throws Exception {
        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName("营销");
        when(categoryService.createCategory(any())).thenReturn(new TemplateCategoryDTO());

        mockMvc.perform(post("/api/template/category")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /category/{id}")
    void updateCategory_invokesService() throws Exception {
        when(categoryService.updateCategory(any(), any())).thenReturn(new TemplateCategoryDTO());

        mockMvc.perform(put("/api/template/category/1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new CategoryCreateRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /category/{id}")
    void deleteCategory_invokesService() throws Exception {
        mockMvc.perform(delete("/api/template/category/1"))
                .andExpect(status().isOk());
        verify(categoryService).deleteCategory(1L);
    }

    @Test
    @DisplayName("GET /category/{id}")
    void getCategory_invokesService() throws Exception {
        when(categoryService.getCategory(1L)).thenReturn(new TemplateCategoryDTO());

        mockMvc.perform(get("/api/template/category/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /category/tag")
    void createTag_invokesService() throws Exception {
        TagCreateRequest req = new TagCreateRequest();
        req.setName("AI");
        when(categoryService.createTag(any())).thenReturn(new TemplateTagDTO());

        mockMvc.perform(post("/api/template/category/tag")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /category/tag/{id}")
    void updateTag_invokesService() throws Exception {
        when(categoryService.updateTag(any(), any())).thenReturn(new TemplateTagDTO());

        mockMvc.perform(put("/api/template/category/tag/1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new TagCreateRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /category/tag/{id}")
    void deleteTag_invokesService() throws Exception {
        mockMvc.perform(delete("/api/template/category/tag/1"))
                .andExpect(status().isOk());
        verify(categoryService).deleteTag(1L);
    }
}





package com.meowflow.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.template.dto.TemplateCreateRequest;
import com.meowflow.template.dto.TemplateDTO;
import com.meowflow.template.dto.TemplateUpdateRequest;
import com.meowflow.template.service.TemplateInteractionService;
import com.meowflow.template.service.TemplateService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TemplateController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("TemplateController HTTP 层")
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TemplateService templateService;
    @MockBean
    private TemplateInteractionService interactionService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/template — 创建")
    void create_invokesService() throws Exception {
        TemplateCreateRequest req = new TemplateCreateRequest();
        req.setName("客户支持 RAG");
        when(templateService.createTemplate(any())).thenReturn(new TemplateDTO());

        mockMvc.perform(post("/api/template")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/template/{id}")
    void update_invokesService() throws Exception {
        when(templateService.updateTemplate(any(), any())).thenReturn(new TemplateDTO());

        mockMvc.perform(put("/api/template/t-1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new TemplateUpdateRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/template/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/template/1"))
                .andExpect(status().isOk());
        verify(templateService).deleteTemplate(1L);
    }

    @Test
    @DisplayName("GET /api/template/{id}")
    void getTemplate_invokesService() throws Exception {
        when(templateService.getTemplate("t-1")).thenReturn(new TemplateDTO());

        mockMvc.perform(get("/api/template/t-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/template/my — 当前用户的模板")
    void myTemplates_invokesService() throws Exception {
        when(templateService.getMyTemplates(anyInt(), anyInt()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/template/my?pageNum=1&pageSize=10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/template/{id}/use")
    void useTemplate_invokesService() throws Exception {
        mockMvc.perform(post("/api/template/t-1/use"))
                .andExpect(status().isOk());
        verify(templateService).useTemplate("t-1");
    }

    @Test
    @DisplayName("POST /api/template/{id}/copy")
    void copyTemplate_invokesService() throws Exception {
        when(templateService.copyTemplate("t-1")).thenReturn(new TemplateDTO());

        mockMvc.perform(post("/api/template/t-1/copy"))
                .andExpect(status().isOk());
    }
}

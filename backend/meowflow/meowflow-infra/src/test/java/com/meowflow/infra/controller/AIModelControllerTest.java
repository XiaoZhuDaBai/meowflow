package com.meowflow.infra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.infra.dto.AIModelDTO;
import com.meowflow.infra.entity.AIModelEntity;
import com.meowflow.infra.service.AIModelConnectionTester;
import com.meowflow.infra.service.AIModelService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AIModelController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AIModelController HTTP 层")
class AIModelControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private AIModelService service;
    @MockBean
    private AIModelConnectionTester connectionTester;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("GET /model/enabled")
    void listEnabled_invokesService() throws Exception {
        when(service.listEnabled()).thenReturn(List.of(new AIModelEntity()));

        mockMvc.perform(get("/api/infra/model/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /model")
    void listAll_invokesService() throws Exception {
        when(service.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/infra/model"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /model/default")
    void default_invokesService() throws Exception {
        when(service.getDefault()).thenReturn(new AIModelEntity());

        mockMvc.perform(get("/api/infra/model/default"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /model/page")
    void page_invokesService() throws Exception {
        when(service.page(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20));

        mockMvc.perform(get("/api/infra/model/page?current=1&size=20&keyword=gpt"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /model/{id}")
    void getOne_invokesService() throws Exception {
        when(service.getById(1L)).thenReturn(new AIModelEntity());

        mockMvc.perform(get("/api/infra/model/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /model")
    void create_invokesService() throws Exception {
        AIModelDTO dto = new AIModelDTO();
        dto.setName("gpt-4");
        when(service.create(any())).thenReturn(new AIModelEntity());

        mockMvc.perform(post("/api/infra/model")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /model/{id}")
    void update_invokesService() throws Exception {
        when(service.update(any(), any())).thenReturn(new AIModelEntity());

        mockMvc.perform(put("/api/infra/model/1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new AIModelDTO())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /model/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/infra/model/1"))
                .andExpect(status().isOk());
        verify(service).delete(1L);
    }

    @Test
    @DisplayName("POST /model/{id}/test")
    void test_invokesService() throws Exception {
        when(connectionTester.test(1L)).thenReturn(java.util.Map.of("ok", true));

        mockMvc.perform(post("/api/infra/model/1/test"))
                .andExpect(status().isOk());
    }
}

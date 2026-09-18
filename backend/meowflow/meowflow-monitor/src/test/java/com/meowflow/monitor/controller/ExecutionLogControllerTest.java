package com.meowflow.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.monitor.dto.ExecutionLogCreateRequest;
import com.meowflow.monitor.entity.ExecutionLog;
import com.meowflow.monitor.service.ExecutionLogService;
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

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ExecutionLogController HTTP 层。
 */
@WebMvcTest(controllers = ExecutionLogController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ExecutionLogController HTTP 层")
class ExecutionLogControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ExecutionLogService logService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /log/start")
    void start_invokesService() throws Exception {
        ExecutionLogCreateRequest req = new ExecutionLogCreateRequest();
        req.setExecutionId("100");
        req.setNodeId("node-1");
        req.setNodeType("LLM");
        ExecutionLog log = new ExecutionLog();
        log.setId(1L);
        log.setNodeId("node-1");
        log.setLevel("info");
        log.setCreatedAt(LocalDateTime.now());
        when(logService.logStart(req)).thenReturn(log);

        mockMvc.perform(post("/api/monitor/log/start")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("POST /log/{logId}/complete")
    void complete_invokesService() throws Exception {
        ExecutionLog log = new ExecutionLog();
        log.setId(1L);
        log.setLevel("info");
        log.setMessage("Execution completed");
        when(logService.logComplete(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(log);

        mockMvc.perform(post("/api/monitor/log/1/complete")
                        .contentType("application/json")
                        .content("\"out\""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /log/{logId}/error")
    void error_invokesService() throws Exception {
        ExecutionLog log = new ExecutionLog();
        log.setId(1L);
        log.setLevel("error");
        when(logService.logError(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(log);

        mockMvc.perform(post("/api/monitor/log/1/error")
                        .contentType("application/json")
                        .content("\"boom\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("error"));
    }

    @Test
    @DisplayName("POST /log/query")
    void query_invokesService() throws Exception {
        when(logService.queryLogs(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(post("/api/monitor/log/query")
                        .contentType("application/json")
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /log/execution/{executionId}")
    void byExecution_invokesService() throws Exception {
        when(logService.getExecutionLogs("100")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/monitor/log/execution/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /log/errors?pageNum=&pageSize=")
    void errors_invokesService() throws Exception {
        when(logService.getErrorLogs(1, 20))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20));

        mockMvc.perform(get("/api/monitor/log/errors?pageNum=1&pageSize=20"))
                .andExpect(status().isOk());
    }
}


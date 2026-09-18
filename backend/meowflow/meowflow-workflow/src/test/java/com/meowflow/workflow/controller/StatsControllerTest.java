package com.meowflow.workflow.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.repository.WorkflowRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatsController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("StatsController HTTP 层")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecutionRepository executionRepository;
    @MockBean
    private WorkflowRepository workflowRepository;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("GET /api/stats/overview")
    void overview_invokesRepo() throws Exception {
        when(executionRepository.countTodayTotal(any())).thenReturn(0L);
        when(executionRepository.countTodaySuccess(any())).thenReturn(0L);
        when(executionRepository.avgCostMsToday(any())).thenReturn(null);
        when(executionRepository.totalCostAmountToday(any())).thenReturn(null);
        when(executionRepository.dailyStats(any(), any())).thenReturn(List.of());
        when(executionRepository.topWorkflows(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.today").exists())
                .andExpect(jsonPath("$.data.trend").isArray())
                .andExpect(jsonPath("$.data.topWorkflows").isArray());
    }

    @Test
    @DisplayName("GET /api/stats/trend")
    void trend_invokesRepo() throws Exception {
        when(executionRepository.dailyStats(any(), any())).thenReturn(List.of(Map.of(
                "exec_date", java.sql.Date.valueOf(LocalDate.now()),
                "total", 5L,
                "success_cnt", 4L)));

        mockMvc.perform(get("/api/stats/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/stats/cost")
    void cost_invokesRepo() throws Exception {
        when(executionRepository.totalCostAmount()).thenReturn(10.0);
        when(executionRepository.costByWorkflow()).thenReturn(List.of());
        when(executionRepository.dailyStats(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/stats/cost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(10.0))
                .andExpect(jsonPath("$.data.byWorkflow").isArray());
    }
}

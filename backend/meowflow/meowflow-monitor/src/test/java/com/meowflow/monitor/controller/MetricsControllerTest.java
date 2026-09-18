package com.meowflow.monitor.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.monitor.service.MetricsService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MetricsController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MetricsController HTTP 层")
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /metrics/query")
    void query_invokesService() throws Exception {
        when(metricsService.queryMetrics(any()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(post("/api/monitor/metrics/query")
                        .contentType("application/json")
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /metrics/latest")
    void latest_invokesService() throws Exception {
        when(metricsService.getLatestMetrics(anyInt())).thenReturn(List.of(new com.meowflow.monitor.dto.MetricDTO()));

        mockMvc.perform(get("/api/monitor/metrics/latest?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /metrics/counter — 不带 tags")
    void counter_invokesService() throws Exception {
        mockMvc.perform(post("/api/monitor/metrics/counter").param("name", "order.created"))
                .andExpect(status().isOk());
        verify(metricsService).recordCounter("order.created");
    }

    @Test
    @DisplayName("POST /metrics/timer — 带 tags")
    void timer_invokesService() throws Exception {
        mockMvc.perform(post("/api/monitor/metrics/timer?name=api.latency&durationMs=42")
                        .contentType("application/json")
                        .content("{\"endpoint\":\"/api/users\"}"))
                .andExpect(status().isOk());
        verify(metricsService).recordTimer("api.latency", 42L, new String[]{"endpoint"});
    }

    @Test
    @DisplayName("POST /metrics/gauge")
    void gauge_invokesService() throws Exception {
        mockMvc.perform(post("/api/monitor/metrics/gauge?name=jvm.heap&value=1024.0"))
                .andExpect(status().isOk());
        verify(metricsService).recordGauge("jvm.heap", 1024.0);
    }

    @Test
    @DisplayName("POST /metrics/workflow/execution")
    void workflowExecution_invokesService() throws Exception {
        mockMvc.perform(post("/api/monitor/metrics/workflow/execution")
                        .param("workflowId", "wf-1").param("success", "true").param("durationMs", "100"))
                .andExpect(status().isOk());
        verify(metricsService).recordWorkflowExecution("wf-1", true, 100L);
    }

    @Test
    @DisplayName("POST /metrics/node/execution")
    void nodeExecution_invokesService() throws Exception {
        mockMvc.perform(post("/api/monitor/metrics/node/execution")
                        .param("workflowId", "wf-1").param("nodeId", "node-A")
                        .param("nodeType", "llm").param("success", "true")
                        .param("durationMs", "200"))
                .andExpect(status().isOk());
        verify(metricsService).recordNodeExecution("wf-1", "node-A", "llm", true, 200L);
    }

    @Test
    @DisplayName("POST /metrics/api/call")
    void apiCall_invokesService() throws Exception {
        mockMvc.perform(post("/api/monitor/metrics/api/call")
                        .param("endpoint", "/users").param("statusCode", "200")
                        .param("durationMs", "10"))
                .andExpect(status().isOk());
        verify(metricsService).recordApiCall("/users", 200, 10L);
    }
}

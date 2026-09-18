package com.meowflow.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.monitor.dto.AlertRuleDTO;
import com.meowflow.monitor.entity.AlertRule;
import com.meowflow.monitor.service.AlertService;
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
import java.util.Map;

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

@WebMvcTest(controllers = AlertController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AlertController HTTP 层")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private AlertService alertService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /alert/rule")
    void createRule_invokesService() throws Exception {
        AlertRuleDTO dto = new AlertRuleDTO();
        dto.setName("CPU 高水位");
        dto.setMetricName("system.cpu");
        dto.setOperator(">");
        dto.setThreshold(80.0);
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        when(alertService.createRule(any())).thenReturn(rule);

        mockMvc.perform(post("/api/monitor/alert/rule")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("PUT /alert/rule/{id}")
    void updateRule_invokesService() throws Exception {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        when(alertService.updateRule(any(), any())).thenReturn(rule);

        mockMvc.perform(put("/api/monitor/alert/rule/1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new AlertRuleDTO())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /alert/rule/{id}")
    void deleteRule_invokesService() throws Exception {
        mockMvc.perform(delete("/api/monitor/alert/rule/1"))
                .andExpect(status().isOk());
        verify(alertService).deleteRule(1L);
    }

    @Test
    @DisplayName("GET /alert/rule/{id}")
    void getRule_invokesService() throws Exception {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        when(alertService.getRule(1L)).thenReturn(rule);

        mockMvc.perform(get("/api/monitor/alert/rule/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /alert/rules")
    void allRules_invokesService() throws Exception {
        when(alertService.getAllRules(anyInt(), anyInt()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.meowflow.monitor.entity.AlertRule>(1, 20));

        mockMvc.perform(get("/api/monitor/alert/rules?pageNum=1&pageSize=20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /alert/rule/{id}/evaluate")
    void evaluate_invokesService() throws Exception {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        when(alertService.getRule(1L)).thenReturn(rule);

        mockMvc.perform(post("/api/monitor/alert/rule/1/evaluate")
                        .contentType("application/json")
                        .content("{\"value\": 95.0}"))
                .andExpect(status().isOk());
        verify(alertService).evaluateRule(any(), org.mockito.ArgumentMatchers.eq(95.0));
    }

    @Test
    @DisplayName("GET /alert/records")
    void records_invokesService() throws Exception {
        when(alertService.getAlerts(any(), anyInt(), anyInt()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.meowflow.monitor.dto.AlertRecordDTO>(1, 20));

        mockMvc.perform(get("/api/monitor/alert/records?status=firing&pageNum=1&pageSize=20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /alert/firing")
    void firing_invokesService() throws Exception {
        when(alertService.getFiringAlerts()).thenReturn(List.of(new com.meowflow.monitor.dto.AlertRecordDTO()));

        mockMvc.perform(get("/api/monitor/alert/firing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /alert/stats")
    void stats_invokesService() throws Exception {
        when(alertService.getFiringAlertCount()).thenReturn(7);

        mockMvc.perform(get("/api/monitor/alert/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firing").value(7));
    }

    @Test
    @DisplayName("POST /alert/{alertId}/resolve")
    void resolve_invokesService() throws Exception {
        mockMvc.perform(post("/api/monitor/alert/1/resolve")
                        .contentType("application/json")
                        .content("{\"comment\": \"fixed\"}"))
                .andExpect(status().isOk());
        verify(alertService).resolveAlert(1L, "fixed");
    }

    @Test
    @DisplayName("POST /alert/silence")
    void createSilence_invokesService() throws Exception {
        com.meowflow.monitor.dto.AlertSilenceCreateDTO dto = new com.meowflow.monitor.dto.AlertSilenceCreateDTO();
        dto.setRuleId("1");
        dto.setAlertChannel("dingtalk");
        dto.setStartTime(java.time.LocalDateTime.now());
        dto.setEndTime(java.time.LocalDateTime.now().plusHours(1));
        com.meowflow.monitor.entity.AlertSilence silence = new com.meowflow.monitor.entity.AlertSilence();
        silence.setId(1L);
        when(alertService.createSilence(any())).thenReturn(silence);

        mockMvc.perform(post("/api/monitor/alert/silence")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /alert/silence/{id}")
    void deleteSilence_invokesService() throws Exception {
        mockMvc.perform(delete("/api/monitor/alert/silence/1"))
                .andExpect(status().isOk());
        verify(alertService).deleteSilence(1L);
    }

    @Test
    @DisplayName("GET /alert/silences")
    void silences_invokesService() throws Exception {
        when(alertService.getSilences(any(), anyInt(), anyInt()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.meowflow.monitor.entity.AlertSilence>(1, 20));

        mockMvc.perform(get("/api/monitor/alert/silences?status=active"))
                .andExpect(status().isOk());
    }
}


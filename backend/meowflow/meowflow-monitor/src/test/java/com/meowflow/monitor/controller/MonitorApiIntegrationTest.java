package com.meowflow.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.monitor.dto.ExecutionLogCreateRequest;
import com.meowflow.monitor.entity.ExecutionLog;
import com.meowflow.monitor.repository.ExecutionLogRepository;
import com.meowflow.monitor.service.ExecutionLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Monitor 真实 HTTP + 真实 DB 集成：logStart → logComplete → logError 三步。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Monitor API 集成 — logStart/logComplete/logError 全链路")
class MonitorApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ExecutionLogRepository logRepository;
    @Autowired
    private ExecutionLogService logService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Long EXECUTION_ID = 91001L;
    private static Long logId;

    @BeforeEach
    void login() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void cleanup() {
        SaTokenMockHelper.clear();
        UserContextHolder.clear();
    }

    @Test
    @Order(1)
    @DisplayName("logStart — POST /api/monitor/log/start 落库一行")
    void logStart_persistsRow() throws Exception {
        ExecutionLogCreateRequest req = new ExecutionLogCreateRequest();
        req.setExecutionId(String.valueOf(EXECUTION_ID));
        req.setNodeId("node-A");
        req.setNodeType("LLM");

        long before = countExecutionLogs();
        mockMvc.perform(post("/api/monitor/log/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        long after = countExecutionLogs();

        assertThat(after).isEqualTo(before + 1);

        // 找到新行
        var logs = logRepository.findByExecutionId(EXECUTION_ID);
        // 取最新一条
        logId = logs.stream()
                .filter(l -> EXECUTION_ID.equals(l.getExecutionId()))
                .findFirst()
                .map(ExecutionLog::getId)
                .orElse(null);
        assertThat(logId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("logComplete — POST /api/monitor/log/{id}/complete 标识 completed")
    void logComplete_marksCompleted() throws Exception {
        mockMvc.perform(post("/api/monitor/log/" + logId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"Hello output\""))
                .andExpect(status().isOk());

        ExecutionLog log = logRepository.findById(logId);
        assertThat(log.getMessage()).contains("completed");
    }

    @Test
    @Order(3)
    @DisplayName("logError — POST /api/monitor/log/{id}/error 切换 level")
    void logError_switchesLevel() throws Exception {
        mockMvc.perform(post("/api/monitor/log/" + logId + "/error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"node failed\""))
                .andExpect(status().isOk());

        ExecutionLog log = logRepository.findById(logId);
        assertThat(log.getLevel()).isEqualTo("error");
    }

    private long countExecutionLogs() {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mf_wf_execution_log", Long.class);
        return c == null ? 0L : c;
    }
}


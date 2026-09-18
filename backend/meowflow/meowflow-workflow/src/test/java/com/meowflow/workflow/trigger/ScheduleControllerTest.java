package com.meowflow.workflow.trigger;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScheduleController HTTP 层 —— 调度管理（基于 Redis key `wf:schedule:`）
 */
@WebMvcTest(controllers = ScheduleController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ScheduleController HTTP 层")
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TriggerManager triggerManager;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/schedule/workflow/{wfId}/node/{nodeId}?cron=...")
    void schedule_invokesManager() throws Exception {
        mockMvc.perform(post("/api/schedule/workflow/1/node/node-A").param("cronExpression", "0 0 * * * ?"))
                .andExpect(status().isOk());
        verify(triggerManager).scheduleCron(1L, "node-A", "0 0 * * * ?");
    }

    @Test
    @DisplayName("DELETE /api/schedule/workflow/{wfId}/node/{nodeId}")
    void cancel_invokesManager() throws Exception {
        mockMvc.perform(delete("/api/schedule/workflow/1/node/node-A"))
                .andExpect(status().isOk());
        verify(triggerManager).cancelScheduledTask(1L, "node-A");
    }

    @Test
    @DisplayName("GET /api/schedule/workflow/{wfId}/node/{nodeId}/status")
    void status_invokesManager() throws Exception {
        when(triggerManager.isScheduled(1L, "node-A")).thenReturn(true);

        mockMvc.perform(get("/api/schedule/workflow/1/node/node-A/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("DELETE /api/schedule/workflow/{wfId} — 取消全部")
    void cancelAll_invokesManager() throws Exception {
        mockMvc.perform(delete("/api/schedule/workflow/1"))
                .andExpect(status().isOk());
        verify(triggerManager).cancelAllTasks(1L);
    }
}

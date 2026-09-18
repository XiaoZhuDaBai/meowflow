package com.meowflow.infra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.infra.dto.IntegrationConfigDTO;
import com.meowflow.infra.entity.IntegrationConfigEntity;
import com.meowflow.infra.integration.IntegrationConfig;
import com.meowflow.infra.integration.IntegrationSender;
import com.meowflow.infra.service.IntegrationConfigService;
import com.meowflow.infra.service.IntegrationService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IntegrationController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("IntegrationController HTTP 层")
class IntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private IntegrationService integrationService;
    @MockBean
    private IntegrationConfigService integrationConfigService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("GET /api/infra/integration/enabled")
    void listEnabled_invokesService() throws Exception {
        when(integrationConfigService.listEnabled()).thenReturn(List.of(new IntegrationConfigEntity()));

        mockMvc.perform(get("/api/infra/integration/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/infra/integration")
    void listAll_invokesService() throws Exception {
        when(integrationConfigService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/infra/integration"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/infra/integration/{id}")
    void getOne_invokesService() throws Exception {
        when(integrationConfigService.getById(1L)).thenReturn(new IntegrationConfigEntity());

        mockMvc.perform(get("/api/infra/integration/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/infra/integration")
    void create_invokesService() throws Exception {
        IntegrationConfigDTO dto = new IntegrationConfigDTO();
        when(integrationConfigService.create(any())).thenReturn(new IntegrationConfigEntity());

        mockMvc.perform(post("/api/infra/integration")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/infra/integration/{id}")
    void update_invokesService() throws Exception {
        when(integrationConfigService.update(any(), any())).thenReturn(new IntegrationConfigEntity());

        mockMvc.perform(put("/api/infra/integration/1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new IntegrationConfigDTO())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/infra/integration/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/infra/integration/1"))
                .andExpect(status().isOk());
        verify(integrationConfigService).delete(1L);
    }

    @Test
    @DisplayName("POST /api/infra/integration/{id}/test")
    void testConnection_invokesService() throws Exception {
        when(integrationConfigService.testConnection(1L)).thenReturn(Map.of("ok", true));

        mockMvc.perform(post("/api/infra/integration/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(true));
    }

    @Test
    @DisplayName("POST /api/infra/integration/configs — 兼容版保存")
    void saveConfig_invokesService() throws Exception {
        mockMvc.perform(post("/api/infra/integration/configs")
                        .contentType("application/json")
                        .content("{\"type\":\"dingtalk\",\"name\":\"my\",\"webhookUrl\":\"https://\",\"secret\":\"x\",\"enabled\":true}"))
                .andExpect(status().isOk());
        verify(integrationService).saveConfig(any(IntegrationConfig.class));
    }

    @Test
    @DisplayName("GET /api/infra/integration/configs/{type}")
    void getConfig_invokesService() throws Exception {
        when(integrationService.getConfig("dingtalk")).thenReturn(new IntegrationConfig());

        mockMvc.perform(get("/api/infra/integration/configs/dingtalk"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/infra/integration/configs")
    void listConfigs_invokesService() throws Exception {
        when(integrationService.listConfigs()).thenReturn(List.of());

        mockMvc.perform(get("/api/infra/integration/configs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/infra/integration/configs/{type}")
    void deleteConfig_invokesService() throws Exception {
        mockMvc.perform(delete("/api/infra/integration/configs/dingtalk"))
                .andExpect(status().isOk());
        verify(integrationService).deleteConfig("dingtalk");
    }

    @Test
    @DisplayName("POST /api/infra/integration/send")
    void send_invokesService() throws Exception {
        IntegrationSender.SendResult r = new IntegrationSender.SendResult(true, "m-1", null, 0L);
        when(integrationService.send(anyString(), anyString())).thenReturn(r);

        mockMvc.perform(post("/api/infra/integration/send")
                        .contentType("application/json")
                        .content("{\"type\":\"dingtalk\",\"message\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageId").value("m-1"));
    }

    @Test
    @DisplayName("POST /api/infra/integration/send/async — 回调方式不校验返回值")
    void sendAsync_invokesService() throws Exception {
        mockMvc.perform(post("/api/infra/integration/send/async")
                        .contentType("application/json")
                        .content("{\"type\":\"dingtalk\",\"message\":\"hi\"}"))
                .andExpect(status().isOk());
        verify(integrationService).sendAsync(anyString(), anyString(), any(IntegrationSender.SendCallback.class));
    }

    @Test
    @DisplayName("POST /api/infra/integration/send/with-retry")
    void sendWithRetry_invokesService() throws Exception {
        mockMvc.perform(post("/api/infra/integration/send/with-retry")
                        .contentType("application/json")
                        .content("{\"type\":\"dingtalk\",\"message\":\"hi\",\"maxRetries\":5}"))
                .andExpect(status().isOk());
        verify(integrationService).sendWithRetry(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("POST /api/infra/integration/configs/dingtalk")
    void createDingtalk_invokesService() throws Exception {
        mockMvc.perform(post("/api/infra/integration/configs/dingtalk")
                        .contentType("application/json")
                        .content("{\"webhookUrl\":\"https://\",\"secret\":\"sec\"}"))
                .andExpect(status().isOk());
        verify(integrationService).createDingtalkConfig(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/infra/integration/configs/feishu")
    void createFeishu_invokesService() throws Exception {
        mockMvc.perform(post("/api/infra/integration/configs/feishu")
                        .contentType("application/json")
                        .content("{\"webhookUrl\":\"https://\"}"))
                .andExpect(status().isOk());
        verify(integrationService).createFeishuConfig(anyString());
    }

    @Test
    @DisplayName("POST /api/infra/integration/configs/wxwork")
    void createWxwork_invokesService() throws Exception {
        mockMvc.perform(post("/api/infra/integration/configs/wxwork")
                        .contentType("application/json")
                        .content("{\"webhookUrl\":\"https://\"}"))
                .andExpect(status().isOk());
        verify(integrationService).createWxworkConfig(anyString());
    }
}

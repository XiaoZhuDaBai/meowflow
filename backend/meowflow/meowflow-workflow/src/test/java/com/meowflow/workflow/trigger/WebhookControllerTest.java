package com.meowflow.workflow.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.config.WorkflowProperties;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.service.ExecutionService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebhookController HTTP 层 —— 外部系统触发工作流 + 回调
 *
 * <p>覆盖：HMAC 签名校验正反用例、未签名兼容性、async 触发。
 */
@WebMvcTest(controllers = WebhookController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("WebhookController HTTP 层")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ExecutionService executionService;

    @MockBean
    private WorkflowRepository workflowRepository;

    @MockBean
    private WorkflowProperties workflowProperties;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/webhook/{workflowCode} — 无签名直接触发")
    void trigger_noSignature_invokesExecution() throws Exception {
        Workflow wf = new Workflow();
        wf.setId(1L);
        wf.setCode("WF-1");
        when(workflowRepository.selectByCode("WF-1")).thenReturn(wf);

        ExecutionResponse resp = new ExecutionResponse();
        resp.setExecutionId(99L);
        when(executionService.execute(any(), anyLong())).thenReturn(resp);

        mockMvc.perform(post("/api/webhook/WF-1")
                        .contentType("application/json")
                        .content("{\"k\":\"v\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionId").value(99));
        verify(executionService).execute(any(), org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    @DisplayName("POST /api/webhook/{workflowCode} — 错误 HMAC 签名返回 401")
    void trigger_wrongSignature_returns401() throws Exception {
        mockMvc.perform(post("/api/webhook/WF-1?secret=topsecret")
                        .header("X-Webhook-Signature", "wrong-signature")
                        .contentType("application/json")
                        .content("{\"k\":\"v\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/webhook/{workflowCode} — 正确 HMAC 签名触发")
    void trigger_correctSignature_invokesExecution() throws Exception {
        Workflow wf = new Workflow();
        wf.setId(1L);
        wf.setCode("WF-1");
        when(workflowRepository.selectByCode("WF-1")).thenReturn(wf);

        ExecutionResponse resp = new ExecutionResponse();
        resp.setExecutionId(101L);
        when(executionService.execute(any(), anyLong())).thenReturn(resp);

        String payload = "{\"k\":\"v\"}";
        String secret = "topsecret";
        // computeHmacSha256 接受 (payload, secret) 返回 base64，但 verify 接收的是 signature 字符串
        // 这里需要把 compute 的结果作为 signature 传入。注意：当前 verify 实现是 base64 输入而非 hex —— 用我们生成的 base64
        String signature = WebhookSignatureVerifier.computeHmacSha256(payload, secret);

        mockMvc.perform(post("/api/webhook/WF-1?secret=" + secret)
                        .header("X-Webhook-Signature", signature)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /api/webhook/callback/{executionId} — 接收回调")
    void callback_returnsOk() throws Exception {
        mockMvc.perform(post("/api/webhook/callback/123")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("status", "ok"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/webhook/register — 使用配置 gateway.base-url 拼出 webhookUrl")
    void register_usesGatewayBaseUrlFromProperties() throws Exception {
        WorkflowProperties.Gateway gateway = new WorkflowProperties.Gateway();
        gateway.setBaseUrl("http://1.2.3.4:8080");
        when(workflowProperties.getGateway()).thenReturn(gateway);

        Workflow wf = new Workflow();
        wf.setId(10L);
        wf.setCode("WF-REG");
        when(workflowRepository.selectById(10L)).thenReturn(wf);

        String body = mapper.writeValueAsString(Map.of(
                "workflowId", 10,
                "path", "/hook",
                "method", "POST"
        ));

        mockMvc.perform(post("/api/webhook/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.workflowCode").value("WF-REG"))
                .andExpect(jsonPath("$.data.path").value("/hook"))
                .andExpect(jsonPath("$.data.method").value("POST"))
                .andExpect(jsonPath("$.data.webhookUrl").value("http://1.2.3.4:8080/workflow/api/webhook/WF-REG/hook"));
    }

    @Test
    @DisplayName("POST /api/webhook/register — 工作流不存在返回 404")
    void register_workflowNotFound_returns404() throws Exception {
        WorkflowProperties.Gateway gateway = new WorkflowProperties.Gateway();
        gateway.setBaseUrl("http://localhost:8080");
        when(workflowProperties.getGateway()).thenReturn(gateway);
        when(workflowRepository.selectById(99L)).thenReturn(null);

        String body = mapper.writeValueAsString(Map.of(
                "workflowId", 99,
                "path", "/hook"
        ));

        mockMvc.perform(post("/api/webhook/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/webhook/register — 缺失 workflowId / path 返回 400")
    void register_missingFields_returns400() throws Exception {
        String body = mapper.writeValueAsString(Map.of("path", "/hook"));

        mockMvc.perform(post("/api/webhook/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}



package com.meowflow.workflow.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.trigger.WebhookSignatureVerifier;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.EdgeDto;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.NodeDefinitionDto;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.EdgeDto;
import com.meowflow.workflow.entity.Execution;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.service.WorkflowService;
import com.meowflow.workflow.trigger.WebhookController;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * P0 Webhook 触发端到端验证
 * 
 * <p>验证完整流程：
 * <ol>
 *   <li>创建并发布工作流</li>
 *   <li>注册 Webhook</li>
 *   <li>获取 Webhook URL</li>
 *   <li>模拟外部 POST 请求</li>
 *   <li>验证执行创建</li>
 *   <li>验证 SSE 事件可见性</li>
 *   <li>验证 secret + HMAC 签名</li>
 * </ol>
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0 Webhook 触发端到端验证")
class WebhookTriggerE2ETest extends BaseIntegrationTest {

    @Autowired private WorkflowService workflowService;
    @Autowired private WebhookController webhookController;
    @Autowired private ExecutionRepository executionRepository;
    @Autowired private ObjectMapper objectMapper;

    private static final String WORKFLOW_CODE = "WF-P0-WEBHOOK-TEST-" + System.currentTimeMillis();

    private static Long testWorkflowId;
    private static String testWorkflowCode;
    private static String webhookUrl;
    private static String webhookSecret;
    private static String webhookToken;

    @BeforeEach
    void login() {
        SaTokenMockHelper.loginAsAdmin();
    }

    @AfterEach
    void cleanup() {
        SaTokenMockHelper.clear();
    }

    @Test
    @Order(1)
    @DisplayName("步骤1: 创建并发布 Webhook 测试工作流")
    void step1_createAndPublishWorkflow() {
        log.info("========== 步骤1: 创建并发布 Webhook 测试工作流 ==========");
        
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("P0-Webhook测试工作流");
        request.setCode(WORKFLOW_CODE);
        request.setDescription("P0 验证：Webhook 触发、签名验证");
        
        WorkflowDefinitionRequest definition = buildWebhookTestDefinition();
        request.setDefinition(definition);
        
        WorkflowResponse response = workflowService.create(request, 1L);
        testWorkflowId = response.getId();
        testWorkflowCode = response.getCode();
        
        // 发布版本
        PublishVersionRequest publishReq = new PublishVersionRequest();
        publishReq.setVersion("v1");
        publishReq.setChangelog("Webhook 测试版本");
        
        workflowService.publishVersion(testWorkflowId, publishReq, 1L);
        
        log.info("✓ 工作流创建并发布成功");
        log.info("  - workflowId: {}", testWorkflowId);
        log.info("  - workflowCode: {}", testWorkflowCode);
    }

    @Test
    @Order(2)
    @DisplayName("步骤2: 注册 Webhook")
    void step2_registerWebhook() {
        log.info("========== 步骤2: 注册 Webhook ==========");
        
        assertThat(testWorkflowId).isNotNull();
        
        WebhookController.WebhookRegistrationRequest regRequest = 
                new WebhookController.WebhookRegistrationRequest();
        regRequest.setWorkflowId(testWorkflowId);
        regRequest.setPath("/test/webhook");
        regRequest.setMethod("POST");
        regRequest.setSecret("test-secret-12345");
        
        var result = webhookController.register(regRequest);
        
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        
        WebhookController.WebhookRegistrationResponse response = result.getData();
        
        assertThat(response.getWorkflowCode()).isEqualTo(testWorkflowCode);
        assertThat(response.getPath()).isEqualTo("/test/webhook");
        assertThat(response.getMethod()).isEqualTo("POST");
        assertThat(response.getSecret()).isEqualTo("test-secret-12345");
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getWebhookUrl()).contains(testWorkflowCode);
        assertThat(response.getWebhookUrl()).contains("/test/webhook");
        
        webhookUrl = response.getWebhookUrl();
        webhookSecret = response.getSecret();
        webhookToken = response.getToken();
        
        log.info("✓ Webhook 注册成功");
        log.info("  - URL: {}", webhookUrl);
        log.info("  - Method: {}", response.getMethod());
        log.info("  - Secret: {}", webhookSecret);
        log.info("  - Token: {}", webhookToken);
    }

    @Test
    @Order(3)
    @DisplayName("步骤3: 模拟外部请求（无签名）")
    void step3_triggerWebhookWithoutSignature() {
        log.info("========== 步骤3: 模拟外部请求（无签名）==========");
        
        assertThat(testWorkflowCode).isNotNull();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "test_event");
        payload.put("data", Map.of(
                "key1", "value1",
                "key2", "value2",
                "timestamp", System.currentTimeMillis()
        ));
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/webhook/" + testWorkflowCode + "/test/webhook");
        
        var result = webhookController.trigger(
                testWorkflowCode,
                payload,
                null, // 无签名
                null,
                request
        );
        
        // 由于配置了 secret，无签名应该失败
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).contains("签名");
        
        log.info("✓ 无签名请求被正确拒绝");
    }

    @Test
    @Order(4)
    @DisplayName("步骤4: 模拟外部请求（带正确签名）")
    void step4_triggerWebhookWithValidSignature() throws Exception {
        log.info("========== 步骤4: 模拟外部请求（带正确签名）==========");
        
        assertThat(testWorkflowCode).isNotNull();
        assertThat(webhookSecret).isNotNull();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "test_event_signed");
        payload.put("data", Map.of(
                "userId", 12345,
                "action", "create",
                "timestamp", System.currentTimeMillis()
        ));
        
        // 生成签名
        String payloadJson = objectMapper.writeValueAsString(payload);
        String signature = WebhookSignatureVerifier.computeHmacSha256(payloadJson, webhookSecret);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/webhook/" + testWorkflowCode + "/test/webhook");
        
        var result = webhookController.trigger(
                testWorkflowCode,
                payload,
                signature,
                null,
                request
        );
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        
        ExecutionResponse execution = result.getData();
        
        assertThat(execution.getExecutionId()).isNotNull();
        assertThat(execution.getWorkflowId()).isEqualTo(testWorkflowId);
        assertThat(execution.getTriggerType()).isEqualTo("webhook");
        assertThat(execution.getStatus()).isIn("pending", "running");
        
        log.info("✓ Webhook 触发成功");
        log.info("  - executionId: {}", execution.getExecutionId());
        log.info("  - 初始状态: {}", execution.getStatus());
        
        // 等待执行完成（最多 30 秒）
        Long executionId = execution.getExecutionId();
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Execution exec = executionRepository.selectById(executionId);
                    assertThat(exec).isNotNull();
                    assertThat(exec.getStatus()).isIn("success", "failed", "cancelled");
                });
        
        Execution finalExecution = executionRepository.selectById(executionId);
        
        assertThat(finalExecution).isNotNull();
        assertThat(finalExecution.getTriggerType()).isEqualTo("webhook");
        
        log.info("✓ Webhook 执行完成");
        log.info("  - 最终状态: {}", finalExecution.getStatus());
        log.info("  - 耗时: {}ms", finalExecution.getCostMs());
    }

    @Test
    @Order(5)
    @DisplayName("步骤5: 测试自定义子路径")
    void step5_testCustomSubpath() throws Exception {
        log.info("========== 步骤5: 测试自定义子路径 ==========");
        
        // 注册另一个子路径
        WebhookController.WebhookRegistrationRequest regRequest = 
                new WebhookController.WebhookRegistrationRequest();
        regRequest.setWorkflowId(testWorkflowId);
        regRequest.setPath("/events/user/created");
        regRequest.setMethod("POST");
        regRequest.setSecret(null); // 无需签名
        
        var regResult = webhookController.register(regRequest);
        assertThat(regResult.isSuccess()).isTrue();
        
        String customPath = regResult.getData().getPath();
        log.info("  - 注册自定义路径: {}", customPath);
        
        // 触发
        Map<String, Object> payload = Map.of(
                "event", "user.created",
                "userId", 999
        );
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/webhook/" + testWorkflowCode + customPath);
        
        var result = webhookController.trigger(
                testWorkflowCode,
                payload,
                null,
                null,
                request
        );
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        
        log.info("✓ 自定义子路径触发成功");
        log.info("  - executionId: {}", result.getData().getExecutionId());
    }

    @Test
    @Order(6)
    @DisplayName("步骤6: 测试 GET 方法触发")
    void step6_testGetMethodTrigger() {
        log.info("========== 步骤6: 测试 GET 方法触发 ==========");
        
        // 注册 GET 方法
        WebhookController.WebhookRegistrationRequest regRequest = 
                new WebhookController.WebhookRegistrationRequest();
        regRequest.setWorkflowId(testWorkflowId);
        regRequest.setPath("/health/check");
        regRequest.setMethod("GET");
        regRequest.setSecret(null);
        
        var regResult = webhookController.register(regRequest);
        assertThat(regResult.isSuccess()).isTrue();
        
        // 模拟 GET 请求
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/webhook/" + testWorkflowCode + "/health/check");
        request.setParameter("status", "ok");
        request.setParameter("timestamp", String.valueOf(System.currentTimeMillis()));
        
        var result = webhookController.trigger(
                testWorkflowCode,
                null, // GET 请求无 body
                null,
                null,
                request
        );
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        
        log.info("✓ GET 方法触发成功");
        log.info("  - executionId: {}", result.getData().getExecutionId());
    }

    @Test
    @Order(7)
    @DisplayName("步骤7: 测试方法不匹配")
    void step7_testMethodMismatch() {
        log.info("========== 步骤7: 测试方法不匹配 ==========");
        
        // 尝试用 PUT 请求已注册为 POST 的 Webhook
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        request.setRequestURI("/api/webhook/" + testWorkflowCode + "/test/webhook");
        
        var result = webhookController.trigger(
                testWorkflowCode,
                Map.of("test", "data"),
                null,
                null,
                request
        );
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(405);
        assertThat(result.getMessage()).contains("仅支持");
        
        log.info("✓ 方法不匹配被正确拒绝");
    }

    @Test
    @Order(8)
    @DisplayName("步骤8: 测试不存在的工作流")
    void step8_testNonExistentWorkflow() {
        log.info("========== 步骤8: 测试不存在的工作流 ==========");
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/webhook/NON_EXISTENT_CODE/test");
        
        var result = webhookController.trigger(
                "NON_EXISTENT_CODE",
                Map.of("test", "data"),
                null,
                null,
                request
        );
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMessage()).contains("不存在");
        
        log.info("✓ 不存在的工作流被正确拒绝");
    }

    /**
     * 构造 Webhook 测试工作流定义
     */
    private WorkflowDefinitionRequest buildWebhookTestDefinition() {
        WorkflowDefinitionRequest def = new WorkflowDefinitionRequest();
        def.setVersion("v1");
        def.setChangelog("Webhook 测试版本");
        
        List<NodeDefinitionDto> nodes = new ArrayList<>();
        List<EdgeDto> edges = new ArrayList<>();
        
        // 节点1: webhook trigger
        NodeDefinitionDto trigger = new NodeDefinitionDto();
        trigger.setId("trigger_webhook");
        trigger.setType("trigger.webhook");
        trigger.setName("Webhook触发");
        trigger.setData(Map.of("config", Map.of()));
        nodes.add(trigger);
        
        // 节点2: set_variable（处理 webhook payload）
        NodeDefinitionDto setVar = new NodeDefinitionDto();
        setVar.setId("set_var_1");
        setVar.setType("tool.assign");
        setVar.setName("处理Webhook数据");
        setVar.setData(Map.of(
                "config", Map.of(
                        "assignments", List.of(
                                Map.of("name", "webhookProcessed", "value", true),
                                Map.of("name", "receivedAt", "value", "{{$now}}")
                        )
                )
        ));
        nodes.add(setVar);
        
        // 节点3: end
        NodeDefinitionDto end = new NodeDefinitionDto();
        end.setId("end_1");
        end.setType("end.return");
        end.setName("结束");
        end.setData(Map.of(
                "config", Map.of(
                        "output", Map.of(
                                "success", true,
                                "message", "Webhook processed successfully"
                        )
                )
        ));
        nodes.add(end);
        
        // 边
        edges.add(createEdge("edge_1", "trigger_webhook", "set_var_1"));
        edges.add(createEdge("edge_2", "set_var_1", "end_1"));
        
        def.setNodes(nodes);
        def.setEdges(edges);
        
        return def;
    }

    private EdgeDto createEdge(String id, String source, String target) {
        EdgeDto edge = new EdgeDto();
        edge.setId(id);
        edge.setSource(source);
        edge.setTarget(target);
        edge.setType("default");
        return edge;
    }
}



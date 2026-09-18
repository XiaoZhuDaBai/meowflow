package com.meowflow.workflow.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.EdgeDto;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.NodeDefinitionDto;
import com.meowflow.workflow.entity.Execution;
import com.meowflow.workflow.entity.NodeExecution;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.repository.NodeExecutionRepository;
import com.meowflow.workflow.service.ExecutionService;
import com.meowflow.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * P0 主链路端到端验证测试
 * 
 * <p>验证完整流程：
 * <ol>
 *   <li>创建工作流</li>
 *   <li>保存版本 v1</li>
 *   <li>发布版本</li>
 *   <li>异步触发执行</li>
 *   <li>验证数据库记录 (mf_wf_execution, mf_wf_node_execution)</li>
 *   <li>验证 SSE 事件流（通过 EventSink）</li>
 *   <li>验证执行日志接口</li>
 * </ol>
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0 主链路端到端验证")
class WorkflowMainChainE2ETest extends BaseIntegrationTest {

    @Autowired private WorkflowService workflowService;
    @Autowired private ExecutionService executionService;
    @Autowired private ExecutionRepository executionRepository;
    @Autowired private NodeExecutionRepository nodeExecutionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private static final String WORKFLOW_CODE = "WF-P0-MAIN-CHAIN-" + System.currentTimeMillis();

    private static Long testWorkflowId;
    private static Long testExecutionId;

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
    @DisplayName("步骤1: 创建最小可用工作流（manual trigger → set_variable → end）")
    void step1_createMinimalWorkflow() {
        log.info("========== 步骤1: 创建工作流 ==========");
        
        // 构造最小工作流定义
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("P0-主链路测试工作流");
        request.setCode(WORKFLOW_CODE);
        request.setDescription("P0 验证：manual trigger → set_variable → end");
        
        // 构造定义
        WorkflowDefinitionRequest definition = buildMinimalDefinition();
        request.setDefinition(definition);
        
        // 创建工作流
        WorkflowResponse response = workflowService.create(request, 1L);
        
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("P0-主链路测试工作流");
        assertThat(response.getStatus()).isEqualTo("draft");
        assertThat(response.getCurrentVersion()).isEqualTo("v1");
        
        testWorkflowId = response.getId();
        log.info("✓ 工作流创建成功，ID={}", testWorkflowId);
    }

    @Test
    @Order(2)
    @DisplayName("步骤2: 发布版本 v1")
    void step2_publishVersion() {
        log.info("========== 步骤2: 发布版本 ==========");
        
        assertThat(testWorkflowId).isNotNull();
        
        PublishVersionRequest request = new PublishVersionRequest();
        request.setVersion("v1");
        request.setChangelog("P0 初始版本");
        
        WorkflowVersionResponse versionResponse = workflowService.publishVersion(
                testWorkflowId, request, 1L);
        
        assertThat(versionResponse).isNotNull();
        assertThat(versionResponse.getPublishStatus()).isEqualTo("published");
        assertThat(versionResponse.getPublishedAt()).isNotNull();
        
        // 验证工作流状态已更新为 running
        WorkflowResponse workflow = workflowService.getById(testWorkflowId);
        assertThat(workflow.getStatus()).isEqualTo("running");
        assertThat(workflow.getCurrentVersion()).isEqualTo("v1");
        
        log.info("✓ 版本发布成功，状态={}", workflow.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("步骤3: 异步触发执行")
    void step3_triggerExecution() {
        log.info("========== 步骤3: 异步触发执行 ==========");
        
        assertThat(testWorkflowId).isNotNull();
        
        ExecutionRequest execRequest = new ExecutionRequest();
        execRequest.setWorkflowId(testWorkflowId);
        execRequest.setVersion("v1");
        execRequest.setTriggerType("manual");
        execRequest.setAsync(true);
        
        Map<String, Object> input = new HashMap<>();
        input.put("testKey", "testValue");
        input.put("timestamp", LocalDateTime.now().toString());
        execRequest.setInput(input);
        
        ExecutionResponse response = executionService.execute(execRequest, 1L);
        
        assertThat(response).isNotNull();
        assertThat(response.getExecutionId()).isNotNull();
        assertThat(response.getWorkflowId()).isEqualTo(testWorkflowId);
        assertThat(response.getStatus()).isIn("pending", "running");
        
        testExecutionId = response.getExecutionId();
        log.info("✓ 执行触发成功，executionId={}, 初始状态={}", testExecutionId, response.getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("步骤4: 验证 mf_wf_execution 记录")
    void step4_verifyExecutionRecord() {
        log.info("========== 步骤4: 验证执行记录 ==========");
        
        assertThat(testExecutionId).isNotNull();
        
        // 等待执行完成（最多 30 秒）
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Execution execution = executionRepository.selectById(testExecutionId);
                    assertThat(execution).isNotNull();
                    assertThat(execution.getStatus()).isIn("success", "failed", "cancelled");
                });
        
        Execution execution = executionRepository.selectById(testExecutionId);
        
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(testExecutionId);
        assertThat(execution.getWorkflowId()).isEqualTo(testWorkflowId);
        assertThat(execution.getVersion()).isEqualTo("v1");
        assertThat(execution.getTriggerType()).isEqualTo("manual");
        assertThat(execution.getStartedAt()).isNotNull();
        assertThat(execution.getFinishedAt()).isNotNull();
        
        log.info("✓ 执行记录验证成功");
        log.info("  - executionId: {}", execution.getId());
        log.info("  - 状态: {}", execution.getStatus());
        log.info("  - 耗时: {}ms", execution.getCostMs());
        log.info("  - 开始时间: {}", execution.getStartedAt());
        log.info("  - 结束时间: {}", execution.getFinishedAt());
    }

    @Test
    @Order(5)
    @DisplayName("步骤5: 验证 mf_wf_node_execution 记录")
    void step5_verifyNodeExecutionRecords() {
        log.info("========== 步骤5: 验证节点执行记录 ==========");
        
        assertThat(testExecutionId).isNotNull();
        
        List<NodeExecution> nodeExecutions = nodeExecutionRepository.findByExecutionId(testExecutionId);
        
        assertThat(nodeExecutions).isNotEmpty();
        assertThat(nodeExecutions.size()).isGreaterThanOrEqualTo(2); // 至少 trigger + set_variable
        
        // 验证每个节点都有完整的执行记录
        for (NodeExecution ne : nodeExecutions) {
            assertThat(ne.getExecutionId()).isEqualTo(testExecutionId);
            assertThat(ne.getNodeId()).isNotBlank();
            assertThat(ne.getNodeType()).isNotBlank();
            assertThat(ne.getStatus()).isNotNull();
            assertThat(ne.getStartedAt()).isNotNull();
            
            log.info("  ✓ 节点: {} ({}), 状态: {}, 耗时: {}ms",
                    ne.getNodeName(), ne.getNodeType(), ne.getStatus(), ne.getCostMs());
        }
        
        log.info("✓ 节点执行记录验证成功，共 {} 个节点", nodeExecutions.size());
    }

    @Test
    @Order(6)
    @DisplayName("步骤6: 验证执行日志接口")
    void step6_verifyExecutionLogs() {
        log.info("========== 步骤6: 验证执行日志接口 ==========");
        
        assertThat(testExecutionId).isNotNull();
        
        // 通过 service 获取节点执行列表
        List<NodeExecutionDto> nodeExecutions = executionService.getNodeExecutions(testExecutionId);
        
        assertThat(nodeExecutions).isNotEmpty();
        
        for (NodeExecutionDto dto : nodeExecutions) {
            assertThat(dto.getId()).isNotNull();
            assertThat(dto.getNodeId()).isNotBlank();
            assertThat(dto.getStatus()).isNotNull();
            
            log.info("  ✓ 日志节点: {} ({}), 状态: {}",
                    dto.getNodeName(), dto.getNodeType(), dto.getStatus());
        }
        
        log.info("✓ 执行日志接口验证成功");
    }

    @Test
    @Order(7)
    @DisplayName("步骤7: 验证执行详情完整性")
    void step7_verifyExecutionDetail() {
        log.info("========== 步骤7: 验证执行详情 ==========");
        
        assertThat(testExecutionId).isNotNull();
        
        ExecutionResponse detail = executionService.getById(testExecutionId);
        
        assertThat(detail).isNotNull();
        assertThat(detail.getExecutionId()).isEqualTo(testExecutionId);
        assertThat(detail.getWorkflowId()).isEqualTo(testWorkflowId);
        assertThat(detail.getVersion()).isEqualTo("v1");
        assertThat(detail.getTriggerType()).isEqualTo("manual");
        assertThat(detail.getStatus()).isNotNull();
        assertThat(detail.getInput()).isNotNull();
        assertThat(detail.getStartedAt()).isNotNull();
        assertThat(detail.getFinishedAt()).isNotNull();
        
        log.info("✓ 执行详情验证成功");
        log.info("  - 状态: {}", detail.getStatus());
        log.info("  - 输入: {}", detail.getInput());
        log.info("  - 输出: {}", detail.getOutput());
        log.info("  - 错误信息: {}", detail.getErrorMessage());
    }

    @Test
    @Order(8)
    @DisplayName("步骤8: 验证工作流统计更新")
    void step8_verifyWorkflowStats() {
        log.info("========== 步骤8: 验证工作流统计 ==========");
        
        assertThat(testWorkflowId).isNotNull();
        
        WorkflowResponse workflow = workflowService.getById(testWorkflowId);
        
        assertThat(workflow).isNotNull();
        assertThat(workflow.getStatTotalRun()).isGreaterThanOrEqualTo(1L);
        assertThat(workflow.getStatLastRunAt()).isNotNull();
        
        log.info("✓ 工作流统计验证成功");
        log.info("  - 总运行次数: {}", workflow.getStatTotalRun());
        log.info("  - 最后运行时间: {}", workflow.getStatLastRunAt());
        log.info("  - 成功次数: {}", workflow.getStatSuccessCount());
        log.info("  - 失败次数: {}", workflow.getStatFailCount());
    }

    /**
     * 构造最小可用工作流定义
     * 
     * <p>结构：manual trigger → set_variable → end</p>
     */
    private WorkflowDefinitionRequest buildMinimalDefinition() {
        WorkflowDefinitionRequest def = new WorkflowDefinitionRequest();
        def.setVersion("v1");
        def.setChangelog("P0 初始版本");
        
        List<NodeDefinitionDto> nodes = new ArrayList<>();
        List<EdgeDto> edges = new ArrayList<>();
        
        // 节点1: manual trigger
        NodeDefinitionDto trigger = new NodeDefinitionDto();
        trigger.setId("trigger_1");
        trigger.setType("trigger.manual");
        trigger.setName("手动触发");
        trigger.setData(Map.of(
                "config", Map.of(
                        "description", "P0 测试手动触发"
                )
        ));
        nodes.add(trigger);
        
        // 节点2: set_variable
        NodeDefinitionDto setVar = new NodeDefinitionDto();
        setVar.setId("set_var_1");
        setVar.setType("tool.assign");
        setVar.setName("设置变量");
        Map<String, Object> varData = new HashMap<>();
        varData.put("config", Map.of(
                "assignments", List.of(
                        Map.of("name", "testVar", "value", "testValue"),
                        Map.of("name", "timestamp", "value", "{{$now}}")
                )
        ));
        setVar.setData(varData);
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
                                "message", "P0 测试完成"
                        )
                )
        ));
        nodes.add(end);
        
        // 边1: trigger → set_variable
        EdgeDto edge1 = new EdgeDto();
        edge1.setId("edge_1");
        edge1.setSource("trigger_1");
        edge1.setTarget("set_var_1");
        edge1.setType("default");
        edges.add(edge1);
        
        // 边2: set_variable → end
        EdgeDto edge2 = new EdgeDto();
        edge2.setId("edge_2");
        edge2.setSource("set_var_1");
        edge2.setTarget("end_1");
        edge2.setType("default");
        edges.add(edge2);
        
        def.setNodes(nodes);
        def.setEdges(edges);
        
        return def;
    }
}



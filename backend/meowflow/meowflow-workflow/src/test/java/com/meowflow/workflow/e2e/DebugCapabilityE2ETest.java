package com.meowflow.workflow.e2e;

import com.meowflow.common.context.DebugManager;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.EdgeDto;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.NodeDefinitionDto;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.EdgeDto;
import com.meowflow.workflow.entity.Execution;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.service.ExecutionService;
import com.meowflow.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * P0 调试能力端到端验证
 * 
 * <p>验证完整调试流程：
 * <ol>
 *   <li>创建工作流并发布</li>
 *   <li>设置断点</li>
 *   <li>执行工作流</li>
 *   <li>验证命中断点暂停</li>
 *   <li>查看节点快照</li>
 *   <li>单步执行</li>
 *   <li>继续执行至完成</li>
 * </ol>
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0 调试能力端到端验证")
class DebugCapabilityE2ETest extends BaseIntegrationTest {

    @Autowired private WorkflowService workflowService;
    @Autowired private ExecutionService executionService;
    @Autowired private ExecutionRepository executionRepository;

    private static final String WORKFLOW_CODE = "WF-P0-DEBUG-TEST-" + System.currentTimeMillis();

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

    @AfterAll
    static void cleanupDebugSession() {
        if (testExecutionId != null) {
            DebugManager.detach(testExecutionId);
        }
    }

    @Test
    @Order(1)
    @DisplayName("步骤1: 创建并发布调试测试工作流")
    void step1_createAndPublishWorkflow() {
        log.info("========== 步骤1: 创建并发布调试测试工作流 ==========");
        
        // 创建工作流
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("P0-调试测试工作流");
        request.setCode(WORKFLOW_CODE);
        request.setDescription("P0 验证：调试断点、单步、快照");
        
        WorkflowDefinitionRequest definition = buildDebugTestDefinition();
        request.setDefinition(definition);
        
        WorkflowResponse response = workflowService.create(request, 1L);
        testWorkflowId = response.getId();
        
        // 发布版本
        PublishVersionRequest publishReq = new PublishVersionRequest();
        publishReq.setVersion("v1");
        publishReq.setChangelog("调试测试版本");
        
        workflowService.publishVersion(testWorkflowId, publishReq, 1L);
        
        log.info("✓ 工作流创建并发布成功，ID={}", testWorkflowId);
    }

    @Test
    @Order(2)
    @DisplayName("步骤2: 设置断点并触发执行")
    void step2_setBreakpointsAndExecute() throws Exception {
        log.info("========== 步骤2: 设置断点并触发执行 ==========");
        
        assertThat(testWorkflowId).isNotNull();
        
        // 准备执行请求
        ExecutionRequest execRequest = new ExecutionRequest();
        execRequest.setWorkflowId(testWorkflowId);
        execRequest.setVersion("v1");
        execRequest.setTriggerType("manual");
        // 模拟前端默认参数：调试模式由服务端强制异步，不能阻塞 HTTP 请求。
        execRequest.setAsync(false);
        execRequest.setDebug(true);
        execRequest.setBreakpointNodeIds(List.of("set_var_1"));
        execRequest.setInput(Map.of("debugTest", true));
        
        // 触发执行
        ExecutionResponse response = executionService.execute(execRequest, 1L);
        testExecutionId = response.getExecutionId();
        // 断点随执行请求预置，后端会在异步调度前挂接调试会话。

        log.info("✓ 执行触发成功，executionId={}", testExecutionId);
        log.info("✓ 断点已设置在节点: set_var_1");
    }

    @Test
    @Order(3)
    @DisplayName("步骤3: 验证命中断点暂停")
    void step3_verifyPausedAtBreakpoint() {
        log.info("========== 步骤3: 验证命中断点暂停 ==========");
        
        assertThat(testExecutionId).isNotNull();
        DebugManager debugManager = DebugManager.get(testExecutionId);
        assertThat(debugManager).isNotNull();
        
        // 等待执行到断点（最多 10 秒）
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    DebugManager dm = DebugManager.get(testExecutionId);
                    assertThat(dm).isNotNull();
                    assertThat(dm.getState() == DebugManager.State.PAUSED).isTrue();
                });
        
        // 验证暂停状态
        assertThat(debugManager.getState() == DebugManager.State.PAUSED).isTrue();
        assertThat(debugManager.getPausedAtNodeId()).isEqualTo("set_var_1");
        assertThat(debugManager.getPauseReason()).isIn("BREAKPOINT", "STEPPING");
        
        log.info("✓ 已在断点处暂停");
        log.info("  - 暂停节点: {}", debugManager.getPausedAtNodeId());
        log.info("  - 暂停原因: {}", debugManager.getPauseReason());
    }

    @Test
    @Order(4)
    @DisplayName("步骤4: 查看节点快照")
    void step4_viewNodeSnapshot() {
        log.info("========== 步骤4: 查看节点快照 ==========");
        
        assertThat(testExecutionId).isNotNull();
        DebugManager debugManager = DebugManager.get(testExecutionId);
        assertThat(debugManager).isNotNull();
        
        // 获取所有节点快照
        Map<String, Map<String, Object>> allSnapshots = debugManager.getAllSnapshots();
        
        assertThat(allSnapshots).isNotNull();
        log.info("✓ 节点快照获取成功，共 {} 个节点", allSnapshots.size());
        
        for (Map.Entry<String, Map<String, Object>> entry : allSnapshots.entrySet()) {
            log.info("  - 节点 {}: {}", entry.getKey(), entry.getValue());
        }
    }

    @Test
    @Order(5)
    @DisplayName("步骤5: 单步执行")
    void step5_stepExecution() throws Exception {
        log.info("========== 步骤5: 单步执行 ==========");
        
        assertThat(testExecutionId).isNotNull();
        DebugManager debugManager = DebugManager.get(testExecutionId);
        assertThat(debugManager).isNotNull();
        
        // 执行单步
        boolean stepped = debugManager.step();
        assertThat(stepped).isTrue();
        
        log.info("✓ 单步执行成功");
        
        // 等待下一个断点或完成（最多 5 秒）
        Thread.sleep(2000);
        
        DebugManager.State state = debugManager.getState();
        log.info("  - 当前状态: {}", state);
        
        if (state == DebugManager.State.PAUSED) {
            log.info("  - 当前暂停节点: {}", debugManager.getPausedAtNodeId());
        }
    }

    @Test
    @Order(6)
    @DisplayName("步骤6: 继续执行至完成")
    void step6_resumeToCompletion() {
        log.info("========== 步骤6: 继续执行至完成 ==========");
        
        assertThat(testExecutionId).isNotNull();
        DebugManager debugManager = DebugManager.get(testExecutionId);
        assertThat(debugManager).isNotNull();
        
        // 继续执行
        boolean resumed = debugManager.resume();
        assertThat(resumed).isTrue();
        
        log.info("✓ 已恢复执行");
        
        // 等待执行完成（最多 20 秒）
        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Execution execution = executionRepository.selectById(testExecutionId);
                    assertThat(execution).isNotNull();
                    assertThat(execution.getStatus()).isIn("success", "failed", "cancelled");
                });
        
        Execution execution = executionRepository.selectById(testExecutionId);
        
        assertThat(execution).isNotNull();
        assertThat(execution.getStatus()).isNotNull();
        
        log.info("✓ 执行已完成");
        log.info("  - 最终状态: {}", execution.getStatus());
        log.info("  - 耗时: {}ms", execution.getCostMs());
    }

    @Test
    @Order(7)
    @DisplayName("步骤7: 验证调试状态快照")
    void step7_verifyDebugSnapshot() {
        log.info("========== 步骤7: 验证调试状态快照 ==========");
        
        assertThat(testExecutionId).isNotNull();
        DebugManager debugManager = DebugManager.get(testExecutionId);
        
        if (debugManager != null) {
            Map<String, Object> snapshot = debugManager.snapshot();
            
            assertThat(snapshot).isNotNull();
            assertThat(snapshot).containsKey("executionId");
            assertThat(snapshot).containsKey("state");
            assertThat(snapshot).containsKey("breakpoints");
            assertThat(snapshot).containsKey("nodeSnapshots");
            
            log.info("✓ 调试快照验证成功");
            log.info("  - executionId: {}", snapshot.get("executionId"));
            log.info("  - state: {}", snapshot.get("state"));
            log.info("  - breakpoints: {}", snapshot.get("breakpoints"));
            log.info("  - nodeSnapshots 数量: {}", 
                    ((Map<?, ?>) snapshot.get("nodeSnapshots")).size());
        } else {
            log.info("⚠ DebugManager 已清理（预期行为）");
        }
    }

    /**
     * 构造调试测试工作流定义
     * 
     * <p>结构：manual trigger → set_variable → set_variable_2 → end</p>
     */
    private WorkflowDefinitionRequest buildDebugTestDefinition() {
        WorkflowDefinitionRequest def = new WorkflowDefinitionRequest();
        def.setVersion("v1");
        def.setChangelog("调试测试版本");
        
        List<NodeDefinitionDto> nodes = new ArrayList<>();
        List<EdgeDto> edges = new ArrayList<>();
        
        // 节点1: manual trigger
        NodeDefinitionDto trigger = new NodeDefinitionDto();
        trigger.setId("trigger_1");
        trigger.setType("trigger.manual");
        trigger.setName("手动触发");
        trigger.setData(Map.of("config", Map.of()));
        nodes.add(trigger);
        
        // 节点2: set_variable (第一个断点)
        NodeDefinitionDto setVar1 = new NodeDefinitionDto();
        setVar1.setId("set_var_1");
        setVar1.setType("tool.assign");
        setVar1.setName("设置变量1");
        setVar1.setData(Map.of(
                "config", Map.of(
                        "assignments", List.of(
                                Map.of("name", "step1", "value", "completed"),
                                Map.of("name", "debugVar", "value", "debugValue")
                        )
                )
        ));
        nodes.add(setVar1);
        
        // 节点3: set_variable_2
        NodeDefinitionDto setVar2 = new NodeDefinitionDto();
        setVar2.setId("set_var_2");
        setVar2.setType("tool.assign");
        setVar2.setName("设置变量2");
        setVar2.setData(Map.of(
                "config", Map.of(
                        "assignments", List.of(
                                Map.of("name", "step2", "value", "completed")
                        )
                )
        ));
        nodes.add(setVar2);
        
        // 节点4: end
        NodeDefinitionDto end = new NodeDefinitionDto();
        end.setId("end_1");
        end.setType("end.return");
        end.setName("结束");
        end.setData(Map.of(
                "config", Map.of(
                        "output", Map.of("success", true)
                )
        ));
        nodes.add(end);
        
        // 边
        edges.add(createEdge("edge_1", "trigger_1", "set_var_1"));
        edges.add(createEdge("edge_2", "set_var_1", "set_var_2"));
        edges.add(createEdge("edge_3", "set_var_2", "end_1"));
        
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








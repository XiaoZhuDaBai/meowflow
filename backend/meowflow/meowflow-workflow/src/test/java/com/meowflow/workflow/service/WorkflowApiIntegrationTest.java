package com.meowflow.workflow.service;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.dto.ExecutionRequest;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.dto.WorkflowCreateRequest;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.EdgeDto;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.NodeDefinitionDto;
import com.meowflow.workflow.dto.PublishVersionRequest;
import com.meowflow.workflow.dto.WorkflowResponse;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Workflow 真实 DB 集成：create → saveVersion → publish → execute
 *
 * <p>依赖 docker-compose 起的 meowflow-postgres。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("WorkflowService 集成 — 完整生命周期")
class WorkflowApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private ExecutionService executionService;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private WorkflowVersionRepository versionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String CODE_1 = "WF-IT-1-" + System.currentTimeMillis();
    private static final String CODE_DUP = "WF-IT-2-" + System.currentTimeMillis();
    private static final String CODE_EXEC = "WF-IT-3-" + System.currentTimeMillis();

    @BeforeEach
    void login() {
        SaTokenMockHelper.loginAsAdmin();
    }

    @AfterEach
    void logout() {
        SaTokenMockHelper.clear();
        UserContextHolder.clear();
    }

    @Test
    @Order(1)
    @DisplayName("create — 创建工作流并返回 ID")
    void createWorkflow() {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("测试工作流_1");
        req.setCode(CODE_1);
        req.setDescription("integration test");
        req.setDefinition(minimalDefinition());

        WorkflowResponse created = workflowService.create(req, 1L);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo("draft");
        assertThat(workflowRepository.selectById(created.getId())).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("重复 code 抛 DATA_ALREADY_EXISTS")
    void duplicateCode_throws() {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("另一个");
        req.setCode(CODE_DUP);
        workflowService.create(req, 1L);

        assertThatThrownBy(() -> {
            WorkflowCreateRequest dup = new WorkflowCreateRequest();
            dup.setName("dup");
            dup.setCode(CODE_DUP);
            workflowService.create(dup, 1L);
        }).isInstanceOf(BizException.class);
    }

    @Test
    @Order(3)
    @DisplayName("execute — 同步执行工作流返回 ExecutionResponse")
    void executeWorkflow() {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("执行测试");
        req.setCode(CODE_EXEC);
        req.setDefinition(minimalDefinition());
        WorkflowResponse created = workflowService.create(req, 1L);

        PublishVersionRequest publish = new PublishVersionRequest();
        publish.setVersion("v1");
        workflowService.publishVersion(created.getId(), publish, 1L);

        ExecutionRequest exec = new ExecutionRequest();
        exec.setWorkflowId(created.getId());
        exec.setTriggerType("manual");
        exec.setAsync(false);
        exec.setInput(Map.of("k1", "v1"));

        ExecutionResponse resp = executionService.execute(exec, 1L);
        assertThat(resp).isNotNull();
        assertThat(resp.getExecutionId()).isNotNull();
    }
    private WorkflowDefinitionRequest minimalDefinition() {
        WorkflowDefinitionRequest def = new WorkflowDefinitionRequest();
        def.setVersion("v1");

        NodeDefinitionDto trigger = new NodeDefinitionDto();
        trigger.setId("trigger_1");
        trigger.setType("trigger.manual");
        trigger.setName("Manual");
        trigger.setData(Map.of("config", Map.of()));

        NodeDefinitionDto end = new NodeDefinitionDto();
        end.setId("end_1");
        end.setType("end.return");
        end.setName("End");
        end.setData(Map.of("config", Map.of()));

        EdgeDto edge = new EdgeDto();
        edge.setId("edge_1");
        edge.setSource("trigger_1");
        edge.setTarget("end_1");
        edge.setType("default");

        def.setNodes(List.of(trigger, end));
        def.setEdges(List.of(edge));
        return def;
    }
}


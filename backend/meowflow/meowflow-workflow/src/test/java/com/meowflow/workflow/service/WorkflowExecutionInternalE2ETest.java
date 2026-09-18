package com.meowflow.workflow.service;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.dto.ExecutionRequest;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.dto.PublishVersionRequest;
import com.meowflow.workflow.dto.WorkflowCreateRequest;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.EdgeDto;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.NodeDefinitionDto;
import com.meowflow.workflow.dto.WorkflowResponse;
import com.meowflow.workflow.repository.ExecutionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@DisplayName("工作流执行链集成 (workflow 内部)")
class WorkflowExecutionInternalE2ETest extends BaseIntegrationTest {

    private static final String WORKFLOW_CODE = "WF-IT-E2E-INT-" + System.currentTimeMillis();

    @Autowired private WorkflowService workflowService;
    @Autowired private ExecutionService executionService;
    @Autowired private ExecutionRepository executionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void login() { SaTokenMockHelper.loginAsAdmin(); }

    @AfterEach
    void cleanup() {
        SaTokenMockHelper.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("executeAsync 落库 — execution 行数 ≥ 1")
    void executeAsync_persistsRow() {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("Internal E2E");
        req.setCode(WORKFLOW_CODE);
        req.setDefinition(minimalDefinition());
        WorkflowResponse wf = workflowService.create(req, 1L);

        PublishVersionRequest publish = new PublishVersionRequest();
        publish.setVersion("v1");
        workflowService.publishVersion(wf.getId(), publish, 1L);

        ExecutionRequest ex = new ExecutionRequest();
        ex.setWorkflowId(wf.getId());
        ex.setAsync(true);
        ex.setTriggerType("manual");
        ex.setInput(Map.of("k", "v"));

        ExecutionResponse resp = executionService.execute(ex, 1L);
        assertThat(resp).isNotNull();
        assertThat(resp.getExecutionId()).isNotNull();

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mf_wf_execution WHERE workflow_id = ?", Long.class, wf.getId());
        assertThat(total).isGreaterThanOrEqualTo(1L);
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


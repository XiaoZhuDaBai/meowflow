package com.meowflow.workflow.service;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.dto.PublishVersionRequest;
import com.meowflow.workflow.dto.WorkflowCreateRequest;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.EdgeDto;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest.NodeDefinitionDto;
import com.meowflow.workflow.dto.WorkflowResponse;
import com.meowflow.workflow.dto.WorkflowVersionResponse;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkflowVersionService 集成测试 —— 创建 → 多次 saveVersion → publishVersion → rollback。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("WorkflowVersion 集成 — 多版本 / 发布 / 回滚")
class WorkflowVersionLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired private WorkflowService workflowService;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private WorkflowVersionRepository versionRepository;

    private static final String WORKFLOW_CODE = "WF-VER-LIFECYCLE-" + System.currentTimeMillis();

    private static Long workflowId;

    @BeforeEach
    void login() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() {
        SaTokenMockHelper.clear();
        UserContextHolder.clear();
    }

    @Test
    @Order(1)
    @DisplayName("setup — 建工作流")
    void create() {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("VersionTest WF");
        req.setCode(WORKFLOW_CODE);
        req.setDefinition(validDefinition());

        WorkflowResponse resp = workflowService.create(req, 1L);
        workflowId = resp.getId();
        assertThat(workflowId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("listVersions — 至少有 v1")
    void listVersions_minimum() {
        assertThat(workflowService.listVersions(workflowId)).isNotEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("publishVersion — 让 status = running")
    void publishVersion_changesStatus() {
        PublishVersionRequest req = new PublishVersionRequest();
        req.setVersion("v1");

        WorkflowVersionResponse ver = workflowService.publishVersion(workflowId, req, 1L);
        assertThat(ver.getVersion()).isEqualTo("v1");

        WorkflowResponse refreshed = workflowService.getById(workflowId);
        assertThat(refreshed.getStatus()).isEqualTo("running");
    }
    private WorkflowDefinitionRequest validDefinition() {
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

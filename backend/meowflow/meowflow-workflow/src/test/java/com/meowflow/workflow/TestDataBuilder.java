package com.meowflow.workflow;

import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.entity.WorkflowVersion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Test data builder utility for creating test objects
 */
public class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class
    }

    public static Workflow createWorkflow() {
        Workflow workflow = new Workflow();
        workflow.setId(1L);
        workflow.setName("Test Workflow");
        workflow.setCode("test-workflow");
        workflow.setDescription("A test workflow");
        workflow.setStatus("draft");
        workflow.setOwnerId(1L);
        workflow.setOrgId(1L);
        workflow.setIsPublic(false);
        workflow.setTags(List.of("test", "demo"));
        workflow.setStatTotalRun(0L);
        workflow.setDeleted(false);
        workflow.setCreateTime(LocalDateTime.now());
        workflow.setUpdateTime(LocalDateTime.now());
        return workflow;
    }

    public static Workflow createWorkflow(Long id, String name) {
        Workflow workflow = createWorkflow();
        workflow.setId(id);
        workflow.setName(name);
        return workflow;
    }

    public static WorkflowVersion createWorkflowVersion() {
        WorkflowVersion version = new WorkflowVersion();
        version.setId(1L);
        version.setWorkflowId(1L);
        version.setVersion("v1");
        version.setDefinition("{}");
        version.setPublishStatus("draft");
        version.setCreateBy(1L);
        version.setCreateTime(LocalDateTime.now());
        return version;
    }

    public static NodeDefinition createNode(String id, NodeType type) {
        return NodeDefinition.builder()
                .id(id)
                .type(type)
                .name("Node " + id)
                .position(new NodeDefinition.Position(0, 0))
                .data(Map.of("config", "value"))
                .inputs(new String[]{})
                .outputs(new String[]{})
                .build();
    }

    public static Edge createEdge(String source, String target) {
        return Edge.builder()
                .id(source + "_" + target)
                .source(source)
                .target(target)
                .build();
    }

    public static WorkflowDefinition createSimpleWorkflow() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition llm = createNode("llm", NodeType.LLM);
        NodeDefinition end = createNode("end", NodeType.END);

        Edge triggerToLlm = createEdge("trigger", "llm");
        Edge llmToEnd = createEdge("llm", "end");

        return WorkflowDefinition.builder()
                .workflowId("test-workflow")
                .version("v1")
                .nodes(List.of(trigger, llm, end))
                .edges(List.of(triggerToLlm, llmToEnd))
                .build();
    }

    public static WorkflowDefinition createDiamondWorkflow() {
        NodeDefinition a = createNode("A", NodeType.TRIGGER_MANUAL);
        NodeDefinition b = createNode("B", NodeType.HTTP);
        NodeDefinition c = createNode("C", NodeType.NOTIFY);
        NodeDefinition d = createNode("D", NodeType.END);

        return WorkflowDefinition.builder()
                .workflowId("diamond-workflow")
                .version("v1")
                .nodes(List.of(a, b, c, d))
                .edges(List.of(
                        createEdge("A", "B"),
                        createEdge("A", "C"),
                        createEdge("B", "D"),
                        createEdge("C", "D")
                ))
                .build();
    }
}

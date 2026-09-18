package com.meowflow.workflow.engine;

import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.EdgeType;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeRouterTest {

    private final EdgeRouter router = new EdgeRouter();

    @Test
    void selectDownstreamEdges_routesByEdgeExpression() {
        ExecutionContext context = ExecutionContext.builder()
                .input(Map.of("score", 90))
                .build();
        NodeDefinition upstream = node("branch", NodeType.CONDITION);
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .nodes(List.of(upstream, node("true", NodeType.END), node("false", NodeType.END)))
                .build();
        List<Edge> edges = List.of(
                conditionEdge("e1", "branch", "true", "{{input.score}} >= 80"),
                conditionEdge("e2", "branch", "false", "{{input.score}} < 80")
        );

        List<String> downstream = router.selectDownstreamEdges(
                upstream, edges, NodeResult.success("branch", NodeType.CONDITION, "branch", Map.of()), definition, context);

        assertThat(downstream).containsExactly("true");
    }

    @Test
    void selectDownstreamEdges_routesByBooleanLabelWhenNoExpression() {
        ExecutionContext context = ExecutionContext.builder().build();
        NodeDefinition upstream = node("branch", NodeType.CONDITION);
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .nodes(List.of(upstream, node("yes", NodeType.END), node("no", NodeType.END)))
                .build();
        List<Edge> edges = List.of(
                labeledConditionEdge("e1", "branch", "yes", "通过"),
                labeledConditionEdge("e2", "branch", "no", "失败")
        );

        List<String> downstream = router.selectDownstreamEdges(
                upstream, edges,
                NodeResult.success("branch", NodeType.CONDITION, "branch", Map.of("selectedBranch", "true")),
                definition, context);

        assertThat(downstream).containsExactly("yes");
    }

    private NodeDefinition node(String id, NodeType type) {
        return NodeDefinition.builder().id(id).type(type).name(id).build();
    }

    private Edge conditionEdge(String id, String source, String target, String expression) {
        return Edge.builder()
                .id(id)
                .source(source)
                .target(target)
                .type(EdgeType.CONDITION)
                .data(Edge.EdgeData.builder()
                        .config(Map.of("expression", expression))
                        .build())
                .build();
    }

    private Edge labeledConditionEdge(String id, String source, String target, String label) {
        return Edge.builder()
                .id(id)
                .source(source)
                .target(target)
                .type(EdgeType.CONDITION)
                .data(Edge.EdgeData.builder().label(label).build())
                .build();
    }
}

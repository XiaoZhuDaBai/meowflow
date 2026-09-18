package com.meowflow.workflow.executor.tool;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListOperatorExecutorTest {

    private final ListOperatorExecutor executor = new ListOperatorExecutor();

    @Test
    void sort_sortsByField() {
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("List")
                .type(NodeType.LIST_OPERATOR)
                .data(Map.of(
                        "list", List.of(
                                Map.of("name", "a", "score", 0.9),
                                Map.of("name", "b", "score", 0.4),
                                Map.of("name", "c", "score", 0.7)
                        ),
                        "operation", "sort",
                        "sortKey", "score",
                        "sortOrder", "desc"
                ))
                .build();

        NodeResult result = executor.execute(ExecutionContext.builder().build(), node);

        assertThat(result.isSuccess()).isTrue();
        List<?> sorted = (List<?>) result.getOutput().get("result");
        assertThat(((Map<?, ?>) sorted.get(0)).get("name")).isEqualTo("a");
        assertThat(((Map<?, ?>) sorted.get(2)).get("name")).isEqualTo("b");
    }

    @Test
    void filter_filtersByExpression() {
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("List")
                .type(NodeType.LIST_OPERATOR)
                .data(Map.of(
                        "list", List.of(
                                Map.of("score", 0.9),
                                Map.of("score", 0.4)
                        ),
                        "operation", "filter",
                        "expression", "item.score > 0.5"
                ))
                .build();

        NodeResult result = executor.execute(ExecutionContext.builder().build(), node);

        assertThat(result.isSuccess()).isTrue();
        List<?> filtered = (List<?>) result.getOutput().get("result");
        assertThat(filtered).hasSize(1);
        assertThat(((Map<?, ?>) filtered.get(0)).get("score")).isEqualTo(0.9);
    }
}

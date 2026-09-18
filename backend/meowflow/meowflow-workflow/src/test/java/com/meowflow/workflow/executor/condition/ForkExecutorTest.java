package com.meowflow.workflow.executor.condition;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ForkExecutor Tests")
class ForkExecutorTest {

    private ForkExecutor executor;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        executor = new ForkExecutor();
        context = ExecutionContext.builder().build();
    }

    @Test
    @DisplayName("getNodeType returns FORK")
    void getNodeType_returnsFORK() {
        assertThat(executor.getNodeType()).isEqualTo(NodeType.FORK);
    }

    @Test
    @DisplayName("executes with branches")
    void execute_withBranches_returnsBranchCount() {
        NodeDefinition node = NodeDefinition.builder()
                .id("fork-1").type(NodeType.FORK).name("Test FORK")
                .data(Map.of("branches", List.of("branch-a", "branch-b", "branch-c")))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput())
                .containsEntry("branchCount", 3)
                .containsEntry("parallel", true);
    }

    @Test
    @DisplayName("executes without branches")
    void execute_noBranches_returnsZeroCount() {
        NodeDefinition node = NodeDefinition.builder()
                .id("fork-1").type(NodeType.FORK).name("Test FORK")
                .data(new HashMap<>())
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("branchCount", 0);
    }

    @Test
    @DisplayName("supports returns true for FORK type")
    void supports_returnsTrueForFORK() {
        assertThat(executor.supports(NodeType.FORK)).isTrue();
    }
}
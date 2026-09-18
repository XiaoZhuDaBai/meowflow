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
@DisplayName("SwitchExecutor Tests")
class SwitchExecutorTest {

    private SwitchExecutor executor;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        executor = new SwitchExecutor();
        context = ExecutionContext.builder().build();
    }

    @Test
    @DisplayName("getNodeType returns SWITCH")
    void getNodeType_returnsSWITCH() {
        assertThat(executor.getNodeType()).isEqualTo(NodeType.SWITCH);
    }

    @Test
    @DisplayName("matches first matching case")
    void execute_matchingCase_returnsCaseBranch() {
        NodeDefinition node = NodeDefinition.builder()
                .id("s-1").type(NodeType.SWITCH).name("Test SWITCH")
                .data(Map.of(
                        "expression", "active",
                        "cases", List.of(
                                Map.of("name", "activeBranch", "value", "active"),
                                Map.of("name", "inactiveBranch", "value", "inactive")
                        ),
                        "defaultBranch", "default"
                ))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("selectedBranch", "activeBranch");
        assertThat(result.getOutput()).containsEntry("resolvedValue", "active");
    }

    @Test
    @DisplayName("falls back to default when no match")
    void execute_noMatchingCase_usesDefault() {
        NodeDefinition node = NodeDefinition.builder()
                .id("s-1").type(NodeType.SWITCH).name("Test SWITCH")
                .data(Map.of(
                        "expression", "pending",
                        "cases", List.of(
                                Map.of("name", "activeBranch", "value", "active"),
                                Map.of("name", "inactiveBranch", "value", "inactive")
                        ),
                        "defaultBranch", "default"
                ))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("selectedBranch", "default");
    }

    @Test
    @DisplayName("handles null cases by using default")
    void execute_nullCases_usesDefault() {
        NodeDefinition node = NodeDefinition.builder()
                .id("s-1").type(NodeType.SWITCH).name("Test SWITCH")
                .data(Map.of(
                        "expression", "anything",
                        "defaultBranch", "default"
                ))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("selectedBranch", "default");
    }

    @Test
    @DisplayName("case matching is case-insensitive")
    void execute_caseInsensitive_returnsMatch() {
        NodeDefinition node = NodeDefinition.builder()
                .id("s-1").type(NodeType.SWITCH).name("Test SWITCH")
                .data(Map.of(
                        "expression", "ACTIVE",
                        "cases", List.of(Map.of("name", "activeBranch", "value", "active")),
                        "defaultBranch", "default"
                ))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.getOutput()).containsEntry("selectedBranch", "activeBranch");
    }

    @Test
    @DisplayName("counts matched cases when multiple match")
    void execute_multipleMatches_returnsCount() {
        NodeDefinition node = NodeDefinition.builder()
                .id("s-1").type(NodeType.SWITCH).name("Test SWITCH")
                .data(Map.of(
                        "expression", "yes",
                        "cases", List.of(
                                Map.of("name", "b1", "value", "yes"),
                                Map.of("name", "b2", "value", "yes")
                        ),
                        "defaultBranch", "default"
                ))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.getOutput()).containsEntry("matchedCount", 2);
    }

    @Test
    @DisplayName("evaluates per-case expression from frontend if-else")
    void execute_perCaseExpression_selectsFirstMatch() {
        ExecutionContext contextWithInput = ExecutionContext.builder()
                .input(Map.of("score", 85))
                .variables(new HashMap<>())
                .build();
        NodeDefinition node = NodeDefinition.builder()
                .id("s-1").type(NodeType.SWITCH).name("Test SWITCH")
                .data(Map.of(
                        "cases", List.of(
                                Map.of("name", "优秀", "expression", "{{input.score}} >= 80"),
                                Map.of("name", "及格", "expression", "{{input.score}} >= 60")
                        ),
                        "defaultBranch", "default"
                ))
                .build();

        NodeResult result = executor.execute(contextWithInput, node);

        assertThat(result.getOutput()).containsEntry("selectedBranch", "优秀");
    }
}

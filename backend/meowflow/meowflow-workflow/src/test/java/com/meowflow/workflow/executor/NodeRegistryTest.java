package com.meowflow.workflow.executor;

import com.meowflow.workflow.definition.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NodeRegistry Tests")
class NodeRegistryTest {

    @Mock
    private NodeExecutor llmExecutor;

    @Mock
    private NodeExecutor httpExecutor;

    @Mock
    private NodeExecutor ifExecutor;

    private NodeRegistry registry;

    @BeforeEach
    void setUp() {
        when(llmExecutor.getNodeType()).thenReturn(NodeType.LLM);
        when(httpExecutor.getNodeType()).thenReturn(NodeType.HTTP);
        when(ifExecutor.getNodeType()).thenReturn(NodeType.IF);

        registry = new NodeRegistry(List.of(llmExecutor, httpExecutor, ifExecutor));
        registry.init();
    }

    @Test
    @DisplayName("init registers all executors by node type")
    void init_registersAllExecutors() {
        assertThat(registry.getRegisteredTypes()).hasSize(3);
    }

    @Test
    @DisplayName("getExecutor returns registered executor by type")
    void getExecutor_registeredType_returnsExecutor() {
        assertThat(registry.getExecutor(NodeType.LLM)).isSameAs(llmExecutor);
        assertThat(registry.getExecutor(NodeType.HTTP)).isSameAs(httpExecutor);
        assertThat(registry.getExecutor(NodeType.IF)).isSameAs(ifExecutor);
    }

    @Test
    @DisplayName("getExecutor returns null for unregistered type")
    void getExecutor_unregisteredType_returnsNull() {
        assertThat(registry.getExecutor(NodeType.TRIGGER_WEBHOOK)).isNull();
    }

    @Test
    @DisplayName("getExecutor by code resolves registered types")
    void getExecutorByCode_registeredType_returnsExecutor() {
        assertThat(registry.getExecutor("llm")).isSameAs(llmExecutor);
        assertThat(registry.getExecutor("LLM")).isSameAs(llmExecutor);
        assertThat(registry.getExecutor("if")).isSameAs(ifExecutor);
    }

    @Test
    @DisplayName("getExecutor by unknown code returns null")
    void getExecutorByCode_unknownCode_returnsNull() {
        assertThat(registry.getExecutor("unknown-node-type")).isNull();
    }

    @Test
    @DisplayName("isRegistered returns true for registered types")
    void isRegistered_registeredType_returnsTrue() {
        assertThat(registry.isRegistered(NodeType.LLM)).isTrue();
    }

    @Test
    @DisplayName("isRegistered returns false for unregistered types")
    void isRegistered_unregisteredType_returnsFalse() {
        assertThat(registry.isRegistered(NodeType.CODE)).isFalse();
    }

    @Test
    @DisplayName("init skips executors with null node type")
    void init_executorWithNullType_skipped() {
        NodeExecutor nullTypeExecutor = org.mockito.Mockito.mock(NodeExecutor.class);
        when(nullTypeExecutor.getNodeType()).thenReturn(null);

        NodeRegistry r = new NodeRegistry(List.of(nullTypeExecutor));
        r.init();

        assertThat(r.getRegisteredTypes()).isEmpty();
    }

    @Nested
    @DisplayName("WorkflowNodeType Coverage")
    class WorkflowNodeTypeTests {

        @Test
        @DisplayName("All control node types are distinct")
        void controlNodeTypes_distinct() {
            assertThat(NodeType.IF).isNotEqualTo(NodeType.SWITCH);
            assertThat(NodeType.IF).isNotEqualTo(NodeType.FORK);
            assertThat(NodeType.FORK).isNotEqualTo(NodeType.JOIN);
            assertThat(NodeType.SWITCH).isNotEqualTo(NodeType.CONDITION);
        }

        @Test
        @DisplayName("Node types have correct codes")
        void nodeTypes_codes() {
            assertThat(NodeType.IF.getCode()).isEqualTo("if");
            assertThat(NodeType.SWITCH.getCode()).isEqualTo("switch");
            assertThat(NodeType.FORK.getCode()).isEqualTo("fork");
            assertThat(NodeType.JOIN.getCode()).isEqualTo("join");
        }

        @Test
        @DisplayName("fromCode resolves case insensitively")
        void fromCode_caseInsensitive() {
            assertThat(NodeType.fromCode("LLM")).isEqualTo(NodeType.LLM);
            assertThat(NodeType.fromCode("if")).isEqualTo(NodeType.IF);
        }

        @Test
        @DisplayName("fromCode throws for unknown code")
        void fromCode_unknown_throws() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> NodeType.fromCode("unknown-xxx"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("fromCode resolves frontend DSL aliases")
        void fromCode_frontendAliases() {
            assertThat(NodeType.fromCode("ai.llm")).isEqualTo(NodeType.LLM);
            assertThat(NodeType.fromCode("trigger.webhook")).isEqualTo(NodeType.TRIGGER_WEBHOOK);
            assertThat(NodeType.fromCode("tool.http")).isEqualTo(NodeType.HTTP);
            assertThat(NodeType.fromCode("condition.switch")).isEqualTo(NodeType.SWITCH);
            assertThat(NodeType.fromCode("ai.question-classifier")).isEqualTo(NodeType.QUESTION_CLASSIFIER);
            assertThat(NodeType.fromCode("tool.list-operator")).isEqualTo(NodeType.LIST_OPERATOR);
            assertThat(NodeType.fromCode("flow.wait")).isEqualTo(NodeType.WAIT);
            assertThat(NodeType.fromCode("tool.sub-workflow")).isEqualTo(NodeType.SUB_WORKFLOW);
            assertThat(NodeType.fromCode("tool.abstract")).isEqualTo(NodeType.CODE);
        }

        @Test
        @DisplayName("fromJson returns null for unknown node types")
        void fromJson_unknown_returnsNull() {
            assertThat(NodeType.fromJson("tool.does-not-exist")).isNull();
        }
    }
}

package com.meowflow.workflow.executor;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NodeExecutor 接口和抽象类测试
 */
class NodeExecutorTest {

    /**
     * Test executor implementation for testing
     */
    static class TestNodeExecutor implements NodeExecutor {
        private final NodeType nodeType;
        private final NodeResult resultToReturn;

        TestNodeExecutor(NodeType nodeType, NodeResult resultToReturn) {
            this.nodeType = nodeType;
            this.resultToReturn = resultToReturn;
        }

        @Override
        public NodeResult execute(ExecutionContext context, NodeDefinition node) {
            return resultToReturn;
        }

        @Override
        public NodeType getNodeType() {
            return nodeType;
        }
    }

    /**
     * Abstract executor implementation for testing
     */
    static class TestAbstractExecutor extends AbstractNodeExecutor {
        private final NodeType nodeType;
        private final NodeResult resultToReturn;
        private final boolean throwException;

        TestAbstractExecutor(NodeType nodeType, NodeResult resultToReturn) {
            this(nodeType, resultToReturn, false);
        }

        TestAbstractExecutor(NodeType nodeType, NodeResult resultToReturn, boolean throwException) {
            this.nodeType = nodeType;
            this.resultToReturn = resultToReturn;
            this.throwException = throwException;
        }

        @Override
        public NodeType getNodeType() {
            return nodeType;
        }

        @Override
        protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
            if (throwException) {
                throw new RuntimeException("Test exception");
            }
            return resultToReturn;
        }
    }

    @Test
    void executeAsync_defaultImpl_callsExecuteSync() throws Exception {
        // Given
        NodeResult expected = NodeResult.success("node-1", NodeType.LLM, "Test", null);
        TestNodeExecutor executor = new TestNodeExecutor(NodeType.LLM, expected);

        ExecutionContext ctx = new ExecutionContext();
        NodeDefinition node = NodeDefinition.builder().id("node-1").build();

        // When
        CompletableFuture<NodeResult> future = executor.executeAsync(ctx, node, java.time.Duration.ofSeconds(10));

        // Then
        assertThat(future).isNotNull();
        assertThat(future.get()).isNotNull();
        assertThat(future.get().getNodeId()).isEqualTo("node-1");
    }

    @Test
    void executeAsync_withException_returnsFailedFuture() {
        // Given - executor that throws
        NodeExecutor executor = new AbstractNodeExecutor() {
            @Override
            public NodeType getNodeType() {
                return NodeType.LLM;
            }

            @Override
            protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
                throw new RuntimeException("Error");
            }
        };

        // When
        CompletableFuture<NodeResult> future = executor.executeAsync(
                new ExecutionContext(),
                NodeDefinition.builder().id("n1").build(),
                java.time.Duration.ofSeconds(1));

        // Then - AbstractNodeExecutor catches business exceptions and returns a failed result.
        assertThat(future).isCompleted();
        assertThat(future.join().getStatus()).isEqualTo(NodeResult.NodeStatus.FAILED);
    }

    @Test
    void getNodeType_defaultImpl_returnsNull() {
        // Given
        NodeExecutor executor = new NodeExecutor() {
            @Override
            public NodeResult execute(ExecutionContext context, NodeDefinition node) {
                return null;
            }
        };

        // Then
        assertThat(executor.getNodeType()).isNull();
    }

    @Test
    void supports_shouldMatchByType() {
        // Given
        NodeExecutor executor = new TestNodeExecutor(NodeType.LLM, null);

        // Then
        assertThat(executor.supports(NodeType.LLM)).isTrue();
        assertThat(executor.supports(NodeType.HTTP)).isFalse();
        assertThat(executor.supports(null)).isFalse();
    }
}


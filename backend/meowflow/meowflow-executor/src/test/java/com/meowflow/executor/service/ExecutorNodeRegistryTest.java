package com.meowflow.executor.service;

import com.meowflow.executor.entity.ExecutorNodeEntity;
import com.meowflow.executor.mapper.ExecutorNodeMapper;
import com.meowflow.executor.registry.ExecutorNode;
import com.meowflow.executor.registry.HealthChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ExecutorNodeRegistry 鍗曞厓娴嬭瘯
 */
@ExtendWith(MockitoExtension.class)
class ExecutorNodeRegistryTest {

    @Mock
    private HealthChecker healthChecker;

    @Mock
    private ExecutorNodeMapper executorNodeMapper;

    private ExecutorNodeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ExecutorNodeRegistry(healthChecker, executorNodeMapper);
    }

    @Test
    void register_shouldCreateNodeAndPersistToDB() {
        // When
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);

        // Then
        assertThat(node).isNotNull();
        assertThat(node.getName()).isEqualTo("Test Node");
        assertThat(node.getHost()).isEqualTo("192.168.1.100");
        assertThat(node.getPort()).isEqualTo(8080);
        assertThat(node.getMaxConcurrentTasks()).isEqualTo(10);
        assertThat(node.getNodeId()).isNotBlank();

        verify(executorNodeMapper).insert(any(ExecutorNodeEntity.class));
        verify(healthChecker).registerNode(node);
    }

    @Test
    void register_shouldHandleDBFailureGracefully() {
        // Given
        doThrow(new RuntimeException("DB Error")).when(executorNodeMapper).insert(any());

        // When
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);

        // Then
        assertThat(node).isNotNull();
        assertThat(node.getNodeId()).isNotBlank();
    }

    @Test
    void unregister_shouldRemoveNodeAndUpdateDB() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);
        String nodeId = node.getNodeId();

        // When
        registry.unregister(nodeId);

        // Then
        assertThat(registry.getNode(nodeId)).isNull();
        verify(executorNodeMapper).update(any(), any());
        verify(healthChecker).unregisterNode(nodeId);
    }

    @Test
    void heartbeat_shouldUpdateNodeAndPersistStats() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);
        String nodeId = node.getNodeId();

        // When
        ExecutorNode updated = registry.heartbeat(nodeId);

        // Then
        assertThat(updated).isNotNull();
        assertThat(updated.getLastHeartbeat()).isNotNull();
        verify(healthChecker).heartbeat(node);
        verify(executorNodeMapper).updateHeartbeat(
                eq(nodeId),
                any(),
                any(LocalDateTime.class),
                anyInt(),
                anyLong(),
                anyLong(),
                anyLong(),
                any(LocalDateTime.class)
        );
    }

    @Test
    void heartbeat_shouldHandleNonExistentNode() {
        // When
        ExecutorNode result = registry.heartbeat("non-existent");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getNode_shouldReturnRegisteredNode() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);

        // When
        ExecutorNode retrieved = registry.getNode(node.getNodeId());

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getNodeId()).isEqualTo(node.getNodeId());
    }

    @Test
    void getAllNodes_shouldReturnAllRegisteredNodes() {
        // Given
        registry.register("Node 1", "192.168.1.100", 8080, 10);
        registry.register("Node 2", "192.168.1.101", 8081, 10);
        registry.register("Node 3", "192.168.1.102", 8082, 10);

        // When
        List<ExecutorNode> nodes = registry.getAllNodes();

        // Then
        assertThat(nodes).hasSize(3);
    }

    @Test
    void getHealthyNodes_shouldReturnHealthyNodesFromChecker() {
        // Given
        ExecutorNode node1 = ExecutorNode.of("node-1", "Node 1", "192.168.1.100", 8080);
        ExecutorNode node2 = ExecutorNode.of("node-2", "Node 2", "192.168.1.101", 8081);
        when(healthChecker.getHealthyNodes()).thenReturn(List.of(node1, node2));

        // When
        List<ExecutorNode> healthy = registry.getHealthyNodes();

        // Then
        assertThat(healthy).hasSize(2);
        verify(healthChecker).getHealthyNodes();
    }

    @Test
    void getNodesByCapability_shouldFilterCorrectly() {
        // Given
        ExecutorNode node1 = registry.register("Node 1", "192.168.1.100", 8080, 10);
        node1.addCapability("python");
        node1.addCapability("docker");

        ExecutorNode node2 = registry.register("Node 2", "192.168.1.101", 8081, 10);
        node2.addCapability("nodejs");

        ExecutorNode node3 = registry.register("Node 3", "192.168.1.102", 8082, 10);
        node3.addCapability("python");

        // When
        List<ExecutorNode> pythonNodes = registry.getNodesByCapability("python");

        // Then
        assertThat(pythonNodes).hasSize(2);
        assertThat(pythonNodes).allMatch(n -> n.hasCapability("python"));
    }

    @Test
    void updateNodeStatus_shouldUpdateStatus() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);
        String nodeId = node.getNodeId();

        // When
        ExecutorNode updated = registry.updateNodeStatus(nodeId, "busy");

        // Then
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo("busy");
        verify(executorNodeMapper).update(any(), any());
    }

    @Test
    void updateNodeCapabilities_shouldReplaceCapabilities() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);
        node.addCapability("old-cap");

        // When
        registry.updateNodeCapabilities(node.getNodeId(), List.of("new-cap1", "new-cap2"));

        // Then
        assertThat(node.getCapabilities()).containsExactlyInAnyOrder("new-cap1", "new-cap2");
        assertThat(node.getCapabilities()).doesNotContain("old-cap");
    }

    @Test
    void updateNodeWeight_shouldChangeWeight() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);

        // When
        registry.updateNodeWeight(node.getNodeId(), 75);

        // Then
        assertThat(node.getWeight()).isEqualTo(75);
    }

    @Test
    void updateNodeMaxConcurrentTasks_shouldChangeLimit() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);

        // When
        registry.updateNodeMaxConcurrentTasks(node.getNodeId(), 20);

        // Then
        assertThat(node.getMaxConcurrentTasks()).isEqualTo(20);
    }

    @Test
    void taskStarted_shouldIncrementCounter() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);

        // When
        ExecutorNode updated = registry.taskStarted(node.getNodeId());

        // Then
        assertThat(updated).isNotNull();
        assertThat(updated.getCurrentTasks().get()).isEqualTo(1);
    }

    @Test
    void taskCompleted_shouldDecrementCounter() {
        // Given
        ExecutorNode node = registry.register("Test Node", "192.168.1.100", 8080, 10);
        registry.taskStarted(node.getNodeId());

        // When
        ExecutorNode updated = registry.taskCompleted(node.getNodeId());

        // Then
        assertThat(updated).isNotNull();
        assertThat(updated.getCurrentTasks().get()).isEqualTo(0);
    }

    @Test
    void getTotalNodes_shouldReturnCorrectCount() {
        // Given
        registry.register("Node 1", "192.168.1.100", 8080, 10);
        registry.register("Node 2", "192.168.1.101", 8081, 10);

        // When
        int total = registry.getTotalNodes();

        // Then
        assertThat(total).isEqualTo(2);
    }

    @Test
    void getHealthyNodesCount_shouldReturnCorrectCount() {
        // Given
        when(healthChecker.getHealthyNodes()).thenReturn(List.of(
                ExecutorNode.of("n1", "N1", "host1", 8080),
                ExecutorNode.of("n2", "N2", "host2", 8081)
        ));

        // When
        int count = registry.getHealthyNodesCount();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void taskOperations_onNonExistentNode_shouldNotThrow() {
        // When/Then
        assertThat(registry.taskStarted("non-existent")).isNull();
        assertThat(registry.taskCompleted("non-existent")).isNull();
        assertThat(registry.updateNodeStatus("non-existent", "busy")).isNull();
    }
}

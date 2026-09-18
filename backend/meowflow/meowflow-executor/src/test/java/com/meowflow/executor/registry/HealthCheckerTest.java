package com.meowflow.executor.registry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HealthChecker Tests")
class HealthCheckerTest {

    private HealthChecker checker;

    @BeforeEach
    void setUp() {
        checker = new HealthChecker();
    }

    @AfterEach
    void tearDown() {
        checker.shutdown();
    }

    @Test
    @DisplayName("test")
    void registerNode_newNode_addsToRegistry() {
        ExecutorNode node = ExecutorNode.of("node-1", "Node 1", "127.0.0.1", 8080);
        checker.registerNode(node);

        assertThat(checker.getNode("node-1")).isSameAs(node);
        assertThat(checker.getAllNodes()).containsKey("node-1");
    }

    @Test
    @DisplayName("unregisterNode - 娉ㄩ攢鑺傜偣")
    void unregisterNode_existingNode_removesFromRegistry() {
        ExecutorNode node = ExecutorNode.of("node-1", "Node 1", "127.0.0.1", 8080);
        checker.registerNode(node);

        checker.unregisterNode("node-1");

        assertThat(checker.getNode("node-1")).isNull();
    }

    @Test
    @DisplayName("test")
    void unregisterNode_nonExisting_doesNotThrow() {
        checker.unregisterNode("nonexistent");
    }

    @Test
    @DisplayName("test")
    void heartbeat_existingNode_updatesHeartbeat() throws InterruptedException {
        ExecutorNode node = ExecutorNode.of("node-1", "Node 1", "127.0.0.1", 8080);
        LocalDateTime beforeHb = node.getLastHeartbeat();
        checker.registerNode(node);

        Thread.sleep(10);
        checker.heartbeat(node);

        assertThat(node.getLastHeartbeat()).isAfterOrEqualTo(beforeHb);
    }

    @Test
    @DisplayName("getHealthyNodes - 杩斿洖娲昏穬鑺傜偣")
    void getHealthyNodes_returnsActiveNodes() {
        ExecutorNode activeNode = ExecutorNode.of("active", "Active", "127.0.0.1", 8080);
        activeNode.setStatus(ExecutorNode.NodeStatus.ACTIVE);
        checker.registerNode(activeNode);

        ExecutorNode inactiveNode = ExecutorNode.of("inactive", "Inactive", "127.0.0.1", 8081);
        inactiveNode.setStatus(ExecutorNode.NodeStatus.INACTIVE);
        checker.registerNode(inactiveNode);

        List<ExecutorNode> healthy = checker.getHealthyNodes();

        assertThat(healthy).hasSize(1);
        assertThat(healthy.get(0).getNodeId()).isEqualTo("active");
    }

    @Test
    @DisplayName("test")
    void getAllNodes_returnsAllRegistered() {
        checker.registerNode(ExecutorNode.of("n1", "N1", "127.0.0.1", 8080));
        checker.registerNode(ExecutorNode.of("n2", "N2", "127.0.0.1", 8081));

        assertThat(checker.getAllNodes()).hasSize(2);
    }
}
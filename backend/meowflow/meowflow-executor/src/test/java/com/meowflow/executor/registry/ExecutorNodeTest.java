package com.meowflow.executor.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorNodeTest {

    private ExecutorNode node;

    @BeforeEach
    void setUp() {
        node = ExecutorNode.of("node-1", "Test Node", "localhost", 8080);
    }

    @Test
    void of_shouldCreateNodeWithDefaults() {
        assertNotNull(node);
        assertEquals("node-1", node.getNodeId());
        assertEquals("Test Node", node.getName());
        assertEquals("localhost", node.getHost());
        assertEquals(8080, node.getPort());
        assertEquals(ExecutorNode.NodeStatus.ACTIVE, node.getStatus());
        assertEquals(100, node.getWeight());
        assertEquals(10, node.getMaxConcurrentTasks());
        assertNotNull(node.getCurrentTasks());
        assertEquals(0, node.getCurrentTasks().get());
        assertNotNull(node.getCapabilities());
        assertNotNull(node.getProperties());
        assertEquals(0, node.getLoadFactor());
    }

    @Test
    void heartbeat_shouldUpdateLastHeartbeat() throws InterruptedException {
        LocalDateTime before = node.getLastHeartbeat();
        Thread.sleep(10);
        node.heartbeat();
        LocalDateTime after = node.getLastHeartbeat();

        assertTrue(after.isAfter(before) || after.isEqual(before));
    }

    @Test
    void isHealthy_shouldReturnTrueForActiveNodeWithRecentHeartbeat() {
        node.setLastHeartbeat(LocalDateTime.now());
        node.setStatus(ExecutorNode.NodeStatus.ACTIVE);

        assertTrue(node.isHealthy());
    }

    @Test
    void isHealthy_shouldReturnFalseForInactiveNode() {
        node.setStatus(ExecutorNode.NodeStatus.INACTIVE);

        assertFalse(node.isHealthy());
    }

    @Test
    void isHealthy_shouldReturnFalseForStaleHeartbeat() {
        node.setLastHeartbeat(LocalDateTime.now().minusSeconds(120));
        node.setStatus(ExecutorNode.NodeStatus.ACTIVE);

        assertFalse(node.isHealthy());
    }

    @Test
    void canAcceptTask_shouldReturnTrueWhenHealthyAndHasCapacity() {
        node.setStatus(ExecutorNode.NodeStatus.ACTIVE);
        node.setLastHeartbeat(LocalDateTime.now());
        node.getCurrentTasks().set(5);

        assertTrue(node.canAcceptTask());
    }

    @Test
    void canAcceptTask_shouldReturnFalseWhenAtMaxCapacity() {
        node.setStatus(ExecutorNode.NodeStatus.ACTIVE);
        node.setLastHeartbeat(LocalDateTime.now());
        node.getCurrentTasks().set(10);

        assertFalse(node.canAcceptTask());
    }

    @Test
    void taskStarted_shouldIncrementCurrentTasks() {
        assertEquals(0, node.getCurrentTasks().get());

        node.taskStarted();

        assertEquals(1, node.getCurrentTasks().get());
        assertTrue(node.getLoadFactor() > 0);
    }

    @Test
    void taskCompleted_shouldDecrementCurrentTasks() {
        node.getCurrentTasks().set(3);

        node.taskCompleted();

        assertEquals(2, node.getCurrentTasks().get());
    }

    @Test
    void addCapability_shouldAddToCapabilities() {
        assertTrue(node.getCapabilities().isEmpty());

        node.addCapability("python");
        node.addCapability("javascript");

        assertEquals(2, node.getCapabilities().size());
        assertTrue(node.getCapabilities().contains("python"));
        assertTrue(node.getCapabilities().contains("javascript"));
    }

    @Test
    void hasCapability_shouldReturnCorrectValue() {
        node.addCapability("python");

        assertTrue(node.hasCapability("python"));
        assertFalse(node.hasCapability("java"));
    }

    @Test
    void getUrl_shouldReturnCorrectUrl() {
        assertEquals("http://localhost:8080", node.getUrl());
    }

    @Test
    void loadFactor_shouldBeCalculatedCorrectly() {
        node.setMaxConcurrentTasks(10);
        node.getCurrentTasks().set(5);

        node.heartbeat();

        assertEquals(0.5, node.getLoadFactor(), 0.001);
    }

    @Test
    void multipleTasks_shouldTrackCorrectly() {
        node.taskStarted();
        node.taskStarted();
        node.taskStarted();

        assertEquals(3, node.getCurrentTasks().get());

        node.taskCompleted();
        node.taskCompleted();

        assertEquals(1, node.getCurrentTasks().get());
    }
}

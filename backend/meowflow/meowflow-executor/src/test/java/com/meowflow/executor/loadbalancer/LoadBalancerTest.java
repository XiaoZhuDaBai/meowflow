package com.meowflow.executor.loadbalancer;

import com.meowflow.executor.registry.ExecutorNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerTest {

    private List<ExecutorNode> testNodes;

    @BeforeEach
    void setUp() {
        testNodes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ExecutorNode node = ExecutorNode.of("node-" + i, "Node " + i, "localhost", 8080 + i);
            testNodes.add(node);
        }
    }

    @Test
    void roundRobin_shouldSelectInOrder() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();

        ExecutorNode first = balancer.select(testNodes);
        ExecutorNode second = balancer.select(testNodes);
        ExecutorNode third = balancer.select(testNodes);
        ExecutorNode fourth = balancer.select(testNodes);

        assertEquals("node-0", first.getNodeId());
        assertEquals("node-1", second.getNodeId());
        assertEquals("node-2", third.getNodeId());
        assertEquals("node-0", fourth.getNodeId());
    }

    @Test
    void roundRobin_shouldReturnNullForEmptyList() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        ExecutorNode result = balancer.select(Collections.emptyList());
        assertNull(result);
    }

    @Test
    void roundRobin_shouldReturnNullForNullList() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        ExecutorNode result = balancer.select(null);
        assertNull(result);
    }

    @Test
    void roundRobin_shouldReturnSingleNode() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        ExecutorNode result = balancer.select(List.of(testNodes.get(0)));
        assertEquals("node-0", result.getNodeId());
    }

    @Test
    void roundRobin_shouldReset() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        balancer.select(testNodes);
        balancer.select(testNodes);
        balancer.reset();
        ExecutorNode result = balancer.select(testNodes);
        assertEquals("node-0", result.getNodeId());
    }

    @Test
    void roundRobin_getName_shouldReturnCorrectName() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        assertEquals("round-robin", balancer.getName());
    }

    @Test
    void random_shouldReturnValidNode() {
        RandomLoadBalancer balancer = new RandomLoadBalancer();
        for (int i = 0; i < 100; i++) {
            ExecutorNode result = balancer.select(testNodes);
            assertNotNull(result);
            assertTrue(result.getNodeId().startsWith("node-"));
        }
    }

    @Test
    void random_shouldReturnNullForEmptyList() {
        RandomLoadBalancer balancer = new RandomLoadBalancer();
        ExecutorNode result = balancer.select(Collections.emptyList());
        assertNull(result);
    }

    @Test
    void random_shouldReturnNullForNullList() {
        RandomLoadBalancer balancer = new RandomLoadBalancer();
        ExecutorNode result = balancer.select(null);
        assertNull(result);
    }

    @Test
    void random_shouldReturnSingleNode() {
        RandomLoadBalancer balancer = new RandomLoadBalancer();
        ExecutorNode result = balancer.select(List.of(testNodes.get(0)));
        assertEquals("node-0", result.getNodeId());
    }

    @Test
    void random_getName_shouldReturnCorrectName() {
        RandomLoadBalancer balancer = new RandomLoadBalancer();
        assertEquals("random", balancer.getName());
    }

    @Test
    void loadBalancerInterface_shouldHaveSelectMethod() {
        LoadBalancer balancer = new RoundRobinLoadBalancer();
        assertDoesNotThrow(() -> balancer.select(testNodes));
        assertDoesNotThrow(() -> balancer.getName());
    }
}

package com.meowflow.executor.service;

import com.meowflow.executor.loadbalancer.LoadBalancer;
import com.meowflow.executor.loadbalancer.RandomLoadBalancer;
import com.meowflow.executor.loadbalancer.RoundRobinLoadBalancer;
import com.meowflow.executor.model.Task;
import com.meowflow.executor.registry.ExecutorNode;
import com.meowflow.executor.service.ExecutorNodeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskDispatchServiceTest {

    @Mock
    private ExecutorNodeRegistry nodeRegistry;

    private TaskDispatchService dispatchService;

    @BeforeEach
    void setUp() throws Exception {
        dispatchService = new TaskDispatchService(nodeRegistry);

        Field loadBalancersField = TaskDispatchService.class.getDeclaredField("loadBalancers");
        loadBalancersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, LoadBalancer> balancers = (Map<String, LoadBalancer>) loadBalancersField.get(dispatchService);
        balancers.put("random", new RandomLoadBalancer());
        balancers.put("round-robin", new RoundRobinLoadBalancer());
    }

    @Test
    void setDistributedMode_shouldToggleState() {
        dispatchService.setDistributedMode(true);
        assertTrue(dispatchService.isDistributedMode());

        dispatchService.setDistributedMode(false);
        assertFalse(dispatchService.isDistributedMode());
    }

    @Test
    void getLoadBalancer_registered_returnsInstance() {
        LoadBalancer balancer = dispatchService.getLoadBalancer("random");
        assertNotNull(balancer);
        assertEquals("random", balancer.getName());
    }

    @Test
    void getLoadBalancer_unknown_returnsNull() {
        assertNull(dispatchService.getLoadBalancer("unknown"));
    }

    @Test
    void getAllLoadBalancers_returnsRegistered() {
        Map<String, LoadBalancer> balancers = dispatchService.getAllLoadBalancers();
        assertNotNull(balancers);
        assertTrue(balancers.containsKey("random"));
        assertTrue(balancers.containsKey("round-robin"));
    }

    @Test
    void handleDeadLetterTask_doesNotThrow() {
        com.meowflow.executor.mq.TaskMessage msg = com.meowflow.executor.mq.TaskMessage.builder()
                .taskId(1L)
                .taskIdentifier("task-1")
                .retryCount(3)
                .build();
        assertDoesNotThrow(() -> dispatchService.handleDeadLetterTask(msg));
    }

    @Test
    void notifyTaskCompleted_invokesRegistry() {
        dispatchService.notifyTaskCompleted("node-1");
        verify(nodeRegistry).taskCompleted("node-1");
    }

    @Test
    void notifyTaskFailed_invokesRegistry() {
        dispatchService.notifyTaskFailed("node-1");
        verify(nodeRegistry).taskCompleted("node-1");
    }

    @Test
    void executeTask_invokesRegistry() {
        com.meowflow.executor.mq.TaskMessage msg = com.meowflow.executor.mq.TaskMessage.builder()
                .taskId(1L)
                .taskIdentifier("task-1")
                .targetNodeId("n1")
                .build();
        ExecutorNode node = ExecutorNode.of("n1", "Node1", "127.0.0.1", 8080);
        when(nodeRegistry.getNode("n1")).thenReturn(node);

        dispatchService.executeTask(msg);

        verify(nodeRegistry).taskStarted("n1");
    }

    private Task createTask(String taskId) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setMaxRetries(3);
        task.setRetryCount(0);
        task.setPriority(50);
        task.setTimeoutMs(300000L);
        return task;
    }
}

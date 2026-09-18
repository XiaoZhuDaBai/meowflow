package com.meowflow.executor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.executor.model.Task;
import com.meowflow.executor.model.TaskQueue;
import com.meowflow.executor.model.TaskStatus;
import com.meowflow.executor.model.TaskType;
import com.meowflow.executor.registry.ExecutorNode;
import com.meowflow.executor.service.ExecutorNodeRegistry;
import com.meowflow.executor.service.TaskDispatchService;
import com.meowflow.executor.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExecutorController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ExecutorController HTTP 层")
class ExecutorControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TaskService taskService;
    @MockBean
    private TaskDispatchService dispatchService;
    @MockBean
    private ExecutorNodeRegistry nodeRegistry;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/executor/nodes/register")
    void registerNode_invokesRegistry() throws Exception {
        ExecutorNode node = new ExecutorNode();
        node.setNodeId("n-1");
        when(nodeRegistry.register(anyString(), anyString(), anyInt(), anyInt())).thenReturn(node);

        mockMvc.perform(post("/api/executor/nodes/register")
                        .contentType("application/json")
                        .content("{\"name\":\"node-1\",\"host\":\"127.0.0.1\",\"port\":8080,\"maxConcurrentTasks\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodeId").value("n-1"));
    }

    @Test
    @DisplayName("DELETE /api/executor/nodes/{nodeId}")
    void unregisterNode_invokesRegistry() throws Exception {
        mockMvc.perform(delete("/api/executor/nodes/n-1"))
                .andExpect(status().isOk());
        verify(nodeRegistry).unregister("n-1");
    }

    @Test
    @DisplayName("POST /api/executor/nodes/{nodeId}/heartbeat")
    void heartbeat_invokesRegistry() throws Exception {
        ExecutorNode node = new ExecutorNode();
        node.setNodeId("n-1");
        when(nodeRegistry.heartbeat(anyString())).thenReturn(node);

        mockMvc.perform(post("/api/executor/nodes/n-1/heartbeat"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/executor/nodes")
    void listNodes_invokesRegistry() throws Exception {
        when(nodeRegistry.getAllNodes()).thenReturn(List.of(new ExecutorNode()));

        mockMvc.perform(get("/api/executor/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/executor/nodes/healthy")
    void listHealthyNodes_invokesRegistry() throws Exception {
        when(nodeRegistry.getHealthyNodes()).thenReturn(List.of());

        mockMvc.perform(get("/api/executor/nodes/healthy"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/executor/nodes/{nodeId}")
    void getNode_invokesRegistry() throws Exception {
        ExecutorNode node = new ExecutorNode();
        when(nodeRegistry.getNode("n-1")).thenReturn(node);

        mockMvc.perform(get("/api/executor/nodes/n-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/executor/nodes/{nodeId}/capabilities")
    void updateCapabilities_invokesRegistry() throws Exception {
        mockMvc.perform(put("/api/executor/nodes/n-1/capabilities")
                        .contentType("application/json")
                        .content("[\"llm\",\"http\"]"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/executor/tasks")
    void createTask_invokesService() throws Exception {
        Task task = new Task();
        task.setTaskId("t-1");
        when(taskService.createTask(anyString(), any(), any())).thenReturn(task);

        mockMvc.perform(post("/api/executor/tasks")
                        .contentType("application/json")
                        .content("{\"name\":\"t\",\"type\":\"WORKFLOW_NODE\",\"params\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("t-1"));
    }

    @Test
    @DisplayName("POST /api/executor/tasks/{taskId}/submit")
    void submitTask_invokesService() throws Exception {
        Task task = new Task();
        task.setTaskId("t-1");
        when(taskService.getTask("t-1")).thenReturn(task);
        when(taskService.submitTask(any())).thenReturn(task);

        mockMvc.perform(post("/api/executor/tasks/t-1/submit"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/executor/tasks/{taskId}/start")
    void startTask_invokesService() throws Exception {
        Task task = new Task();
        when(taskService.getTask("t-1")).thenReturn(task);

        mockMvc.perform(post("/api/executor/tasks/t-1/start"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/executor/tasks/{taskId}/complete")
    void completeTask_invokesService() throws Exception {
        mockMvc.perform(post("/api/executor/tasks/t-1/complete")
                        .contentType("application/json")
                        .content("{\"result\":{\"k\":\"v\"}}"))
                .andExpect(status().isOk());
        verify(taskService).completeTask(anyString(), any());
    }

    @Test
    @DisplayName("POST /api/executor/tasks/{taskId}/fail")
    void failTask_invokesService() throws Exception {
        mockMvc.perform(post("/api/executor/tasks/t-1/fail")
                        .contentType("application/json")
                        .content("{\"errorMessage\":\"oops\"}"))
                .andExpect(status().isOk());
        verify(taskService).failTask(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/executor/tasks/{taskId}/cancel")
    void cancelTask_invokesService() throws Exception {
        mockMvc.perform(post("/api/executor/tasks/t-1/cancel"))
                .andExpect(status().isOk());
        verify(taskService).cancelTask(anyString());
    }

    @Test
    @DisplayName("GET /api/executor/tasks/{taskId}")
    void getTask_invokesService() throws Exception {
        Task task = new Task();
        when(taskService.getTask("t-1")).thenReturn(task);

        mockMvc.perform(get("/api/executor/tasks/t-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/executor/tasks")
    void listTasks_invokesService() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of());

        mockMvc.perform(get("/api/executor/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/executor/tasks?status=PENDING")
    void listTasks_byStatus_invokesService() throws Exception {
        when(taskService.getTasksByStatus(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/executor/tasks?status=PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/executor/tasks/{taskId}")
    void deleteTask_invokesService() throws Exception {
        mockMvc.perform(delete("/api/executor/tasks/t-1"))
                .andExpect(status().isOk());
        verify(taskService).deleteTask("t-1");
    }

    @Test
    @DisplayName("GET /api/executor/queues")
    void listQueues_invokesService() throws Exception {
        when(taskService.getAllQueues()).thenReturn(List.of(new TaskQueue("q-1", "Test Queue", TaskType.HTTP_REQUEST)));

        mockMvc.perform(get("/api/executor/queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/executor/queues/{queueId}/pause")
    void pauseQueue_invokesService() throws Exception {
        mockMvc.perform(post("/api/executor/queues/q-1/pause"))
                .andExpect(status().isOk());
        verify(taskService).pauseQueue("q-1");
    }

    @Test
    @DisplayName("POST /api/executor/queues/{queueId}/resume")
    void resumeQueue_invokesService() throws Exception {
        mockMvc.perform(post("/api/executor/queues/q-1/resume"))
                .andExpect(status().isOk());
        verify(taskService).resumeQueue("q-1");
    }

    @Test
    @DisplayName("GET /api/executor/status - 自包含调度状态")
    void status_invokesRegistry() throws Exception {
        when(nodeRegistry.getTotalNodes()).thenReturn(3);
        when(nodeRegistry.getHealthyNodesCount()).thenReturn(2);
        when(taskService.getAllTasks()).thenReturn(List.of());
        when(taskService.getTasksByStatus(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/executor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNodes").value(3))
                .andExpect(jsonPath("$.data.healthyNodes").value(2));
    }

    @Test
    @DisplayName("GET /api/executor/health")
    void health_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/executor/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}

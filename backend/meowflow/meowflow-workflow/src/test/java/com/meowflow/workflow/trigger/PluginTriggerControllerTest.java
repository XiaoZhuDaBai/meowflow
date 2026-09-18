package com.meowflow.workflow.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.redis.RedisService;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import com.meowflow.workflow.service.ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PluginTriggerControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper mapper;
    private WorkflowRepository workflowRepository;
    private WorkflowVersionRepository versionRepository;
    private ExecutionService executionService;
    private PluginRegistryService pluginRegistryService;
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        workflowRepository = mock(WorkflowRepository.class);
        versionRepository = mock(WorkflowVersionRepository.class);
        executionService = mock(ExecutionService.class);
        pluginRegistryService = mock(PluginRegistryService.class);
        redisService = mock(RedisService.class);

        PluginTriggerController controller = new PluginTriggerController(
                workflowRepository,
                versionRepository,
                executionService,
                pluginRegistryService,
                redisService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerDelegatesToRegistry() throws Exception {
        when(pluginRegistryService.register(anyMap()))
                .thenReturn(Map.of("pluginId", "p1", "status", "active"));

        mockMvc.perform(post("/api/plugin/register")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("pluginId", "p1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pluginId").value("p1"));
    }

    @Test
    void listReturnsPlugins() throws Exception {
        when(pluginRegistryService.list()).thenReturn(List.of(Map.of("pluginId", "p1")));

        mockMvc.perform(get("/api/plugin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pluginId").value("p1"));
    }

    @Test
    void getReturnsPlugin() throws Exception {
        when(pluginRegistryService.get("p1")).thenReturn(Map.of("pluginId", "p1"));

        mockMvc.perform(get("/api/plugin/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pluginId").value("p1"));
    }

    @Test
    void updateStatusReturnsUpdatedPlugin() throws Exception {
        when(pluginRegistryService.updateStatus("p1", "disabled"))
                .thenReturn(Map.of("pluginId", "p1", "status", "disabled"));

        mockMvc.perform(put("/api/plugin/p1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"disabled\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disabled"));
    }

    @Test
    void deleteReturns404WhenPluginMissing() throws Exception {
        when(pluginRegistryService.delete("missing")).thenReturn(false);

        mockMvc.perform(delete("/api/plugin/missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void triggerUsesRegisteredWorkflow() throws Exception {
        when(pluginRegistryService.get("p1"))
                .thenReturn(Map.of("pluginId", "p1", "workflowId", 10));

        mockMvc.perform(post("/api/plugin/trigger/p1")
                        .contentType("application/json")
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.workflowId").value(10));
    }

    @Test
    void triggerRejectsInactivePlugin() throws Exception {
        when(pluginRegistryService.get("p1"))
                .thenReturn(Map.of("pluginId", "p1", "status", "inactive"));

        mockMvc.perform(post("/api/plugin/trigger/p1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void triggerRejectsDuplicateEvent() throws Exception {
        when(pluginRegistryService.get("p1"))
                .thenReturn(Map.of("pluginId", "p1", "workflowId", 10));
        when(redisService.tryLock(eq("plugin:dedup:event-1"), eq(300L))).thenReturn(false);

        mockMvc.perform(post("/api/plugin/trigger/p1")
                        .header("X-Plugin-Event-Id", "event-1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }
}

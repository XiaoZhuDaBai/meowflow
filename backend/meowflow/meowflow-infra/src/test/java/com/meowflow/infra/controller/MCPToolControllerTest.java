package com.meowflow.infra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.infra.mcp.MCPTool;
import com.meowflow.infra.service.MCPToolService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MCPToolController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MCPToolController HTTP 层")
class MCPToolControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private MCPToolService mcpToolService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /mcp/tools")
    void registerTool_invokesService() throws Exception {
        MCPToolController.MCPToolRequest req = new MCPToolController.MCPToolRequest();
        req.setName("mytool");
        req.setDescription("d");
        req.setProvider("test");
        req.setAdapterType("stdio");
        req.setEndpoint("");

        mockMvc.perform(post("/api/infra/mcp/tools")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(mcpToolService).registerTool(any(MCPTool.class));
    }

    @Test
    @DisplayName("DELETE /mcp/tools/{name}")
    void unregister_invokesService() throws Exception {
        mockMvc.perform(delete("/api/infra/mcp/tools/mytool"))
                .andExpect(status().isOk());
        verify(mcpToolService).unregisterTool("mytool");
    }

    @Test
    @DisplayName("GET /mcp/tools/{name}")
    void getTool_invokesService() throws Exception {
        when(mcpToolService.getTool("mytool")).thenReturn(MCPTool.of("mytool", "d", "p", "stdio"));

        mockMvc.perform(get("/api/infra/mcp/tools/mytool"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /mcp/tools")
    void listTools_invokesService() throws Exception {
        when(mcpToolService.getAllTools()).thenReturn(List.of(MCPTool.of("mytool", "d", "p", "stdio")));

        mockMvc.perform(get("/api/infra/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /mcp/tools?provider=test")
    void listToolsByProvider_invokesService() throws Exception {
        when(mcpToolService.getToolsByProvider("test")).thenReturn(List.of());

        mockMvc.perform(get("/api/infra/mcp/tools?provider=test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /mcp/tools/{name}/execute")
    void executeTool_invokesService() throws Exception {
        when(mcpToolService.executeTool(anyString(), any())).thenReturn(Map.of("result", "ok"));

        mockMvc.perform(post("/api/infra/mcp/tools/mytool/execute")
                        .contentType("application/json")
                        .content("{\"p1\":\"v1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /mcp/tools/{name}/enable")
    void enable_invokesService() throws Exception {
        mockMvc.perform(put("/api/infra/mcp/tools/mytool/enable"))
                .andExpect(status().isOk());
        verify(mcpToolService).enableTool("mytool");
    }

    @Test
    @DisplayName("PUT /mcp/tools/{name}/disable")
    void disable_invokesService() throws Exception {
        mockMvc.perform(put("/api/infra/mcp/tools/mytool/disable"))
                .andExpect(status().isOk());
        verify(mcpToolService).disableTool("mytool");
    }
}

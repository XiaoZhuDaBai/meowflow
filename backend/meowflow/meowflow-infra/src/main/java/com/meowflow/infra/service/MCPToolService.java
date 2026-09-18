package com.meowflow.infra.service;

import com.meowflow.infra.mcp.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具服务
 */
@Slf4j
@Service
public class MCPToolService {

    private final MCPToolRegistry registry;

    public MCPToolService(MCPToolRegistry registry) {
        this.registry = registry;
    }

    public void registerTool(MCPTool tool) {
        registry.registerTool(tool);
    }

    public void unregisterTool(String name) {
        registry.unregisterTool(name);
    }

    public MCPTool getTool(String name) {
        return registry.getTool(name);
    }

    public List<MCPTool> getAllTools() {
        return registry.getAllTools();
    }

    public List<MCPTool> getToolsByProvider(String provider) {
        return registry.getToolsByProvider(provider);
    }

    public Object executeTool(String toolName, Map<String, Object> params) {
        return registry.execute(toolName, params);
    }

    public boolean isToolAvailable(String name) {
        return registry.isToolAvailable(name);
    }

    public void enableTool(String name) {
        registry.enableTool(name);
    }

    public void disableTool(String name) {
        registry.disableTool(name);
    }

    public MCPTool createStdioTool(String name, String description, String provider, String command) {
        MCPTool tool = MCPTool.of(name, description, provider, "stdio");
        tool.setEndpoint(command);
        return tool;
    }

    public MCPTool createSseTool(String name, String description, String provider, String endpoint, Map<String, String> headers) {
        MCPTool tool = MCPTool.of(name, description, provider, "sse");
        tool.setEndpoint(endpoint);
        tool.setHeaders(headers);
        return tool;
    }
}

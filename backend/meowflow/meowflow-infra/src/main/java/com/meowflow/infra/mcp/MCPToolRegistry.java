package com.meowflow.infra.mcp;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具注册中心
 */
@Slf4j
@Component
public class MCPToolRegistry {

    private final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
    private final Map<String, MCPToolExecutor> executors = new ConcurrentHashMap<>();

    public void registerTool(MCPTool tool) {
        tools.put(tool.getName(), tool);
        MCPToolExecutor executor = createExecutor(tool);
        executors.put(tool.getName(), executor);
        log.info("Registered MCP tool: {} from provider: {}", tool.getName(), tool.getProvider());
    }

    public void unregisterTool(String name) {
        tools.remove(name);
        executors.remove(name);
        log.info("Unregistered MCP tool: {}", name);
    }

    public MCPTool getTool(String name) {
        return tools.get(name);
    }

    public List<MCPTool> getAllTools() {
        return tools.values().stream().toList();
    }

    public List<MCPTool> getToolsByProvider(String provider) {
        return tools.values().stream()
                .filter(t -> t.getProvider().equals(provider))
                .toList();
    }

    public MCPToolExecutor getExecutor(String name) {
        MCPToolExecutor executor = executors.get(name);
        if (executor == null) {
            MCPTool tool = tools.get(name);
            if (tool == null) {
                throw new BizException(ResultCode.DATA_NOT_FOUND, "MCP 工具不存在: " + name);
            }
            executor = createExecutor(tool);
            executors.put(name, executor);
        }
        return executor;
    }

    public Object execute(String toolName, Map<String, Object> params) {
        MCPToolExecutor executor = getExecutor(toolName);
        return executor.execute(params);
    }

    private MCPToolExecutor createExecutor(MCPTool tool) {
        return switch (tool.getAdapterType().toLowerCase()) {
            case "stdio" -> new com.meowflow.infra.mcp.stdio.StdioAdapter(tool);
            case "sse" -> new com.meowflow.infra.mcp.sse.SseAdapter(tool);
            default -> throw new BizException(ResultCode.PARAM_ERROR, "不支持的 MCP 适配器类型: " + tool.getAdapterType());
        };
    }

    public boolean isToolAvailable(String name) {
        MCPTool tool = tools.get(name);
        return tool != null && tool.isEnabled();
    }

    public void enableTool(String name) {
        MCPTool tool = tools.get(name);
        if (tool != null) {
            tool.setEnabled(true);
        }
    }

    public void disableTool(String name) {
        MCPTool tool = tools.get(name);
        if (tool != null) {
            tool.setEnabled(false);
        }
    }
}

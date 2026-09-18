package com.meowflow.infra.mcp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具定义
 */
@Data
public class MCPTool implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private String provider;
    private String adapterType;
    private String endpoint;
    private List<ToolParameter> parameters;
    private Map<String, String> headers;
    private boolean enabled;

    @Data
    public static class ToolParameter implements Serializable {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private String defaultValue;
    }

    public static MCPTool of(String name, String description, String provider, String adapterType) {
        MCPTool tool = new MCPTool();
        tool.setName(name);
        tool.setDescription(description);
        tool.setProvider(provider);
        tool.setAdapterType(adapterType);
        tool.setEnabled(true);
        return tool;
    }
}

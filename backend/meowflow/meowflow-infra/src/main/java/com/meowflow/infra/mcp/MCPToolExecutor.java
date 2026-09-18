package com.meowflow.infra.mcp;

import java.util.Map;

/**
 * MCP 工具执行器接口
 */
public interface MCPToolExecutor {

    /**
     * 执行工具
     *
     * @param params 参数
     * @return 执行结果
     */
    Object execute(Map<String, Object> params);

    /**
     * 获取工具定义
     */
    MCPTool getTool();

    /**
     * 健康检查
     */
    boolean isHealthy();
}

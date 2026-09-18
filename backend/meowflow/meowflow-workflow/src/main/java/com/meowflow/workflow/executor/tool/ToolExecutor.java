package com.meowflow.workflow.executor.tool;

import com.meowflow.common.exception.NodeException;
import com.meowflow.infra.service.MCPToolService;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 工具节点执行器。
 * <p>
 * 通过 {@link MCPToolService} 调用已注册的 MCP 工具。
 * 配置参数：
 * <ul>
 *   <li>toolName (String): 工具名称（必需）</li>
 *   <li>input (Map): 工具输入参数</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutor extends AbstractNodeExecutor {

    private final MCPToolService mcpToolService;

    @Override
    public NodeType getNodeType() {
        return NodeType.TOOL;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String toolName = resolveToString(input.get("toolName"), context);
        if (toolName == null || toolName.isEmpty()) {
            throw new NodeException(node.getId(), node.getType().getCode(), "toolName is required for tool node");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> toolInput = (Map<String, Object>) input.get("input");

        if (toolInput != null) {
            toolInput = context.resolveMap(toolInput);
        }

        log.info("MCP 工具执行: tool={}, params={}", toolName, toolInput);

        Object result;
        try {
            result = mcpToolService.executeTool(toolName, toolInput != null ? toolInput : new HashMap<>());
        } catch (Exception e) {
            log.error("MCP 工具执行失败: tool={}, error={}", toolName, e.getMessage(), e);
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "MCP 工具执行失败: " + e.getMessage(), e);
        }

        Map<String, Object> output = new HashMap<>();
        output.put("toolName", toolName);
        output.put("result", result);
        output.put("executed", true);

        log.info("MCP 工具执行成功: tool={}", toolName);

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private String resolveToString(Object value, ExecutionContext context) {
        if (value == null) return null;
        if (value instanceof String s) {
            Object resolved = context.resolveExpression(s);
            return resolved != null ? resolved.toString() : null;
        }
        return String.valueOf(value);
    }
}

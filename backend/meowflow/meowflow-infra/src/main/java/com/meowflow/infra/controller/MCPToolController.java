package com.meowflow.infra.controller;

import com.meowflow.common.result.Result;
import com.meowflow.infra.mcp.MCPTool;
import com.meowflow.infra.service.MCPToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/infra/mcp")
@Tag(name = "MCP 工具", description = "MCP 工具注册与管理接口")
public class MCPToolController {

    private final MCPToolService mcpToolService;

    public MCPToolController(MCPToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    @PostMapping("/tools")
    @Operation(summary = "注册 MCP 工具", description = "注册一个新的 MCP 工具")
    public Result<Void> registerTool(@RequestBody MCPToolRequest request) {
        MCPTool tool = MCPTool.of(request.getName(), request.getDescription(),
                request.getProvider(), request.getAdapterType());
        tool.setEndpoint(request.getEndpoint());
        mcpToolService.registerTool(tool);
        return Result.success();
    }

    @DeleteMapping("/tools/{name}")
    @Operation(summary = "注销 MCP 工具", description = "注销指定的 MCP 工具")
    public Result<Void> unregisterTool(@PathVariable String name) {
        mcpToolService.unregisterTool(name);
        return Result.success();
    }

    @GetMapping("/tools/{name}")
    @Operation(summary = "获取 MCP 工具", description = "获取指定工具的信息")
    public Result<MCPTool> getTool(@PathVariable String name) {
        return Result.success(mcpToolService.getTool(name));
    }

    @GetMapping("/tools")
    @Operation(summary = "列出 MCP 工具", description = "列出所有已注册的 MCP 工具")
    public Result<List<MCPTool>> listTools(
            @Parameter(description = "按提供者筛选") @RequestParam(required = false) String provider) {
        if (provider != null && !provider.isEmpty()) {
            return Result.success(mcpToolService.getToolsByProvider(provider));
        }
        return Result.success(mcpToolService.getAllTools());
    }

    @PostMapping("/tools/{name}/execute")
    @Operation(summary = "执行 MCP 工具", description = "执行指定的 MCP 工具")
    public Result<Object> executeTool(
            @PathVariable String name,
            @RequestBody Map<String, Object> params) {
        Object result = mcpToolService.executeTool(name, params);
        return Result.success(result);
    }

    @PutMapping("/tools/{name}/enable")
    @Operation(summary = "启用工具", description = "启用指定的 MCP 工具")
    public Result<Void> enableTool(@PathVariable String name) {
        mcpToolService.enableTool(name);
        return Result.success();
    }

    @PutMapping("/tools/{name}/disable")
    @Operation(summary = "禁用工具", description = "禁用指定的 MCP 工具")
    public Result<Void> disableTool(@PathVariable String name) {
        mcpToolService.disableTool(name);
        return Result.success();
    }

    @GetMapping("/tools/{name}/available")
    @Operation(summary = "检查工具可用性", description = "检查指定工具是否可用")
    public Result<Boolean> isToolAvailable(@PathVariable String name) {
        return Result.success(mcpToolService.isToolAvailable(name));
    }

    @Data
    public static class MCPToolRequest {
        @Parameter(description = "工具名称")
        private String name;

        @Parameter(description = "工具描述")
        private String description;

        @Parameter(description = "提供者")
        private String provider;

        @Parameter(description = "适配器类型: stdio/sse")
        private String adapterType;

        @Parameter(description = "端点地址或命令")
        private String endpoint;
    }
}

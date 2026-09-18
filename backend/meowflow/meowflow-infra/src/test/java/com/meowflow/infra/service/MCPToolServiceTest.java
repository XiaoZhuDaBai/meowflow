package com.meowflow.infra.service;

import com.meowflow.infra.mcp.MCPTool;
import com.meowflow.infra.mcp.MCPToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MCPToolService 单元测试
 */
@DisplayName("MCPToolService Tests")
class MCPToolServiceTest {

    private MCPToolRegistry registry;
    private MCPToolService service;

    @BeforeEach
    void setUp() {
        registry = new MCPToolRegistry();
        service = new MCPToolService(registry);
    }

    @Nested
    @DisplayName("registerTool Tests")
    class RegisterToolTests {

        @Test
        @DisplayName("Should register a STDIO tool successfully")
        void shouldRegisterStdioTool() {
            MCPTool tool = MCPTool.of("search-tool", "搜索工具", "test-provider", "stdio");
            tool.setEndpoint("npx search-tool");

            service.registerTool(tool);

            MCPTool registered = service.getTool("search-tool");
            assertThat(registered).isNotNull();
            assertThat(registered.getName()).isEqualTo("search-tool");
            assertThat(registered.getAdapterType()).isEqualTo("stdio");
        }

        @Test
        @DisplayName("Should register an SSE tool successfully")
        void shouldRegisterSseTool() {
            MCPTool tool = MCPTool.of("api-tool", "API工具", "api-provider", "sse");
            tool.setEndpoint("https://api.example.com/mcp");
            tool.setHeaders(Map.of("Authorization", "Bearer token123"));

            service.registerTool(tool);

            MCPTool registered = service.getTool("api-tool");
            assertThat(registered).isNotNull();
            assertThat(registered.getAdapterType()).isEqualTo("sse");
            assertThat(registered.getEndpoint()).isEqualTo("https://api.example.com/mcp");
        }
    }

    @Nested
    @DisplayName("unregisterTool Tests")
    class UnregisterToolTests {

        @Test
        @DisplayName("Should unregister existing tool")
        void shouldUnregisterExistingTool() {
            MCPTool tool = MCPTool.of("test-tool", "测试工具", "provider", "stdio");
            service.registerTool(tool);

            service.unregisterTool("test-tool");

            assertThat(service.getTool("test-tool")).isNull();
        }

        @Test
        @DisplayName("Should handle unregistering non-existent tool gracefully")
        void shouldHandleUnregisterNonExistentTool() {
            service.unregisterTool("non-existent-tool");

            assertThat(service.getTool("non-existent-tool")).isNull();
        }
    }

    @Nested
    @DisplayName("listTools Tests")
    class ListToolsTests {

        @Test
        @DisplayName("Should return all registered tools")
        void shouldReturnAllTools() {
            service.registerTool(MCPTool.of("tool-1", "工具1", "provider", "stdio"));
            service.registerTool(MCPTool.of("tool-2", "工具2", "provider", "sse"));
            service.registerTool(MCPTool.of("tool-3", "工具3", "other-provider", "stdio"));

            List<MCPTool> allTools = service.getAllTools();

            assertThat(allTools).hasSize(3);
        }

        @Test
        @DisplayName("Should filter tools by provider")
        void shouldFilterToolsByProvider() {
            service.registerTool(MCPTool.of("tool-1", "工具1", "provider-A", "stdio"));
            service.registerTool(MCPTool.of("tool-2", "工具2", "provider-A", "sse"));
            service.registerTool(MCPTool.of("tool-3", "工具3", "provider-B", "stdio"));

            List<MCPTool> providerATools = service.getToolsByProvider("provider-A");

            assertThat(providerATools).hasSize(2);
            assertThat(providerATools).allMatch(t -> t.getProvider().equals("provider-A"));
        }
    }

    @Nested
    @DisplayName("toolAvailability Tests")
    class ToolAvailabilityTests {

        @Test
        @DisplayName("Should report tool as available after registration")
        void shouldReportToolAvailable() {
            MCPTool tool = MCPTool.of("test-tool", "测试工具", "provider", "stdio");
            service.registerTool(tool);

            assertThat(service.isToolAvailable("test-tool")).isTrue();
        }

        @Test
        @DisplayName("Should report tool as unavailable after disabling")
        void shouldReportToolUnavailableAfterDisabling() {
            MCPTool tool = MCPTool.of("test-tool", "测试工具", "provider", "stdio");
            service.registerTool(tool);

            service.disableTool("test-tool");

            assertThat(service.isToolAvailable("test-tool")).isFalse();
        }

        @Test
        @DisplayName("Should re-enable disabled tool")
        void shouldReEnableDisabledTool() {
            MCPTool tool = MCPTool.of("test-tool", "测试工具", "provider", "stdio");
            service.registerTool(tool);
            service.disableTool("test-tool");

            service.enableTool("test-tool");

            assertThat(service.isToolAvailable("test-tool")).isTrue();
        }
    }

    @Nested
    @DisplayName("createTool Tests")
    class CreateToolTests {

        @Test
        @DisplayName("Should create STDIO tool with factory method")
        void shouldCreateStdioTool() {
            MCPTool tool = service.createStdioTool(
                    "my-tool", "我的工具", "my-provider", "node my-tool.js"
            );

            assertThat(tool).isNotNull();
            assertThat(tool.getName()).isEqualTo("my-tool");
            assertThat(tool.getAdapterType()).isEqualTo("stdio");
            assertThat(tool.getEndpoint()).isEqualTo("node my-tool.js");
            assertThat(tool.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should create SSE tool with headers")
        void shouldCreateSseToolWithHeaders() {
            Map<String, String> headers = Map.of(
                    "Authorization", "Bearer token123",
                    "X-API-Key", "key456"
            );

            MCPTool tool = service.createSseTool(
                    "sse-tool", "SSE工具", "sse-provider",
                    "https://api.example.com/mcp", headers
            );

            assertThat(tool).isNotNull();
            assertThat(tool.getAdapterType()).isEqualTo("sse");
            assertThat(tool.getHeaders()).containsEntry("Authorization", "Bearer token123");
        }
    }
}

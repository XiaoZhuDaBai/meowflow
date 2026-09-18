package com.meowflow.infra.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.infra.client.ChatClient.ChatOptions;
import com.meowflow.infra.client.ChatClient.ChatResponse;
import com.meowflow.infra.client.ChatClient.Message;
import com.meowflow.infra.service.ChatClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP 工具参数提取器 - 使用 LLM 自动从用户问题中提取工具参数
 * 借鉴 ragent 的 LLMMcpParameterExtractor 设计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMMcpParameterExtractor {

    private final ChatClientFactory chatClientFactory;
    private final ObjectMapper objectMapper;
    
    // 默认参数提取使用的模型
    private static final String DEFAULT_EXTRACTION_MODEL = "gpt-4o-mini";
    
    // 提取参数的 System Prompt
    private static final String PARAM_EXTRACTION_SYSTEM_PROMPT = """
        You are a tool parameter extraction assistant. Your task is to extract parameters from user queries for tool calls.
        
        Given a tool definition and user question, you must output a valid JSON object containing the extracted parameters.
        
        Rules:
        1. Output ONLY valid JSON, no other text or explanation
        2. Include all required parameters
        3. For optional parameters, include only if mentioned in the question
        4. Use null for parameters that cannot be determined
        5. All string values must be properly quoted
        
        Example:
        Tool: {"name": "get_weather", "parameters": {"city": {"type": "string"}, "days": {"type": "integer", "default": 1}}}
        Question: "What's the weather in Beijing tomorrow?"
        Output: {"city": "Beijing", "days": 1}
        """;

    /**
     * 从用户问题中提取工具参数
     *
     * @param userQuestion 用户问题
     * @param tool MCP 工具定义
     * @return 提取的参数 Map
     */
    public Map<String, Object> extractParameters(String userQuestion, MCPTool tool) {
        return extractParameters(userQuestion, tool, null);
    }

    /**
     * 从用户问题中提取工具参数（支持自定义提示词）
     *
     * @param userQuestion 用户问题
     * @param tool MCP 工具定义
     * @param customPromptTemplate 自定义提示词模板（可选）
     * @return 提取的参数 Map
     */
    public Map<String, Object> extractParameters(String userQuestion, MCPTool tool, String customPromptTemplate) {
        String systemPrompt = customPromptTemplate != null ? customPromptTemplate : PARAM_EXTRACTION_SYSTEM_PROMPT;
        
        String toolDefinition = buildToolDefinitionJson(tool);
        String userPrompt = String.format("""
            Tool: %s
            Question: %s
            Output:
            """, toolDefinition, userQuestion);
        
        List<Message> messages = List.of(
            Message.system(systemPrompt),
            Message.user(userPrompt)
        );
        
        try {
            ChatResponse response = chatClientFactory.chat(
                DEFAULT_EXTRACTION_MODEL,
                messages,
                ChatOptions.of(0.1, 256) // 低温度确保稳定性
            );
            
            return parseJsonResponse(response.content(), tool);
        } catch (Exception e) {
            log.error("Failed to extract parameters for tool {}: {}", tool.getName(), e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 构建工具定义 JSON 字符串
     */
    private String buildToolDefinitionJson(MCPTool tool) {
        try {
            Map<String, Object> toolDef = new HashMap<>();
            toolDef.put("name", tool.getName());
            toolDef.put("description", tool.getDescription());
            
            Map<String, Object> parameters = new HashMap<>();
            if (tool.getParameters() != null) {
                for (MCPTool.ToolParameter param : tool.getParameters()) {
                    Map<String, Object> paramDef = new HashMap<>();
                    paramDef.put("type", param.getType());
                    if (param.getDescription() != null) {
                        paramDef.put("description", param.getDescription());
                    }
                    if (param.isRequired()) {
                        paramDef.put("required", true);
                    }
                    if (param.getDefaultValue() != null) {
                        paramDef.put("default", param.getDefaultValue());
                    }
                    parameters.put(param.getName(), paramDef);
                }
            }
            toolDef.put("parameters", parameters);
            
            return objectMapper.writeValueAsString(toolDef);
        } catch (Exception e) {
            log.error("Failed to build tool definition JSON: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 解析 LLM 返回的 JSON 响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String jsonContent, MCPTool tool) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 清理响应内容
            String cleanedJson = cleanJsonResponse(jsonContent);
            
            if (cleanedJson == null || cleanedJson.isEmpty()) {
                return result;
            }
            
            JsonNode root = objectMapper.readTree(cleanedJson);
            
            if (tool.getParameters() != null) {
                for (MCPTool.ToolParameter param : tool.getParameters()) {
                    JsonNode value = root.get(param.getName());
                    if (value != null && !value.isNull()) {
                        result.put(param.getName(), convertValue(value, param.getType()));
                    } else if (param.getDefaultValue() != null) {
                        result.put(param.getName(), convertValue(param.getDefaultValue(), param.getType()));
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to parse JSON response: {}", e.getMessage());
        }
        
        return result;
    }

    /**
     * 清理 JSON 响应（移除 markdown 代码块等）
     */
    private String cleanJsonResponse(String content) {
        if (content == null) return null;
        
        // 移除 markdown 代码块标记
        content = content.replaceAll("```json\\s*", "");
        content = content.replaceAll("```\\s*", "");
        content = content.trim();
        
        // 如果内容以大括号包裹，直接返回
        if (content.startsWith("{") && content.endsWith("}")) {
            return content;
        }
        
        // 尝试提取 JSON 部分
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }

    /**
     * 类型转换
     */
    private Object convertValue(Object value, String type) {
        if (value == null) return null;
        
        String strValue = value.toString();
        
        return switch (type.toLowerCase()) {
            case "integer", "int" -> {
                try {
                    yield Integer.parseInt(strValue.replaceAll("[^0-9-]", ""));
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            case "number", "float", "double" -> {
                try {
                    yield Double.parseDouble(strValue.replaceAll("[^0-9.-]", ""));
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            case "boolean", "bool" -> {
                yield Boolean.parseBoolean(strValue);
            }
            default -> strValue;
        };
    }

    /**
     * 验证参数是否完整
     */
    public boolean validateParameters(Map<String, Object> params, MCPTool tool) {
        if (tool.getParameters() == null) {
            return true;
        }
        
        for (MCPTool.ToolParameter param : tool.getParameters()) {
            if (param.isRequired() && (!params.containsKey(param.getName()) || params.get(param.getName()) == null)) {
                log.warn("Missing required parameter: {} for tool: {}", param.getName(), tool.getName());
                return false;
            }
        }
        
        return true;
    }

    /**
     * 获取缺失的必填参数
     */
    public List<String> getMissingRequiredParams(Map<String, Object> params, MCPTool tool) {
        if (tool.getParameters() == null) {
            return List.of();
        }
        
        return tool.getParameters().stream()
            .filter(param -> param.isRequired() && (!params.containsKey(param.getName()) || params.get(param.getName()) == null))
            .map(MCPTool.ToolParameter::getName)
            .toList();
    }
}

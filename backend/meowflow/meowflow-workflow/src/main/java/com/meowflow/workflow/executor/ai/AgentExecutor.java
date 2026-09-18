package com.meowflow.workflow.executor.ai;

import com.meowflow.common.util.JsonUtils;
import com.meowflow.infra.agent.AgentSessionService;
import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.infra.chat.Message;
import com.meowflow.infra.service.MCPToolService;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AgentExecutor extends AbstractAIExecutor {

    private final MCPToolService mcpToolService;
    private final AgentSessionService sessionService;

    public AgentExecutor(ChatModelGateway gateway, 
                        MCPToolService mcpToolService,
                        AgentSessionService sessionService) {
        super(gateway);
        this.mcpToolService = mcpToolService;
        this.sessionService = sessionService;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.AGENT;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String query = resolveString(input.getOrDefault("query", input.get("input")), context);
        String instruction = resolveString(input.get("instruction"), context);
        String strategy = input.get("strategy") != null ? input.get("strategy").toString() : "function-calling";
        Object toolsValue = input.get("tools");
        String tools = toolsValue instanceof List
                ? String.join(", ", ((List<?>) toolsValue).stream().map(String::valueOf).toList())
                : toolsValue != null ? toolsValue.toString() : "";
        Integer maxIterations = parseInt(input.get("maxIterations"), 5);
        
        // 会话记忆支持
        String sessionId = input.get("sessionId") != null ? input.get("sessionId").toString() : null;
        Boolean enableMemory = input.get("enableMemory") != null 
                ? Boolean.parseBoolean(input.get("enableMemory").toString()) 
                : false;
        Integer maxRounds = parseInt(input.get("maxRounds"), 10);
        
        // 获取或创建会话
        if (Boolean.TRUE.equals(enableMemory)) {
            Long userId = null; // TODO: Extract userId from context if available
            String agentNodeId = node.getId();
            sessionId = sessionService.getOrCreateSession(userId, agentNodeId, sessionId);
            log.info("Agent using session: {}", sessionId);
        }

        if ("function-calling".equalsIgnoreCase(strategy)) {
            List<Map<String, Object>> toolSchemas = buildToolSchemas();
            if (!toolSchemas.isEmpty()) {
                return executeNative(context, node, input, query, instruction, toolSchemas, maxIterations, 
                                   sessionId, enableMemory, maxRounds);
            }
        }

        // ReAct 策略执行（带会话记忆）
        return executeReAct(context, node, input, query, instruction, tools, maxIterations,
                          sessionId, enableMemory, maxRounds);
    }

    /**
     * ReAct 策略执行（JSON 格式）
     */
    private NodeResult executeReAct(ExecutionContext context, NodeDefinition node, Map<String, Object> input,
                                   String query, String instruction, String tools, Integer maxIterations,
                                   String sessionId, Boolean enableMemory, Integer maxRounds) {
        String system = "You are an agent. Respond ONLY with JSON: "
                + "{\"action\":\"tool\",\"name\":\"...\",\"arguments\":{...}} or "
                + "{\"action\":\"final\",\"answer\":\"...\"}.";
        String user = "Tools available: " + tools
                + "\nInstruction: " + (instruction != null ? instruction : "")
                + "\nQuery: " + query
                + "\nMax iterations: " + maxIterations;

        List<Message> messages = new ArrayList<>();
        
        // 加载会话历史
        if (Boolean.TRUE.equals(enableMemory) && sessionId != null) {
            messages.addAll(sessionService.getSessionHistory(sessionId, maxRounds));
            log.info("Loaded {} messages from session {}", messages.size(), sessionId);
        }
        
        messages.add(Message.system(system));
        messages.add(Message.user(user));
        
        // 保存用户消息到会话
        if (Boolean.TRUE.equals(enableMemory) && sessionId != null) {
            sessionService.appendUserMessage(sessionId, query);
        }
        
        List<Map<String, Object>> toolOutputs = new ArrayList<>();
        List<String> reasoning = new ArrayList<>();
        String answer = "";

        for (int i = 0; i < maxIterations; i++) {
            ChatResponse response = complete(context, input, messages);
            reasoning.add(response.getContent());
            Map<String, Object> parsed = extractJsonObject(response.getContent());

            if (!"tool".equalsIgnoreCase(String.valueOf(parsed.get("action")))
                    || parsed.get("name") == null) {
                answer = parsed.get("answer") != null
                        ? parsed.get("answer").toString()
                        : response.getContent();
                break;
            }

            String toolName = parsed.get("name").toString();
            Object arguments = parsed.get("arguments");
            Map<String, Object> args = arguments instanceof Map
                    ? (Map<String, Object>) arguments
                    : Map.of();
            Object result;
            try {
                result = mcpToolService.executeTool(toolName, args);
            } catch (Exception e) {
                result = Map.of("error", e.getMessage());
            }
            toolOutputs.add(Map.of("name", toolName, "result", result));
            messages.add(Message.assistant(response.getContent()));
            messages.add(Message.user("Tool result: " + JsonUtils.toJson(result)));
            
            // 保存工具调用到会话
            if (Boolean.TRUE.equals(enableMemory) && sessionId != null) {
                sessionService.appendToolMessage(sessionId, toolName, result);
            }
        }
        if (answer.isBlank() && !reasoning.isEmpty()) {
            answer = reasoning.get(reasoning.size() - 1);
        }
        
        // 保存 Assistant 回复到会话
        if (Boolean.TRUE.equals(enableMemory) && sessionId != null) {
            sessionService.appendAssistantMessage(sessionId, answer);
        }

        Map<String, Object> output = new HashMap<>();
        output.put("text", answer);
        output.put("answer", answer);
        output.put("toolOutputs", toolOutputs);
        output.put("reasoning", reasoning);
        output.put("iterations", toolOutputs.size());
        output.put("sessionId", sessionId);
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output)
                .withCostToken(toolOutputs.size() * 100);
    }

    /**
     * Function-calling 策略执行（原生工具调用）
     */
    private NodeResult executeNative(ExecutionContext context,
                                     NodeDefinition node,
                                     Map<String, Object> input,
                                     String query,
                                     String instruction,
                                     List<Map<String, Object>> toolSchemas,
                                     Integer maxIterations,
                                     String sessionId,
                                     Boolean enableMemory,
                                     Integer maxRounds) {
        List<Message> messages = new ArrayList<>();
        
        // 加载会话历史
        if (Boolean.TRUE.equals(enableMemory) && sessionId != null) {
            messages.addAll(sessionService.getSessionHistory(sessionId, maxRounds));
            log.info("Loaded {} messages from session {}", messages.size(), sessionId);
        }
        
        messages.add(Message.system("You are an agent. Use tools when needed."));
        messages.add(Message.user("Instruction: " + (instruction != null ? instruction : "")
                + "\nQuery: " + query));
        
        // 保存用户消息到会话
        if (Boolean.TRUE.equals(enableMemory) && sessionId != null) {
            sessionService.appendUserMessage(sessionId, query);
        }
        
        List<Map<String, Object>> toolOutputs = new ArrayList<>();
        List<String> reasoning = new ArrayList<>();
        String answer = "";

        for (int i = 0; i < maxIterations; i++) {
            Map<String, Object> extraParams = Map.of(
                    "tools", toolSchemas,
                    "tool_choice", "auto"
            );
            ChatResponse response = complete(context, input, messages, extraParams);
            reasoning.add(response.getContent());

            List<Map<String, Object>> toolCalls = response.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                answer = response.getContent();
                break;
            }

            messages.add(Message.assistantWithToolCalls(response.getContent(), toolCalls));
            for (Map<String, Object> call : toolCalls) {
                Object functionObj = call.get("function");
                if (!(functionObj instanceof Map<?, ?> function)) continue;
                String toolName = function.get("name") != null ? function.get("name").toString() : "";
                Map<String, Object> args = parseArguments(function.get("arguments"));
                Object result;
                try {
                    result = mcpToolService.executeTool(toolName, args);
                } catch (Exception e) {
                    result = Map.of("error", e.getMessage());
                }
                toolOutputs.add(Map.of("name", toolName, "result", result));
                messages.add(Message.tool(toolName, JsonUtils.toJson(result)));
                
                // 保存工具调用到会话
                if (Boolean.TRUE.equals(enableMemory) && sessionId != null) {
                    sessionService.appendToolMessage(sessionId, toolName, result);
                }
            }
        }
        if (answer.isBlank() && !reasoning.isEmpty()) {
            answer = reasoning.get(reasoning.size() - 1);
        }
        
        // 保存 Assistant 回复到会话
        if (Boolean.TRUE.equals(enableMemory) && sessionId != null) {
            sessionService.appendAssistantMessage(sessionId, answer);
        }

        Map<String, Object> output = new HashMap<>();
        output.put("text", answer);
        output.put("answer", answer);
        output.put("toolOutputs", toolOutputs);
        output.put("reasoning", reasoning);
        output.put("iterations", toolOutputs.size());
        output.put("strategy", "function-calling");
        output.put("sessionId", sessionId);
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output)
                .withCostToken(toolOutputs.size() * 100);
    }

    private List<Map<String, Object>> buildToolSchemas() {
        return mcpToolService.getAllTools().stream()
                .map(tool -> {
                    Map<String, Object> properties = new HashMap<>();
                    List<String> required = new ArrayList<>();
                    if (tool.getParameters() != null) {
                        for (var param : tool.getParameters()) {
                            properties.put(param.getName(), Map.of(
                                    "type", param.getType() != null ? param.getType() : "string",
                                    "description", param.getDescription() != null ? param.getDescription() : ""
                            ));
                            if (param.isRequired()) required.add(param.getName());
                        }
                    }
                    return Map.<String, Object>of(
                            "type", "function",
                            "function", Map.of(
                                    "name", tool.getName(),
                                    "description", tool.getDescription() != null ? tool.getDescription() : "",
                                    "parameters", Map.of(
                                            "type", "object",
                                            "properties", properties,
                                            "required", required
                                    )
                            )
                    );
                })
                .toList();
    }

    private Map<String, Object> parseArguments(Object value) {
        if (value instanceof Map) return (Map<String, Object>) value;
        if (value != null && !value.toString().isBlank()) {
            return extractJsonObject(value.toString());
        }
        return Map.of();
    }
}

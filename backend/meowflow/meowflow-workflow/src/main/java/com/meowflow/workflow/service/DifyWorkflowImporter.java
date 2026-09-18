package com.meowflow.workflow.service;

import com.meowflow.common.util.JsonUtils;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dify DSL 到 MeowFlow 工作流定义的转换器。
 */
public final class DifyWorkflowImporter {

    private DifyWorkflowImporter() {
    }

    public record ImportedDify(String name, String description, WorkflowDefinitionRequest definition) {
    }

    public static ImportedDify tryImport(String json) {
        if (json == null || json.isBlank()) return null;
        Map<String, Object> root = JsonUtils.fromJsonToMap(json);
        if (root == null) return null;

        Object appObj = root.get("app");
        Object workflowObj = root.get("workflow");
        if (!(appObj instanceof Map) || !(workflowObj instanceof Map)) return null;

        Map<?, ?> app = (Map<?, ?>) appObj;
        Map<?, ?> workflow = (Map<?, ?>) workflowObj;
        Object graphObj = workflow.get("graph");
        if (!(graphObj instanceof Map)) return null;
        Map<?, ?> graph = (Map<?, ?>) graphObj;

        String name = stringValue(app.get("name"), "Dify 导入工作流");
        String description = stringValue(app.get("description"), "");

        WorkflowDefinitionRequest definition = new WorkflowDefinitionRequest();
        definition.setVersion("v1");
        definition.setNodes(convertNodes(graph.get("nodes")));
        definition.setEdges(convertEdges(graph.get("edges")));
        definition.setVariables(convertVariables(workflow.get("environment_variables")));
        definition.setInputSchema(new HashMap<>());
        definition.setOutputSchema(new HashMap<>());
        definition.setChangelog("从 Dify DSL 导入");
        return new ImportedDify(name, description, definition);
    }

    private static List<WorkflowDefinitionRequest.NodeDefinitionDto> convertNodes(Object raw) {
        List<WorkflowDefinitionRequest.NodeDefinitionDto> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) return result;

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            WorkflowDefinitionRequest.NodeDefinitionDto dto = new WorkflowDefinitionRequest.NodeDefinitionDto();
            dto.setId(stringValue(map.get("id"), "node_" + result.size()));
            dto.setType(mapNodeType(stringValue(map.get("type"), null)));
            dto.setName(stringValue(map.get("title"), dto.getType()));

            WorkflowDefinitionRequest.PositionDto position = new WorkflowDefinitionRequest.PositionDto();
            if (map.get("position") instanceof Map<?, ?> pos) {
                position.setX(numberValue(pos.get("x"), result.size() * 280));
                position.setY(numberValue(pos.get("y"), 120));
            } else {
                position.setX(result.size() * 280);
                position.setY(120);
            }
            dto.setPosition(position);

            Map<String, Object> data = new HashMap<>();
            if (map.get("data") instanceof Map<?, ?> sourceData) {
                data.putAll((Map<String, Object>) sourceData);
            }
            dto.setData(data);
            result.add(dto);
        }
        return result;
    }

    private static List<WorkflowDefinitionRequest.EdgeDto> convertEdges(Object raw) {
        List<WorkflowDefinitionRequest.EdgeDto> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) return result;

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            WorkflowDefinitionRequest.EdgeDto dto = new WorkflowDefinitionRequest.EdgeDto();
            dto.setId(stringValue(map.get("id"), "edge_" + result.size()));
            dto.setSource(stringValue(map.get("source"), ""));
            dto.setTarget(stringValue(map.get("target"), ""));
            dto.setSourceHandle(stringValue(map.get("sourceHandle"), null));
            dto.setTargetHandle(stringValue(map.get("targetHandle"), null));
            dto.setType("default");

            WorkflowDefinitionRequest.EdgeDataDto data = new WorkflowDefinitionRequest.EdgeDataDto();
            if (map.get("data") instanceof Map<?, ?> edgeData) {
                data.setLabel(stringValue(edgeData.get("label"), null));
                if (edgeData.get("config") instanceof Map<?, ?> config) {
                    data.setConfig((Map<String, Object>) config);
                }
            }
            dto.setData(data);
            result.add(dto);
        }
        return result;
    }

    private static Map<String, Object> convertVariables(Object raw) {
        if (!(raw instanceof List<?> list)) return new HashMap<>();
        Map<String, Object> variables = new HashMap<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            Object key = map.get("id");
            Object value = map.get("value");
            if (key != null) variables.put(key.toString(), value);
        }
        return variables;
    }

    private static String mapNodeType(String difyType) {
        if (difyType == null) return "note";
        return switch (difyType) {
            case "start" -> "trigger.manual";
            case "end", "answer" -> "end";
            case "llm" -> "ai.llm";
            case "knowledge-retrieval" -> "knowledge.search";
            case "question-classifier" -> "ai.question-classifier";
            case "if-else" -> "flow.if-else";
            case "code" -> "tool.code";
            case "template-transform" -> "flow.template-transform";
            case "http-request" -> "tool.http";
            case "tool" -> "tool.mcp";
            case "variable-aggregator" -> "flow.aggregation";
            case "variable-assigner" -> "tool.variable-assigner";
            case "iteration" -> "flow.iteration";
            case "parameter-extractor" -> "ai.parameter-extractor";
            case "agent" -> "ai.agent";
            case "document-extractor" -> "tool.document-extractor";
            case "list-operator" -> "tool.list-operator";
            default -> "note";
        };
    }

    private static String stringValue(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }

    private static double numberValue(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

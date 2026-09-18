package com.meowflow.workflow.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {

    private String workflowId;

    private String version;

    private String changelog;

    private List<NodeDefinition> nodes;

    private List<Edge> edges;

    private Map<String, Object> variables;

    private Map<String, Object> inputSchema;

    private Map<String, Object> outputSchema;

    private Map<String, Object> config;

    public NodeDefinition findNode(String nodeId) {
        if (nodes == null) return null;
        return nodes.stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    public List<NodeDefinition> getTriggerNodes() {
        if (nodes == null) return List.of();
        return nodes.stream()
                .filter(NodeDefinition::isTrigger)
                .toList();
    }

    public List<NodeDefinition> getEndNodes() {
        if (nodes == null) return List.of();
        return nodes.stream()
                .filter(NodeDefinition::isEnd)
                .toList();
    }
}


package com.meowflow.workflow.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeDefinition {

    private String id;

    private NodeType type;

    private String name;

    private Position position;

    private Map<String, Object> data;

    private String[] inputs;

    private String[] outputs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private double x;
        private double y;
    }

    public boolean isTrigger() {
        return type != null && type.isTrigger();
    }

    public boolean isEnd() {
        return type != null && type.isEnd();
    }
}

package com.meowflow.common.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 前端 ReactFlow 编辑器使用的工作流 JSON。
 * <p>node 中 position 字段是 ReactFlow 的固有约定，与 {@link WorkflowFullDefinition} 的 x/y 互转。
 * <p>edge.type 取值：default / straight / step / smoothstep / condition / loop / error。
 *
 * @author MeowFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditorWorkflowJson {

    private String version;
    private List<Node> nodes;
    private List<Edge> edges;
    private Map<String, Object> variables;
    private Map<String, Object> viewport;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private String id;
        private String type;
        private String name;
        private Position position;
        private Map<String, Object> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private Double x;
        private Double y;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edge {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private String targetHandle;
        private String type;
        private String label;
        private Map<String, Object> data;
    }
}

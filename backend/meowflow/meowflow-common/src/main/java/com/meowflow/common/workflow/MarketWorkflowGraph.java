package com.meowflow.common.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模板市场与详情页使用的轻量工作流图（直接渲染 SVG）。
 * <p>与 {@link WorkflowFullDefinition} / {@link EditorWorkflowJson} 的关键差异：
 * <ul>
 *   <li>节点矩形直接给出 cx/cy/w/h，无需执行器进一步归一化</li>
 *   <li>无拖拽端口、监听器、schema 等冗余字段</li>
 *   <li>edge 仅保留 id/source/target/label，便于曲线绘制</li>
 * </ul>
 *
 * @author MeowFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketWorkflowGraph {

    private String version;
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphNode {
        private String id;
        private String type;
        private String name;
        private String description;
        private String category;
        private String icon;
        /** 矩形中心点 X */
        private Double cx;
        /** 矩形中心点 Y */
        private Double cy;
        /** 节点宽（与 ReactFlow / 引擎一致 240） */
        private Double w;
        /** 节点高（与 ReactFlow / 引擎一致 60） */
        private Double h;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEdge {
        private String id;
        private String source;
        private String target;
        private String label;
        private String type;
        private Integer sourceIndex;
        private Integer targetIndex;
    }
}

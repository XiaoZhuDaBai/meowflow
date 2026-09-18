package com.meowflow.common.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流引擎侧完整定义（执行器、版本管理）。
 * <p>这也是 <code>mf_wf_workflow_version.definition JSONB</code> 列的标准结构。
 * <p>字段命名采用 UI 友好（小写驼峰）。前端编辑器、模板市场的 JSON 都能无损转换为它。
 *
 * @author MeowFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowFullDefinition {

    /** Schema 版本标识，默认 "v1" */
    private String version;

    /** 节点列表，节点坐标存放于 {@code x} / {@code y}（与 ReactFlow 一致） */
    private List<Node> nodes;

    /** 连线列表 */
    private List<Edge> edges;

    /** 全局变量 */
    private Map<String, Object> variables;

    /** 输入 schema */
    private Map<String, Object> inputSchema;

    /** 输出 schema */
    private Map<String, Object> outputSchema;

    /** 监听器、并发等运行配置 */
    private Map<String, Object> config;

    /** 节点类型枚举值 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private String id;
        private String type;        // trigger / ai.llm / http.request / condition.if / ...
        private String name;        // 显示名
        private String description; // 描述（编辑器悬停提示）
        private Double x;           // 画布 X，与前端保持一致
        private Double y;           // 画布 Y
        private String category;    // 节点分类（ui 着色）
        private String icon;        // icon class (fontawesome / emoji)
        private Map<String, Object> data;       // 节点参数
        private String[] inputs;     // 输入端口 id
        private String[] outputs;    // 输出端口 id
    }

    /** 连线定义 */
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
        private String type;        // default / condition / loop / error
        private String label;
        private Integer sourceIndex;
        private Integer targetIndex;
        private Map<String, Object> data; // 条件等附挂信息
    }
}

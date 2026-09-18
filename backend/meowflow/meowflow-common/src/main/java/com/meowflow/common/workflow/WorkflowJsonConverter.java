package com.meowflow.common.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 三套工作流 JSON 之间的转换器。
 *
 * <p>目标：在"执行器 JSON"、"编辑器 JSON"、"模板市场 JSON"三种表示间无歧义互转，
 * 并在三方协议中保留节点 (id/type/name/坐标/参数) 与连线 (source/target/type/label)。
 *
 * <p>使用 {@link JsonUtils}（meowflow-common 共享 Jackson 实例）保证时区、命名策略一致。
 *
 * @author MeowFlow Team
 */
public final class WorkflowJsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WorkflowJsonConverter() {
    }

    public static WorkflowFullDefinition toFullDefinition(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return MAPPER.readValue(json, WorkflowFullDefinition.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid workflow JSON: " + e.getMessage(), e);
        }
    }

    public static EditorWorkflowJson toEditorJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return MAPPER.readValue(json, EditorWorkflowJson.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid editor JSON: " + e.getMessage(), e);
        }
    }

    public static MarketWorkflowGraph toMarketGraph(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return MAPPER.readValue(json, MarketWorkflowGraph.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid market graph: " + e.getMessage(), e);
        }
    }

    /**
     * 将任意 schema 的 JSON 转换成 {@link WorkflowFullDefinition}。
     * <p>能够识别：{@link WorkflowFullDefinition} / {@link EditorWorkflowJson} / {@link MarketWorkflowGraph}
     * 以及它们的 JSON string 等价物。
     */
    public static WorkflowFullDefinition normalize(String json) {
        if (json == null || json.isEmpty()) return null;
        WorkflowFullDefinition direct = tryParse(json, WorkflowFullDefinition.class);
        if (direct != null && (direct.getNodes() != null || direct.getEdges() != null)) {
            return direct;
        }
        EditorWorkflowJson editor = tryParse(json, EditorWorkflowJson.class);
        if (editor != null) {
            return fromEditor(editor);
        }
        MarketWorkflowGraph market = tryParse(json, MarketWorkflowGraph.class);
        if (market != null) {
            return fromMarket(market);
        }
        throw new IllegalArgumentException("Unsupported workflow JSON shape");
    }

    /**
     * 将 {@link WorkflowFullDefinition} 转成模板市场 {@link MarketWorkflowGraph}。
     * <p>用于详情页预览：执行器 JSON 经过归一化后交给前端画 SVG。
     */
    public static MarketWorkflowGraph toMarketGraph(WorkflowFullDefinition def, double nodeWidth, double nodeHeight) {
        if (def == null) return null;
        MarketWorkflowGraph graph = new MarketWorkflowGraph();
        graph.setVersion(def.getVersion());
        List<MarketWorkflowGraph.GraphNode> nodes = new ArrayList<>();
        if (def.getNodes() != null) {
            for (WorkflowFullDefinition.Node n : def.getNodes()) {
                double w = nodeWidth > 0 ? nodeWidth : 240d;
                double h = nodeHeight > 0 ? nodeHeight : 60d;
                double x = n.getX() == null ? 0 : n.getX();
                double y = n.getY() == null ? 0 : n.getY();
                nodes.add(MarketWorkflowGraph.GraphNode.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .name(n.getName())
                        .description(n.getDescription())
                        .category(n.getCategory())
                        .icon(n.getIcon())
                        .cx(x + w / 2.0)
                        .cy(y + h / 2.0)
                        .w(w)
                        .h(h)
                        .build());
            }
        }
        graph.setNodes(nodes);

        List<MarketWorkflowGraph.GraphEdge> edges = new ArrayList<>();
        if (def.getEdges() != null) {
            for (WorkflowFullDefinition.Edge e : def.getEdges()) {
                edges.add(MarketWorkflowGraph.GraphEdge.builder()
                        .id(e.getId())
                        .source(e.getSource())
                        .target(e.getTarget())
                        .label(e.getLabel())
                        .type(e.getType())
                        .sourceIndex(e.getSourceIndex())
                        .targetIndex(e.getTargetIndex())
                        .build());
            }
        }
        graph.setEdges(edges);
        return graph;
    }

    /** Editor JSON → Full Definition */
    public static WorkflowFullDefinition fromEditor(EditorWorkflowJson editor) {
        if (editor == null) return null;
        WorkflowFullDefinition def = new WorkflowFullDefinition();
        def.setVersion(editor.getVersion());
        def.setVariables(editor.getVariables());

        List<WorkflowFullDefinition.Node> nodes = new ArrayList<>();
        if (editor.getNodes() != null) {
            for (EditorWorkflowJson.Node en : editor.getNodes()) {
                Double x = null, y = null;
                if (en.getPosition() != null) {
                    x = en.getPosition().getX();
                    y = en.getPosition().getY();
                }
                Object nameObj = en.getData() == null ? null : en.getData().get("name");
                Object categoryObj = en.getData() == null ? null : en.getData().get("category");
                Object iconObj = en.getData() == null ? null : en.getData().get("icon");
                nodes.add(WorkflowFullDefinition.Node.builder()
                        .id(en.getId())
                        .type(en.getType())
                        .name(nameObj instanceof String s ? s : en.getName())
                        .x(x)
                        .y(y)
                        .category(categoryObj instanceof String s ? s : null)
                        .icon(iconObj instanceof String s ? s : null)
                        .data(en.getData())
                        .build());
            }
        }
        def.setNodes(nodes);

        List<WorkflowFullDefinition.Edge> edges = new ArrayList<>();
        if (editor.getEdges() != null) {
            for (EditorWorkflowJson.Edge ee : editor.getEdges()) {
                String edgeType = ee.getType();
                String label = ee.getLabel();
                if (ee.getData() != null) {
                    if (label == null) {
                        Object dataLabel = ee.getData().get("label");
                        if (dataLabel instanceof String s) label = s;
                    }
                    if (edgeType == null) {
                        Object dt = ee.getData().get("type");
                        if (dt instanceof String s) edgeType = s;
                    }
                }
                edges.add(WorkflowFullDefinition.Edge.builder()
                        .id(ee.getId())
                        .source(ee.getSource())
                        .target(ee.getTarget())
                        .sourceHandle(ee.getSourceHandle())
                        .targetHandle(ee.getTargetHandle())
                        .type(edgeType)
                        .label(label)
                        .data(ee.getData())
                        .build());
            }
        }
        def.setEdges(edges);
        return def;
    }

    /** Market Graph → Full Definition（坐标左转 ½ 宽高恢复为 x/y） */
    public static WorkflowFullDefinition fromMarket(MarketWorkflowGraph graph) {
        if (graph == null) return null;
        WorkflowFullDefinition def = new WorkflowFullDefinition();
        def.setVersion(graph.getVersion());
        List<WorkflowFullDefinition.Node> nodes = new ArrayList<>();
        if (graph.getNodes() != null) {
            for (MarketWorkflowGraph.GraphNode gn : graph.getNodes()) {
                double w = gn.getW() == null ? 240 : gn.getW();
                double h = gn.getH() == null ? 60 : gn.getH();
                double cx = gn.getCx() == null ? w / 2 : gn.getCx();
                double cy = gn.getCy() == null ? h / 2 : gn.getCy();
                nodes.add(WorkflowFullDefinition.Node.builder()
                        .id(gn.getId())
                        .type(gn.getType())
                        .name(gn.getName())
                        .description(gn.getDescription())
                        .category(gn.getCategory())
                        .icon(gn.getIcon())
                        .x(cx - w / 2.0)
                        .y(cy - h / 2.0)
                        .build());
            }
        }
        def.setNodes(nodes);

        List<WorkflowFullDefinition.Edge> edges = new ArrayList<>();
        if (graph.getEdges() != null) {
            for (MarketWorkflowGraph.GraphEdge ge : graph.getEdges()) {
                edges.add(WorkflowFullDefinition.Edge.builder()
                        .id(ge.getId())
                        .source(ge.getSource())
                        .target(ge.getTarget())
                        .type(ge.getType())
                        .label(ge.getLabel())
                        .sourceIndex(ge.getSourceIndex())
                        .targetIndex(ge.getTargetIndex())
                        .build());
            }
        }
        def.setEdges(edges);
        return def;
    }

    public static String toJsonString(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot serialize: " + e.getMessage(), e);
        }
    }

    private static <T> T tryParse(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception ignored) {
            return null;
        }
    }
}

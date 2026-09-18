package com.meowflow.workflow.executor.condition;

import com.meowflow.common.context.CancellationToken;
import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.EdgeType;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.engine.*;
import com.meowflow.workflow.executor.NodeExecutor;
import com.meowflow.workflow.executor.NodeRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LOOP 节点子图驱动。
 *
 * <p>将 LOOP 节点从简单的计数器改造为真正的子图执行器。
 * 每轮迭代：
 * <ol>
 *   <li>从 {@code context.getLoopCursors()} 读取当前迭代游标。</li>
 *   <li>构建子作用域 {@link ExecutionContext}。</li>
 *   <li>在子作用域内执行 LOOP 内置子图（通过引擎的 ready-queue 机制）。</li>
 *   <li>收集迭代输出，更新游标。</li>
 *   <li>条件满足时退出循环。</li>
 * </ol>
 *
 * <p>迭代状态保存在 {@code context.loopCursors[nodeId]} 中，可在快照中序列化。
 */
@Slf4j
@Component
public class LoopSubgraphDriver {

    private final EdgeRouter edgeRouter;
    private final NodeExecutionRecorder nodeExecutionRecorder;
    private final CancellationRegistry cancellationRegistry;
    private final java.util.concurrent.Executor nodeExecutorPool;
    private final NodeRegistry nodeRegistry;

    public LoopSubgraphDriver(EdgeRouter edgeRouter,
                              NodeExecutionRecorder nodeExecutionRecorder,
                              CancellationRegistry cancellationRegistry,
                              @Qualifier("nodeExecutorPool") java.util.concurrent.Executor nodeExecutorPool,
                              @Lazy NodeRegistry nodeRegistry) {
        this.edgeRouter = edgeRouter;
        this.nodeExecutionRecorder = nodeExecutionRecorder;
        this.cancellationRegistry = cancellationRegistry;
        this.nodeExecutorPool = nodeExecutorPool;
        this.nodeRegistry = nodeRegistry;
    }

    /**
     * 执行 LOOP 节点的子图。
     *
     * @param parentContext 父执行上下文
     * @param loopNode     LOOP 节点定义
     * @param input        输入参数
     * @param token        取消令牌
     * @return 迭代执行结果
     */
    @SuppressWarnings("unchecked")
    public LoopExecutionResult executeSubgraph(
            ExecutionContext parentContext,
            NodeDefinition loopNode,
            Map<String, Object> input,
            CancellationToken token) {

        LoopCursor cursor = getOrCreateCursor(parentContext, loopNode.getId());
        Object itemsObj = input.get("items");
        if (itemsObj == null) {
            itemsObj = input.get("source");
        }
        if (itemsObj == null) {
            itemsObj = input.get("iterator");
        }
        List<Map<String, Object>> items = parseItems(itemsObj);
        Integer maxIterations = parseInt(input.get("maxIterations"), 100);
        Integer maxDurationSeconds = parseInt(input.get("maxDurationSeconds"), 300);
        long startTimeMs = System.currentTimeMillis();

        List<Map<String, Object>> iterationResults = new ArrayList<>();
        Map<String, Object> lastOutput = new HashMap<>();
        String errorMode = input.get("errorMode") != null
                ? input.get("errorMode").toString()
                : "abort";

        if (items == null || items.isEmpty()) {
            return LoopExecutionResult.builder()
                    .nodeId(loopNode.getId())
                    .iterations(0)
                    .exitReason("no_items")
                    .iterationResults(Collections.emptyList())
                    .lastOutput(Map.of("iterations", 0, "exited", true, "exitReason", "no_items"))
                    .build();
        }

        // 构建子图：获取 LOOP 节点的内置子图
        SubgraphDef subgraph = buildSubgraph(loopNode, parentContext.getDefinition());
        if (subgraph.isEmpty()) {
            // 无内置子图时，退化为计数器
            int iterations = Math.min(items.size(), maxIterations);
            return LoopExecutionResult.builder()
                    .nodeId(loopNode.getId())
                    .iterations(iterations)
                    .exitReason(iterations >= maxIterations ? "max_iterations" : "completed")
                    .iterationResults(Collections.emptyList())
                    .lastOutput(Map.of(
                            "iterations", iterations,
                            "exited", true,
                            "exitReason", iterations >= maxIterations ? "max_iterations" : "completed"
                    ))
                    .build();
        }

        // 迭代主循环
        while (cursor.getIteration() < maxIterations) {
            // 检查取消
            if (token.isCancelled()) {
                log.info("Loop {} cancelled at iteration {}", loopNode.getId(), cursor.getIteration());
                break;
            }

            // 检查总时长
            if ((System.currentTimeMillis() - startTimeMs) / 1000L > maxDurationSeconds) {
                log.debug("Loop {} exceeded max duration {}s", loopNode.getId(), maxDurationSeconds);
                break;
            }

            if (cursor.getIteration() >= items.size()) {
                break;
            }

            Object currentItem = items.get(cursor.getIteration());
            log.debug("Loop {} iteration {}: item={}", loopNode.getId(), cursor.getIteration(), currentItem);

            // 构建子作用域
            ExecutionContext subContext = buildSubContext(parentContext, loopNode, cursor, currentItem);

            // 执行子图
            Map<String, Object> subOutput = executeSubgraphNodes(subContext, subgraph, token);
            lastOutput = subOutput;
            boolean subgraphOk = !Boolean.FALSE.equals(subOutput.get("_success"));
            Map<String, Object> iterationEntry = new HashMap<>();
            iterationEntry.put("iteration", cursor.getIteration());
            iterationEntry.put("item", currentItem);
            iterationEntry.put("output", subgraphOk ? subOutput : null);
            if (subOutput.get("_error") != null) {
                iterationEntry.put("error", subOutput.get("_error"));
            }
            iterationResults.add(iterationEntry);

            // 递增游标
            cursor.incrementIteration();
            updateCursor(parentContext, loopNode.getId(), cursor);

            if (!subgraphOk) {
                if ("abort".equalsIgnoreCase(errorMode)) {
                    return LoopExecutionResult.builder()
                            .nodeId(loopNode.getId())
                            .iterations(cursor.getIteration())
                            .exitReason("subgraph_failed")
                            .success(false)
                            .errorMessage(String.valueOf(subOutput.get("_error")))
                            .iterationResults(iterationResults)
                            .lastOutput(lastOutput)
                            .build();
                }
                // skip / continue：跳过失败项，继续处理下一项。
                continue;
            }

            // 检查提前退出条件
            Object exitFlag = subOutput.get("loop_exit");
            if (Boolean.TRUE.equals(exitFlag)) {
                log.debug("Loop {} exited early at iteration {} due to loop_exit=true",
                        loopNode.getId(), cursor.getIteration() - 1);
                break;
            }
        }

        String exitReason;
        if (cursor.getIteration() >= maxIterations) {
            exitReason = "max_iterations";
        } else if (token.isCancelled()) {
            exitReason = "cancelled";
        } else if ((System.currentTimeMillis() - startTimeMs) / 1000L > maxDurationSeconds) {
            exitReason = "max_duration";
        } else {
            exitReason = "completed";
        }

        Map<String, Object> finalOutput = new HashMap<>(lastOutput);
        finalOutput.put("iterations", cursor.getIteration());
        finalOutput.put("exited", true);
        finalOutput.put("exitReason", exitReason);
        finalOutput.put("iterationResults", iterationResults);

        return LoopExecutionResult.builder()
                .nodeId(loopNode.getId())
                .iterations(cursor.getIteration())
                .exitReason(exitReason)
                .success(true)
                .iterationResults(iterationResults)
                .lastOutput(finalOutput)
                .build();
    }

    private LoopCursor getOrCreateCursor(ExecutionContext context, String nodeId) {
        Map<String, LoopCursor> loopCursors = context.getLoopCursors();
        if (loopCursors == null) {
            loopCursors = new HashMap<>();
            context.setLoopCursors(loopCursors);
        }
        return loopCursors.computeIfAbsent(nodeId, k -> new LoopCursor());
    }

    private void updateCursor(ExecutionContext context, String nodeId, LoopCursor cursor) {
        Map<String, LoopCursor> loopCursors = context.getLoopCursors();
        if (loopCursors == null) {
            loopCursors = new HashMap<>();
            context.setLoopCursors(loopCursors);
        }
        loopCursors.put(nodeId, cursor);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseItems(Object itemsObj) {
        if (itemsObj == null) return Collections.emptyList();
        if (itemsObj instanceof Iterable) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : (Iterable<?>) itemsObj) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                } else {
                    result.add(Map.of("value", item));
                }
            }
            return result;
        }
        if (itemsObj instanceof Number) {
            int count = ((Number) itemsObj).intValue();
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < count; i++) result.add(Map.of("index", i));
            return result;
        }
        return Collections.emptyList();
    }

    private int parseInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); }
        catch (Exception e) { return defaultVal; }
    }

    private SubgraphDef buildSubgraph(NodeDefinition loopNode, WorkflowDefinition definition) {
        Map<String, Object> data = loopNode.getData();
        if (data == null) return new SubgraphDef(Collections.emptyList(), Collections.emptyMap());

        Map<String, Object> config = data.get("config") instanceof Map
                ? (Map<String, Object>) data.get("config")
                : data;

        Object embedded = config.get("subgraph");
        if (embedded instanceof Map) {
            return buildEmbeddedSubgraph((Map<String, Object>) embedded);
        }

        Object subgraphNodes = config.get("subgraphNodes");
        if (subgraphNodes instanceof List<?> rawNodes && !rawNodes.isEmpty()) {
            if (rawNodes.get(0) instanceof String) {
                @SuppressWarnings("unchecked")
                List<String> nodeIds = (List<String>) rawNodes;
                List<NodeDefinition> nodes = new ArrayList<>();
                Map<String, List<Edge>> edges = new HashMap<>();

                for (String nid : nodeIds) {
                    NodeDefinition nd = definition.findNode(nid);
                    if (nd != null) nodes.add(nd);
                }

                for (Edge edge : definition.getEdges()) {
                    if (nodeIds.contains(edge.getSource()) && nodeIds.contains(edge.getTarget())) {
                        edges.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
                    }
                }

                return new SubgraphDef(nodes, edges);
            }
            return buildSubgraphFromMaps(rawNodes);
        }

        return new SubgraphDef(Collections.emptyList(), Collections.emptyMap());
    }

    private SubgraphDef buildEmbeddedSubgraph(Map<String, Object> embedded) {
        Object nodesObj = embedded.get("nodes");
        Object edgesObj = embedded.get("edges");
        List<NodeDefinition> nodes = nodesObj instanceof List ? parseNodes((List<?>) nodesObj) : List.of();
        List<Edge> edges = edgesObj instanceof List ? parseEdges((List<?>) edgesObj) : List.of();

        Map<String, List<Edge>> edgeMap = new HashMap<>();
        for (Edge edge : edges) {
            edgeMap.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
        }
        return new SubgraphDef(nodes, edgeMap);
    }

    private SubgraphDef buildSubgraphFromMaps(List<?> rawNodes) {
        List<NodeDefinition> nodes = parseNodes(rawNodes);
        return new SubgraphDef(nodes, Collections.emptyMap());
    }

    private List<NodeDefinition> parseNodes(List<?> rawNodes) {
        List<NodeDefinition> nodes = new ArrayList<>();
        for (Object item : rawNodes) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> map = (Map<?, ?>) item;
            NodeDefinition node = new NodeDefinition();
            node.setId(stringValue(map.get("id")));
            node.setName(stringValue(map.get("name")));
            node.setType(NodeType.fromJson(stringValue(map.get("type"))));

            Map<String, Object> data = new HashMap<>();
            if (map.get("category") != null) data.put("category", map.get("category"));
            if (map.get("description") != null) data.put("description", map.get("description"));
            if (map.get("config") instanceof Map) data.put("config", map.get("config"));
            else if (map.get("data") instanceof Map) data.put("config", ((Map<?, ?>) map.get("data")).get("config"));
            node.setData(data);

            double x = doubleValue(map.get("x"), 0);
            double y = doubleValue(map.get("y"), 0);
            node.setPosition(new NodeDefinition.Position(x, y));
            nodes.add(node);
        }
        return nodes;
    }

    private List<Edge> parseEdges(List<?> rawEdges) {
        List<Edge> edges = new ArrayList<>();
        for (Object item : rawEdges) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> map = (Map<?, ?>) item;
            Edge edge = new Edge();
            edge.setId(stringValue(map.get("id")));
            edge.setSource(stringValue(map.get("source")));
            edge.setTarget(stringValue(map.get("target")));
            edge.setType(EdgeType.fromJson(stringValue(map.get("type"))));

            Edge.EdgeData edgeData = new Edge.EdgeData();
            edgeData.setLabel(stringValue(map.get("label")));
            if (map.get("config") instanceof Map) {
                edgeData.setConfig((Map<String, Object>) map.get("config"));
            }
            edge.setData(edgeData);
            edges.add(edge);
        }
        return edges;
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private double doubleValue(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private ExecutionContext buildSubContext(ExecutionContext parent,
                                            NodeDefinition loopNode,
                                            LoopCursor cursor,
                                            Object currentItem) {
        Map<String, Object> subVars = new HashMap<>(parent.getVariables());
        subVars.put("loop.currentItem", currentItem);
        subVars.put("loop.iteration", cursor.getIteration());
        subVars.put("loop.nodeId", loopNode.getId());
        subVars.put("item", currentItem);
        subVars.put("index", cursor.getIteration());

        return ExecutionContext.builder()
                .executionId(parent.getExecutionId())
                .workflowId(parent.getWorkflowId())
                .version(parent.getVersion())
                .definition(parent.getDefinition())
                .variables(subVars)
                .nodeResults(new ConcurrentHashMap<>(parent.getNodeResults()))
                .cancellationToken(parent.getCancellationToken())
                .build();
    }

    private Map<String, Object> executeSubgraphNodes(ExecutionContext context,
                                                     SubgraphDef subgraph,
                                                     CancellationToken token) {
        Map<String, Object> output = new HashMap<>();
        List<NodeDefinition> ordered = orderSubgraph(subgraph);
        for (NodeDefinition node : ordered) {
            if (token.isCancelled()) break;
            if (node.getType() == NodeType.NOTE) continue;

            NodeExecutor executor = nodeRegistry.getExecutor(node.getType());
            if (executor == null) {
                output.put("_success", false);
                output.put("_error", "Unsupported node type in subgraph: " + node.getType());
                break;
            }

            nodeExecutionRecorder.recordStarted(context.getExecutionId(), node, null);
            NodeResult result = executor.execute(context, node);
            nodeExecutionRecorder.recordFinished(context.getExecutionId(), node, result);
            context.setNodeResult(node.getId(), result);

            if (!result.isSuccess()) {
                output.put("_success", false);
                output.put("_error", result.getErrorMessage());
                output.put("_nodeId", node.getId());
                break;
            }

            if (result.getOutput() != null) {
                output.put(node.getId(), result.getOutput());
                output.putAll(result.getOutput());
            }
        }
        output.putIfAbsent("_success", true);
        return output;
    }

    private List<NodeDefinition> orderSubgraph(SubgraphDef subgraph) {
        try {
            List<Edge> edges = subgraph.getEdges().values().stream()
                    .flatMap(List::stream)
                    .toList();
            WorkflowDefinition subDefinition = WorkflowDefinition.builder()
                    .nodes(subgraph.getNodes())
                    .edges(edges)
                    .build();
            return DAGSorter.topologicalSort(subDefinition).sortedNodes();
        } catch (Exception e) {
            log.warn("Subgraph topological sort failed, using config order: {}", e.getMessage());
            return subgraph.getNodes();
        }
    }

    // ── Inner types ──────────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    public static class LoopExecutionResult {
        private String nodeId;
        private int iterations;
        private String exitReason;
        @lombok.Builder.Default
        private boolean success = true;
        private String errorMessage;
        private List<Map<String, Object>> iterationResults;
        private Map<String, Object> lastOutput;
    }

    private static class SubgraphDef {
        private final List<NodeDefinition> nodes;
        private final Map<String, List<Edge>> edges;

        SubgraphDef(List<NodeDefinition> nodes, Map<String, List<Edge>> edges) {
            this.nodes = nodes;
            this.edges = edges;
        }

        List<NodeDefinition> getNodes() { return nodes; }
        Map<String, List<Edge>> getEdges() { return edges; }
        boolean isEmpty() { return nodes.isEmpty(); }
    }
}


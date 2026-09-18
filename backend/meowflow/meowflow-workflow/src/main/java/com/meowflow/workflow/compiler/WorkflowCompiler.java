package com.meowflow.workflow.compiler;

import com.meowflow.common.exception.WorkflowException;
import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.engine.DAGSorter;
import com.meowflow.workflow.executor.NodeRegistry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流发布前编译器。
 *
 * 在工作流版本发布前进行全面校验并生成 {@link ExecutionPlan}。
 * 校验失败时抛出异常阻止发布，避免带病工作流进入执行阶段。
 *
 * <p>校验内容：
 * <ol>
 *   <li>节点列表非空。</li>
 *   <li>存在触发节点。</li>
 *   <li>无环。</li>
 *   <li>每个节点都有注册的 {@link com.meowflow.workflow.executor.NodeExecutor}。</li>
 *   <li>孤立节点检测（入度 = 0 且非触发节点，或出度 = 0 且非结束节点）。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowCompiler {

    private final NodeRegistry nodeRegistry;

    /**
     * 执行计划：发布时生成的一次性编译产物。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecutionPlan {
        /** 工作流 ID */
        private Long workflowId;
        /** 版本 */
        private String version;
        /** 拓扑排序后的节点顺序 */
        private List<NodeDefinition> sortedNodes;
        /** nodeId -> 出边列表（目标节点 ID 列表） */
        private Map<String, List<String>> outgoingEdges;
        /** nodeId -> 入边数量 */
        private Map<String, Integer> inDegrees;
        /** 触发节点 ID 列表 */
        private List<String> triggerNodeIds;
        /** DAG 无环标识 */
        private boolean acyclic;
    }

    /**
     * 编译并生成执行计划。
     *
     * @param definition 工作流定义
     * @param workflowId 工作流 ID（用于日志）
     * @return 编译产物；永远不返回 null
     * @throws WorkflowException 校验失败时抛出
     */
    public ExecutionPlan compile(WorkflowDefinition definition, Long workflowId) {
        log.info("Compiling workflowId={}, version={}", workflowId, definition.getVersion());

        List<NodeDefinition> nodes = definition.getNodes();
        List<Edge> edges = definition.getEdges();

        // 1. 空节点检查
        if (nodes == null || nodes.isEmpty()) {
            throw new WorkflowException(workflowId.toString(), "工作流节点列表为空");
        }

        // 2. 触发节点检查
        List<NodeDefinition> triggers = nodes.stream()
                .filter(NodeDefinition::isTrigger)
                .collect(Collectors.toList());
        if (triggers.isEmpty()) {
            throw new WorkflowException(workflowId.toString(), "工作流没有触发节点");
        }

        // 3. 执行器注册检查：未实现/不支持的节点必须在发布前暴露，而不是运行到一半才失败。
        List<String> unsupported = nodes.stream()
                .filter(n -> n.getType() == null
                        || (n.getType() != NodeType.NOTE && !nodeRegistry.isRegistered(n.getType())))
                .map(n -> n.getId() + " (" + n.getName() + ")" + (n.getType() != null ? " type=" + n.getType() : " type=unknown"))
                .toList();
        if (!unsupported.isEmpty()) {
            throw new WorkflowException(workflowId.toString(),
                    "存在未注册执行器的节点: " + String.join(", ", unsupported));
        }

        // 4. 环检测
        try {
            DAGSorter.SortResult sortResult = DAGSorter.topologicalSort(definition);
            if (sortResult.sortedNodes().isEmpty()) {
                throw new WorkflowException(workflowId.toString(), "工作流拓扑为空（仅包含 NOTE 节点）");
            }
            log.debug("Workflow {} is acyclic, {} nodes in topological order",
                    workflowId, sortResult.sortedNodes().size());

            // 5. 孤立节点检测
            detectOrphanNodes(definition, sortResult, triggers);

            return ExecutionPlan.builder()
                    .workflowId(workflowId)
                    .version(definition.getVersion())
                    .sortedNodes(sortResult.sortedNodes())
                    .outgoingEdges(sortResult.outgoingEdges())
                    .inDegrees(computeInDegrees(sortResult.sortedNodes(), edges))
                    .triggerNodeIds(triggers.stream().map(NodeDefinition::getId).collect(Collectors.toList()))
                    .acyclic(true)
                    .build();

        } catch (WorkflowException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException(workflowId.toString(), "工作流编译失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Integer> computeInDegrees(List<NodeDefinition> sortedNodes, List<Edge> edges) {
        Map<String, Integer> inDegrees = new java.util.HashMap<>();
        for (NodeDefinition node : sortedNodes) {
            inDegrees.put(node.getId(), 0);
        }
        for (Edge edge : edges) {
            if (inDegrees.containsKey(edge.getTarget())) {
                inDegrees.merge(edge.getTarget(), 1, Integer::sum);
            }
        }
        return inDegrees;
    }

    private void detectOrphanNodes(WorkflowDefinition definition,
                                   DAGSorter.SortResult sortResult,
                                   List<NodeDefinition> triggers) {
        Set<String> triggerIds = triggers.stream()
                .map(NodeDefinition::getId)
                .collect(Collectors.toSet());

        Map<String, Integer> inDegrees = computeInDegrees(sortResult.sortedNodes(), definition.getEdges());
        Map<String, Long> outDegrees = new java.util.HashMap<>();
        for (NodeDefinition node : sortResult.sortedNodes()) {
            outDegrees.put(node.getId(), 0L);
        }
        for (Edge edge : definition.getEdges()) {
            if (outDegrees.containsKey(edge.getSource())) {
                outDegrees.merge(edge.getSource(), 1L, Long::sum);
            }
        }

        List<String> orphans = new ArrayList<>();
        for (NodeDefinition node : sortResult.sortedNodes()) {
            boolean isOrphan = (inDegrees.getOrDefault(node.getId(), 0) == 0 && !triggerIds.contains(node.getId()))
                    || (outDegrees.getOrDefault(node.getId(), 0L) == 0 && !node.isEnd());
            if (isOrphan) {
                orphans.add(node.getId() + " (" + node.getType() + ")");
            }
        }

        if (!orphans.isEmpty()) {
            log.warn("Workflow contains orphan nodes (will never be reached): {}", orphans);
        }
    }
}

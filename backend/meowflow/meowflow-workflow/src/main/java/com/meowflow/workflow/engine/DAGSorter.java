package com.meowflow.workflow.engine;

import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.common.exception.WorkflowException;

import java.util.*;
import java.util.stream.Collectors;

public class DAGSorter {

    public record SortResult(
            List<NodeDefinition> sortedNodes,
            Map<String, Integer> nodeIndex,
            Map<String, List<String>> outgoingEdges
    ) {}

    public static SortResult topologicalSort(WorkflowDefinition definition) {
        List<NodeDefinition> nodes = definition.getNodes();
        List<Edge> edges = definition.getEdges();

        if (nodes == null || nodes.isEmpty()) {
            return new SortResult(List.of(), Map.of(), Map.of());
        }

        // NOTE is a UI decoration; it has no execution semantics and must
        // not block the topological sort. Strip it from the graph before
        // sorting — we still keep it on the original definition so the
        // editor can render it.
        List<NodeDefinition> execNodes = nodes.stream()
                .filter(n -> n.getType() != com.meowflow.workflow.definition.NodeType.NOTE)
                .toList();
        if (execNodes.isEmpty()) {
            return new SortResult(List.of(), Map.of(), Map.of());
        }

        Map<String, NodeDefinition> nodeMap = execNodes.stream()
                .collect(Collectors.toMap(NodeDefinition::getId, n -> n));

        Map<String, List<String>> incomingCount = new HashMap<>();
        Map<String, List<String>> outgoingEdges = new HashMap<>();

        for (NodeDefinition node : execNodes) {
            incomingCount.put(node.getId(), new ArrayList<>());
            outgoingEdges.put(node.getId(), new ArrayList<>());
        }

        for (Edge edge : edges) {
            String source = edge.getSource();
            String target = edge.getTarget();
            // Skip edges that touch NOTE nodes so they don't pollute
            // degree counts.
            NodeDefinition src = nodeMap.get(source);
            NodeDefinition tgt = nodeMap.get(target);
            if (src != null && tgt != null) {
                incomingCount.get(target).add(source);
                outgoingEdges.get(source).add(target);
            }
        }

        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> nodeIndex = new HashMap<>();

        int idx = 0;
        for (NodeDefinition node : execNodes) {
            int degree = incomingCount.get(node.getId()).size();
            inDegree.put(node.getId(), degree);
            nodeIndex.put(node.getId(), idx++);
            if (degree == 0) {
                queue.offer(node.getId());
            }
        }

        List<NodeDefinition> sorted = new ArrayList<>();
        int processed = 0;

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            sorted.add(nodeMap.get(nodeId));
            processed++;

            for (String targetId : outgoingEdges.get(nodeId)) {
                int newDegree = inDegree.get(targetId) - 1;
                inDegree.put(targetId, newDegree);
                if (newDegree == 0) {
                    queue.offer(targetId);
                }
            }
        }

        if (processed != execNodes.size()) {
            List<String> cycleNodes = inDegree.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .toList();
            throw new WorkflowException(null, "工作流存在循环依赖: " + cycleNodes);
        }

        return new SortResult(sorted, nodeIndex, outgoingEdges);
    }

    /**
     * Kahn algorithm topological sort for nodes and edges.
     *
     * @param nodes Node list
     * @param edges Edge list (from -> to)
     * @return Sorted node list
     */
    public List<NodeDefinition> topologicalSort(List<NodeDefinition> nodes, List<Edge> edges) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        // Same NOTE-stripping policy as the WorkflowDefinition overload.
        List<NodeDefinition> execNodes = nodes.stream()
                .filter(n -> n.getType() != com.meowflow.workflow.definition.NodeType.NOTE)
                .toList();
        if (execNodes.isEmpty()) {
            return List.of();
        }

        Map<String, NodeDefinition> nodeMap = execNodes.stream()
                .collect(Collectors.toMap(NodeDefinition::getId, n -> n));

        Map<String, List<String>> incomingCount = new HashMap<>();
        Map<String, List<String>> outgoingEdges = new HashMap<>();

        for (NodeDefinition node : execNodes) {
            incomingCount.put(node.getId(), new ArrayList<>());
            outgoingEdges.put(node.getId(), new ArrayList<>());
        }

        for (Edge edge : edges) {
            String source = edge.getSource();
            String target = edge.getTarget();
            if (nodeMap.containsKey(source) && nodeMap.containsKey(target)) {
                incomingCount.get(target).add(source);
                outgoingEdges.get(source).add(target);
            }
        }

        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (NodeDefinition node : execNodes) {
            inDegree.put(node.getId(), incomingCount.get(node.getId()).size());
            if (incomingCount.get(node.getId()).isEmpty()) {
                queue.offer(node.getId());
            }
        }

        List<NodeDefinition> sorted = new ArrayList<>();
        int processed = 0;

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            sorted.add(nodeMap.get(nodeId));
            processed++;

            for (String targetId : outgoingEdges.get(nodeId)) {
                int newDegree = inDegree.get(targetId) - 1;
                inDegree.put(targetId, newDegree);
                if (newDegree == 0) {
                    queue.offer(targetId);
                }
            }
        }

        if (processed != execNodes.size()) {
            List<String> cycleNodes = inDegree.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .toList();
            throw new DAGHasCycleException(cycleNodes);
        }

        return sorted;
    }

    /**
     * Detect circular dependencies in the workflow graph.
     *
     * @param nodes Node list
     * @param edges Edge list (from -> to)
     * @throws DAGHasCycleException when cycle is detected
     */
    public void validateNoCycle(List<NodeDefinition> nodes, List<Edge> edges) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // Strip NOTE nodes so they don't introduce phantom degrees.
        List<NodeDefinition> execNodes = nodes.stream()
                .filter(n -> n.getType() != com.meowflow.workflow.definition.NodeType.NOTE)
                .toList();
        if (execNodes.isEmpty()) return;

        Map<String, NodeDefinition> nodeMap = execNodes.stream()
                .collect(Collectors.toMap(NodeDefinition::getId, n -> n));

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> outgoingEdges = new HashMap<>();

        for (NodeDefinition node : execNodes) {
            inDegree.put(node.getId(), 0);
            outgoingEdges.put(node.getId(), new ArrayList<>());
        }

        for (Edge edge : edges) {
            String source = edge.getSource();
            String target = edge.getTarget();
            if (nodeMap.containsKey(source) && nodeMap.containsKey(target)) {
                inDegree.put(target, inDegree.get(target) + 1);
                outgoingEdges.get(source).add(target);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (NodeDefinition node : execNodes) {
            if (inDegree.get(node.getId()) == 0) {
                queue.offer(node.getId());
            }
        }

        int processed = 0;
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            processed++;

            for (String targetId : outgoingEdges.get(nodeId)) {
                int newDegree = inDegree.get(targetId) - 1;
                inDegree.put(targetId, newDegree);
                if (newDegree == 0) {
                    queue.offer(targetId);
                }
            }
        }

        if (processed != execNodes.size()) {
            List<String> cycleNodes = inDegree.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .toList();
            throw new DAGHasCycleException("工作流存在循环依赖: " + cycleNodes, cycleNodes);
        }
    }

    /**
     * Find batches of nodes that can be executed in parallel.
     * Each batch contains nodes that have no dependencies on each other.
     *
     * @param nodes Node list
     * @param edges Edge list (from -> to)
     * @return List of batches, each batch can be executed in parallel
     */
    public List<List<NodeDefinition>> getParallelBatches(List<NodeDefinition> nodes, List<Edge> edges) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        // Strip NOTE nodes (UI decoration) so they don't sit in a batch.
        List<NodeDefinition> execNodes = nodes.stream()
                .filter(n -> n.getType() != com.meowflow.workflow.definition.NodeType.NOTE)
                .toList();
        if (execNodes.isEmpty()) return List.of();

        Map<String, NodeDefinition> nodeMap = execNodes.stream()
                .collect(Collectors.toMap(NodeDefinition::getId, n -> n));

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> outgoingEdges = new HashMap<>();

        for (NodeDefinition node : execNodes) {
            inDegree.put(node.getId(), 0);
            outgoingEdges.put(node.getId(), new ArrayList<>());
        }

        for (Edge edge : edges) {
            String source = edge.getSource();
            String target = edge.getTarget();
            if (nodeMap.containsKey(source) && nodeMap.containsKey(target)) {
                inDegree.put(target, inDegree.get(target) + 1);
                outgoingEdges.get(source).add(target);
            }
        }

        List<List<NodeDefinition>> batches = new ArrayList<>();

        while (!nodeMap.isEmpty()) {
            List<NodeDefinition> batch = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0 && nodeMap.containsKey(entry.getKey())) {
                    batch.add(nodeMap.get(entry.getKey()));
                }
            }

            if (batch.isEmpty()) {
                break;
            }

            batches.add(batch);

            for (NodeDefinition node : batch) {
                nodeMap.remove(node.getId());

                for (String targetId : outgoingEdges.getOrDefault(node.getId(), List.of())) {
                    inDegree.merge(targetId, -1, Integer::sum);
                }
            }
        }

        return batches;
    }

    public static List<String> getUpstreamNodes(WorkflowDefinition definition, String nodeId) {
        List<Edge> edges = definition.getEdges();
        return edges.stream()
                .filter(e -> e.getTarget().equals(nodeId))
                .map(Edge::getSource)
                .toList();
    }

    public static List<String> getDownstreamNodes(WorkflowDefinition definition, String nodeId) {
        List<Edge> edges = definition.getEdges();
        return edges.stream()
                .filter(e -> e.getSource().equals(nodeId))
                .map(Edge::getTarget)
                .toList();
    }

    public static boolean hasCycle(WorkflowDefinition definition) {
        try {
            topologicalSort(definition);
            return false;
        } catch (DAGHasCycleException e) {
            return true;
        } catch (WorkflowException e) {
            return true;
        }
    }
}

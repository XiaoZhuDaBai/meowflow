package com.meowflow.workflow.engine;

import com.meowflow.common.exception.WorkflowException;
import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DAGSorterTest {

    @Test
    void topologicalSort_shouldSortLinearGraph() {
        NodeDefinition node1 = createNode("n1", NodeType.TRIGGER_MANUAL);
        NodeDefinition node2 = createNode("n2", NodeType.LLM);
        NodeDefinition node3 = createNode("n3", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("n1", "n2"),
            createEdge("n2", "n3")
        );

        WorkflowDefinition definition = createDefinition(List.of(node1, node2, node3), edges);

        DAGSorter.SortResult result = DAGSorter.topologicalSort(definition);

        assertEquals(3, result.sortedNodes().size());
        assertEquals("n1", result.sortedNodes().get(0).getId());
        assertEquals("n2", result.sortedNodes().get(1).getId());
        assertEquals("n3", result.sortedNodes().get(2).getId());
    }

    @Test
    void topologicalSort_shouldSortDiamondGraph() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition nodeA = createNode("nodeA", NodeType.LLM);
        NodeDefinition nodeB = createNode("nodeB", NodeType.LLM);
        NodeDefinition merge = createNode("merge", NodeType.JOIN);
        NodeDefinition end = createNode("end", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("trigger", "nodeA"),
            createEdge("trigger", "nodeB"),
            createEdge("nodeA", "merge"),
            createEdge("nodeB", "merge"),
            createEdge("merge", "end")
        );

        WorkflowDefinition definition = createDefinition(List.of(trigger, nodeA, nodeB, merge, end), edges);

        DAGSorter.SortResult result = DAGSorter.topologicalSort(definition);

        assertEquals(5, result.sortedNodes().size());
        int triggerIdx = result.sortedNodes().indexOf(trigger);
        int nodeAIdx = result.sortedNodes().indexOf(nodeA);
        int nodeBIdx = result.sortedNodes().indexOf(nodeB);
        int mergeIdx = result.sortedNodes().indexOf(merge);
        int endIdx = result.sortedNodes().indexOf(end);

        assertTrue(triggerIdx < nodeAIdx);
        assertTrue(triggerIdx < nodeBIdx);
        assertTrue(nodeAIdx < mergeIdx);
        assertTrue(nodeBIdx < mergeIdx);
        assertTrue(mergeIdx < endIdx);
    }

    @Test
    void topologicalSort_shouldHandleEmptyGraph() {
        WorkflowDefinition definition = createDefinition(List.of(), List.of());
        DAGSorter.SortResult result = DAGSorter.topologicalSort(definition);
        assertTrue(result.sortedNodes().isEmpty());
    }

    @Test
    void topologicalSort_shouldDetectCycle() {
        NodeDefinition node1 = createNode("n1", NodeType.TRIGGER_MANUAL);
        NodeDefinition node2 = createNode("n2", NodeType.LLM);
        NodeDefinition node3 = createNode("n3", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("n1", "n2"),
            createEdge("n2", "n3"),
            createEdge("n3", "n1")
        );

        WorkflowDefinition definition = createDefinition(List.of(node1, node2, node3), edges);

        assertThrows(WorkflowException.class, () -> {
            DAGSorter.topologicalSort(definition);
        });
    }

    @Test
    void hasCycle_shouldReturnTrueForCyclicGraph() {
        NodeDefinition node1 = createNode("n1", NodeType.TRIGGER_MANUAL);
        NodeDefinition node2 = createNode("n2", NodeType.LLM);

        List<Edge> edges = List.of(
            createEdge("n1", "n2"),
            createEdge("n2", "n1")
        );

        WorkflowDefinition definition = createDefinition(List.of(node1, node2), edges);

        assertTrue(DAGSorter.hasCycle(definition));
    }

    @Test
    void hasCycle_shouldReturnFalseForAcyclicGraph() {
        NodeDefinition node1 = createNode("n1", NodeType.TRIGGER_MANUAL);
        NodeDefinition node2 = createNode("n2", NodeType.LLM);
        NodeDefinition node3 = createNode("n3", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("n1", "n2"),
            createEdge("n2", "n3")
        );

        WorkflowDefinition definition = createDefinition(List.of(node1, node2, node3), edges);

        assertFalse(DAGSorter.hasCycle(definition));
    }

    @Test
    void getParallelBatches_shouldGroupIndependentNodes() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition nodeA = createNode("nodeA", NodeType.LLM);
        NodeDefinition nodeB = createNode("nodeB", NodeType.LLM);
        NodeDefinition nodeC = createNode("nodeC", NodeType.LLM);
        NodeDefinition end = createNode("end", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("trigger", "nodeA"),
            createEdge("trigger", "nodeB"),
            createEdge("nodeA", "nodeC"),
            createEdge("nodeB", "nodeC"),
            createEdge("nodeC", "end")
        );

        DAGSorter sorter = new DAGSorter();
        List<List<NodeDefinition>> batches = sorter.getParallelBatches(
            List.of(trigger, nodeA, nodeB, nodeC, end), edges);

        assertEquals(4, batches.size());
        assertEquals(1, batches.get(0).size());
        assertEquals("trigger", batches.get(0).get(0).getId());
        assertEquals(2, batches.get(1).size());
        assertEquals(1, batches.get(2).size());
        assertEquals("nodeC", batches.get(2).get(0).getId());
        assertEquals(1, batches.get(3).size());
        assertEquals("end", batches.get(3).get(0).getId());
    }

    @Test
    void getUpstreamNodes_shouldReturnDirectPredecessors() {
        NodeDefinition node1 = createNode("n1", NodeType.TRIGGER_MANUAL);
        NodeDefinition node2 = createNode("n2", NodeType.LLM);
        NodeDefinition node3 = createNode("n3", NodeType.LLM);

        List<Edge> edges = List.of(
            createEdge("n1", "n2"),
            createEdge("n1", "n3"),
            createEdge("n2", "n3")
        );

        WorkflowDefinition definition = createDefinition(List.of(node1, node2, node3), edges);

        List<String> upstream = DAGSorter.getUpstreamNodes(definition, "n3");
        assertEquals(2, upstream.size());
        assertTrue(upstream.contains("n1"));
        assertTrue(upstream.contains("n2"));
    }

    @Test
    void getDownstreamNodes_shouldReturnDirectSuccessors() {
        NodeDefinition node1 = createNode("n1", NodeType.TRIGGER_MANUAL);
        NodeDefinition node2 = createNode("n2", NodeType.LLM);
        NodeDefinition node3 = createNode("n3", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("n1", "n2"),
            createEdge("n1", "n3")
        );

        WorkflowDefinition definition = createDefinition(List.of(node1, node2, node3), edges);

        List<String> downstream = DAGSorter.getDownstreamNodes(definition, "n1");
        assertEquals(2, downstream.size());
        assertTrue(downstream.contains("n2"));
        assertTrue(downstream.contains("n3"));
    }

    @Test
    void outgoingEdges_shouldMapCorrectly() {
        NodeDefinition node1 = createNode("n1", NodeType.TRIGGER_MANUAL);
        NodeDefinition node2 = createNode("n2", NodeType.LLM);
        NodeDefinition node3 = createNode("n3", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("n1", "n2"),
            createEdge("n1", "n3")
        );

        WorkflowDefinition definition = createDefinition(List.of(node1, node2, node3), edges);

        DAGSorter.SortResult result = DAGSorter.topologicalSort(definition);

        assertEquals(2, result.outgoingEdges().get("n1").size());
        assertTrue(result.outgoingEdges().get("n1").contains("n2"));
        assertTrue(result.outgoingEdges().get("n1").contains("n3"));
        assertTrue(result.outgoingEdges().get("n2").isEmpty());
        assertTrue(result.outgoingEdges().get("n3").isEmpty());
    }

    @Test
    void nodeIndex_shouldMapCorrectly() {
        NodeDefinition node1 = createNode("n1", NodeType.TRIGGER_MANUAL);
        NodeDefinition node2 = createNode("n2", NodeType.LLM);
        NodeDefinition node3 = createNode("n3", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("n1", "n2"),
            createEdge("n2", "n3")
        );

        WorkflowDefinition definition = createDefinition(List.of(node1, node2, node3), edges);

        DAGSorter.SortResult result = DAGSorter.topologicalSort(definition);

        assertEquals(0, result.nodeIndex().get("n1"));
        assertEquals(1, result.nodeIndex().get("n2"));
        assertEquals(2, result.nodeIndex().get("n3"));
    }

    private NodeDefinition createNode(String id, NodeType type) {
        return NodeDefinition.builder()
            .id(id)
            .name(id)
            .type(type)
            .build();
    }

    private Edge createEdge(String source, String target) {
        Edge edge = new Edge();
        edge.setSource(source);
        edge.setTarget(target);
        return edge;
    }

    private WorkflowDefinition createDefinition(List<NodeDefinition> nodes, List<Edge> edges) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setNodes(nodes);
        definition.setEdges(edges);
        return definition;
    }
}

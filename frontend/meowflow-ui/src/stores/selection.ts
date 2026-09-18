import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

/**
 * Selection store - 统一管理画布上的节点/边选择状态
 * 支持多选、清除、加选等操作
 */
export const useSelectionStore = defineStore('selection', () => {
  const selectedNodeIds = ref<Set<string>>(new Set());
  const selectedEdgeIds = ref<Set<string>>(new Set());
  const hoveredNodeId = ref<string | null>(null);
  const hoveredEdgeId = ref<string | null>(null);
  // 框选起始点（屏幕坐标）
  const selectionBox = ref<{ x: number; y: number; width: number; height: number } | null>(null);

  const selectedNodes = computed(() => Array.from(selectedNodeIds.value));
  const selectedEdges = computed(() => Array.from(selectedEdgeIds.value));
  const hasSelection = computed(() =>
    selectedNodeIds.value.size > 0 || selectedEdgeIds.value.size > 0,
  );
  const selectionCount = computed(() =>
    selectedNodeIds.value.size + selectedEdgeIds.value.size,
  );

  function selectNode(id: string, additive = false) {
    if (!additive) {
      selectedNodeIds.value = new Set([id]);
      selectedEdgeIds.value = new Set();
    } else {
      const next = new Set(selectedNodeIds.value);
      next.add(id);
      selectedNodeIds.value = next;
    }
  }

  function deselectNode(id: string) {
    const next = new Set(selectedNodeIds.value);
    next.delete(id);
    selectedNodeIds.value = next;
  }

  function toggleNode(id: string) {
    if (selectedNodeIds.value.has(id)) {
      deselectNode(id);
    } else {
      selectNode(id, true);
    }
  }

  function selectEdge(id: string, additive = false) {
    if (!additive) {
      selectedEdgeIds.value = new Set([id]);
      selectedNodeIds.value = new Set();
    } else {
      const next = new Set(selectedEdgeIds.value);
      next.add(id);
      selectedEdgeIds.value = next;
    }
  }

  function selectMultiple(nodeIds: string[] = [], edgeIds: string[] = []) {
    selectedNodeIds.value = new Set(nodeIds);
    selectedEdgeIds.value = new Set(edgeIds);
  }

  function addRange(nodeIds: string[]) {
    const next = new Set(selectedNodeIds.value);
    for (const id of nodeIds) next.add(id);
    selectedNodeIds.value = next;
  }

  function clearSelection() {
    selectedNodeIds.value = new Set();
    selectedEdgeIds.value = new Set();
  }

  function setHoveredNode(id: string | null) {
    hoveredNodeId.value = id;
  }

  function setHoveredEdge(id: string | null) {
    hoveredEdgeId.value = id;
  }

  function setSelectionBox(box: { x: number; y: number; width: number; height: number } | null) {
    selectionBox.value = box;
  }

  return {
    selectedNodeIds,
    selectedEdgeIds,
    hoveredNodeId,
    hoveredEdgeId,
    selectionBox,
    selectedNodes,
    selectedEdges,
    hasSelection,
    selectionCount,
    selectNode,
    deselectNode,
    toggleNode,
    selectEdge,
    selectMultiple,
    addRange,
    clearSelection,
    setHoveredNode,
    setHoveredEdge,
    setSelectionBox,
  };
});

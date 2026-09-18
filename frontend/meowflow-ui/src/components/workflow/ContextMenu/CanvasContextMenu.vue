<template>
  <div class="canvas-context-menu-host" />
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted } from 'vue';
import { useSelectionStore } from '@/stores/selection';
import { useWorkflowStore } from '@/stores/workflow';
import { useCanvasStore } from '@/stores/canvas';
import WorkflowContextMenu from './WorkflowContextMenu.vue';

const workflow = useWorkflowStore();
const selection = useSelectionStore();
const canvas = useCanvasStore();

const state = ref<{
  visible: boolean;
  x: number;
  y: number;
  mode: 'pane' | 'node' | 'edge';
  nodeId?: string;
  edgeId?: string;
}>({
  visible: false,
  x: 0,
  y: 0,
  mode: 'pane',
});

function open(x: number, y: number, mode: 'pane' | 'node' | 'edge', id?: string) {
  state.value = {
    visible: true,
    x,
    y,
    mode,
    nodeId: mode === 'node' ? id : undefined,
    edgeId: mode === 'edge' ? id : undefined,
  };
}

function close() {
  state.value.visible = false;
}

function handlePaneContextMenu(e: MouseEvent) {
  // 仅当右键画布空白处时显示 (画布根元素)，如 .react-flow__pane
  const target = e.target as HTMLElement;
  // 排除节点、边等内部元素 — 找到最接近的容器
  if (target.closest('.react-flow__node') || target.closest('.react-flow__edge')) {
    return;
  }
  e.preventDefault();
  open(e.clientX, e.clientY, 'pane');
  selection.clearSelection();
}

function handleContextMenu(e: MouseEvent) {
  const target = e.target as HTMLElement;
  const nodeEl = target.closest('.react-flow__node') as HTMLElement | null;
  const edgeEl = target.closest('.react-flow__edge') as HTMLElement | null;

  if (nodeEl) {
    e.preventDefault();
    const nodeId = nodeEl.getAttribute('data-id');
    if (nodeId) {
      if (!selection.selectedNodeIds.has(nodeId)) {
        selection.selectNode(nodeId);
      }
      open(e.clientX, e.clientY, 'node', nodeId);
    }
    return;
  }

  if (edgeEl) {
    e.preventDefault();
    const edgeId = edgeEl.getAttribute('data-testid')?.replace('rf__edge-', '')
      || edgeEl.getAttribute('data-id');
    if (edgeId) {
      open(e.clientX, e.clientY, 'edge', edgeId);
    }
  }
}

function handleAction(action: { type: string; [k: string]: any }) {
  if (action.type === 'copy') {
    // 触发复制（由 shortcut 处理即可）
    document.execCommand('copy');
  } else if (action.type === 'duplicate') {
    if (state.value.nodeId) {
      selection.selectNode(state.value.nodeId);
      document.execCommand('copy');
      document.execCommand('paste');
    }
  } else if (action.type === 'delete') {
    if (state.value.mode === 'node' && state.value.nodeId) {
      workflow.removeNode(state.value.nodeId);
    } else if (state.value.mode === 'edge' && state.value.edgeId) {
      workflow.removeEdge(state.value.edgeId);
    }
  } else if (action.type === 'rename') {
    // 通过事件总线/全局方法触发重命名
    window.dispatchEvent(
      new CustomEvent('meowflow:rename-node', { detail: { nodeId: state.value.nodeId } }),
    );
  } else if (action.type === 'disconnect') {
    if (state.value.nodeId) {
      const edges = workflow.current?.edges.filter(
        (e) => e.source === state.value.nodeId || e.target === state.value.nodeId,
      ) ?? [];
      for (const edge of edges) workflow.removeEdge(edge.id);
    }
  } else if (action.type === 'fit-view') {
    window.dispatchEvent(new CustomEvent('meowflow:fit-view'));
  } else if (action.type === 'toggle-minimap') {
    canvas.toggleMinimap();
  } else if (action.type === 'toggle-grid') {
    canvas.toggleGrid();
  } else if (action.type === 'paste') {
    document.execCommand('paste');
  }
  close();
}

onMounted(() => {
  const root = document.body;
  // 使用 capture 模式以便优先拦截
  root.addEventListener('contextmenu', (e: MouseEvent) => {
    const target = e.target as HTMLElement;
    if (!target) return;
    // 仅当画布区域内触发
    const inCanvas = !!target.closest('.workflow-canvas, .react-flow');
    if (!inCanvas) return;
    handleContextMenu(e);
    handlePaneContextMenu(e);
  });
});

onBeforeUnmount(() => {
  // 清理 listeners (因 anonymous handle，实际不卸载；这里做安全忽略)
});
</script>

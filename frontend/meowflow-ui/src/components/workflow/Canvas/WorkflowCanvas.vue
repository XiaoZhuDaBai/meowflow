<script setup lang="ts">
import { shallowRef, onBeforeUnmount, onMounted, nextTick, watch } from 'vue';
import type { WorkflowNode, WorkflowEdge } from '@/types/workflow';
import { useSelectionStore } from '@/stores/selection';
import { useCanvasStore } from '@/stores/canvas';
import CanvasContextMenu from '@/components/workflow/ContextMenu/CanvasContextMenu.vue';

const props = defineProps<{
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  readOnly?: boolean;
  debugMode?: boolean;
  breakpointNodeIds?: string[];
  pausedAtNodeId?: string;
}>();

const emit = defineEmits<{
  (e: 'update:nodes', updates: { id: string; x: number; y: number }[]): void;
  (e: 'connect', payload: { source: string; target: string; sourcePort?: string; targetPort?: string }): void;
  (e: 'select-node', node: WorkflowNode | null): void;
  (e: 'select-edge', edge: { id: string; source: string; target: string; label?: string; edgeType?: string; config?: Record<string, any> } | null): void;
  (e: 'drop-node', payload: { def: { type: string; name: string; category: string }; x: number; y: number }): void;
  (e: 'drop-note', payload: { def: { type: string; name: string; color?: string; icon?: string }; x: number; y: number }): void;
  (e: 'note-text', payload: { id: string; text: string }): void;
  (e: 'note-color', payload: { id: string; color: string }): void;
  (e: 'note-resize', payload: { id: string; width: number; height: number }): void;
  (e: 'fit-view'): void;
  (e: 'save'): void;
  (e: 'toggle-breakpoint', nodeId: string): void;
}>();

const containerRef = shallowRef<HTMLDivElement>();
const rootRef = shallowRef<any>(null);
const fitViewRef = shallowRef<(() => void) | null>(null);
let resizeObserver: ResizeObserver | null = null;
let visibilityPollingHandle: ReturnType<typeof setInterval> | null = null;

/**
 * 父容器是否已具备有效尺寸。ReactFlow 在 0 尺寸下初始化后会卡死，
 * 因此只有当容器真正"可见"才挂载 React 树。
 */
function hasVisibleSize(): boolean {
  const el = containerRef.value;
  if (!el) return false;
  const rect = el.getBoundingClientRect();
  return rect.width > 1 && rect.height > 1;
}

// Stores
const selectionStore = useSelectionStore();
const canvasStore = useCanvasStore();

/** Mount the React tree once; later prop changes are reconciled via syncReact. */
async function mountCanvas() {
  await nextTick();
  if (!containerRef.value) {
    if (import.meta.env.DEV) console.warn('[Canvas] no container, skip');
    return;
  }
  // ReactFlow 在 0 尺寸下挂载后无法恢复，因此等可见时再挂。
  if (!hasVisibleSize()) {
    waitForVisibleAndMount();
    return;
  }
  if (rootRef.value) return; // already mounted; let React diff on its own.

  let React: any, createRoot: any, CanvasWithProvider: any;
  try {
    React = await import('react');
    createRoot = (await import('react-dom/client')).createRoot;
    CanvasWithProvider = (await import('../Canvas/ReactFlowCanvas')).CanvasWithProvider;
  } catch (e) {
    console.error('[Canvas] import failed:', e);
    return;
  }

  rootRef.value = createRoot(containerRef.value);
  try {
    rootRef.value.render(
      React.createElement(CanvasWithProvider, buildCanvasProps()),
    );
  } catch (e) {
    console.error('[Canvas] render failed:', e);
  }
}

/**
 * 容器尺寸为 0 时（典型场景：嵌在 el-tab-pane 未激活的 pane 里），
 * 持续轮询直到有可见尺寸再挂载。
 */
function waitForVisibleAndMount() {
  if (visibilityPollingHandle) return;
  let tries = 0;
  visibilityPollingHandle = setInterval(() => {
    tries += 1;
    if (rootRef.value || !containerRef.value) {
      stopVisibilityPolling();
      return;
    }
    if (hasVisibleSize()) {
      stopVisibilityPolling();
      mountCanvas();
      return;
    }
    // 兜底：约 5s 后放弃，避免内存泄漏
    if (tries > 100) {
      stopVisibilityPolling();
    }
  }, 50);
}

function stopVisibilityPolling() {
  if (visibilityPollingHandle) {
    clearInterval(visibilityPollingHandle);
    visibilityPollingHandle = null;
  }
}

function buildCanvasProps() {
  return {
    nodes: props.nodes,
    edges: props.edges,
    readOnly: props.readOnly,
    debugMode: props.debugMode,
    breakpointNodeIds: props.breakpointNodeIds,
    pausedAtNodeId: props.pausedAtNodeId,
    showMinimap: canvasStore.minimapVisible,
    showGrid: canvasStore.gridVisible,
    onNodesChange: (updates: { id: string; x: number; y: number }[]) =>
      emit('update:nodes', updates),
    onConnect: (source: string, target: string, sourceHandle?: string, targetHandle?: string) =>
      emit('connect', {
        source,
        target,
        sourcePort: sourceHandle,
        targetPort: targetHandle,
      }),
    onSelectNode: (node: WorkflowNode | null) =>
      emit('select-node', node),
    onSelectEdge: (edge: any) =>
      emit('select-edge', edge),
    onSelectionChange: (sel: { nodeIds: string[]; edgeIds: string[] }) => {
      selectionStore.selectMultiple(sel.nodeIds, sel.edgeIds);
    },
    onDropNode: (def: { type: string; name: string; category: string }, flowPos: { x: number; y: number }) => {
      emit('drop-node', { def, x: flowPos.x, y: flowPos.y });
    },
    onDropNote: (def: { type: string; name: string; color?: string; icon?: string }, flowPos: { x: number; y: number }) => {
      emit('drop-note', { def, x: flowPos.x, y: flowPos.y });
    },
    onNoteTextChange: (id: string, text: string) => {
      emit('note-text', { id, text });
    },
    onNoteColorChange: (id: string, color: string) => {
      emit('note-color', { id, color });
    },
    onNoteResize: (id: string, width: number, height: number) => {
      emit('note-resize', { id, width, height });
    },
    onFitView: (fit: () => void) => {
      fitViewRef.value = fit;
    },
    onToggleBreakpoint: (nodeId: string) => emit('toggle-breakpoint', nodeId),
  };
}

let syncVersion = 0;
async function syncReact() {
  if (!rootRef.value) return;
  const version = ++syncVersion;
  try {
    const [{ createElement }, { CanvasWithProvider }] = await Promise.all([
      import('react'),
      import('../Canvas/ReactFlowCanvas'),
    ]);
    if (version !== syncVersion || !rootRef.value) return;
    rootRef.value.render(createElement(CanvasWithProvider, buildCanvasProps()));
  } catch (e) {
    console.error('[Canvas] update failed:', e);
  }
}

onMounted(() => {
  // 延迟挂载 React 树，等待父容器真正可见。
  // 关键点：模板预览里 WorkflowCanvas 嵌在 el-tab-pane 中，pane
  // 未激活时是 display:none，ReactFlow 会拿到 0 尺寸并布局错乱，
  // 切回 canvas tab 时画布就不见了。这里等 RAF 两帧再挂载，
  // 给外层 layout 留出完成时机。
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      mountCanvas();
    });
  });

  // 监听 fit-view 外部事件
  window.addEventListener('meowflow:fit-view', triggerFitView);

  // 监听容器尺寸变化：tab 切换 / dialog 高度变化时，
  // 一旦有可见尺寸立刻补挂 React 树（mountCanvas 内部已做幂等保护）。
  if (typeof ResizeObserver !== 'undefined' && containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      if (!rootRef.value && hasVisibleSize()) {
        mountCanvas();
      }
    });
    resizeObserver.observe(containerRef.value);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('meowflow:fit-view', triggerFitView);
  stopVisibilityPolling();
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
  if (rootRef.value) {
    try { rootRef.value.unmount(); } catch (_) {}
    rootRef.value = null;
  }
});

// 当节点/边或画布偏好变化时，把最新 props 推送到已有 React root。
watch(
  [
    () => props.nodes,
    () => props.edges,
    () => props.readOnly,
    () => props.debugMode,
    () => props.breakpointNodeIds,
    () => props.pausedAtNodeId,
    () => canvasStore.minimapVisible,
    () => canvasStore.gridVisible,
  ],
  () => syncReact(),
  { deep: true },
);

function triggerFitView() {
  if (fitViewRef.value) {
    fitViewRef.value();
  } else {
    // fitViewRef 还没就绪（多半是 React 树还没挂载），尝试挂一下。
    mountCanvas();
  }
}

function fitView() {
  triggerFitView();
}

defineExpose({ fitView });
</script>

<template>
  <div class="canvas-wrapper">
    <div ref="containerRef" class="canvas-host" />
    <CanvasContextMenu />
  </div>
</template>

<style scoped>
.canvas-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.canvas-host {
  width: 100%;
  height: 100%;
  background: #f8fafc;
}
</style>

<style>
.react-flow__node { cursor: grab !important; }
.react-flow__node:active { cursor: grabbing !important; }
.react-flow__edge-path { stroke-linecap: round; }
.react-flow__minimap {
  border: 1px solid #e2e8f0 !important;
  border-radius: 8px !important;
  overflow: hidden !important;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
}
.react-flow__controls { display: none !important; }
.react-flow__attribution { display: none !important; }

.react-flow__handle {
  opacity: 0 !important;
}
.react-flow__node:hover .react-flow__handle {
  opacity: 1 !important;
}

.react-flow__node.selected {
  outline: none;
}

.react-flow__edge.selected .react-flow__edge-path {
  stroke: #6366f1 !important;
  stroke-width: 2.5 !important;
}

.react-flow__selection {
  background: rgba(99,102,241,0.08) !important;
  border: 1px solid #6366f1 !important;
}
</style>

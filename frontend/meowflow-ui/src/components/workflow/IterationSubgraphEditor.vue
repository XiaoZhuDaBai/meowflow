<template>
  <el-dialog v-model="visible" title="编辑迭代子流程" width="1120px" top="4vh" destroy-on-close>
    <div class="subgraph-editor">
      <div class="palette-pane">
        <BlockSelector :featured-types="[]" @select="onAddNode" />
      </div>

      <div class="canvas-pane">
        <WorkflowCanvas
          :nodes="nodes"
          :edges="edges"
          @update:nodes="onMoveNodes"
          @connect="onConnect"
          @select-node="selectedNode = $event"
          @drop-node="onDropNode"
        />
      </div>

      <div class="config-pane">
        <template v-if="selectedNode">
          <div class="pane-title">子流程节点配置</div>
          <el-input v-model="selectedNode.name" placeholder="节点名称" @change="onNodeNameChange" />
          <NodeConfigDispatcher
            :node="selectedNode"
            :def="selectedDef"
            :config-value="selectedNode.config ?? {}"
            :variable-groups="[]"
            @update="onNodeConfigChange"
          />
        </template>
        <div v-else class="empty-config">选择节点后配置参数</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="commit">保存子流程</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import BlockSelector from './selector/BlockSelector.vue';
import WorkflowCanvas from './Canvas/WorkflowCanvas.vue';
import NodeConfigDispatcher from './config/NodeConfigDispatcher.vue';
import { findNodeDefinition } from '@/mock/nodes';
import { createNodeId, createEdgeId } from '@/mock/workflows';
import type { WorkflowNode, WorkflowEdge } from '@/types/workflow';
import type { NodeDefinition } from '@/types/node';

const visible = defineModel<boolean>({ default: false });
const emit = defineEmits<{
  (e: 'update:subgraph', value: { nodes: WorkflowNode[]; edges: WorkflowEdge[] }): void;
}>();

const props = defineProps<{
  subgraph: { nodes?: WorkflowNode[]; edges?: WorkflowEdge[] };
}>();

const nodes = ref<WorkflowNode[]>([]);
const edges = ref<WorkflowEdge[]>([]);
const selectedNode = ref<WorkflowNode | null>(null);

const selectedDef = computed<NodeDefinition | null>(() => {
  return selectedNode.value ? findNodeDefinition(selectedNode.value.type) ?? null : null;
});

watch(visible, (open) => {
  if (!open) return;
  nodes.value = clone(props.subgraph?.nodes ?? []);
  edges.value = clone(props.subgraph?.edges ?? []);
  selectedNode.value = null;
});

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value));
}

function onAddNode(def: NodeDefinition) {
  const offset = nodes.value.length * 32;
  nodes.value = [
    ...nodes.value,
    {
      id: createNodeId(),
      type: def.type,
      category: def.category,
      name: def.name,
      x: 120 + offset,
      y: 120 + offset,
      config: {},
      status: 'pending',
    },
  ];
}

function onDropNode(payload: { def: NodeDefinition; x: number; y: number }) {
  nodes.value = [
    ...nodes.value,
    {
      id: createNodeId(),
      type: payload.def.type,
      category: payload.def.category,
      name: payload.def.name,
      x: payload.x,
      y: payload.y,
      config: {},
      status: 'pending',
    },
  ];
}

function onMoveNodes(updates: { id: string; x: number; y: number }[]) {
  const map = new Map(updates.map((u) => [u.id, u]));
  nodes.value = nodes.value.map((n) => {
    const next = map.get(n.id);
    return next ? { ...n, x: next.x, y: next.y } : n;
  });
}

function onConnect(payload: {
  source: string;
  target: string;
  sourcePort?: string;
  targetPort?: string;
}) {
  if (payload.source === payload.target) return;
  if (edges.value.some((e) => e.source === payload.source && e.target === payload.target)) return;
  const sourceNode = nodes.value.find((n) => n.id === payload.source);
  const targetNode = nodes.value.find((n) => n.id === payload.target);
  const patch: { edgeType?: 'condition'; label?: string } = {};
  if (sourceNode?.type === 'flow.condition') {
    patch.edgeType = 'condition';
    patch.label = payload.sourcePort === 'false'
      ? 'false'
      : payload.sourcePort === 'true'
        ? 'true'
        : edges.value.filter((e) => e.source === payload.source).length === 0
          ? 'true'
          : 'false';
  } else if (sourceNode?.type === 'flow.if-else') {
    patch.edgeType = 'condition';
    patch.label = payload.sourcePort || targetNode?.name || payload.target;
  }
  edges.value = [...edges.value, {
    id: createEdgeId(),
    source: payload.source,
    target: payload.target,
    ...patch,
  }];
}

function onNodeNameChange() {
  if (!selectedNode.value) return;
  nodes.value = nodes.value.map((n) => (n.id === selectedNode.value!.id ? { ...selectedNode.value! } : n));
}

function onNodeConfigChange(config: Record<string, any>) {
  if (!selectedNode.value) return;
  selectedNode.value = { ...selectedNode.value, config: { ...(selectedNode.value.config ?? {}), ...config } };
  nodes.value = nodes.value.map((n) => (n.id === selectedNode.value!.id ? { ...selectedNode.value! } : n));
}

function commit() {
  emit('update:subgraph', {
    nodes: clone(nodes.value),
    edges: clone(edges.value),
  });
  visible.value = false;
}
</script>

<style scoped>
.subgraph-editor {
  display: grid;
  grid-template-columns: 220px 1fr 300px;
  gap: 10px;
  height: 660px;
}
.palette-pane,
.canvas-pane,
.config-pane {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-primary);
  overflow: hidden;
}
.palette-pane { overflow: auto; }
.canvas-pane { min-width: 0; }
.config-pane { padding: 12px; overflow: auto; }
.pane-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}
.config-pane :deep(.el-input) { margin-bottom: 10px; }
.empty-config {
  color: var(--text-tertiary);
  font-size: 12px;
  text-align: center;
  margin-top: 20px;
}
</style>

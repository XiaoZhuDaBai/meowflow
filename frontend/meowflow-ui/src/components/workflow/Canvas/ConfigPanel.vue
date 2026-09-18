<template>
  <div
    class="config-panel"
    v-if="node || edge"
    :style="{ width: `${panelWidth}px` }"
  >
    <!-- 左侧拖拽手柄 -->
    <div
      class="resize-handle"
      :class="{ dragging: isDragging }"
      @mousedown="onResizeStart"
      title="拖拽调整宽度"
    >
      <div class="handle-grip" />
    </div>

    <div class="panel-header">
      <span>{{ node ? '节点配置' : '连线配置' }}</span>
      <button class="close-btn" @click="$emit('close')" title="关闭">
        <i class="fa-solid fa-xmark"></i>
      </button>
    </div>
    <div class="panel-body">
      <template v-if="node">
        <!-- 节点元数据卡 -->
        <div class="meta-card">
          <div class="meta-row">
            <!-- 状态 -->
            <span class="meta-badge" :style="{ color: getStatusStyle(node.status).color, background: getStatusStyle(node.status).bg }">
              <i class="fa-solid fa-circle" style="font-size:6px"></i>
              {{ getStatusStyle(node.status).label }}
            </span>
            <!-- 分类 -->
            <span class="meta-badge" :style="{ color: getCategoryStyle(node.category).color, background: getCategoryStyle(node.category).bg }">
              {{ nodeDef?.name ?? node.category ?? '节点' }}
            </span>
          </div>
          <div class="meta-row meta-row-2">
            <span class="meta-chip">
              <i class="fa-solid fa-fingerprint"></i>
              <span class="mono" :title="node.id">{{ shortId(node.id) }}</span>
            </span>
            <span class="meta-chip">
              <i class="fa-solid fa-font"></i>
              <code>{{ node.type }}</code>
            </span>
          </div>
          <!-- 模板来源 -->
          <div v-if="templateId" class="meta-from-template">
            <i class="fa-solid fa-layer-group"></i>
            <span>来自模板</span>
          </div>
        </div>

        <!-- 入参 / 出参 -->
        <template v-if="nodeDef">
          <div v-if="nodeDef.inputs?.length" class="io-section">
            <div class="io-title"><i class="fa-solid fa-signs-post"></i> 入参</div>
            <div class="io-chips">
              <span v-for="p in nodeDef.inputs" :key="p.key" class="io-chip">
                <code>{{ p.key }}</code>
                <span>{{ p.label }}</span>
              </span>
            </div>
          </div>
          <div v-if="nodeDef.outputs?.length" class="io-section">
            <div class="io-title"><i class="fa-solid fa-paper-plane"></i> 出参</div>
            <div class="io-chips">
              <span v-for="p in nodeDef.outputs" :key="p.key" class="io-chip out">
                <code>{{ p.key }}</code>
                <span>{{ p.label }}</span>
              </span>
            </div>
          </div>
        </template>

        <el-divider style="margin: 12px 0" />

        <!-- 基础信息 -->
        <div class="form-group">
          <label>节点名称</label>
          <el-input
            v-model="localName"
            size="small"
            placeholder="节点名称"
            @change="onNameChange"
          />
        </div>
        <div class="form-group">
          <label>节点类型</label>
          <el-input :model-value="node.type" size="small" disabled />
        </div>
        <div class="form-group">
          <label>节点描述</label>
          <el-input
            v-model="localDescription"
            size="small"
            type="textarea"
            :rows="2"
            placeholder="描述..."
            @change="onDescriptionChange"
          />
        </div>

        <el-divider style="margin: 12px 0" />

        <!-- 核心：接入 NodeConfigDispatcher -->
        <NodeConfigDispatcher
          v-if="nodeDef"
          :node="node"
          :def="nodeDef"
          :config-value="node.config ?? {}"
          :variable-groups="variableGroups"
          :workflow-id="workflowStore.current?.id != null ? String(workflowStore.current.id) : undefined"
          @update="onConfigChange"
        />
        <GenericConfig
          v-else-if="nodeDef === null"
          :params="[]"
          :model-value="node.config ?? {}"
          @update:model-value="onConfigChange"
        />
      </template>

      <template v-else-if="edge">
        <div class="meta-card">
          <div class="meta-row">
            <span class="meta-badge" :style="{ color: edgeTypeStyle.color, background: edgeTypeStyle.bg }">
              <i :class="edgeTypeStyle.icon"></i>
              {{ edgeTypeStyle.label }}
            </span>
          </div>
          <div class="meta-row meta-row-2">
            <span class="meta-chip">
              <i class="fa-solid fa-fingerprint"></i>
              <span class="mono" :title="edge.id">{{ shortId(edge.id) }}</span>
            </span>
          </div>
        </div>

        <el-divider style="margin: 12px 0" />

        <div class="form-group">
          <label>起点节点</label>
          <el-input :model-value="sourceNodeName" size="small" disabled />
        </div>
        <div class="form-group">
          <label>终点节点</label>
          <el-input :model-value="targetNodeName" size="small" disabled />
        </div>

        <div class="form-group">
          <label>连线类型</label>
          <el-select v-model="localEdgeType" size="small" @change="onEdgeTypeChange">
            <el-option value="default" label="默认（顺序）" />
            <el-option value="condition" label="条件分支" />
            <el-option value="loop" label="循环" />
            <el-option value="error" label="异常" />
          </el-select>
        </div>

        <div class="form-group">
          <label>连线标签 (画布显示)</label>
          <el-input
            v-model="localLabel"
            size="small"
            placeholder="例如 通过 / 失败 / 重试"
            @change="onLabelChange"
          />
        </div>

        <el-divider style="margin: 12px 0" />

        <div v-if="localEdgeType === 'condition'" class="form-group">
          <label>条件表达式</label>
          <el-input
            v-model="localConfig.expression"
            type="textarea"
            :rows="3"
            size="small"
            placeholder='{{input.score}} >= 80'
            @change="onConfigFieldChange"
          />
          <span class="hint">支持 <code>{{ '\{\{var.path\}\}' }}</code> 变量，例如 <code>{{ '\{\{input.score\}\}' }} &gt;= 80</code></span>
        </div>

        <div v-else-if="localEdgeType === 'loop'" class="form-group">
          <label>循环条件</label>
          <el-input
            v-model="localConfig.condition"
            size="small"
            placeholder="{{hasMore}} === true"
            @change="onConfigFieldChange"
          />
          <span class="hint">为 true 时持续循环，为 false 时退出</span>
        </div>

        <div v-else-if="localEdgeType === 'error'" class="form-group">
          <label>触发错误的状态</label>
          <el-select v-model="localConfig.errorOn" size="small" multiple @change="onConfigFieldChange">
            <el-option value="failed" label="失败" />
            <el-option value="timeout" label="超时" />
            <el-option value="exception" label="异常" />
          </el-select>
        </div>

        <div v-else class="form-group">
          <label class="muted">默认连线无需额外条件</label>
        </div>

        <el-divider style="margin: 12px 0" />

        <div class="form-group">
          <label>备注 / 自定义参数 (JSON)</label>
          <el-input
            :model-value="customParamsJson"
            type="textarea"
            :rows="3"
            size="small"
            placeholder='{"key": "value"}'
            @change="onCustomParamsChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onBeforeUnmount } from 'vue';
import type { WorkflowNode } from '@/types/workflow';
import type { NodeDefinition } from '@/types/node';
import type { VariableGroup } from '../config/VariablePicker.vue';
import NodeConfigDispatcher from '@/components/workflow/config/NodeConfigDispatcher.vue';
import GenericConfig from '@/components/workflow/config/GenericConfig.vue';
import { findNodeDefinition } from '@/mock/nodes';
import { useWorkflowStore } from '@/stores/workflow';
import { buildVariableGroups } from '@/utils/variableGraph';

export interface SelectedEdge {
  id: string;
  source: string;
  target: string;
  sourcePort?: string;
  targetPort?: string;
  label?: string;
  edgeType?: 'default' | 'condition' | 'loop' | 'error';
  config?: Record<string, any>;
}

const props = defineProps<{
  node?: WorkflowNode | null;
  edge?: SelectedEdge | null;
  def?: NodeDefinition | null;
  templateId?: string;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'update-node-config', config: Record<string, any>): void;
  (e: 'update-node-name', name: string): void;
  (e: 'update-node-description', description: string): void;
  (e: 'update-edge', patch: Partial<SelectedEdge>): void;
}>();

// 面板宽度（可拖拽调整）
const PANEL_WIDTH_KEY = 'meowflow.config-panel.width';
const DEFAULT_WIDTH = 280;
const MIN_WIDTH = 240;
const MAX_WIDTH = 720;

function loadPanelWidth(): number {
  try {
    const raw = localStorage.getItem(PANEL_WIDTH_KEY);
    if (raw) {
      const w = Number(raw);
      if (Number.isFinite(w) && w >= MIN_WIDTH && w <= MAX_WIDTH) {
        return w;
      }
    }
  } catch {
    /* ignore */
  }
  return DEFAULT_WIDTH;
}

const panelWidth = ref(loadPanelWidth());
const isDragging = ref(false);
let dragStartX = 0;
let dragStartWidth = 0;

function onResizeStart(e: MouseEvent) {
  e.preventDefault();
  dragStartX = e.clientX;
  dragStartWidth = panelWidth.value;
  isDragging.value = true;

  document.addEventListener('mousemove', onResizeMove);
  document.addEventListener('mouseup', onResizeEnd);
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
}

function onResizeMove(e: MouseEvent) {
  const delta = dragStartX - e.clientX;
  const next = dragStartWidth + delta;
  panelWidth.value = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, next));
}

function onResizeEnd() {
  isDragging.value = false;
  document.removeEventListener('mousemove', onResizeMove);
  document.removeEventListener('mouseup', onResizeEnd);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';

  try {
    localStorage.setItem(PANEL_WIDTH_KEY, String(panelWidth.value));
  } catch {
    /* ignore */
  }
}

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onResizeMove);
  document.removeEventListener('mouseup', onResizeEnd);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
});

const localName = ref('');
const localDescription = ref('');

const workflowStore = useWorkflowStore();

const nodeDef = computed<NodeDefinition | null>(() => {
  if (props.node) return findNodeDefinition(props.node.type) ?? null;
  return props.def ?? null;
});

const STATUS_MAP: Record<string, { label: string; color: string; bg: string }> = {
  pending:   { label: '待执行', color: '#64748b', bg: '#f1f5f9' },
  running:   { label: '执行中', color: '#2563eb', bg: '#eff6ff' },
  success:   { label: '成功',   color: '#16a34a', bg: '#f0fdf4' },
  failed:    { label: '失败',   color: '#dc2626', bg: '#fef2f2' },
  skipped:   { label: '跳过',   color: '#d97706', bg: '#fffbeb' },
};

function getStatusStyle(status?: string) {
  return STATUS_MAP[status ?? 'pending'] ?? STATUS_MAP['pending'];
}

const CATEGORY_COLOR_MAP: Record<string, { color: string; bg: string }> = {
  trigger: { color: '#7c3aed', bg: '#ede9fe' },
  ai:      { color: '#1d4ed8', bg: '#dbeafe' },
  flow:    { color: '#059669', bg: '#d1fae5' },
  tool:    { color: '#d97706', bg: '#fef3c7' },
  notify:  { color: '#db2777', bg: '#fce7f3' },
};

function getCategoryStyle(category?: string) {
  return CATEGORY_COLOR_MAP[category ?? 'flow'] ?? { color: '#475569', bg: '#f1f5f9' };
}

function shortId(id: string) {
  return id.length > 12 ? id.slice(0, 6) + '…' + id.slice(-4) : id;
}

// ===================== Edge 配置 =====================

const localEdgeType = ref<SelectedEdge['edgeType']>('default');
const localLabel = ref('');
const localConfig = ref<Record<string, any>>({});
const customParamsJson = ref('');

interface EdgeTypeStyle { label: string; icon: string; color: string; bg: string }
const EDGE_TYPE_STYLES: Record<string, EdgeTypeStyle> = {
  default:   { label: '默认',   icon: 'fa-solid fa-arrow-right',  color: '#475569', bg: '#f1f5f9' },
  condition: { label: '条件',   icon: 'fa-solid fa-code-branch',   color: '#d97706', bg: '#fef3c7' },
  loop:      { label: '循环',   icon: 'fa-solid fa-rotate',        color: '#059669', bg: '#d1fae5' },
  error:     { label: '异常',   icon: 'fa-solid fa-triangle-exclamation', color: '#dc2626', bg: '#fee2e2' },
};

const edgeTypeStyle = computed<EdgeTypeStyle>(() => EDGE_TYPE_STYLES[localEdgeType.value ?? 'default'] ?? EDGE_TYPE_STYLES.default);

const sourceNodeName = computed(() => {
  const id = props.edge?.source;
  if (!id) return '';
  return workflowStore.current?.nodes.find((n) => n.id === id)?.name ?? id;
});

const targetNodeName = computed(() => {
  const id = props.edge?.target;
  if (!id) return '';
  return workflowStore.current?.nodes.find((n) => n.id === id)?.name ?? id;
});

watch(
  () => props.edge,
  (e) => {
    if (!e) {
      localEdgeType.value = 'default';
      localLabel.value = '';
      localConfig.value = {};
      customParamsJson.value = '';
      return;
    }
    localEdgeType.value = (e.edgeType ?? 'default') as SelectedEdge['edgeType'];
    localLabel.value = e.label ?? '';
    const cfg: Record<string, any> = { ...(e.config ?? {}) };
    if (localEdgeType.value === 'condition' && cfg.expression === undefined) cfg.expression = '';
    if (localEdgeType.value === 'loop' && cfg.condition === undefined) cfg.condition = '';
    if (localEdgeType.value === 'error' && !Array.isArray(cfg.errorOn)) cfg.errorOn = ['failed'];
    localConfig.value = cfg;
    const reserved = new Set(['expression', 'condition', 'errorOn']);
    const extras: Record<string, any> = {};
    Object.keys(localConfig.value).forEach((k) => {
      if (!reserved.has(k)) extras[k] = localConfig.value[k];
    });
    customParamsJson.value = Object.keys(extras).length ? JSON.stringify(extras, null, 2) : '';
  },
  { immediate: true },
);

function onEdgeTypeChange(t: SelectedEdge['edgeType']) {
  localEdgeType.value = t;
  const cfg: Record<string, any> = { ...localConfig.value };
  if (t === 'condition' && cfg.expression === undefined) cfg.expression = '';
  if (t === 'loop' && cfg.condition === undefined) cfg.condition = '';
  if (t === 'error' && !Array.isArray(cfg.errorOn)) cfg.errorOn = ['failed'];
  localConfig.value = cfg;
  emit('update-edge', { edgeType: t, config: { ...cfg } });
}

function onLabelChange(label: string) {
  localLabel.value = label;
  emit('update-edge', { label });
}

function onConfigFieldChange() {
  emit('update-edge', { config: { ...localConfig.value } });
}

function onCustomParamsChange(text: string) {
  customParamsJson.value = text;
  if (!text.trim()) {
    const reserved = new Set(['expression', 'condition', 'errorOn']);
    const next: Record<string, any> = {};
    Object.keys(localConfig.value).forEach((k) => {
      if (reserved.has(k)) next[k] = localConfig.value[k];
    });
    localConfig.value = next;
    emit('update-edge', { config: { ...next } });
    return;
  }
  try {
    const parsed = JSON.parse(text);
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const reserved = new Set(['expression', 'condition', 'errorOn']);
      const next: Record<string, any> = {};
      Object.keys(localConfig.value).forEach((k) => {
        if (reserved.has(k)) next[k] = localConfig.value[k];
      });
      Object.keys(parsed).forEach((k) => {
        next[k] = (parsed as any)[k];
      });
      localConfig.value = next;
      emit('update-edge', { config: { ...next } });
    }
  } catch {
    /* ignore invalid JSON */
  }
}

// ===================== Node 配置 =====================

watch(
  () => props.node,
  (n) => {
    if (n) {
      localName.value = n.name ?? '';
      localDescription.value = n.description ?? '';
    }
  },
  { immediate: true },
);

function onNameChange() {
  emit('update-node-name', localName.value);
}

function onDescriptionChange() {
  emit('update-node-description', localDescription.value);
}

function onConfigChange(config: Record<string, any>) {
  emit('update-node-config', config);
}

const variableGroups = computed<VariableGroup[]>(() => {
  if (!props.node) return [];
  return buildVariableGroups(workflowStore.current, props.node.id, workflowStore.current);
});
</script>

<style scoped>
.config-panel {
  position: relative;
  background: #fff;
  border-left: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  min-width: 240px;
  max-width: 720px;
}

/* 左侧拖拽手柄 */
.resize-handle {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.15s;
}

.resize-handle:hover,
.resize-handle.dragging {
  background-color: rgba(99, 102, 241, 0.08);
}

.resize-handle.dragging {
  background-color: rgba(99, 102, 241, 0.15);
}

.handle-grip {
  width: 2px;
  height: 32px;
  background: #cbd5e1;
  border-radius: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.resize-handle:hover .handle-grip,
.resize-handle.dragging .handle-grip {
  opacity: 1;
  background: #6366f1;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  font-size: 12px;
  padding: 2px;
}
.close-btn:hover {
  color: #ef4444;
}

.panel-body {
  padding: 16px;
  overflow-y: auto;
  flex: 1;
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
  font-weight: 500;
}

/* 节点元数据卡片 */
.meta-card {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.meta-row-2 {
  border-top: 1px dashed #e2e8f0;
  padding-top: 8px;
  margin-top: -2px;
}

.meta-badge {
  font-size: 10.5px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #64748b;
}

.meta-chip i { font-size: 10px; }
.meta-chip code {
  font-size: 10px;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 1px 5px;
  border-radius: 4px;
  color: #6366f1;
}
.meta-chip .mono {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  color: #94a3b8;
}

.meta-from-template {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10.5px;
  color: #6366f1;
  background: #ede9fe;
  padding: 3px 8px;
  border-radius: 6px;
  width: fit-content;
}

/* 入参/出参 */
.io-section {
  margin-bottom: 10px;
}

.io-title {
  font-size: 10.5px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.io-title i { color: #6366f1; font-size: 9.5px; }

.io-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.io-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 2px 7px;
  border-radius: 5px;
  color: #64748b;
}

.io-chip code {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  color: #6366f1;
  font-size: 9.5px;
}

.io-chip.out {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #16a34a;
}
.io-chip.out code { color: #16a34a; }

/* Edge 配置 */
.hint {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.5;
}
.hint code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 10.5px;
  color: #475569;
}
.muted {
  color: var(--text-tertiary);
  font-size: 11px;
}
</style>

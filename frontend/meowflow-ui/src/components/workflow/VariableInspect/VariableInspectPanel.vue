<template>
  <div class="variable-inspect-panel" :style="{ width: `${panelWidth}px` }">
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
      <div class="header-title">
        <i class="fa-solid fa-list-tree" />
        <span>变量检查器</span>
      </div>
      <button class="close-btn" @click="emit('close')">
        <i class="fa-solid fa-xmark" />
      </button>
    </div>

    <div class="panel-body">
      <div v-if="!node" class="empty">
        <i class="fa-solid fa-circle-info" />
        <p>选择画布上的节点以查看其可用的输入/输出变量</p>
      </div>

      <template v-else>
        <!-- 当前节点标题 -->
        <div class="section">
          <div class="node-header">
            <div class="node-info">
              <div class="node-name">{{ node.name }}</div>
              <div class="node-type">{{ node.type }}</div>
            </div>
          </div>
        </div>

        <!-- 输入变量 -->
        <div class="section">
          <div class="section-title">
            <i class="fa-solid fa-arrow-down" />
            <span>输入变量</span>
            <el-tag size="small" type="info">{{ consumerInputs.length }}</el-tag>
          </div>
          <div v-if="consumerInputs.length === 0" class="empty-state">
            此节点无输入变量
          </div>
          <div v-else class="var-list">
            <div
              v-for="v in consumerInputs"
              :key="v.name"
              class="var-item input-item"
            >
              <div class="var-info">
                <span class="var-name">
                  {{ v.name }}
                  <span v-if="v.required" class="required">*</span>
                </span>
                <span class="var-type">{{ v.type }}</span>
              </div>
              <div v-if="v.description" class="var-desc">{{ v.description }}</div>
            </div>
          </div>
        </div>

        <!-- 输出变量 -->
        <div class="section">
          <div class="section-title">
            <i class="fa-solid fa-arrow-up" />
            <span>输出变量</span>
            <el-tag size="small" type="success">{{ providerOutputs.length }}</el-tag>
          </div>
          <div v-if="providerOutputs.length === 0" class="empty-state">
            此节点无输出变量
          </div>
          <div v-else class="var-list">
            <div
              v-for="v in providerOutputs"
              :key="v.name"
              class="var-item output-item"
              @click="copyRef(v.name)"
            >
              <div class="var-info">
                <span class="var-name">{{ v.name }}</span>
                <span class="var-type">{{ v.type }}</span>
              </div>
              <div v-if="v.description" class="var-desc">{{ v.description }}</div>
              <div class="copy-hint">点击复制引用</div>
            </div>
          </div>
        </div>

        <!-- 上游提供方 -->
        <div class="section" v-if="upstreamProviders.length > 0">
          <div class="section-title">
            <i class="fa-solid fa-arrow-right" />
            <span>可引用的上游变量</span>
            <el-tag size="small">{{ upstreamProviders.length }}</el-tag>
          </div>
          <div class="var-list">
            <div
              v-for="provider in upstreamProviders"
              :key="provider.nodeId"
              class="provider-group"
            >
              <div class="provider-name">
                <i :class="categoryIcon(provider.category)" />
                {{ provider.nodeName }}
              </div>
              <div class="provider-vars">
                <div
                  v-for="v in provider.variables"
                  :key="v.name"
                  class="ref-item"
                  @click="copyRef(provider.nodeName, v.name)"
                >
                  <span class="ref-name">{{ provider.nodeName }}.{{ v.name }}</span>
                  <span class="ref-type">{{ v.type }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
import { ElTag, ElMessage } from 'element-plus';
import type { WorkflowNode } from '@/types/workflow';
import {
  getConsumerInputs,
  getProviderOutputs,
  getUpstreamProviders,
  buildVariableReference,
} from '@/utils/variableInspector';
import { useWorkflowStore } from '@/stores/workflow';

const props = defineProps<{
  node: WorkflowNode | null;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const store = useWorkflowStore();

// 面板宽度可拖拽调整
const PANEL_WIDTH_KEY = 'meowflow.variable-inspect.width';
const DEFAULT_WIDTH = 320;
const MIN_WIDTH = 240;
const MAX_WIDTH = 600;

function loadPanelWidth(): number {
  try {
    const raw = localStorage.getItem(PANEL_WIDTH_KEY);
    if (raw) {
      const w = Number(raw);
      if (Number.isFinite(w) && w >= MIN_WIDTH && w <= MAX_WIDTH) return w;
    }
  } catch { /* ignore */ }
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
  // 面板在右侧，向左拖动增加宽度
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
  } catch { /* ignore */ }
}

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onResizeMove);
  document.removeEventListener('mouseup', onResizeEnd);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
});

const consumerInputs = computed(() => {
  if (!props.node) return [];
  return getConsumerInputs(props.node);
});

const providerOutputs = computed(() => {
  if (!props.node) return [];
  return getProviderOutputs(props.node);
});

const upstreamProviders = computed(() => {
  if (!props.node) return [];
  return getUpstreamProviders(
    store.current?.nodes ?? [],
    store.current?.edges ?? [],
    props.node.id,
  );
});

function categoryIcon(category: string) {
  const map: Record<string, string> = {
    trigger: 'fa-solid fa-bolt',
    ai: 'fa-solid fa-brain',
    flow: 'fa-solid fa-diagram-project',
    tool: 'fa-solid fa-wrench',
    notify: 'fa-solid fa-paper-plane',
  };
  return map[category] ?? 'fa-solid fa-circle';
}

async function copyRef(nodeName: string, varName?: string) {
  const ref = buildVariableReference(nodeName, varName || 'output');
  try {
    await navigator.clipboard.writeText(ref);
    ElMessage.success(`已复制: ${ref}`);
  } catch {
    ElMessage.warning('复制失败,请检查浏览器权限');
  }
}
</script>

<style scoped>
.variable-inspect-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 320px;
  background: #fff;
  border-left: 1px solid #e2e8f0;
  box-shadow: -4px 0 16px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  z-index: 25;
  min-width: 240px;
  max-width: 600px;
}

/* 左侧拖拽手柄 */
.resize-handle {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  z-index: 30;
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
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #64748b;
}
.close-btn:hover {
  background: #f1f5f9;
  color: #ef4444;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #94a3b8;
  gap: 12px;
  font-size: 13px;
}
.empty i {
  font-size: 32px;
  color: #cbd5e1;
}

.section {
  margin-bottom: 16px;
}

.node-header {
  padding: 8px 0;
}

.node-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.node-type {
  font-size: 11px;
  color: #94a3b8;
  font-family: monospace;
  margin-top: 2px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.empty-state {
  padding: 8px 0;
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

.var-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.var-item {
  padding: 8px 10px;
  background: #f8fafc;
  border-radius: 6px;
  cursor: default;
  transition: background 0.15s;
}
.var-item.input-item {
  border-left: 3px solid #6366f1;
}
.var-item.output-item {
  border-left: 3px solid #10b981;
  cursor: pointer;
}
.var-item.output-item:hover {
  background: #f0fdf4;
}

.var-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.var-name {
  font-size: 12px;
  color: #1e293b;
  font-family: monospace;
}

.required {
  color: #ef4444;
  font-weight: 700;
}

.var-type {
  font-size: 10px;
  padding: 2px 6px;
  background: #fff;
  border-radius: 4px;
  color: #63648b;
  font-family: monospace;
}

.var-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.copy-hint {
  font-size: 10px;
  color: #94a3b8;
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.var-item.output-item:hover .copy-hint {
  opacity: 1;
}

.provider-group {
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 4px;
}

.provider-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}
.provider-name i {
  font-size: 11px;
}

.provider-vars {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ref-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.15s;
}
.ref-item:hover {
  background: #eef2ff;
}

.ref-name {
  font-family: monospace;
  color: #1e293b;
}

.ref-type {
  font-size: 10px;
  padding: 1px 4px;
  background: #fff;
  border-radius: 3px;
  color: #64748b;
}
</style>

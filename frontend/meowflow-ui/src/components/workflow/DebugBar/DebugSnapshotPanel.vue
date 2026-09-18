<script setup lang="ts">
/**
 * 调试模式变量快照面板 (DebugSnapshotPanel)
 *
 * 在调试模式下展示所有已执行节点的输出变量
 * 点击节点切换查看不同节点的快照
 */
import { computed, ref } from 'vue';
import { useDebugStore } from '@/stores/debug';

const debug = useDebugStore();

const selectedNodeId = ref<string>('');

// 所有有快照的节点
const availableNodes = computed(() => {
  return Object.keys(debug.nodeSnapshots).sort();
});

const currentSnapshot = computed(() => {
  const nodeId = selectedNodeId.value || debug.pausedAtNodeId;
  if (!nodeId) return null;
  return debug.nodeSnapshots[nodeId] || null;
});

function selectNode(nodeId: string) {
  selectedNodeId.value = nodeId;
}

function formatValue(value: any): string {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (typeof value === 'string') return `"${value}"`;
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }
  return String(value);
}

function getValueType(value: any): string {
  if (value === null) return 'null';
  if (Array.isArray(value)) return `Array(${value.length})`;
  if (typeof value === 'object') return 'Object';
  return typeof value;
}
</script>

<template>
  <div class="snapshot-panel" v-if="debug.isDebugMode">
    <div class="panel-header">
      <span class="title">
        <span class="dot"></span>
        调试快照
      </span>
      <span class="count">{{ availableNodes.length }} 个节点已执行</span>
    </div>

    <!-- 节点列表 -->
    <div class="node-tabs">
      <div
        v-for="nodeId in availableNodes"
        :key="nodeId"
        class="tab"
        :class="{
          active: (selectedNodeId || debug.pausedAtNodeId) === nodeId,
          paused: debug.pausedAtNodeId === nodeId
        }"
        @click="selectNode(nodeId)"
      >
        <span class="tab-dot" v-if="debug.pausedAtNodeId === nodeId"></span>
        {{ nodeId }}
      </div>
      <div v-if="availableNodes.length === 0" class="empty">
        等待节点执行…
      </div>
    </div>

    <!-- 变量列表 -->
    <div class="var-table" v-if="currentSnapshot">
      <div
        v-for="(value, key) in currentSnapshot"
        :key="key"
        class="var-row"
      >
        <div class="var-key">
          <span class="key-name">{{ key }}</span>
          <span class="key-type">{{ getValueType(value) }}</span>
        </div>
        <div class="var-value">
          <pre>{{ formatValue(value) }}</pre>
        </div>
      </div>
    </div>
    <div class="empty-state" v-else>
      <p>选择上方节点查看其执行快照</p>
    </div>
  </div>
</template>

<style scoped>
.snapshot-panel {
  position: absolute;
  top: 44px;  /* 调试控制条下方 */
  right: 0;
  bottom: 0;
  width: 380px;
  background: #fff;
  border-left: 1px solid #e2e8f0;
  box-shadow: -4px 0 16px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  z-index: 24;
  font-size: 13px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  background: linear-gradient(180deg, #fef3c7 0%, #fffbeb 100%);
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #92400e;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f59e0b;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.count {
  font-size: 11px;
  color: #78716c;
  background: rgba(245, 158, 11, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
}

.node-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px 12px;
  border-bottom: 1px solid #f1f5f9;
  max-height: 120px;
  overflow-y: auto;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
  font-family: monospace;
  transition: all 0.15s;
}

.tab:hover {
  background: #eef2ff;
  border-color: #6366f1;
}

.tab.active {
  background: #6366f1;
  color: #fff;
  border-color: #4f46e5;
}

.tab.paused {
  border-color: #f59e0b;
  background: #fef3c7;
  color: #92400e;
}

.tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  animation: pulse 1s infinite;
}

.empty {
  width: 100%;
  padding: 12px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
}

.var-table {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.var-row {
  margin-bottom: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
}

.var-key {
  display: flex;
  justify-content: space-between;
  padding: 6px 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-size: 12px;
}

.key-name {
  font-weight: 600;
  font-family: monospace;
  color: #1e293b;
}

.key-type {
  font-size: 10px;
  color: #6366f1;
  background: #eef2ff;
  padding: 2px 6px;
  border-radius: 3px;
}

.var-value {
  padding: 8px 10px;
  background: #fff;
  font-family: monospace;
  font-size: 12px;
}

.var-value pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: #334155;
  max-height: 200px;
  overflow-y: auto;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}
</style>

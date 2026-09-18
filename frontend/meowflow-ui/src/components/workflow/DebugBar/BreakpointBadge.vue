<script setup lang="ts">
/**
 * 节点断点徽章 (BreakpointBadge)
 *
 * 显示在节点右上角的小红点 + 切换断点
 */
import { computed } from 'vue';
import { useDebugStore } from '@/stores/debug';

const props = defineProps<{
  nodeId: string;
}>();

const debug = useDebugStore();

const hasBreakpoint = computed(() => debug.breakpointNodeIds.has(props.nodeId));
const isPausedHere = computed(
  () => debug.pausedAtNodeId === props.nodeId && debug.debugState === 'PAUSED'
);

function handleClick(e: MouseEvent) {
  e.stopPropagation();
  if (!debug.isDebugMode) return;
  debug.toggleBreakpoint(props.nodeId);
}
</script>

<template>
  <div
    v-if="debug.isDebugMode || hasBreakpoint"
    class="breakpoint-badge"
    :class="{ active: hasBreakpoint, paused: isPausedHere }"
    :title="hasBreakpoint ? '点击移除断点' : '点击设置断点'"
    @click="handleClick"
  >
    <span v-if="isPausedHere" class="pulse"></span>
  </div>
</template>

<style scoped>
.breakpoint-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #cbd5e1;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 10;
}

.breakpoint-badge:hover {
  transform: scale(1.2);
  border-color: #ef4444;
}

.breakpoint-badge.active {
  background: #ef4444;
  border-color: #b91c1c;
}

.breakpoint-badge.paused {
  background: #f59e0b;
  border-color: #d97706;
  animation: shake 0.6s ease-in-out infinite;
}

@keyframes shake {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

.pulse {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: #f59e0b;
  opacity: 0.4;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(2); opacity: 0; }
}
</style>

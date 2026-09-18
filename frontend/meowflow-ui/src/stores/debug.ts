/**
 * 调试状态管理 (断点 / 单步 / 暂停 / 继续)
 *
 * 参考 Coze Studio / Dify Workflow debug 设计：
 * - breakpointNodeIds: 用户在画布上标记的断点
 * - debugState: RUNNING | PAUSED | STEPPING | STOPPED | IDLE
 * - pausedAtNodeId: 当前暂停节点
 * - nodeSnapshots: 节点执行快照（用于变量查看器）
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { serviceUrl } from '@/api/endpoints';

export type DebugState = 'IDLE' | 'RUNNING' | 'PAUSED' | 'STEPPING' | 'STOPPED';

interface DebugSnapshot {
  executionId: string | number;
  state: DebugState;
  pausedAtNodeId: string;
  pauseReason: string;
  breakpoints: string[];
  nodeSnapshots: Record<string, Record<string, any>>;
}

export const useDebugStore = defineStore('debug', () => {
  // ---------- State ----------
  const currentExecutionId = ref<string | number | null>(null);
  const breakpointNodeIds = ref<Set<string>>(new Set());
  const debugState = ref<DebugState>('IDLE');
  const pausedAtNodeId = ref<string>('');
  const pauseReason = ref<string>('');
  const nodeSnapshots = ref<Record<string, Record<string, any>>>({});
  const isDebugMode = ref<boolean>(false); // 用户是否进入调试模式

  // 状态轮询定时器
  let pollHandle: ReturnType<typeof setInterval> | null = null;

  // ---------- Computed ----------
  const isPaused = computed(() => debugState.value === 'PAUSED');
  const isRunning = computed(() =>
    debugState.value === 'RUNNING' || debugState.value === 'STEPPING'
  );
  const isStopped = computed(() => debugState.value === 'STOPPED' || debugState.value === 'IDLE');

  const breakpointCount = computed(() => breakpointNodeIds.value.size);

  /** 当前快照的某个节点的变量（响应式） */
  function getNodeSnapshot(nodeId: string): Record<string, any> | null {
    return nodeSnapshots.value[nodeId] || null;
  }

  // ---------- Actions ----------

  /** 切换调试模式开关 */
  function toggleDebugMode() {
    isDebugMode.value = !isDebugMode.value;
    if (!isDebugMode.value) {
      clearBreakpoints();
    }
  }

  function enterDebugMode() {
    isDebugMode.value = true;
  }

  function exitDebugMode() {
    isDebugMode.value = false;
    clearBreakpoints();
    stopPolling();
    currentExecutionId.value = null;
    debugState.value = 'IDLE';
    pausedAtNodeId.value = '';
    pauseReason.value = '';
    nodeSnapshots.value = {};
  }

  /** 添加断点 */
  function addBreakpoint(nodeId: string) {
    breakpointNodeIds.value.add(nodeId);
    // 触发响应式更新
    breakpointNodeIds.value = new Set(breakpointNodeIds.value);
    syncBreakpointsToServer();
  }

  /** 移除断点 */
  function removeBreakpoint(nodeId: string) {
    breakpointNodeIds.value.delete(nodeId);
    breakpointNodeIds.value = new Set(breakpointNodeIds.value);
    syncBreakpointsToServer();
  }

  /** 切换断点 */
  function toggleBreakpoint(nodeId: string) {
    if (breakpointNodeIds.value.has(nodeId)) {
      removeBreakpoint(nodeId);
    } else {
      addBreakpoint(nodeId);
    }
  }

  /** 清空断点 */
  function clearBreakpoints() {
    breakpointNodeIds.value = new Set();
    syncBreakpointsToServer();
  }

  /** 同步断点到后端 */
  async function syncBreakpointsToServer() {
    if (!currentExecutionId.value) return;
    try {
      const { default: http } = await import('@/api/http');
      await http.post(
        serviceUrl('workflow', `/api/execution/${currentExecutionId.value}/debug/breakpoints`),
        Array.from(breakpointNodeIds.value),
      );
    } catch (e) {
      console.warn('[debug] sync breakpoints failed', e);
    }
  }

  /** 绑定到执行实例（开始调试运行） */
  function bindExecution(executionId: string | number) {
    currentExecutionId.value = executionId;
    debugState.value = 'RUNNING';
    void syncBreakpointsToServer();
    startPolling();
  }

  /** 恢复执行 */
  async function resume() {
    if (!currentExecutionId.value) return;
    try {
      const { default: http } = await import('@/api/http');
      await http.post(serviceUrl('workflow', `/api/execution/${currentExecutionId.value}/debug/resume`));
      debugState.value = 'RUNNING';
    } catch (e) {
      console.error('[debug] resume failed', e);
    }
  }

  /** 单步执行 */
  async function step() {
    if (!currentExecutionId.value) return;
    try {
      const { default: http } = await import('@/api/http');
      await http.post(serviceUrl('workflow', `/api/execution/${currentExecutionId.value}/debug/step`));
      debugState.value = 'STEPPING';
    } catch (e) {
      console.error('[debug] step failed', e);
    }
  }

  /** 停止调试 */
  async function stop() {
    if (!currentExecutionId.value) return;
    try {
      const { default: http } = await import('@/api/http');
      await http.post(serviceUrl('workflow', `/api/execution/${currentExecutionId.value}/debug/stop`));
      debugState.value = 'STOPPED';
      stopPolling();
    } catch (e) {
      console.error('[debug] stop failed', e);
    }
  }

  // ---------- 状态轮询 ----------

  function startPolling(intervalMs = 1000) {
    stopPolling();
    pollHandle = setInterval(refreshStatus, intervalMs);
  }

  function stopPolling() {
    if (pollHandle) {
      clearInterval(pollHandle);
      pollHandle = null;
    }
  }

  async function refreshStatus() {
    if (!currentExecutionId.value) return;
    try {
      const { default: http } = await import('@/api/http');
      const res = await http.get(serviceUrl('workflow', `/api/execution/${currentExecutionId.value}/debug/status`));
      const data = (res as any).data as DebugSnapshot | undefined;
      if (!data) return;
      debugState.value = data.state || 'RUNNING';
      pausedAtNodeId.value = data.pausedAtNodeId || '';
      pauseReason.value = data.pauseReason || '';
      nodeSnapshots.value = data.nodeSnapshots || {};
      // 终态停止轮询
      if (data.state === 'STOPPED' || (data.state === 'IDLE' && !pausedAtNodeId.value)) {
        // 不立刻停止，让前端继续展示最终态
      }
    } catch (e) {
      // 静默忽略
    }
  }

  return {
    // state
    currentExecutionId,
    breakpointNodeIds,
    debugState,
    pausedAtNodeId,
    pauseReason,
    nodeSnapshots,
    isDebugMode,
    // computed
    isPaused,
    isRunning,
    isStopped,
    breakpointCount,
    getNodeSnapshot,
    // actions
    toggleDebugMode,
    enterDebugMode,
    exitDebugMode,
    addBreakpoint,
    removeBreakpoint,
    toggleBreakpoint,
    clearBreakpoints,
    bindExecution,
    resume,
    step,
    stop,
    refreshStatus,
    startPolling,
    stopPolling,
  };
});

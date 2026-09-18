import { onBeforeUnmount, ref, type Ref } from 'vue';
import { executionApi } from '@/api/execution';
import type { Execution, ExecutionLog } from '@/types/execution';
import type { NodeStatus } from '@/types/workflow';
import { useWorkflowStore } from '@/stores/workflow';
import { useLogStore } from '@/stores/log';
import { useDebugStore } from '@/stores/debug';
import {
  eventStatusToExecutionStatus,
  eventStatusToNodeStatus,
} from '@/api/execution';

export interface UseWorkflowRunOptions {
  /** 是否在 useWorkflowRun() 销毁时自动 close SSE（默认 true） */
  autoClose?: boolean;
  /** 订阅阶段是否同时把节点事件 push 到 useLogStore（默认 true） */
  forwardLogs?: boolean;
}

export interface UseWorkflowRunReturn {
  /** 当前正在运行的 executionId */
  runningId: Ref<string | number | null>;
  /** 当前执行状态 */
  status: Ref<string>;
  /** 手动停止订阅 */
  stop: () => void;
  /** 启动一次执行 + 订阅 SSE */
  run: (workflowId: string | number, input?: Record<string, any>) => Promise<Execution>;
  /** 仅订阅已有 execution 的 SSE 事件（详情页 / 历史日志页用） */
  subscribe: (executionId: string | number) => () => void;
}

/**
 * 工作流试运行 composable
 *
 * 职责：
 * 1. 调用 `executionApi.execute` 同步触发执行。
 * 2. 立即用返回的 executionId 订阅 `/api/execution/{id}/events` SSE。
 * 3. 把每条事件映射成 ExecutionLog（写入 useLogStore）和 NodeStatus（写入 useWorkflowStore.setNodeStatus）。
 * 4. 收到终态事件（execution_succeeded/failed/cancelled）后自动关闭连接。
 *
 * 同时保留 mock 模式：当 `VITE_USE_MOCK=true` 时跳过后端 SSE，使用本地定时器模拟节点进度
 * （与 Editor.vue 之前的 streamMockExecution 行为一致），保证脱网开发可用。
 */
export function useWorkflowRun(options: UseWorkflowRunOptions = {}): UseWorkflowRunReturn {
  const { autoClose = true, forwardLogs = true } = options;
  const workflowStore = useWorkflowStore();
  const logStore = useLogStore();
  const debugStore = useDebugStore();

  const runningId = ref<string | number | null>(null);
  const status = ref<string>('idle');
  let unsubscribe: (() => void) | null = null;
  let mockTimer: ReturnType<typeof setTimeout> | null = null;

  function stop() {
    if (runningId.value && unsubscribe) {
      // 先取消后端执行
      executionApi.cancel(runningId.value).catch((err) => {
        console.warn('Failed to cancel execution:', err);
      });
    }
    if (unsubscribe) {
      unsubscribe();
      unsubscribe = null;
    }
    if (mockTimer) {
      clearTimeout(mockTimer);
      mockTimer = null;
    }
    runningId.value = null;
    if (status.value !== 'idle') status.value = 'stopped';
  }

  function pushLog(level: ExecutionLog['level'], message: string, nodeId?: string, nodeName?: string) {
    if (!forwardLogs) return;
    logStore.push({ level, message, nodeId, nodeName });
  }

  function applyNodeStatus(nodeId: string | undefined, raw: string | undefined) {
    if (!nodeId) return;
    const normalized = eventStatusToNodeStatus(raw);
    workflowStore.setNodeStatus(nodeId, normalized as NodeStatus);
  }

  function subscribeMock(executionId: string | number, nodes: { id: string; name: string }[]) {
    runningId.value = executionId;
    status.value = 'running';
    let i = 0;
    const tick = () => {
      if (i >= nodes.length) {
        pushLog('success', '工作流执行完毕');
        status.value = 'success';
        if (forwardLogs) logStore.setRunning(false);
        runningId.value = null;
        return;
      }
      const n = nodes[i];
      workflowStore.setNodeStatus(n.id, 'running');
      pushLog('info', `执行节点「${n.name}」`, n.id, n.name);
      mockTimer = setTimeout(() => {
        if (status.value === 'stopped') return;
        const failed = Math.random() < 0.05;
        workflowStore.setNodeStatus(n.id, failed ? 'failed' : 'success');
        pushLog(
          failed ? 'error' : 'success',
          failed ? `节点「${n.name}」执行失败` : `节点「${n.name}」执行成功`,
          n.id,
          n.name,
        );
        i++;
        tick();
      }, 350);
    };
    mockTimer = setTimeout(tick, 200);
  }

  /**
   * 仅订阅既有 executionId 的 SSE 事件（详情页 / 历史日志页）。
   * 返回一个取消订阅的函数。
   */
  function subscribe(executionId: string | number): () => void {
    // 清掉旧订阅
    if (unsubscribe) {
      unsubscribe();
      unsubscribe = null;
    }
    if (mockTimer) {
      clearTimeout(mockTimer);
      mockTimer = null;
    }
    runningId.value = executionId;
    status.value = 'running';

    const isMock = import.meta.env.VITE_USE_MOCK === 'true';

    if (isMock) {
      // mock 模式：使用工作流 store 中的节点生成假进度
      const nodes = (workflowStore.current?.nodes ?? []).map((n) => ({ id: n.id, name: n.name }));
      subscribeMock(executionId, nodes);
      return () => stop();
    }

    unsubscribe = executionApi.subscribeEvents(executionId, {
      onOpen: () => {
        pushLog('info', '已订阅执行事件流');
      },
      onClose: () => {
        if (status.value === 'running') status.value = 'finished';
        if (forwardLogs) logStore.setRunning(false);
      },
      onError: (err) => {
        pushLog('error', `事件流异常: ${err.message}`);
        status.value = 'failed';
        if (forwardLogs) logStore.setRunning(false);
      },
      onExecutionStarted: () => {
        status.value = 'running';
      },
      onExecutionSucceeded: () => {
        status.value = 'success';
        pushLog('success', '工作流执行成功');
      },
      onExecutionFailed: (data) => {
        status.value = 'failed';
        const errMsg = data?.data?.error || data?.error || '执行失败';
        pushLog('error', `工作流执行失败: ${errMsg}`);
      },
      onExecutionCancelled: () => {
        status.value = 'cancelled';
        pushLog('warning', '工作流已取消');
      },
      onNodeStarted: (data) => {
        const nodeId = data?.node_id;
        const nodeName = data?.node_name;
        applyNodeStatus(nodeId, 'running');
        pushLog('info', `节点「${nodeName || nodeId}」开始执行`, nodeId, nodeName);
      },
      onNodeFinished: (data) => {
        const nodeId = data?.node_id;
        const nodeName = data?.node_name;
        const nodeStatus = data?.data?.status ?? 'success';
        applyNodeStatus(nodeId, nodeStatus);
        pushLog(
          nodeStatus === 'failed' ? 'error' : 'success',
          `节点「${nodeName || nodeId}」执行${nodeStatus === 'failed' ? '失败' : '成功'}`,
          nodeId,
          nodeName,
        );
      },
      onNodeStreamDelta: (data) => {
        const nodeId = data?.node_id;
        const delta = data?.data?.delta ?? '';
        if (delta) pushLog('info', String(delta), nodeId);
      },
    });
    return () => stop();
  }

  async function run(workflowId: string | number, input?: Record<string, any>): Promise<Execution> {
    stop();
    logStore.clear();
    workflowStore.resetNodeStatuses();
    logStore.setRunning(true);
    pushLog('info', `工作流开始执行（workflowId=${workflowId}）`);

    const isMock = import.meta.env.VITE_USE_MOCK === 'true';
    const result = await executionApi.execute({
      workflowId,
      input,
      trigger: 'manual',
      async: !isMock, // 真实模式下使用异步执行
      debug: debugStore.isDebugMode,
      breakpointNodeIds: debugStore.isDebugMode
        ? Array.from(debugStore.breakpointNodeIds)
        : undefined,
    });
    runningId.value = result.id;
    status.value = eventStatusToExecutionStatus(result.status);
    if (debugStore.isDebugMode) {
      debugStore.bindExecution(result.id);
    }

    if (isMock) {
      const nodes = (workflowStore.current?.nodes ?? []).map((n) => ({ id: n.id, name: n.name }));
      subscribeMock(result.id, nodes);
    } else {
      subscribe(result.id);
    }

    return result;
  }

  if (autoClose) {
    onBeforeUnmount(() => stop());
  }

  return { runningId, status, stop, run, subscribe };
}



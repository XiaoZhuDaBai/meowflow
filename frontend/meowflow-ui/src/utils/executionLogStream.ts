import { executionApi } from '@/api/execution';
import type { ExecutionLog, LogLevel } from '@/types/execution';

export type SSECallback = (log: ExecutionLog & { finished?: boolean }) => void;

/**
 * 执行日志流。
 *
 * 真实模式复用 {@link executionApi.subscribeEvents}（`/workflow/api/execution/{id}/events`，
 * 内部用 fetch + ReadableStream 解析 SSE，可携带 Authorization 头）。
 *
 * 注意：这里**不能**用 `new EventSource('/api/execution/{id}/logs/stream')`：
 *  - 该路径既缺网关前缀 `/workflow`（Vite 只代理 `/workflow`、`/user` 等前缀），
 *  - 后端也没有 `/logs/stream` 这个路由（真实端点是 `/{id}/events`），
 *  - 且 EventSource 无法自定义请求头，sa-token 读不到 Authorization 会 401。
 *
 * mock 模式（VITE_USE_MOCK=true）下沿用本地定时器模拟节点进度，保证脱网开发可用。
 */
export class ExecutionLogStream {
  private unsubscribe: (() => void) | null = null;
  private timer: any = null;
  private cancelled = false;
  private paused = false;
  private buffer: (ExecutionLog & { finished?: boolean })[] = [];

  constructor(
    private readonly executionId: string,
    private readonly onMessage: SSECallback,
    private readonly options: { mock?: boolean; nodes?: { name: string; type: string }[] } = {},
  ) {
    if (options.mock === true || isMockEnabled()) {
      this.runMockStream();
    } else {
      this.runSSE();
    }
  }

  pause() {
    this.paused = true;
  }

  resume() {
    if (this.paused) {
      this.paused = false;
      this.flush();
    }
  }

  stop() {
    this.cancelled = true;
    if (this.unsubscribe) {
      this.unsubscribe();
      this.unsubscribe = null;
    }
    if (this.timer) {
      clearTimeout(this.timer);
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  private flush() {
    const buf = this.buffer.slice();
    this.buffer = [];
    buf.forEach((entry) => {
      if (!this.cancelled && !this.paused) this.onMessage(entry);
    });
  }

  /** 统一出口：暂停时入缓冲区，否则直接回调。 */
  private push(entry: ExecutionLog & { finished?: boolean }) {
    if (this.cancelled) return;
    if (this.paused) {
      this.buffer.push(entry);
      return;
    }
    this.onMessage(entry);
  }

  private log(level: LogLevel, message: string, nodeId?: string, nodeName?: string) {
    this.push({
      id: `log-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
      timestamp: new Date().toISOString(),
      level,
      message,
      nodeId,
      nodeName,
    });
  }

  private runSSE() {
    this.unsubscribe = executionApi.subscribeEvents(this.executionId, {
      onOpen: () => this.log('info', '已订阅执行事件流'),
      onError: (err) => this.log('error', `事件流异常: ${err.message}`),
      onExecutionStarted: () => this.log('info', '工作流开始执行'),
      onExecutionSucceeded: () => {
        this.log('success', '工作流执行成功');
        this.push({
          id: `log-end-${Date.now()}`,
          timestamp: new Date().toISOString(),
          level: 'success',
          message: '工作流执行完毕',
          finished: true,
        });
        this.stop();
      },
      onExecutionFailed: (data) => {
        const msg = data?.data?.error || data?.error || data?.errorMessage || '执行失败';
        this.log('error', `工作流执行失败: ${msg}`);
        this.stop();
      },
      onExecutionCancelled: () => {
        this.log('warning', '工作流已取消');
        this.stop();
      },
      onNodeStarted: (data) =>
        this.log('info', `执行节点「${data?.node_name || data?.node_id}」`, data?.node_id, data?.node_name),
      onNodeFinished: (data) => {
        const status = data?.data?.status ?? 'success';
        const failed = status === 'failed' || status === 'timed_out';
        this.log(
          failed ? 'error' : status === 'skipped' ? 'warning' : 'success',
          `节点「${data?.node_name || data?.node_id}」${failed ? '执行失败' : status === 'skipped' ? '已跳过' : '执行成功'}`,
          data?.node_id,
          data?.node_name,
        );
      },
      onNodeStreamDelta: (data) => {
        const delta = data?.data?.delta ?? '';
        if (delta) this.log('info', String(delta), data?.node_id);
      },
    });
  }

  private runMockStream() {
    const nodes = this.options.nodes ?? [
      { name: 'Webhook 触发器', type: 'trigger.webhook' },
      { name: 'LLM 对话', type: 'ai.llm' },
    ];

    let i = 0;
    const total = nodes.length + 2;
    const tick = () => {
      if (this.cancelled) return;
      if (this.paused) {
        this.timer = setTimeout(tick, 400);
        return;
      }
      if (i >= total) {
        this.push({
          id: `log-end-${Date.now()}`,
          timestamp: new Date().toISOString(),
          level: 'success',
          message: '工作流执行完毕',
          finished: true,
        });
        this.stop();
        return;
      }
      const n = nodes[i - 1] ?? null;
      if (i === 0) {
        this.log('info', '工作流开始执行');
      } else if (i === total - 1) {
        this.log('success', '所有节点执行完毕');
      } else if (n) {
        this.log('info', `执行节点「${n.name}」(${n.type})`, undefined, n.name);
        setTimeout(() => {
          if (this.cancelled) return;
          const failed = Math.random() < 0.05;
          this.log(
            failed ? 'error' : 'success',
            failed ? `节点「${n.name}」执行失败` : `节点「${n.name}」执行成功`,
            undefined,
            n.name,
          );
        }, 200);
      }
      i++;
      this.timer = setTimeout(tick, 700);
    };

    setTimeout(tick, 200);
  }
}

function isMockEnabled(): boolean {
  return import.meta.env.VITE_USE_MOCK === 'true';
}

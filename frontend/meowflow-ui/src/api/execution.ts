/**
 * 执行 API（与后端 ExecutionController 对齐）
 *
 * 后端基础路径: `/api/execution`（单数；service gateway 前缀为 `/workflow`）。
 * 真实网关下 URL 形如 `/workflow/api/execution/...`，
 * Mock 模式下保持原 `/execution/...` 路径以复用现有 mock handler。
 *
 * 端点映射:
 *   POST   /api/execution                -> execute(req)
 *   POST   /api/execution/{id}/cancel    -> cancel(id)
 *   GET    /api/execution/{id}           -> getById(id)
 *   GET    /api/execution/page           -> page(params)
 *   GET    /api/execution/{id}/nodes     -> getNodeExecutions(id)
 *   GET    /api/execution/{id}/logs      -> getLogs(id, params)
 *   GET    /api/execution/workflow/{id}  -> getByWorkflow(workflowId, params)
 *
 * 后端 ExecutionResponse 字段为 `executionId / triggerType / errorMessage / costMs /
 * costAmount / startedAt / finishedAt`；通过 adaptExecutionResponse 适配为前端统一
 * 字段（id / trigger / error / duration / cost / startTime / endTime）。
 *
 * PageResponse(records, total, current, size, pages) -> adaptPageResponse 适配为
 * 前端 PaginatedResponse(items, total, page, pageSize, totalPages)。
 *
 * 限制：后端没有 /retry 端点；调用方应改用 execute(req) 重新提交原始 workflowId。
 * 限制：后端没有 /execute-async 端点；executeAsync() 方法已标注为 @deprecated，请使用 execute() 替代。
 */

import http from './http';
import { serviceUrl } from './endpoints';
import type { PaginatedResponse } from '@/types/workflow';
import type {
  Execution,
  NodeExecution,
  ExecutionLog,
  ExecutionStatus,
  NodeExecutionStatus,
} from '@/types/execution';
import {
  adaptExecutionPageResponse,
  adaptExecutionResponse,
  adaptExecutionLog,
} from '@/types/execution';
import { getAccessToken } from '@/utils/auth';

const BACKEND_BASE = '/api/execution';
const MOCK_BASE = '/execution';

export interface ExecutionRequest {
  workflowId: string | number;
  version?: string;
  trigger?: 'manual' | 'webhook' | 'schedule' | 'form';
  /** 触发方式（同 trigger，保留 triggerType 别名对齐后端 ExecutionRequest） */
  triggerType?: string;
  input?: Record<string, any>;
  async?: boolean;
  /** 调试模式：后端在异步调度前预置断点。 */
  debug?: boolean;
  breakpointNodeIds?: string[];
}

export interface ExecutionResult {
  executionId: string | number;
  workflowId: string | number;
  status: string;
  output?: Record<string, any>;
  error?: string;
  startTime?: string;
  endTime?: string;
  duration?: number;
  nodes?: NodeExecution[];
}

/** 列表查询参数。对齐后端 ExecutionQueryRequest / listByWorkflow */
export interface ListExecutionsParams {
  page?: number;
  pageSize?: number;
  /** 当前页（后端参数名 current） */
  current?: number;
  /** 每页大小（后端参数名 size） */
  size?: number;
  status?: string;
  workflowId?: string | number;
  trigger?: string;
  keyword?: string;
  startDate?: string;
  endDate?: string;
}

function buildUrl(backendPath: string, mockPath: string = backendPath.replace(/^\/api\/execution/, '/execution')): string {
  return serviceUrl('workflow', backendPath, mockPath);
}

function buildListParams(params?: ListExecutionsParams): Record<string, any> {
  if (!params) return {};
  const out: Record<string, any> = {};
  if (params.workflowId !== undefined) out.workflowId = params.workflowId;
  if (params.status) out.status = params.status;
  if (params.trigger) out.trigger = params.trigger;
  if (params.keyword) out.keyword = params.keyword;
  if (params.startDate) out.startDate = params.startDate;
  if (params.endDate) out.endDate = params.endDate;
  // 后端 page 接口使用 current / size
  if (params.current !== undefined) out.current = params.current;
  else if (params.page !== undefined) out.current = params.page;
  if (params.size !== undefined) out.size = params.size;
  else if (params.pageSize !== undefined) out.size = params.pageSize;
  return out;
}

export const executionApi = {
  // ==================== 执行操作 ====================

  /**
   * 同步执行工作流。
   * 后端: POST /api/execution
   */
  execute(req: ExecutionRequest): Promise<Execution> {
    return http
      .post(buildUrl(`${BACKEND_BASE}`), {
        workflowId: req.workflowId,
        version: req.version,
        triggerType: req.triggerType ?? req.trigger ?? 'manual',
        input: req.input,
        async: req.async ?? false,
        debug: req.debug ?? false,
        breakpointNodeIds: req.breakpointNodeIds,
      })
      .then((r: any) => adaptExecutionResponse(r.data));
  },

  /**
   * 异步执行工作流。
   * 复用 POST /api/execution，并传递 async=true。
   */
  executeAsync(workflowId: string | number, input?: Record<string, any>): Promise<Execution> {
    return executionApi.execute({ workflowId, input, async: true });
  },

  /**
   * 取消执行。
   * 后端: POST /api/execution/{id}/cancel
   */
  cancel(executionId: string | number): Promise<void> {
    return http
      .post(buildUrl(`${BACKEND_BASE}/${executionId}/cancel`))
      .then(() => undefined);
  },

  /**
   * 提交人工输入，继续等待中的执行。
   * 后端: POST /api/execution/{executionId}/human-input/{nodeId}
   */
  submitHumanInput(
    executionId: string | number,
    nodeId: string,
    input: Record<string, any>,
  ): Promise<void> {
    return http
      .post(buildUrl(`${BACKEND_BASE}/${executionId}/human-input/${encodeURIComponent(nodeId)}`), input)
      .then(() => undefined);
  },

  // ==================== 执行记录查询 ====================

  /**
   * 获取执行详情。
   * 后端: GET /api/execution/{id}
   */
  getById(executionId: string | number): Promise<Execution> {
    return http
      .get(buildUrl(`${BACKEND_BASE}/${executionId}`))
      .then((r: any) => adaptExecutionResponse(r.data));
  },

  /**
   * 分页查询执行记录。
   * 后端: GET /api/execution/page  (query params)
   */
  page(params?: ListExecutionsParams): Promise<PaginatedResponse<Execution>> {
    return http
      .get(buildUrl(`${BACKEND_BASE}/page`), { params: buildListParams(params) })
      .then((r: any) => adaptExecutionPageResponse(r.data));
  },

  /**
   * 获取节点执行列表。
   * 后端: GET /api/execution/{id}/nodes
   */
  getNodeExecutions(executionId: string | number): Promise<NodeExecution[]> {
    return http
      .get(buildUrl(`${BACKEND_BASE}/${executionId}/nodes`))
      .then((r: any) => (Array.isArray(r.data) ? r.data.map(adaptNodeExecutionSafe) : []));
  },

  /**
   * 获取执行日志。可按 nodeId / level 过滤。
   * 后端: GET /api/execution/{id}/logs?nodeId=&level=
   */
  getLogs(
    executionId: string | number,
    params?: { level?: string; nodeId?: string },
  ): Promise<ExecutionLog[]> {
    const query: Record<string, string> = {};
    if (params?.level) query.level = params.level;
    if (params?.nodeId) query.nodeId = params.nodeId;
    return http
      .get(buildUrl(`${BACKEND_BASE}/${executionId}/logs`), { params: query })
      .then((r: any) => (Array.isArray(r.data) ? r.data.map(adaptExecutionLog) : []));
  },

  /**
   * 查询指定工作流的执行记录。
   * 后端: GET /api/execution/workflow/{workflowId}
   */
  getByWorkflow(
    workflowId: string | number,
    params?: { page?: number; pageSize?: number },
  ): Promise<PaginatedResponse<Execution>> {
    const query: Record<string, any> = {};
    if (params?.page !== undefined) query.current = params.page;
    if (params?.pageSize !== undefined) query.size = params.pageSize;
    return http
      .get(buildUrl(`${BACKEND_BASE}/workflow/${workflowId}`), { params: query })
      .then((r: any) => adaptExecutionPageResponse(r.data));
  },

  // ==================== SSE 实时事件流 ====================

  /**
   * 订阅执行事件流。
   *
   * 后端: GET /api/execution/{id}/events?after=cursor
   * 数据源来自 Redis Stream（参见 RedisStreamEventSink）。
   *
   * 由于 EventSource 不支持自定义 header（sa-token 必须从 Authorization 头读取），
   * 改用 fetch + ReadableStream 按行解析 SSE 文本，保持鉴权链路一致。
   *
   * 事件结构：
   *   event: "node_started" | "node_finished" | "node_stream_delta" | "execution_started"
   *          | "execution_succeeded" | "execution_failed" | "execution_cancelled"
   *          | ...
   *   id: "<eventId>"
   *   data: JSON 文本
   */
  subscribeEvents(
    executionId: string | number,
    handlers: WorkflowEventHandlers,
    options?: { after?: string },
  ): () => void {
    const url = `${buildUrl(`${BACKEND_BASE}/${executionId}/events`)}${
      options?.after ? `?after=${encodeURIComponent(options.after)}` : ''
    }`;
    const token = getAccessToken();
    const ac = new AbortController();
    let cancelled = false;
    const finish = () => {
      if (cancelled) return;
      cancelled = true;
      queueMicrotask(() => {
        try { ac.abort(); } catch { /* ignore */ }
      });
    };

    const handleSseLine = (line: string, currentEvent: { event: string; id: string; data: string[] }) => {
      if (!line) {
        // 事件块结束 -> 派发
        const dataText = currentEvent.data.join('\n');
        if (dataText) {
          try {
            const rawPayload = JSON.parse(dataText);
            const parsed = rawPayload?.data && typeof rawPayload.data === 'object'
              ? { ...rawPayload, ...rawPayload.data, data: rawPayload.data.data ?? rawPayload.data }
              : rawPayload;
            const eventType = currentEvent.event || parsed.event || 'message';
            handlers.onEvent?.(eventType, parsed, currentEvent.id);
            switch (eventType) {
              case 'node_started':
                handlers.onNodeStarted?.(parsed);
                break;
              case 'node_finished':
                handlers.onNodeFinished?.(parsed);
                break;
              case 'node_stream_delta':
                handlers.onNodeStreamDelta?.(parsed);
                break;
              case 'execution_started':
                handlers.onExecutionStarted?.(parsed);
                break;
              case 'execution_succeeded':
                handlers.onExecutionSucceeded?.(parsed);
                finish();
                break;
              case 'execution_failed':
                handlers.onExecutionFailed?.(parsed);
                finish();
                break;
              case 'execution_cancelled':
                handlers.onExecutionCancelled?.(parsed);
                finish();
                break;
            }
          } catch {
            // 忽略非 JSON 行
          }
        }
        currentEvent.event = '';
        currentEvent.id = '';
        currentEvent.data = [];
        return;
      }
      if (line.startsWith(':')) return; // 注释行
      const colonIdx = line.indexOf(':');
      const field = colonIdx >= 0 ? line.slice(0, colonIdx) : line;
      let value = colonIdx >= 0 ? line.slice(colonIdx + 1) : '';
      if (value.startsWith(' ')) value = value.slice(1);
      switch (field) {
        case 'event':
          currentEvent.event = value;
          break;
        case 'id':
          currentEvent.id = value;
          break;
        case 'data':
          currentEvent.data.push(value);
          break;
      }
    };

    (async () => {
      try {
        const headers: Record<string, string> = { Accept: 'text/event-stream' };
        if (token) headers.Authorization = `Bearer ${token}`;
        const resp = await fetch(url, { headers, signal: ac.signal });
        if (!resp.ok || !resp.body) {
          handlers.onError?.(new Error(`SSE connection failed: HTTP ${resp.status}`));
          return;
        }
        handlers.onOpen?.();
        const reader = resp.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buf = '';
        let currentEvent = { event: '', id: '', data: [] as string[] };
        while (!cancelled) {
          const { value, done } = await reader.read();
          if (done) break;
          buf += decoder.decode(value, { stream: true });
          // SSE 行以 \n 分隔；后端追加 \n
          let nlIdx = buf.indexOf('\n');
          while (nlIdx >= 0) {
            const rawLine = buf.slice(0, nlIdx);
            buf = buf.slice(nlIdx + 1);
            const line = rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine;
            handleSseLine(line, currentEvent);
            nlIdx = buf.indexOf('\n');
          }
        }
      } catch (e: any) {
        if (e?.name !== 'AbortError') {
          handlers.onError?.(e);
        }
      } finally {
        handlers.onClose?.();
      }
    })();

    return () => {
      cancelled = true;
      try { ac.abort(); } catch { /* ignore */ }
    };
  },
};

/**
 * SSE 事件订阅回调。方法可选；onEvent 是兜底回调（所有事件都触发）。
 */
export interface WorkflowEventHandlers {
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (err: Error) => void;
  onEvent?: (event: string, data: any, id: string) => void;
  onExecutionStarted?: (data: any) => void;
  onExecutionSucceeded?: (data: any) => void;
  onExecutionFailed?: (data: any) => void;
  onExecutionCancelled?: (data: any) => void;
  onNodeStarted?: (data: any) => void;
  onNodeFinished?: (data: any) => void;
  onNodeStreamDelta?: (data: any) => void;
}

/**
 * 执行事件类型
 */
export interface WorkflowEvent {
  event: string;
  id: string;
  execution_id?: string | number;
  workflow_id?: string | number;
  node_id?: string;
  node_type?: string;
  node_name?: string;
  timestamp?: string;
  data?: Record<string, any>;
}

/**
 * 把后端 SSE 节点状态字符串归一到前端 NodeExecutionStatus。
 */
export function eventStatusToNodeStatus(raw: any): NodeExecutionStatus {
  const v = typeof raw === 'string' ? raw.toLowerCase() : '';
  switch (v) {
    case 'success':
    case 'succeeded':
      return 'success';
    case 'failed':
    case 'error':
      return 'failed';
    case 'cancelled':
    case 'canceled':
    case 'timed_out':
    case 'timeout':
      return 'skipped';
    case 'running':
    case 'processing':
      return 'running';
    case 'skipped':
      return 'skipped';
    default:
      return 'pending';
  }
}

/**
 * 把后端 SSE 执行状态字符串归一到前端 ExecutionStatus。
 */
export function eventStatusToExecutionStatus(raw: any): ExecutionStatus {
  const v = typeof raw === 'string' ? raw.toLowerCase() : '';
  switch (v) {
    case 'success':
    case 'succeeded':
      return 'success';
    case 'failed':
    case 'error':
      return 'failed';
    case 'cancelled':
    case 'canceled':
      return 'cancelled';
    case 'running':
    case 'processing':
      return 'running';
    default:
      return 'pending';
  }
}

function adaptNodeExecutionSafe(raw: any): NodeExecution {
  if (!raw) return raw;
  // NodeExecutionDto 与 ExecutionResponse.nodes 同结构；通过 execution 类型适配函数
  // 复用 adaptExecutionResponse 对 nodes 的处理。这里直接通过 import。
  const r = raw as Record<string, any>;
  return {
    ...r,
    id: r.id,
    nodeId: r.nodeId,
    nodeName: r.nodeName,
    nodeType: r.nodeType,
    status: r.status,
    input: r.input,
    output: r.output,
    error: r.errorMessage ?? r.error,
    errorMessage: r.errorMessage,
    retryCount: r.retryCount,
    duration: r.costMs ?? r.duration,
    costMs: r.costMs,
    costToken: r.costToken,
    startTime: r.startedAt ?? r.startTime,
    startedAt: r.startedAt,
    endTime: r.finishedAt ?? r.endTime,
    finishedAt: r.finishedAt,
    createTime: r.createTime,
  };
}



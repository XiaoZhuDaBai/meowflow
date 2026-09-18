/**
 * 执行与监控相关类型
 *
 * 数据来源：后端 `meowflow-workflow` 的 `ExecutionController`
 *
 * 后端 `ExecutionResponse` 字段：
 *   - executionId      Long
 *   - workflowId       Long
 *   - workflowName     String
 *   - version          String
 *   - triggerType      String  (manual/webhook/schedule/form)
 *   - status           String  (running/success/failed/cancelled/...)
 *   - input            Map<String,Object>
 *   - output           Map<String,Object>
 *   - errorMessage     String
 *   - costMs           Long    (耗时毫秒)
 *   - costToken        Integer
 *   - costAmount       Double  (费用, 元)
 *   - startedAt        String  (ISO)
 *   - finishedAt       String  (ISO)
 *   - nodes            List<NodeExecutionDto>
 *
 * 后端 `NodeExecutionDto` 字段：
 *   - id           Long
 *   - nodeId       String
 *   - nodeType     String
 *   - nodeName     String
 *   - status       String
 *   - input        Map
 *   - output       Map
 *   - errorMessage String
 *   - retryCount   Integer
 *   - costMs       Long
 *   - startedAt    String
 *   - finishedAt   String
 *
 * 后端 `PageResponse<T>` 字段：
 *   - records  List<T>
 *   - total    Long
 *   - current  Integer
 *   - size     Integer
 *   - pages    Integer
 *
 * 后端日志级别枚举：DEBUG / INFO / WARN / ERROR / SUCCESS
 */

import type { PaginatedResponse } from './workflow';
import { adaptPageResponse, type BackendPage } from './api';

// =============================================================================
// 枚举与状态
// =============================================================================

/**
 * 前端统一的执行状态。后端使用字符串（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED），
 * 通过 normalizeExecutionStatus 做大小写 / 同义词兼容。
 */
export type ExecutionStatus =
  | 'pending'
  | 'running'
  | 'success'
  | 'failed'
  | 'cancelled'
  | 'skipped'
  | 'waiting';

/** 节点执行状态 */
export type NodeExecutionStatus =
  | 'pending'
  | 'running'
  | 'success'
  | 'failed'
  | 'skipped'
  | 'waiting';

/** 触发方式 */
export type ExecutionTrigger = 'manual' | 'webhook' | 'schedule' | 'form';

/** 日志级别（统一小写） */
export type LogLevel = 'debug' | 'info' | 'warning' | 'error' | 'success';

// =============================================================================
// 实体（对齐后端 ExecutionResponse / NodeExecutionDto / ExecutionLogDto）
// =============================================================================

/**
 * 执行实体。前端 `id` 字段直接映射自后端 `executionId`（避免在视图层大量重命名），
 * 同时保留 `executionId` 别名供外部 API 直接使用。
 */
export interface Execution {
  /** 执行 ID（前端统一字段名） */
  id: string | number;
  /** 后端 executionId 别名 */
  executionId?: string | number;
  workflowId: string | number;
  workflowName?: string;
  version?: string;
  trigger: ExecutionTrigger | string;
  status: ExecutionStatus | string;
  input?: Record<string, any>;
  output?: Record<string, any>;
  error?: string;
  /** 错误信息（后端字段） */
  errorMessage?: string;
  /** 耗时（毫秒） */
  duration?: number;
  /** 后端 costMs */
  costMs?: number;
  /** 费用（元） */
  cost?: number;
  /** 后端 costAmount */
  costAmount?: number;
  /** Token 消耗 */
  costToken?: number;
  /** 开始时间（前端统一字段名） */
  startTime?: string;
  /** 后端 startedAt */
  startedAt?: string;
  /** 结束时间（前端统一字段名） */
  endTime?: string;
  /** 后端 finishedAt */
  finishedAt?: string;
  /** 节点执行列表 */
  nodes?: NodeExecution[];
}

/** 节点执行实体 */
export interface NodeExecution {
  /** 节点执行 ID（后端） */
  id?: string | number;
  nodeId: string;
  nodeName: string;
  nodeType: string;
  status: NodeExecutionStatus | string;
  input?: Record<string, any>;
  output?: Record<string, any>;
  error?: string;
  /** 错误信息（后端字段） */
  errorMessage?: string;
  retryCount?: number;
  /** 耗时（毫秒） */
  duration?: number;
  /** 后端 costMs */
  costMs?: number;
  /** 消耗 token */
  costToken?: number;
  startTime?: string;
  /** 后端 startedAt */
  startedAt?: string;
  endTime?: string;
  /** 后端 finishedAt */
  finishedAt?: string;
  /** 后端 createTime */
  createTime?: string;
}

/**
 * 统一 ExecutionLog 形态：
 * - 后端 ExecutionLogDto 字段：id, executionId, nodeId, level, message, payload, createdAt
 * - 前端视图字段：timestamp (=createdAt), nodeName, details (=payload)
 */
export interface ExecutionLog {
  id: string | number;
  timestamp: string;
  executionId?: string | number;
  nodeId?: string;
  /** 前端展示用的节点名（部分视图需要；后端未返回，留可选） */
  nodeName?: string;
  level: LogLevel;
  message: string;
  details?: Record<string, any>;
}

// =============================================================================
// 适配函数：后端 ExecutionResponse -> 前端 Execution
// =============================================================================

/**
 * 后端返回的执行响应（ExecutionResponse）适配到前端 Execution。
 *
 * 字段映射：
 *   executionId  -> id, executionId
 *   triggerType  -> trigger
 *   errorMessage -> error, errorMessage
 *   costMs       -> duration, costMs
 *   costAmount   -> cost, costAmount
 *   startedAt    -> startTime, startedAt
 *   finishedAt   -> endTime, finishedAt
 */
export function adaptExecutionResponse(raw: any): Execution {
  if (!raw) return raw;
  const r = raw as Record<string, any>;
  const id = r.executionId ?? r.id;
  const trigger = normalizeTrigger(r.triggerType ?? r.trigger);
  const status = normalizeExecutionStatus(r.status);
  const nodes = Array.isArray(r.nodes) ? r.nodes.map(adaptNodeExecutionDto) : r.nodes;
  const adapted: Execution = {
    ...r,
    id,
    executionId: id,
    workflowId: r.workflowId,
    workflowName: r.workflowName,
    version: r.version,
    trigger,
    status,
    input: r.input,
    output: r.output,
    error: r.errorMessage ?? r.error,
    errorMessage: r.errorMessage,
    duration: r.costMs ?? r.duration,
    costMs: r.costMs,
    cost: r.costAmount ?? r.cost,
    costAmount: r.costAmount,
    costToken: r.costToken,
    startTime: r.startedAt ?? r.startTime,
    startedAt: r.startedAt,
    endTime: r.finishedAt ?? r.endTime,
    finishedAt: r.finishedAt,
    nodes,
  };
  return adapted;
}

/** 把后端 NodeExecutionDto 适配到前端 NodeExecution */
export function adaptNodeExecutionDto(raw: any): NodeExecution {
  if (!raw) return raw;
  const r = raw as Record<string, any>;
  return {
    ...r,
    id: r.id,
    nodeId: r.nodeId,
    nodeName: r.nodeName,
    nodeType: r.nodeType,
    status: normalizeNodeStatus(r.status),
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

/** 触发方式同义词/大小写归一 */
const TRIGGER_MAP: Record<string, ExecutionTrigger> = {
  manual: 'manual',
  webhook: 'webhook',
  schedule: 'schedule',
  cron: 'schedule',
  form: 'form',
  api: 'webhook',
};

export function normalizeTrigger(value: any): ExecutionTrigger | string {
  if (!value) return 'manual';
  const key = String(value).toLowerCase();
  return TRIGGER_MAP[key] ?? key;
}

const STATUS_MAP: Record<string, ExecutionStatus> = {
  pending: 'pending',
  waiting: 'waiting',
  running: 'running',
  processing: 'running',
  success: 'success',
  succeeded: 'success',
  completed: 'success',
  failed: 'failed',
  error: 'failed',
  fail: 'failed',
  cancelled: 'cancelled',
  canceled: 'cancelled',
  skipped: 'skipped',
};

export function normalizeExecutionStatus(value: any): ExecutionStatus | string {
  if (!value) return 'pending';
  const key = String(value).toLowerCase();
  return STATUS_MAP[key] ?? key;
}

const NODE_STATUS_MAP: Record<string, NodeExecutionStatus> = {
  pending: 'pending',
  waiting: 'waiting',
  running: 'running',
  success: 'success',
  succeeded: 'success',
  failed: 'failed',
  error: 'failed',
  skipped: 'skipped',
};

export function normalizeNodeStatus(value: any): NodeExecutionStatus | string {
  if (!value) return 'pending';
  const key = String(value).toLowerCase();
  return NODE_STATUS_MAP[key] ?? key;
}

// =============================================================================
// 日志级别适配（与历史保持一致：后端全大写 -> 前端小写）
// =============================================================================

const LOG_LEVEL_MAP: Record<string, LogLevel> = {
  DEBUG: 'debug',
  INFO: 'info',
  WARN: 'warning',
  WARNING: 'warning',
  ERROR: 'error',
  SUCCESS: 'success',
};

export function mapBackendLogLevel(level: any): LogLevel {
  if (!level) return 'info';
  return LOG_LEVEL_MAP[String(level).toUpperCase()] ?? 'info';
}

export function mapFrontendLogLevel(level: LogLevel): string {
  const found = Object.entries(LOG_LEVEL_MAP).find(([, v]) => v === level);
  return found ? found[0] : 'INFO';
}

/** 适配后端 ExecutionLogDto -> 前端 ExecutionLog */
export function adaptExecutionLog(raw: any): ExecutionLog {
  if (!raw) return raw;
  const r = raw as Record<string, any>;
  return {
    ...r,
    id: r.id,
    executionId: r.executionId,
    nodeId: r.nodeId,
    level: mapBackendLogLevel(r.level),
    message: r.message ?? '',
    details: r.payload ?? r.details,
    timestamp: r.createdAt ?? r.timestamp ?? new Date().toISOString(),
  };
}

/**
 * 适配后端 PageResponse(records,total,current,size,pages) -> 前端 PaginatedResponse
 * 同时把 records 中每一项通过 adaptExecutionResponse 转换。
 */
export function adaptExecutionPageResponse(
  raw: BackendPage<any> | PaginatedResponse<Execution> | undefined | null,
): PaginatedResponse<Execution> {
  const adapted = adaptPageResponse<any>(raw);
  return {
    ...adapted,
    items: (adapted.items ?? []).map(adaptExecutionResponse),
  };
}

// =============================================================================
// 兼容旧版（外部仍可能引用）
// =============================================================================

/** 兼容旧 normalizeExecutionLog 命名 */
export function normalizeExecutionLog(raw: any): ExecutionLog {
  return adaptExecutionLog(raw);
}

// 让旧 usage `import type { NodeExecution, Execution } from '@/types/execution'` 继续工作
export type { ExecutionLog as FrontendExecutionLog };

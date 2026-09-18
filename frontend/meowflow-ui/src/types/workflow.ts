export type WorkflowStatus = 'draft' | 'published' | 'running' | 'stopped' | 'archived' | 'deleted';

/**
 * 工作流状态映射
 * - 后端使用大写字符串（DRAFT / PUBLISHED / ACTIVE / STOPPED / ARCHIVED / DELETED），
 *   历史接口兼容小写 `active` -> `running`。
 * - 前端统一使用上述字面量。
 */
const workflowStatusMap: Record<'backend' | 'frontend', Record<string, string>> = {
  backend: {
    draft: 'draft',
    DRAFT: 'draft',
    published: 'published',
    PUBLISHED: 'published',
    active: 'running',
    ACTIVE: 'running',
    running: 'running',
    RUNNING: 'running',
    stopped: 'stopped',
    STOPPED: 'stopped',
    archived: 'archived',
    ARCHIVED: 'archived',
    deleted: 'deleted',
    DELETED: 'deleted',
  },
  frontend: {
    draft: 'DRAFT',
    published: 'PUBLISHED',
    running: 'ACTIVE',
    stopped: 'STOPPED',
    archived: 'ARCHIVED',
    deleted: 'DELETED',
  },
};

export function mapBackendStatus(status: string): WorkflowStatus {
  if (!status) return 'draft';
  return (workflowStatusMap.backend[status] ?? 'draft') as WorkflowStatus;
}

export function mapFrontendStatus(status: WorkflowStatus): string {
  return workflowStatusMap.frontend[status] ?? 'DRAFT';
}

export type NodeStatus = 'pending' | 'running' | 'success' | 'failed' | 'skipped' | 'waiting';

export type ExecutionStatus = 'running' | 'success' | 'failed' | 'cancelled' | 'waiting' | 'skipped';

export type ExecutionTrigger = 'manual' | 'webhook' | 'schedule' | 'form';

export type NodeCategory = 'trigger' | 'ai' | 'flow' | 'tool' | 'notify';

/** A node on the workflow canvas */
export interface WorkflowNode {
  id: string;
  type: string;            // node type id, e.g. "trigger.webhook"
  category: NodeCategory;
  name: string;
  description?: string;
  x: number;
  y: number;
  config: Record<string, any>;
  status?: NodeStatus;
}

/** A point used as a polyline routing control point */
export interface EdgePoint {
  x: number;
  y: number;
}

/** An edge between two nodes */
export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  sourcePort?: string;
  targetPort?: string;
  label?: string;
  edgeType?: 'default' | 'condition' | 'loop' | 'error';
  config?: Record<string, any>;
  /** Manual control points for the polyline. When empty/undefined, polyline auto-routes. */
  controlPoints?: EdgePoint[];
}

export type { Variable, VariableProvider, VariableEdge } from './variable';

/**
 * 工作流版本定义（与后端 WorkflowDefinitionRequest 对齐）。
 * 与 editor 内的 {@link WorkflowNode}/{@link WorkflowEdge} 不同：
 * node 坐标在 `position`，配置在 `data.config`。
 */
export interface WorkflowDefinitionPayload {
  version?: string;
  nodes: Array<{
    id: string;
    type: string;
    name: string;
    position: { x: number; y: number };
    data: Record<string, any>;
  }>;
  edges: Array<{
    id: string;
    source: string;
    target: string;
    sourceHandle?: string;
    targetHandle?: string;
    type?: string;
    data?: {
      label?: string;
      type?: string;
      config?: Record<string, any>;
    };
  }>;
  variables?: Record<string, any>;
  inputSchema?: Record<string, any>;
  outputSchema?: Record<string, any>;
  changelog?: string;
}

export interface WorkflowStats {
  executions: number;
  successRate: number;
  avgDuration: number;
  totalCost: number;
  todayExecutions: number;
  todayCost: number;
}

export interface Workflow {
  id: string;
  name: string;
  description?: string;
  category: string;
  categoryId?: string;
  groupId?: string;
  code?: string;
  icon?: string;
  tags?: string[];
  isPublic?: boolean;
  status: WorkflowStatus;
  version: string | number;
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  publishedAt?: string;
  /** 模板来源 ID（从模板创建的工作流会有此字段） */
  templateId?: string;
  /** 当前版本的定义（与后端 WorkflowResponse.definition 对齐）。mock 时代码节点 / 边，从此字段反推。 */
  definition?: WorkflowDefinitionPayload;
  stats: WorkflowStats;
}

export interface WorkflowListItem {
  id: string;
  name: string;
  description?: string;
  category: string;
  categoryId?: string;
  code?: string;
  icon?: string;
  tags?: string[];
  isPublic?: boolean;
  status: WorkflowStatus;
  version: string | number;
  createdAt: string;
  updatedAt: string;
  stats: WorkflowStats;
  executionStatus?: ExecutionStatus;
}

export interface CreateWorkflowRequest {
  name: string;
  description?: string;
  category?: string;
  categoryId?: string;
  groupId?: string;
  code?: string;
  icon?: string;
  tags?: string[];
  isPublic?: boolean;
  nodes?: WorkflowNode[];
  edges?: WorkflowEdge[];
}

export interface UpdateWorkflowRequest {
  name?: string;
  description?: string;
  category?: string;
  categoryId?: string;
  groupId?: string;
  code?: string;
  icon?: string;
  tags?: string[];
  isPublic?: boolean;
  nodes?: WorkflowNode[];
  edges?: WorkflowEdge[];
  status?: WorkflowStatus;
}

export interface NodeExecution {
  nodeId: string;
  nodeName: string;
  nodeType: string;
  status: NodeStatus;
  startTime: string;
  endTime?: string;
  duration?: number;
  input?: any;
  output?: any;
  error?: string;
}

export interface ExecutionLog {
  id: string;
  timestamp: string;
  level: 'debug' | 'info' | 'warning' | 'error' | 'success';
  nodeId?: string;
  nodeName?: string;
  message: string;
  details?: any;
}

export interface Execution {
  id: string;
  workflowId: string;
  workflowName: string;
  status: ExecutionStatus;
  trigger: ExecutionTrigger;
  startTime: string;
  endTime?: string;
  duration?: number;
  cost?: number;
  nodes: NodeExecution[];
  error?: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
  traceId?: string;
}

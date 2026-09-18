import http from './http';
import { isMockEnabled, serviceUrl } from './endpoints';
import type {
  Workflow,
  WorkflowListItem,
  WorkflowNode,
  WorkflowEdge,
  WorkflowStats,
  CreateWorkflowRequest,
  UpdateWorkflowRequest,
  PaginatedResponse,
  WorkflowDefinitionPayload,
} from '@/types/workflow';
import { mapBackendStatus, mapFrontendStatus } from '@/types/workflow';
import { adaptPageResponse } from '@/types/api';
import type { ListParams } from './types';

export interface WorkflowVersion {
  id: string;
  workflowId: string;
  version: string;
  publishStatus: 'draft' | 'published' | string;
  changelog?: string;
  publishedAt?: string;
  publishedBy?: string;
  createdAt?: string;
}

export type { WorkflowDefinitionPayload };

const mockBase = '/workflows';
const backendBase = '/api/workflow';

function endpoint(suffix = '', mockSuffix = suffix): string {
  return serviceUrl('workflow', `${backendBase}${suffix}`, `${mockBase}${mockSuffix}`);
}

export const workflowApi = {
  async getList(params?: ListParams): Promise<PaginatedResponse<WorkflowListItem>> {
    const query = isMockEnabled
      ? params
      : {
          current: params?.page ?? 1,
          size: params?.pageSize ?? 20,
          keyword: params?.keyword,
          status: params?.status ? mapFrontendStatus(params.status as any) : undefined,
          categoryId: params?.category && /^\d+$/.test(params.category) ? params.category : undefined,
        };
    const raw = await http.get(endpoint('/page', ''), { params: query }).then((r: any) => r.data);
    const page = adaptPageResponse<any>(raw, params?.page ?? 1, params?.pageSize ?? 20);
    return { ...page, items: page.items.map(mapWorkflowListItem) };
  },

  async getById(id: string): Promise<Workflow> {
    if (isMockEnabled) {
      // 与真实路径一致：mock 响应顶层挂 definition，mapWorkflow 优先用 definition 拆节点 / 边
      const raw = await http.get(endpoint(`/${id}`)).then((r: any) => r.data);
      if (raw?.definition && (raw.definition.nodes || raw.definition.edges)) {
        return mapWorkflow(raw, raw.definition);
      }
      return mapWorkflow(raw);
    }

    // 1. 优先从 WorkflowResponse.definition 直接拿节点 / 边（避免再发一次 export）。
    // 2. 仅当 definition 为空时退化到 /export 兜底。
    const raw = await http.get(endpoint(`/${id}`)).then((r: any) => r.data);
    const inlineDef = raw?.definition;
    if (inlineDef && (inlineDef.nodes || inlineDef.edges)) {
      return mapWorkflow(raw, inlineDef);
    }
    const exported = await this.export(id).catch(() => null);
    return mapWorkflow(raw, extractDefinition(exported));
  },

  async create(data: CreateWorkflowRequest): Promise<Workflow> {
    if (isMockEnabled) {
      // 与后端 WorkflowCreateRequest 对齐：把 nodes/edges 折成 definition
      const definition = toDefinition(data.nodes ?? [], data.edges ?? [], '创建工作流');
      const raw = await http
        .post(endpoint(), {
          name: data.name,
          description: data.description,
          categoryId: numericId(data.categoryId),
          groupId: numericId(data.groupId),
          icon: data.icon,
          tags: data.tags,
          isPublic: data.isPublic ?? false,
          definition,
        })
        .then((r: any) => r.data);
      return mapWorkflow(raw, definition);
    }

    const definition = toDefinition(data.nodes ?? [], data.edges ?? [], '创建工作流');
    const raw = await http
      .post(endpoint(), {
        name: data.name,
        code: data.code || undefined,
        description: data.description,
        categoryId: numericId(data.categoryId),
        groupId: numericId(data.groupId),
        icon: data.icon,
        tags: data.tags,
        isPublic: data.isPublic ?? false,
        definition,
      })
      .then((r: any) => r.data);
    return mapWorkflow(raw, definition);
  },

  async update(id: string, data: UpdateWorkflowRequest): Promise<Workflow> {
    if (isMockEnabled) {
      // 与真实后端同语义：先 PUT metadata，再 POST /versions 保存定义
      const metadata = await http
        .put(endpoint(`/${id}`), {
          name: data.name,
          description: data.description,
          categoryId: numericId(data.categoryId),
          groupId: numericId(data.groupId),
          icon: data.icon,
          tags: data.tags,
          isPublic: data.isPublic,
          status: data.status ? mapFrontendStatus(data.status) : undefined,
        })
        .then((r: any) => r.data);

      let version: WorkflowVersion | null = null;
      if (data.nodes || data.edges) {
        const definition = toDefinition(data.nodes ?? [], data.edges ?? [], '保存编辑器内容');
        version = await http
          .post(endpoint(`/${id}/versions`, `/${id}/versions`), definition)
          .then((r: any) => mapVersion(r.data));
      }
      return mapWorkflow(
        { ...metadata, currentVersion: version?.version ?? metadata?.currentVersion },
        data.nodes || data.edges ? toDefinition(data.nodes ?? [], data.edges ?? []) : undefined,
      );
    }

    const metadata = await http
      .put(endpoint(`/${id}`), {
        name: data.name,
        description: data.description,
        categoryId: numericId(data.categoryId),
        groupId: numericId(data.groupId),
        icon: data.icon,
        tags: data.tags,
        isPublic: data.isPublic,
        status: data.status ? mapFrontendStatus(data.status) : undefined,
      })
      .then((r: any) => r.data);

    let version: WorkflowVersion | null = null;
    if (data.nodes || data.edges) {
      version = await this.saveVersion(
        id,
        toDefinition(data.nodes ?? [], data.edges ?? [], '保存编辑器内容'),
      );
    }
    return mapWorkflow(
      { ...metadata, currentVersion: version?.version ?? metadata?.currentVersion },
      data.nodes || data.edges ? toDefinition(data.nodes ?? [], data.edges ?? []) : undefined,
    );
  },

  delete(id: string): Promise<void> {
    return http.delete(endpoint(`/${id}`)).then(() => undefined);
  },

  async duplicate(id: string): Promise<Workflow> {
    const raw = await http
      .post(endpoint(`/${id}/copy`, `/${id}/duplicate`), {})
      .then((r: any) => r.data);
    if (raw) return mapWorkflow(raw);
    const page = await this.getList({ page: 1, pageSize: 1 });
    if (!page.items[0]) throw new Error('复制成功，但未能读取副本');
    return this.getById(page.items[0].id);
  },

  async saveVersion(id: string, definition: WorkflowDefinitionPayload): Promise<WorkflowVersion> {
    if (isMockEnabled) {
      // 与真实后端对齐：POST /api/workflow/{id}/versions，body 为 WorkflowDefinitionRequest
      const raw = await http
        .post(endpoint(`/${id}/versions`, `/${id}/versions`), definition)
        .then((r: any) => r.data);
      return mapVersion(raw);
    }
    const raw = await http
      .post(endpoint(`/${id}/versions`), definition)
      .then((r: any) => r.data);
    return mapVersion(raw);
  },

  async publish(id: string, version?: string): Promise<Workflow> {
    if (isMockEnabled) {
      const raw = await http.post(endpoint(`/${id}/publish`), {}).then((r: any) => r.data);
      return mapWorkflow(raw);
    }
    const targetVersion = version || String((await this.getById(id)).version);
    if (!targetVersion) throw new Error('请先保存工作流版本');
    await http.post(endpoint(`/${id}/versions/publish`), {
      version: targetVersion,
      changelog: `发布 ${targetVersion}`,
    });
    return this.getById(id);
  },

  async stop(id: string): Promise<Workflow | null> {
    const raw = await http.post(endpoint(`/${id}/stop`), {}).then((r: any) => r.data);
    return raw ? mapWorkflow(raw) : null;
  },

  run(id: string, input?: Record<string, any>): Promise<any> {
    if (isMockEnabled) {
      return http.post(endpoint(`/${id}/run`), { input }).then((r: any) => r.data);
    }
    return http
      .post(serviceUrl('workflow', '/api/execution', '/execution'), {
        workflowId: numericId(id) ?? id,
        triggerType: 'manual',
        input,
        async: false,
      })
      .then((r: any) => r.data);
  },

  async versions(id: string): Promise<WorkflowVersion[]> {
    if (isMockEnabled) {
      const wf = await this.getById(id);
      return [{
        id: `${id}-${wf.version}`,
        workflowId: id,
        version: String(wf.version),
        publishStatus: wf.status === 'running' ? 'published' : 'draft',
        publishedAt: wf.publishedAt,
        createdAt: wf.updatedAt,
      }];
    }
    const raw = await http.get(endpoint(`/${id}/versions`)).then((r: any) => r.data);
    return (raw ?? []).map(mapVersion);
  },

  async rollback(id: string, version: string): Promise<WorkflowVersion> {
    if (isMockEnabled) throw new Error('Mock 环境不支持版本回滚');
    const raw = await http
      .post(endpoint(`/${id}/rollback/${encodeURIComponent(version)}`), {})
      .then((r: any) => r.data);
    return mapVersion(raw);
  },

  async export(id: string, includeHistory = false): Promise<string> {
    if (isMockEnabled) {
      const wf = await this.getById(id);
      return JSON.stringify({
        version: '1.0',
        exportedAt: new Date().toISOString(),
        workflow: wf,
        latestDefinition: toDefinition(wf.nodes, wf.edges),
      }, null, 2);
    }
    return http
      .get(endpoint(`/${id}/${includeHistory ? 'export-with-history' : 'export'}`))
      .then((r: any) => typeof r.data === 'string' ? r.data : JSON.stringify(r.data, null, 2));
  },

  async import(json: string): Promise<{ workflowId: string; version: string }> {
    if (isMockEnabled) {
      const payload = JSON.parse(json);
      const definition = extractDefinition(json);
      const graph = fromDefinition(definition);
      const created = await this.create({
        name: `${payload.workflow?.name ?? '导入工作流'} - 导入`,
        description: payload.workflow?.description,
        category: payload.workflow?.category ?? '其他',
        nodes: graph.nodes,
        edges: graph.edges,
      });
      return { workflowId: created.id, version: String(created.version) };
    }
    const raw = await http.post(endpoint('/import'), { json }).then((r: any) => r.data);
    return { workflowId: String(raw.workflowId), version: String(raw.version) };
  },

  // ==================== 触发器注册 ====================

  /**
   * 注册 Webhook 触发器。
   * 后端: POST /api/webhook/register
   * 请求: { workflowId, path, method, secret? }
   * 返回: { workflowCode, path, method, secret?, token, webhookUrl }
   */
  async registerWebhook(req: {
    workflowId: string | number;
    path: string;
    method?: 'GET' | 'POST' | 'PUT';
    secret?: string;
  }): Promise<{
    workflowCode: string;
    path: string;
    method: string;
    secret?: string;
    token?: string;
    webhookUrl: string;
  }> {
    if (isMockEnabled) {
      // mock 模式：直接本地拼一个 URL
      const path = req.path.startsWith('/') ? req.path : `/${req.path}`;
      const method = (req.method ?? 'POST').toUpperCase();
      const code = `wf_${req.workflowId}`;
      return {
        workflowCode: code,
        path,
        method,
        secret: req.secret,
        token: Math.random().toString(36).slice(2, 18),
        webhookUrl: `${typeof window !== 'undefined' ? window.location.origin : ''}/api/webhook/${code}${path}`,
      };
    }
    return http
      .post(serviceUrl('workflow', '/api/webhook/register'), {
        workflowId: req.workflowId,
        path: req.path,
        method: req.method ?? 'POST',
        secret: req.secret,
      })
      .then((r: any) => r.data);
  },

  /**
   * 注册一个定时任务。
   * 后端: POST /api/schedule/workflow/{workflowId}/node/{nodeId}?cronExpression=...
   */
  async registerSchedule(req: {
    workflowId: string | number;
    nodeId: string;
    cronExpression: string;
  }): Promise<void> {
    if (isMockEnabled) return;
    await http.post(
      serviceUrl('workflow', `/api/schedule/workflow/${req.workflowId}/node/${req.nodeId}`),
      null,
      { params: { cronExpression: req.cronExpression } },
    );
  },

  /**
   * 取消定时任务。
   * 后端: DELETE /api/schedule/workflow/{workflowId}/node/{nodeId}
   * （已被 com.meowflow.workflow.trigger.ScheduleController#cancel 覆盖，前端 / cancel 已对接）
   */
  async unregisterSchedule(req: { workflowId: string | number; nodeId: string }): Promise<void> {
    if (isMockEnabled) return;
    await http.delete(
      serviceUrl('workflow', `/api/schedule/workflow/${req.workflowId}/node/${req.nodeId}`),
    );
  },
};

export function toDefinition(
  nodes: WorkflowNode[],
  edges: WorkflowEdge[],
  changelog?: string,
): WorkflowDefinitionPayload {
  return {
    nodes: nodes.map((node) => ({
      id: node.id,
      type: node.type,
      name: node.name,
      position: { x: node.x, y: node.y },
      data: {
        category: node.category,
        description: node.description,
        config: node.config,
      },
    })),
    edges: edges.map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourcePort,
      targetHandle: edge.targetPort,
      type: edge.edgeType ?? 'default',
      data: {
        label: edge.label,
        type: edge.edgeType ?? 'default',
        config: edge.config ?? {},
      },
    })),
    variables: {},
    inputSchema: {},
    outputSchema: {},
    changelog,
  };
}

function extractDefinition(exported?: string | null): WorkflowDefinitionPayload | undefined {
  if (!exported) return undefined;
  try {
    const payload = typeof exported === 'string' ? JSON.parse(exported) : exported;
    return payload.latestDefinition ?? payload.definition ?? undefined;
  } catch {
    return undefined;
  }
}

function fromDefinition(definition?: WorkflowDefinitionPayload): { nodes: WorkflowNode[]; edges: WorkflowEdge[] } {
  if (!definition) return { nodes: [], edges: [] };
  return {
    nodes: (definition.nodes ?? []).map((node: any) => ({
      id: String(node.id),
      type: node.type,
      name: node.name || node.data?.name || node.type,
      category: node.data?.category || inferCategory(node.type),
      description: node.data?.description,
      x: Number(node.position?.x ?? node.x ?? 0),
      y: Number(node.position?.y ?? node.y ?? 0),
      config: node.data?.config ?? node.config ?? node.data ?? {},
      status: 'pending',
    })),
    edges: (definition.edges ?? []).map((edge: any) => {
      const edgeData = edge.data ?? {};
      const rawType = edge.type ?? edgeData.type ?? 'default';
      const edgeType = rawType === 'condition-true' || rawType === 'condition-false'
        ? 'condition'
        : rawType;
      return {
        id: String(edge.id),
        source: String(edge.source),
        target: String(edge.target),
        sourcePort: edge.sourceHandle ?? edge.sourcePort,
        targetPort: edge.targetHandle ?? edge.targetPort,
        edgeType: edgeType as WorkflowEdge['edgeType'],
        label: edgeData.label ?? edge.label,
        config: edgeData.config ?? edge.config ?? {},
      };
    }),
  };
}

function mapWorkflow(raw: any, definition?: WorkflowDefinitionPayload): Workflow {
  const graph = definition
    ? fromDefinition(definition)
    : { nodes: raw?.nodes ?? [], edges: raw?.edges ?? [] };
  return {
    id: String(raw?.id ?? ''),
    name: raw?.name ?? '未命名工作流',
    description: raw?.description ?? '',
    category: raw?.categoryName ?? raw?.category ?? '未分类',
    categoryId: raw?.categoryId != null ? String(raw.categoryId) : undefined,
    groupId: raw?.groupId != null ? String(raw.groupId) : undefined,
    code: raw?.code,
    icon: raw?.icon,
    tags: raw?.tags ?? [],
    isPublic: raw?.isPublic ?? false,
    status: mapBackendStatus(raw?.status ?? 'draft'),
    version: raw?.currentVersion ?? raw?.version ?? 'v1',
    nodes: graph.nodes,
    edges: graph.edges,
    createdBy: String(raw?.createdBy ?? raw?.ownerId ?? ''),
    createdAt: raw?.createdAt ?? raw?.createTime ?? new Date().toISOString(),
    updatedAt: raw?.updatedAt ?? raw?.updateTime ?? raw?.createTime ?? new Date().toISOString(),
    publishedAt: raw?.publishedAt,
    stats: mapStats(raw),
  };
}

function mapWorkflowListItem(raw: any): WorkflowListItem {
  const wf = mapWorkflow(raw);
  const { nodes: _nodes, edges: _edges, createdBy: _createdBy, groupId: _groupId, ...item } = wf;
  void _nodes; void _edges; void _createdBy; void _groupId;
  return item;
}

function mapStats(raw: any): WorkflowStats {
  const stats = raw?.stats ?? {};
  const totalRuns = Number(stats.executions ?? raw?.statTotalRun ?? 0);
  const successCount = Number(raw?.statSuccessCount ?? stats.successCount ?? 0);
  const failCount = Number(raw?.statFailCount ?? stats.failCount ?? 0);
  const completed = successCount + failCount;
  const successRate = completed > 0 ? successCount / completed : Number(stats.successRate ?? 0);
  return {
    executions: totalRuns,
    successRate,
    avgDuration: Number(raw?.statAvgDurationMs ?? stats.avgDuration ?? 0),
    totalCost: Number(raw?.statTotalCost ?? stats.totalCost ?? 0),
    todayExecutions: Number(raw?.statTodayRunCount ?? stats.todayExecutions ?? 0),
    todayCost: Number(raw?.statTodayCost ?? stats.todayCost ?? 0),
  };
}

function mapVersion(raw: any): WorkflowVersion {
  return {
    id: String(raw?.id ?? ''),
    workflowId: String(raw?.workflowId ?? ''),
    version: String(raw?.version ?? ''),
    publishStatus: raw?.publishStatus ?? 'draft',
    changelog: raw?.changelog,
    publishedAt: raw?.publishedAt,
    publishedBy: raw?.publishedBy != null ? String(raw.publishedBy) : undefined,
    createdAt: raw?.createdAt ?? raw?.createTime,
  };
}

function numericId(value?: string): number | undefined {
  if (!value || !/^\d+$/.test(String(value))) return undefined;
  return Number(value);
}

function inferCategory(type: string): WorkflowNode['category'] {
  const prefix = String(type).split('.')[0];
  if (prefix === 'trigger') return 'trigger';
  if (prefix === 'ai') return 'ai';
  if (prefix === 'flow') return 'flow';
  if (prefix === 'notify') return 'notify';
  return 'tool';
}

import { dayjs } from '@/utils/format';
import { createEdgeId, createNodeId, createWorkflowId, loadWorkflows, saveWorkflows } from './workflows';
import { loadTemplates } from './templates';
import { buildCostStats, buildOverview } from './stats';
import { buildExecutionDetail, buildLogs } from './logs';
import { MOCK_USER } from './users';
import { DEFAULT_INTEGRATIONS, DEFAULT_LLM_MODELS } from './llm';
import { NODE_CATALOG } from './nodes';
import { STORAGE_KEYS } from '@/utils/constants';
import { getStoredJson, setStoredJson } from '@/utils/auth';
import type { Execution, NodeCategory, Workflow, WorkflowListItem } from '@/types/workflow';
import { getMockOrgs, addMockOrg, deleteMockOrg } from './organization';

export interface MockRequest {
  url: string;
  method: string; // GET, POST, PUT, DELETE
  params?: Record<string, any>;
  body?: any;
}

export interface MockResponse<T> {
  code: number;
  message: string;
  data: T;
}

function ok<T>(data: T, message = 'success'): MockResponse<T> {
  return { code: 200, message, data };
}

function err(message: string, code = 400): MockResponse<null> {
  return { code, message, data: null };
}

/** Match Express-style path "/workflows/:id" against a concrete URL "/workflows/abc". */
function matchRoute(pattern: string, url: string): Record<string, string> | null {
  const ps = pattern.split('/').filter(Boolean);
  const us = url.split('?')[0].split('/').filter(Boolean);
  if (ps.length !== us.length) return null;
  const params: Record<string, string> = {};
  for (let i = 0; i < ps.length; i++) {
    const p = ps[i];
    const u = us[i];
    if (p.startsWith(':')) params[p.slice(1)] = decodeURIComponent(u);
    else if (p !== u) return null;
  }
  return params;
}

function toListItem(wf: Workflow, executionStatus?: Execution['status']): WorkflowListItem {
  const { nodes, edges, definition, ...rest } = wf;
  void nodes; void edges; void definition;
  return { ...rest, executionStatus };
}

function getExecutionsByWorkflow(workflowId?: string): Execution[] {
  const all = getStoredJson<Execution[]>(STORAGE_KEYS.EXECUTIONS, buildLogs(20));
  return workflowId ? all.filter((e) => e.workflowId === workflowId) : all;
}

// -----------------------------------------------------------------------------
// Handler table — order matters: most specific patterns first
// -----------------------------------------------------------------------------
type Handler = (req: MockRequest, pathParams: Record<string, string>) => MockResponse<any>;

// 将内置模板简写分类（transform / end / action / data / control）映射为合法的 NodeCategory
const CATEGORY_MAP: Record<string, NodeCategory> = {
  transform: 'flow',
  end: 'flow',
  action: 'tool',
  data: 'tool',
  control: 'flow',
  trigger: 'trigger',
  ai: 'ai',
  tool: 'tool',
  notify: 'notify',
};

function normalizeCategory(raw?: string, nodeType?: string): NodeCategory {
  if (raw && CATEGORY_MAP[raw]) return CATEGORY_MAP[raw] as NodeCategory;
  if (nodeType) {
    if (nodeType.startsWith('trigger')) return 'trigger';
    if (nodeType.startsWith('ai.')) return 'ai';
    if (nodeType.startsWith('notify')) return 'notify';
    if (nodeType.startsWith('http') || nodeType.startsWith('tool') || nodeType.startsWith('db')) return 'tool';
    if (
      nodeType.startsWith('condition') ||
      nodeType.startsWith('flow') ||
      nodeType.startsWith('code') ||
      nodeType.startsWith('transform') ||
      nodeType.startsWith('end')
    ) {
      return 'flow';
    }
  }
  return 'flow';
}

const handlers: Array<{ method: string; pattern: string; handler: Handler }> = [
  // -------- workflows --------
  {
    method: 'GET',
    pattern: '/workflows',
    handler: (req) => {
      const list = loadWorkflows();
      const execs = getExecutionsByWorkflow();
      let items = list.map((w) => {
        const recent = execs.find((e) => e.workflowId === w.id);
        return toListItem(w, recent?.status);
      });
      const { keyword, category, status } = req.params ?? {};
      if (keyword) {
        const k = String(keyword).toLowerCase();
        items = items.filter((i) => i.name.toLowerCase().includes(k) || (i.description ?? '').toLowerCase().includes(k));
      }
      if (category && category !== 'all') items = items.filter((i) => i.category === category);
      if (status && status !== 'all') items = items.filter((i) => i.status === status);
      const page = Number(req.params?.page ?? 1);
      const pageSize = Number(req.params?.pageSize ?? 20);
      const start = (page - 1) * pageSize;
      const slice = items.slice(start, start + pageSize);
      return ok({ items: slice, total: items.length, page, pageSize, totalPages: Math.max(1, Math.ceil(items.length / pageSize)) });
    },
  },
  {
    method: 'POST',
    pattern: '/workflows',
    handler: (req) => {
      const list = loadWorkflows();
      const id = createWorkflowId();
      const now = dayjs().toISOString();
      // 与后端对齐：definition 是 WorkflowDefinitionRequest 形状
      const def = req.body?.definition ?? null;
      const seedNodes = def
        ? def.nodes?.map((n: any) => ({
            id: n.id,
            type: n.type,
            category: n.data?.category ?? normalizeCategory(undefined, n.type),
            name: n.name,
            description: n.data?.description,
            x: n.position?.x ?? 100,
            y: n.position?.y ?? 100,
            config: n.data?.config ?? {},
          })) ?? []
        : [
            {
              id: createNodeId(),
              type: 'trigger.webhook',
              category: 'trigger',
              name: 'Webhook 触发器',
              x: 100,
              y: 100,
              config: {},
            },
          ];
      const seedEdges = def
        ? def.edges?.map((e: any) => ({
            id: e.id,
            source: e.source,
            target: e.target,
            sourcePort: e.sourceHandle,
            targetPort: e.targetHandle,
            label: e.data?.label,
          })) ?? []
        : [];
      const wf: Workflow = {
        id,
        name: req.body?.name || '未命名工作流',
        description: req.body?.description || '',
        category: req.body?.categoryName ?? req.body?.category ?? '其他',
        categoryId: req.body?.categoryId != null ? String(req.body.categoryId) : undefined,
        groupId: req.body?.groupId != null ? String(req.body.groupId) : undefined,
        code: req.body?.code,
        icon: req.body?.icon,
        tags: req.body?.tags ?? [],
        isPublic: req.body?.isPublic ?? false,
        status: 'draft',
        version: def?.version ?? 'v1',
        nodes: seedNodes,
        edges: seedEdges,
        createdBy: '小猪大白',
        createdAt: now,
        updatedAt: now,
        definition: def,
        stats: {
          executions: 0, successRate: 0, avgDuration: 0, totalCost: 0, todayExecutions: 0, todayCost: 0,
        },
      };
      const next = [wf, ...list];
      saveWorkflows(next);
      return ok(wf);
    },
  },
  {
    method: 'GET',
    pattern: '/workflows/:id',
    handler: (req, p) => {
      const list = loadWorkflows();
      const wf = list.find((w) => w.id === p.id);
      if (!wf) return err('工作流不存在', 404);
      // 模拟后端 WorkflowResponse.definition：在响应顶层挂上 definition
      return ok({ ...wf, definition: wf.definition });
    },
  },
  {
    method: 'PUT',
    pattern: '/workflows/:id',
    handler: (req, p) => {
      const list = loadWorkflows();
      const idx = list.findIndex((w) => w.id === p.id);
      if (idx < 0) return err('工作流不存在', 404);
      const prev = list[idx];
      // 定义（definition）单独覆盖；其余 metadata 字段合并
      const { definition: _def, nodes: _n, edges: _e, ...rest } = req.body ?? {};
      void _def; void _n; void _e;
      const merged: Workflow = {
        ...prev,
        ...rest,
        version: (Number(prev.version) || 1) + 1,
        updatedAt: dayjs().toISOString(),
      };
      // 如果请求里带 definition，则覆盖本地持久化的 definition；否则保持原有
      if (req.body?.definition) {
        merged.definition = req.body.definition;
      }
      // 如果请求里带 nodes/edges，则覆盖本地 nodes/edges（与旧 mock 形状兼容）
      if (req.body?.nodes) merged.nodes = req.body.nodes;
      if (req.body?.edges) merged.edges = req.body.edges;
      list[idx] = merged;
      saveWorkflows(list);
      return ok({ ...merged, definition: merged.definition });
    },
  },
  {
    method: 'POST',
    pattern: '/workflows/:id/versions',
    handler: (req, p) => {
      const list = loadWorkflows();
      const idx = list.findIndex((w) => w.id === p.id);
      if (idx < 0) return err('工作流不存在', 404);
      const prev = list[idx];
      const def = req.body;
      const nextVersion = `v${(Number(String(prev.version).replace(/^v/i, '')) || 0) + 1}`;
      const graph = def
        ? {
            nodes: def.nodes?.map((n: any) => ({
              id: n.id,
              type: n.type,
              category: n.data?.category ?? normalizeCategory(undefined, n.type),
              name: n.name,
              description: n.data?.description,
              x: n.position?.x ?? 100,
              y: n.position?.y ?? 100,
              config: n.data?.config ?? {},
            })) ?? [],
            edges: def.edges?.map((e: any) => ({
              id: e.id,
              source: e.source,
              target: e.target,
              sourcePort: e.sourceHandle,
              targetPort: e.targetHandle,
              label: e.data?.label,
            })) ?? [],
          }
        : { nodes: prev.nodes, edges: prev.edges };
      const updated: Workflow = {
        ...prev,
        version: nextVersion,
        nodes: graph.nodes,
        edges: graph.edges,
        definition: def ?? prev.definition,
        updatedAt: dayjs().toISOString(),
      };
      list[idx] = updated;
      saveWorkflows(list);
      return ok({
        id: `${p.id}-${Date.now()}`,
        workflowId: p.id,
        version: nextVersion,
        publishStatus: 'draft',
        changelog: def?.changelog,
        createdAt: dayjs().toISOString(),
      });
    },
  },
  {
    method: 'DELETE',
    pattern: '/workflows/:id',
    handler: (req, p) => {
      const list = loadWorkflows();
      const next = list.filter((w) => w.id !== p.id);
      saveWorkflows(next);
      return ok(null, '删除成功');
    },
  },
  {
    method: 'POST',
    pattern: '/workflows/:id/publish',
    handler: (_req, p) => {
      const list = loadWorkflows();
      const idx = list.findIndex((w) => w.id === p.id);
      if (idx < 0) return err('工作流不存在', 404);
      list[idx] = { ...list[idx], status: 'running', publishedAt: dayjs().toISOString(), updatedAt: dayjs().toISOString() };
      saveWorkflows(list);
      return ok(list[idx]);
    },
  },
  {
    method: 'POST',
    pattern: '/workflows/:id/run',
    handler: (req, p) => {
      const list = loadWorkflows();
      const wf = list.find((w) => w.id === p.id);
      if (!wf) return err('工作流不存在', 404);
      const exec: Execution = {
        id: `exec-${Date.now().toString(36)}`,
        workflowId: wf.id,
        workflowName: wf.name,
        status: 'running',
        trigger: 'manual',
        startTime: dayjs().toISOString(),
        nodes: [],
      };
      const all = getStoredJson<Execution[]>(STORAGE_KEYS.EXECUTIONS, buildLogs(20));
      all.unshift(exec);
      setStoredJson(STORAGE_KEYS.EXECUTIONS, all.slice(0, 60));
      // bump executions
      wf.stats = { ...wf.stats, executions: wf.stats.executions + 1, todayExecutions: wf.stats.todayExecutions + 1 };
      saveWorkflows(list);
      return ok(exec);
    },
  },
  {
    method: 'POST',
    pattern: '/workflows/:id/stop',
    handler: (_req, p) => {
      const list = loadWorkflows();
      const idx = list.findIndex((w) => w.id === p.id);
      if (idx < 0) return err('工作流不存在', 404);
      list[idx] = { ...list[idx], status: 'stopped', updatedAt: dayjs().toISOString() };
      saveWorkflows(list);
      return ok(null);
    },
  },
  {
    method: 'POST',
    pattern: '/workflows/:id/duplicate',
    handler: (_req, p) => {
      const list = loadWorkflows();
      const wf = list.find((w) => w.id === p.id);
      if (!wf) return err('工作流不存在', 404);
      const id = createWorkflowId();
      const now = dayjs().toISOString();
      const copy: Workflow = {
        ...JSON.parse(JSON.stringify(wf)),
        id,
        name: `${wf.name} - 副本`,
        status: 'draft',
        version: 1,
        createdAt: now,
        updatedAt: now,
        publishedAt: undefined,
        stats: { executions: 0, successRate: 0, avgDuration: 0, totalCost: 0, todayExecutions: 0, todayCost: 0 },
      };
      saveWorkflows([copy, ...list]);
      return ok(copy);
    },
  },

  // -------- templates --------
  {
    method: 'GET',
    pattern: '/templates',
    handler: (req) => {
      const all = loadTemplates();
      const { keyword, category } = req.params ?? {};
      let items = all.slice();
      if (keyword) {
        const k = String(keyword).toLowerCase();
        items = items.filter((t) => t.name.toLowerCase().includes(k) || t.description.toLowerCase().includes(k));
      }
      if (category && category !== 'all') items = items.filter((t) => t.category === category);
      return ok({ items, total: items.length });
    },
  },
  {
    method: 'GET',
    pattern: '/templates/:id',
    handler: (_req, p) => {
      const all = loadTemplates();
      const t = all.find((x) => x.id === p.id);
      return t ? ok(t) : err('模板不存在', 404);
    },
  },
  {
    method: 'POST',
    pattern: '/templates/:id/use',
    handler: (_req, p) => {
      const all = loadTemplates();
      const t = all.find((x) => x.id === p.id);
      if (!t) return err('模板不存在', 404);

      const tAny = t as any;
      const jsonRaw = tAny.__workflowJson ?? tAny.workflowJson;
      if (!jsonRaw) return err('该模板暂未配置工作流定义', 400);

      let templateNodes: any[] = [];
      let templateEdges: any[] = [];
      try {
        const parsed = JSON.parse(jsonRaw);
        templateNodes = parsed?.nodes ?? [];
        templateEdges = parsed?.edges ?? [];
      } catch {
        return err('工作流定义格式错误', 500);
      }

      // 将模板节点内 ID（字符串）映射为全局唯一 ID
      const idMap: Record<string, string> = {};
      const newNodes: Workflow['nodes'] = templateNodes.map((n, idx) => {
        const newId = createNodeId();
        idMap[n.id ?? `node-${idx}`] = newId;
        return {
          id: newId,
          type: n.type || 'unknown',
          category: normalizeCategory(n.category, n.type),
          name: n.name || n.type || `节点 ${idx + 1}`,
          description: n.description,
          x: typeof n.x === 'number' ? n.x : idx * 300 + 120,
          y: typeof n.y === 'number' ? n.y : 200,
          config: n.config && typeof n.config === 'object' ? n.config : {},
          status: 'pending' as const,
        };
      });

      // 重映射所有边的 source/target
      const VALID_EDGE_TYPES = new Set(['default', 'condition', 'loop', 'error']);
      const newEdges: Workflow['edges'] = templateEdges.map((e) => {
        const rawType = e.type ?? 'default';
        return {
          id: createEdgeId(),
          source: idMap[e.source] ?? e.source,
          target: idMap[e.target] ?? e.target,
          label: e.label,
          edgeType: VALID_EDGE_TYPES.has(rawType) ? rawType : 'default',
          config: e.config && typeof e.config === 'object' ? e.config : {},
        };
      });

      const list = loadWorkflows();
      const id = createWorkflowId();
      const now = dayjs().toISOString();
      const wf: Workflow = {
        id,
        name: `${t.name} - 副本`,
        description: t.description,
        category: t.category,
        status: 'draft',
        version: 1,
        nodes: newNodes,
        edges: newEdges,
        templateId: t.id,
        createdBy: '小猪大白',
        createdAt: now,
        updatedAt: now,
        stats: { executions: 0, successRate: 0, avgDuration: 0, totalCost: 0, todayExecutions: 0, todayCost: 0 },
      };
      saveWorkflows([wf, ...list]);
      return ok(wf);
    },
  },

  // -------- template like/favorite --------
  {
    method: 'POST',
    pattern: '/templates/:id/like',
    handler: (_req, p) => {
      const all = loadTemplates();
      const t = all.find((x) => x.id === p.id);
      if (!t) return err('模板不存在', 404);
      (t as any).likes = ((t as any).likes ?? 0) + 1;
      (t as any).isLiked = true;
      return ok({ likes: (t as any).likes, isLiked: true });
    },
  },
  {
    method: 'DELETE',
    pattern: '/templates/:id/like',
    handler: (_req, p) => {
      const all = loadTemplates();
      const t = all.find((x) => x.id === p.id);
      if (!t) return err('模板不存在', 404);
      (t as any).likes = Math.max(0, ((t as any).likes ?? 1) - 1);
      (t as any).isLiked = false;
      return ok({ likes: (t as any).likes, isLiked: false });
    },
  },
  {
    method: 'POST',
    pattern: '/templates/:id/favorite',
    handler: (_req, p) => {
      const all = loadTemplates();
      const t = all.find((x) => x.id === p.id);
      if (!t) return err('模板不存在', 404);
      (t as any).isFavorited = true;
      return ok({ isFavorited: true });
    },
  },
  {
    method: 'DELETE',
    pattern: '/templates/:id/favorite',
    handler: (_req, p) => {
      const all = loadTemplates();
      const t = all.find((x) => x.id === p.id);
      if (!t) return err('模板不存在', 404);
      (t as any).isFavorited = false;
      return ok({ isFavorited: false });
    },
  },
  {
    method: 'GET',
    pattern: '/templates/favorites',
    handler: () => {
      const all = loadTemplates();
      const items = all.filter((t) => (t as any).isFavorited);
      return ok({ items, total: items.length });
    },
  },

  // -------- stats --------
  {
    method: 'GET',
    pattern: '/stats/overview',
    handler: () => ok(buildOverview()),
  },
  {
    method: 'GET',
    pattern: '/stats/trend',
    handler: () => ok(buildOverview().trend),
  },
  {
    method: 'GET',
    pattern: '/stats/cost',
    handler: () => ok(buildCostStats()),
  },

  // -------- logs --------
  {
    method: 'GET',
    pattern: '/logs',
    handler: (req) => {
      const all = getStoredJson<Execution[]>(STORAGE_KEYS.EXECUTIONS, buildLogs(20));
      const { workflowId, status, page = 1, pageSize = 20 } = req.params ?? {};
      let items = all.slice();
      if (workflowId) items = items.filter((e) => e.workflowId === workflowId);
      if (status && status !== 'all') items = items.filter((e) => e.status === status);
      const p = Number(page);
      const ps = Number(pageSize);
      const start = (p - 1) * ps;
      const slice = items.slice(start, start + ps);
      return ok({ items: slice, total: items.length, page: p, pageSize: ps, totalPages: Math.max(1, Math.ceil(items.length / ps)) });
    },
  },
  {
    method: 'GET',
    pattern: '/logs/:execId',
    handler: (_req, p) => {
      const all = getStoredJson<Execution[]>(STORAGE_KEYS.EXECUTIONS, buildLogs(20));
      const found = all.find((e) => e.id === p.execId);
      return found ? ok(found) : ok(buildExecutionDetail(p.execId) ?? buildExecutionDetail('exec-stub'));
    },
  },

  // -------- users --------
  {
    method: 'GET',
    pattern: '/users/me',
    handler: () => ok(MOCK_USER),
  },
  {
    method: 'PUT',
    pattern: '/users/me',
    handler: (req) => ok({ ...MOCK_USER, ...req.body }),
  },

  // -------- system --------
  {
    method: 'GET',
    pattern: '/system/llm-models',
    handler: () => ok(DEFAULT_LLM_MODELS),
  },
  {
    method: 'PUT',
    pattern: '/system/llm-models/:id',
    handler: (req, p) => {
      const list = DEFAULT_LLM_MODELS.map((m) => (m.id === p.id ? { ...m, ...req.body } : m));
      return ok(list.find((m) => m.id === p.id) ?? null);
    },
  },
  {
    method: 'GET',
    pattern: '/system/integrations',
    handler: () => ok(DEFAULT_INTEGRATIONS),
  },
  {
    method: 'PUT',
    pattern: '/system/integrations/:id',
    handler: (req, p) => {
      const updated = DEFAULT_INTEGRATIONS.map((i) => (i.id === p.id ? { ...i, ...req.body } : i));
      return ok(updated.find((i) => i.id === p.id) ?? null);
    },
  },

  // -------- nodes catalog --------
  {
    method: 'GET',
    pattern: '/nodes/catalog',
    handler: () => ok(NODE_CATALOG),
  },

  // -------- executions (与后端 ExecutionController 对齐) --------
  {
    method: 'POST',
    pattern: '/execution',
    handler: (req) => {
      const wfId = req.body?.workflowId;
      const list = loadWorkflows();
      const wf = list.find((w) => w.id === wfId);
      if (!wf) return err('工作流不存在', 404);
      const exec: Execution = {
        id: `exec-${Date.now().toString(36)}`,
        workflowId: wf.id,
        workflowName: wf.name,
        status: 'success',
        trigger: req.body?.trigger ?? 'manual',
        startTime: dayjs().toISOString(),
        endTime: dayjs().toISOString(),
        duration: 1200,
        cost: 0.05,
        nodes: [],
      };
      const all = getStoredJson<Execution[]>(STORAGE_KEYS.EXECUTIONS, buildLogs(20));
      all.unshift(exec);
      setStoredJson(STORAGE_KEYS.EXECUTIONS, all.slice(0, 60));
      wf.stats = { ...wf.stats, executions: wf.stats.executions + 1, todayExecutions: wf.stats.todayExecutions + 1 };
      saveWorkflows(list);
      return ok({
        executionId: exec.id,
        workflowId: exec.workflowId,
        status: exec.status,
        output: { mocked: true },
        startTime: exec.startTime,
        endTime: exec.endTime,
        duration: exec.duration,
        nodes: exec.nodes,
      });
    },
  },
  {
    method: 'POST',
    pattern: '/executions/:workflowId/execute-async',
    handler: (req, p) => {
      const wf = loadWorkflows().find((w) => w.id === p.workflowId);
      if (!wf) return err('工作流不存在', 404);
      const execId = `exec-${Date.now().toString(36)}`;
      return ok({ executionId: execId });
    },
  },
  {
    method: 'POST',
    pattern: '/execution/:id/cancel',
    handler: (_req, p) => ok(null, '已取消'),
  },
  {
    method: 'GET',
    pattern: '/execution/:id',
    handler: (_req, p) => {
      const all = getStoredJson<Execution[]>(STORAGE_KEYS.EXECUTIONS, buildLogs(20));
      const found = all.find((e) => e.id === p.id);
      return found ? ok(found) : ok(buildExecutionDetail(p.id) ?? buildExecutionDetail('exec-stub'));
    },
  },
  {
    method: 'GET',
    pattern: '/execution/page',
    handler: (req) => {
      const all = getStoredJson<Execution[]>(STORAGE_KEYS.EXECUTIONS, buildLogs(20));
      const { workflowId, status, page = 1, pageSize = 20 } = req.params ?? {};
      let items = all.slice();
      if (workflowId) items = items.filter((e) => e.workflowId === workflowId);
      if (status && status !== 'all') items = items.filter((e) => e.status === status);
      const p = Number(page);
      const ps = Number(pageSize);
      const start = (p - 1) * ps;
      const slice = items.slice(start, start + ps);
      return ok({ items: slice, total: items.length, page: p, pageSize: ps, totalPages: Math.max(1, Math.ceil(items.length / ps)) });
    },
  },
  {
    method: 'GET',
    pattern: '/execution/:id/nodes',
    handler: (_req, p) => {
      const detail = buildExecutionDetail(p.id);
      return ok(detail?.nodes ?? []);
    },
  },
  {
    method: 'GET',
    pattern: '/execution/:id/logs',
    handler: () => {
      const now = Date.now();
      const levels = ['INFO', 'INFO', 'INFO', 'DEBUG', 'WARN', 'ERROR', 'SUCCESS'];
      const msgs = [
        '工作流开始执行',
        '触发器已接收请求',
        '节点 LLM 对话开始',
        '缓存写入完成',
        '命中限流，已重试',
        '子节点失败: AI 分类',
        '工作流执行成功',
      ];
      const logs = Array.from({ length: msgs.length }, (_, i) => ({
        id: now + i,
        executionId: 0,
        nodeId: `n${(i % 3) + 1}`,
        level: levels[i],
        message: msgs[i],
        payload: { index: i },
        createdAt: new Date(now + i * 100).toISOString(),
      }));
      return ok(logs);
    },
  },
  {
    method: 'GET',
    pattern: '/execution/workflow/:workflowId',
    handler: (req, p) => {
      const all = getStoredJson<Execution[]>(STORAGE_KEYS.EXECUTIONS, buildLogs(20));
      const filtered = all.filter((e) => e.workflowId === p.workflowId);
      const page = Number(req.params?.page ?? 1);
      const pageSize = Number(req.params?.pageSize ?? 20);
      const start = (page - 1) * pageSize;
      const slice = filtered.slice(start, start + pageSize);
      return ok({ items: slice, total: filtered.length, page, pageSize, totalPages: Math.max(1, Math.ceil(filtered.length / pageSize)) });
    },
  },

  // -------- auth (mock 演示用) --------
  {
    method: 'POST',
    pattern: '/auth/login',
    handler: (req) => {
      const u = req.body?.username ?? MOCK_USER.username;
      const access = 'mock-access-' + Date.now().toString(36);
      const refresh = 'mock-refresh-' + Date.now().toString(36);
      return ok({
        accessToken: access,
        refreshToken: refresh,
        tokenType: 'Bearer',
        expiresIn: 7200,
        user: {
          id: MOCK_USER.id,
          username: u,
          nickname: MOCK_USER.nickname,
          email: MOCK_USER.email,
          avatar: MOCK_USER.avatar,
          roles: MOCK_USER.roles,
          permissions: MOCK_USER.permissions,
        },
      });
    },
  },
  {
    method: 'GET',
    pattern: '/captcha/generate',
    handler: () => {
      // Mock: 生成一个纯色 SVG base64 作为假验证码图
      const uuid = 'mock-' + Date.now().toString(36);
      const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="40"><rect width="120" height="40" fill="#e8f0fe"/><text x="60" y="26" font-family="Arial" font-size="20" fill="#1a73e8" text-anchor="middle" font-weight="bold">1234</text></svg>';
      const base64 = 'data:image/svg+xml;base64,' + btoa(svg);
      return ok({ uuid, img: base64 });
    },
  },
  {
    method: 'POST',
    pattern: '/auth/refresh',
    handler: () => {
      const access = 'mock-access-' + Date.now().toString(36);
      const refresh = 'mock-refresh-' + Date.now().toString(36);
      return ok({
        accessToken: access,
        refreshToken: refresh,
        tokenType: 'Bearer',
        expiresIn: 7200,
        user: {
          id: MOCK_USER.id,
          username: MOCK_USER.username,
          nickname: MOCK_USER.nickname,
          email: MOCK_USER.email,
          avatar: MOCK_USER.avatar,
          roles: MOCK_USER.roles,
          permissions: MOCK_USER.permissions,
        },
      });
    },
  },
  {
    method: 'POST',
    pattern: '/auth/logout',
    handler: () => ok(null),
  },
  {
    method: 'GET',
    pattern: '/auth/me',
    handler: () => ok(MOCK_USER),
  },

  // -------- auth: register (mock) --------
  {
    method: 'POST',
    pattern: '/auth/register',
    handler: (req) => {
      const { username, email } = req.body ?? {};
      if (!username || !email) return err('用户名和邮箱不能为空', 400);
      const access = 'mock-access-' + Date.now().toString(36);
      const refresh = 'mock-refresh-' + Date.now().toString(36);
      return ok({
        accessToken: access,
        refreshToken: refresh,
        tokenType: 'Bearer',
        expiresIn: 7200,
        user: {
          id: 'mock-' + Date.now().toString(36),
          username,
          nickname: username,
          email,
          roles: ['common'],
          permissions: MOCK_USER.permissions,
        },
      });
    },
  },

  // -------- auth: send email code (mock) --------
  {
    method: 'POST',
    pattern: '/auth/email-code',
    handler: (req) => {
      const { email } = req.body ?? {};
      if (!email) return err('邮箱不能为空', 400);
      return ok(null, '验证码已发送');
    },
  },

  // -------- auth: reset password (mock) --------
  {
    method: 'POST',
    pattern: '/auth/password/reset',
    handler: (req) => {
      const { email, code } = req.body ?? {};
      if (!email || !code) return err('邮箱和验证码不能为空', 400);
      return ok(null, '密码重置成功');
    },
  },

  // -------- organizations (orgs) --------
  {
    method: 'GET',
    pattern: '/api/v1/orgs/tree',
    handler: () => {
      return ok(getMockOrgs());
    },
  },
  {
    method: 'POST',
    pattern: '/api/v1/orgs',
    handler: (req) => {
      const { name, parentId, code, leader } = req.body ?? {};
      if (!name) return err('组织名称不能为空', 400);
      const newOrg = {
        id: `org-${Date.now().toString(36)}`,
        parentId: parentId || '0',
        name,
        code: code || '',
        leader: leader || '',
        status: '1',
        children: [],
      };
      addMockOrg(newOrg);
      return ok(newOrg);
    },
  },
  {
    method: 'GET',
    pattern: '/api/v1/orgs',
    handler: (req) => {
      const { keyword, pageNum = 1, pageSize = 10 } = req.params ?? {};
      let list = getMockOrgs();
      if (keyword) {
        const k = String(keyword).toLowerCase();
        list = list.filter((o) => o.name.toLowerCase().includes(k) || (o.code ?? '').toLowerCase().includes(k));
      }
      const total = list.length;
      const p = Number(pageNum);
      const ps = Number(pageSize);
      const start = (p - 1) * ps;
      return ok({
        records: list.slice(start, start + ps),
        total,
        current: p,
        size: ps,
        pages: Math.max(1, Math.ceil(total / ps)),
      });
    },
  },
  {
    method: 'DELETE',
    pattern: '/api/v1/orgs/:id',
    handler: (_req, p) => {
      const success = deleteMockOrg(p.id);
      return success ? ok(null) : err('组织不存在', 404);
    },
  },
];

export function findHandler(method: string, url: string): { handler: Handler; pathParams: Record<string, string> } | null {
  for (const h of handlers) {
    if (h.method !== method.toUpperCase()) continue;
    const params = matchRoute(h.pattern, url);
    if (params) return { handler: h.handler, pathParams: params };
  }
  return null;
}

export function listMockRoutes(): Array<{ method: string; pattern: string }> {
  return handlers.map((h) => ({ method: h.method, pattern: h.pattern }));
}
import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import type { Workflow, WorkflowEdge, WorkflowListItem, WorkflowNode } from '@/types/workflow';
import type { CreateWorkflowRequest } from '@/types/workflow';
import { workflowApi } from '@/api/workflow';
import { parseWorkflowJson } from '@/api/template';
import { ElMessage } from '@/utils/notify';
import { createEdgeId, createNodeId } from '@/mock/workflows';
import { getStoredJson, setStoredJson } from '@/utils/auth';

interface HistoryState {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
}

const HISTORY_KEY = (id: string) => `meowflow.history.${id || '__draft__'}`;

export const useWorkflowStore = defineStore('workflow', () => {
  const list = ref<WorkflowListItem[]>([]);
  const total = ref(0);
  const loading = ref(false);

  const current = ref<Workflow | null>(null);
  const nodes = computed<WorkflowNode[]>(() => current.value?.nodes ?? []);
  const edges = computed<WorkflowEdge[]>(() => current.value?.edges ?? []);

  // 历史记录
  const historyStack = ref<HistoryState[]>([]);
  const historyIndex = ref(-1);

  const canUndo = computed(() => historyIndex.value > 0);
  const canRedo = computed(() => historyIndex.value < historyStack.value.length - 1);

  // 脏标记：最近一次“已保存”或“刚从后端拉回”的快照
  const lastSavedSnapshot = ref<string>('');
  // 用户改过任何东西即置 true；显式保存/重置后置 false
  const dirtyFlag = ref(false);

  function markDirty() {
    if (current.value) dirtyFlag.value = true;
  }

  function markSaved() {
    if (!current.value) {
      lastSavedSnapshot.value = '';
      dirtyFlag.value = false;
      return;
    }
    lastSavedSnapshot.value = JSON.stringify({
      name: current.value.name,
      description: current.value.description,
      category: current.value.category,
      nodes: current.value.nodes,
      edges: current.value.edges,
    });
    dirtyFlag.value = false;
  }

  const isDirty = computed(() => dirtyFlag.value);

  // 兜底：万一某些 mutation 没显式调用 markDirty，深比较兜底检测一次
  watch(
    () => current.value,
    (val) => {
      if (!val) {
        dirtyFlag.value = false;
        lastSavedSnapshot.value = '';
        return;
      }
      if (!lastSavedSnapshot.value) return;
      const snapshot = JSON.stringify({
        name: val.name,
        description: val.description,
        category: val.category,
        nodes: val.nodes,
        edges: val.edges,
      });
      if (snapshot !== lastSavedSnapshot.value) dirtyFlag.value = true;
    },
    { deep: true },
  );

  // 当 current 切换时,加载历史
  watch(
    () => current.value?.id,
    (id) => {
      const key = HISTORY_KEY(id || '');
      const cached = getStoredJson<{ stack: HistoryState[]; index: number } | null>(key, null);
      if (cached) {
        historyStack.value = cached.stack;
        historyIndex.value = cached.index;
      } else {
        historyStack.value = [];
        historyIndex.value = -1;
        if (current.value) pushHistory();
      }
    },
  );

  // 持久化历史
  watch(
    [historyStack, historyIndex, () => current.value?.id],
    () => {
      if (!current.value) return;
      setStoredJson(HISTORY_KEY(current.value.id), {
        stack: historyStack.value.slice(-50),
        index: historyIndex.value,
      });
    },
    { deep: true },
  );

  // -------------------------------------------------------------------------- //
  // CRUD
  // -------------------------------------------------------------------------- //
  async function fetchList(params?: any) {
    loading.value = true;
    try {
      const res = await workflowApi.getList(params);
      list.value = res.items;
      total.value = res.total;
    } finally {
      loading.value = false;
    }
  }

  async function fetchOne(id: string) {
    loading.value = true;
    try {
      current.value = await workflowApi.getById(id);
      markSaved();
    } finally {
      loading.value = false;
    }
  }

  function resetCurrent() {
    current.value = null;
    historyStack.value = [];
    historyIndex.value = -1;
    lastSavedSnapshot.value = '';
    dirtyFlag.value = false;
  }

  function ensureCurrent(name = '未命名工作流') {
    if (current.value) return;
    const now = new Date().toISOString();
    current.value = {
      id: '',
      name,
      description: '',
      category: '其他',
      status: 'draft',
      version: 1,
      nodes: [],
      edges: [],
      createdBy: '小猪大白',
      createdAt: now,
      updatedAt: now,
      stats: { executions: 0, successRate: 0, avgDuration: 0, totalCost: 0, todayExecutions: 0, todayCost: 0 },
    };
    pushHistory();
    markSaved();
  }

  function addNode(node: Partial<WorkflowNode> & { type: string; category: any; name: string }) {
    if (!current.value) ensureCurrent();
    pushHistory();
    const newNode: WorkflowNode = {
      id: node.id ?? createNodeId(),
      type: node.type,
      category: node.category,
      name: node.name,
      description: node.description,
      x: node.x ?? 200,
      y: node.y ?? 200,
      config: node.config ?? {},
      status: 'pending',
    };
    current.value!.nodes = [...current.value!.nodes, newNode];
    current.value!.updatedAt = new Date().toISOString();
    markDirty();
    return newNode.id;
  }

  function addNodeAt(pos: { x: number; y: number }, type: string): string | null {
    const id = createNodeId();
    const ok = addNode({
      id,
      type,
      category: 'trigger',
      name: type,
      x: pos.x,
      y: pos.y,
      config: {},
    } as any);
    return ok;
  }

  function removeNode(id: string) {
    if (!current.value) return;
    pushHistory();
    current.value.nodes = current.value.nodes.filter((n) => n.id !== id);
    current.value.edges = current.value.edges.filter((e) => e.source !== id && e.target !== id);
    current.value.updatedAt = new Date().toISOString();
    markDirty();
  }

  function moveNode(id: string, x: number, y: number, recordHistory = false) {
    if (!current.value) return;
    const idx = current.value.nodes.findIndex((nn) => nn.id === id);
    if (idx < 0) return;
    const n = current.value.nodes[idx];
    if (n.x === x && n.y === y) return;
    // Replace the array to create a new reference (ensures Vue watchers trigger)
    current.value.nodes = current.value.nodes.map((nn) =>
      nn.id === id ? { ...nn, x, y } : nn,
    );
    if (recordHistory) {
      current.value.updatedAt = new Date().toISOString();
      pushHistory();
    }
    markDirty();
  }

  function updateNodeConfig(id: string, config: Record<string, any>, name?: string) {
    if (!current.value) return;
    const idx = current.value.nodes.findIndex((nn) => nn.id === id);
    if (idx < 0) return;
    const n = current.value.nodes[idx];
    const merged = { ...n.config, ...config };
    // If no actual change, skip
    if (JSON.stringify(merged) === JSON.stringify(n.config) && (name === undefined || name === n.name)) {
      return;
    }
    pushHistory();
    // Replace the array to create a new reference
    current.value.nodes = current.value.nodes.map((nn) =>
      nn.id === id
        ? { ...nn, config: merged, ...(name !== undefined ? { name } : {}) }
        : nn,
    );
    current.value.updatedAt = new Date().toISOString();
    markDirty();
  }

  /**
   * 更新节点顶层描述（与 config 分离，序列化时写入 definition.data.description）。
   */
  function updateNodeDescription(id: string, description: string) {
    if (!current.value) return;
    const idx = current.value.nodes.findIndex((nn) => nn.id === id);
    if (idx < 0) return;
    const n = current.value.nodes[idx];
    if ((n.description ?? '') === description) return;
    pushHistory();
    current.value.nodes = current.value.nodes.map((nn) =>
      nn.id === id ? { ...nn, description } : nn,
    );
    current.value.updatedAt = new Date().toISOString();
    markDirty();
  }

  function addEdge(source: string, target: string, patch?: Partial<WorkflowEdge>) {
    if (!current.value) return;
    if (source === target) return;
    if (current.value.edges.some((e) => e.source === source && e.target === target)) return;
    pushHistory();
    current.value.edges = [
      ...current.value.edges,
      { id: createEdgeId(), source, target, ...patch },
    ];
    current.value.updatedAt = new Date().toISOString();
    markDirty();
  }

  function removeEdge(id: string) {
    if (!current.value) return;
    pushHistory();
    current.value.edges = current.value.edges.filter((e) => e.id !== id);
    current.value.updatedAt = new Date().toISOString();
    markDirty();
  }

  function updateEdge(id: string, patch: Partial<WorkflowEdge>) {
    if (!current.value) return;
    const idx = current.value.edges.findIndex((x) => x.id === id);
    if (idx === -1) return;
    pushHistory();
    current.value.edges = current.value.edges.map((e, i) =>
      i === idx ? { ...e, ...patch } : e,
    );
    current.value.updatedAt = new Date().toISOString();
    markDirty();
  }

  // 历史管理 ------------------------------------------------------------
  function pushHistory() {
    if (!current.value) return;
    historyStack.value = historyStack.value.slice(0, historyIndex.value + 1);
    historyStack.value.push({
      nodes: JSON.parse(JSON.stringify(current.value.nodes)),
      edges: JSON.parse(JSON.stringify(current.value.edges)),
    });
    historyIndex.value = historyStack.value.length - 1;
    if (historyStack.value.length > 50) {
      historyStack.value.shift();
      historyIndex.value--;
    }
  }

  function undo() {
    if (!current.value || historyIndex.value <= 0) return;
    historyIndex.value--;
    restoreHistory();
  }

  function redo() {
    if (!current.value || historyIndex.value >= historyStack.value.length - 1) return;
    historyIndex.value++;
    restoreHistory();
  }

  function restoreHistory() {
    if (!current.value) return;
    const history = historyStack.value[historyIndex.value];
    if (!history) return;
    current.value.nodes = JSON.parse(JSON.stringify(history.nodes));
    current.value.edges = JSON.parse(JSON.stringify(history.edges));
    current.value.updatedAt = new Date().toISOString();
    markDirty();
  }

  // 执行态 ---------------------------------------------------------------
  function setNodeStatus(id: string, status: WorkflowNode['status']) {
    if (!current.value) return;
    const n = current.value.nodes.find((nn) => nn.id === id);
    if (n) n.status = status;
  }

  function resetNodeStatuses() {
    if (!current.value) return;
    current.value.nodes.forEach((n) => (n.status = 'pending'));
  }

  // -------------------------------------------------------------------------- //
  // 创建
  // -------------------------------------------------------------------------- //
  async function createWorkflow(opts: {
    name: string;
    description?: string;
    category?: string;
    icon?: string;
    nodes?: WorkflowNode[];
    edges?: WorkflowEdge[];
  }): Promise<Workflow> {
    const payload: CreateWorkflowRequest = {
      name: opts.name,
      description: opts.description,
      category: opts.category,
      icon: opts.icon,
      nodes: opts.nodes,
      edges: opts.edges ?? [],
    };
    const created = await workflowApi.create(payload);
    current.value = created;
    markSaved();
    return created;
  }

  /**
   * 从模板创建工作流：先把模板定义迁移为编辑器节点，再真正持久化到后端。
   * 不再只写入前端 store 后跳到无 ID 的编辑器。
   */
  async function createFromTemplate(
    templateId: string,
    templateName: string,
    workflowJson?: string,
    meta?: { description?: string; categoryId?: string; icon?: string },
  ): Promise<Workflow> {
    void templateId;
    bootstrapFromTemplate(templateId, templateName, workflowJson);
    if (!current.value || current.value.nodes.length === 0) {
      throw new Error('模板工作流定义为空，无法创建');
    }
    const created = await workflowApi.create({
      name: current.value.name,
      description: meta?.description || current.value.description,
      categoryId: meta?.categoryId,
      icon: meta?.icon,
      nodes: current.value.nodes,
      edges: current.value.edges,
    });
    current.value = created;
    markSaved();
    return created;
  }
  async function importDsl(json: string): Promise<{ workflowId: string; version: string }> {
    return await workflowApi.import(json);
  }

  // -------------------------------------------------------------------------- //
  // 持久化
  // -------------------------------------------------------------------------- //
  async function saveCurrent() {
    if (!current.value) return;
    if (!current.value.id) {
      const created = await workflowApi.create({
        name: current.value.name,
        description: current.value.description,
        category: current.value.category,
        nodes: current.value.nodes,
        edges: current.value.edges,
      });
      current.value = created;
      markSaved();
      ElMessage.success('已保存为新工作流');
    } else {
      const updated = await workflowApi.update(current.value.id, {
        name: current.value.name,
        description: current.value.description,
        category: current.value.category,
        nodes: current.value.nodes,
        edges: current.value.edges,
      });
      current.value = updated;
      markSaved();
      ElMessage.success('保存成功');
    }
  }

  async function removeById(id: string) {
    await workflowApi.delete(id);
    list.value = list.value.filter((w) => w.id !== id);
    ElMessage.success('删除成功');
  }

  async function publishCurrent() {
    if (!current.value?.id) {
      ElMessage.warning('请先保存工作流');
      return;
    }
    const wf = await workflowApi.publish(current.value.id);
    current.value = wf;
    markSaved();
    ElMessage.success('已发布');
  }

  function guessCategory(nodeType?: string): string {
    if (!nodeType) return 'other';
    const t = nodeType.toLowerCase();
    if (t.startsWith('trigger')) return 'trigger';
    if (t.startsWith('ai.') || t.startsWith('llm') || t.startsWith('classify')) return 'ai';
    if (t.startsWith('condition') || t.startsWith('loop') || t.startsWith('parallel') || t.startsWith('wait')) return 'flow';
    if (t.startsWith('http') || t.startsWith('db') || t.startsWith('code') || t.startsWith('assign')) return 'tool';
    if (t.startsWith('notify') || t.startsWith('dingtalk') || t.startsWith('wxwork') || t.startsWith('feishu') || t.startsWith('email') || t.startsWith('sms')) return 'notify';
    return 'other';
  }

  async function runCurrent(input?: Record<string, any>) {
    if (!current.value?.id) {
      ElMessage.warning('请先保存工作流');
      return;
    }
    return workflowApi.run(current.value.id, input);
  }

  function bootstrapFromTemplate(templateId: string, templateName: string, workflowJson?: string) {
    const now = new Date().toISOString();

    // 尝试从 workflowJson 解析节点和连线
    if (workflowJson) {
      const parsed = parseWorkflowJson(workflowJson);
      if (!parsed.error && parsed.nodes.length > 0) {
        const nodeMap: Record<string, string> = {};
        const migratedNodes: WorkflowNode[] = parsed.nodes.map((n, idx) => {
          const newId = n.id || createNodeId();
          nodeMap[n.id || `node-${idx}`] = newId;
          // 兼容仅有 cx/cy 的归一化结构（来自 workflowGraph）
          const fallbackX = (idx * 300) + 120;
          const fallbackY = 200;
          const nx = typeof n.x === 'number'
            ? n.x
            : typeof n.cx === 'number'
              ? n.cx - 120
              : fallbackX;
          const ny = typeof n.y === 'number'
            ? n.y
            : typeof n.cy === 'number'
              ? n.cy - 30
              : fallbackY;
          return {
            id: newId,
            type: n.type || 'unknown',
            category: (n.category || guessCategory(n.type)) as import('@/types/workflow').NodeCategory,
            name: n.name || n.type || `节点 ${idx + 1}`,
            description: n.description,
            x: nx,
            y: ny,
            config: n.config ?? {},
            status: 'pending' as const,
          };
        });

        const migratedEdges: WorkflowEdge[] = parsed.edges
          .filter((e) => nodeMap[e.source] && nodeMap[e.target])
          .map((e) => ({
            id: e.id || createEdgeId(),
            source: nodeMap[e.source],
            target: nodeMap[e.target],
            label: e.label,
            edgeType: (e.edgeType as WorkflowEdge['edgeType']) ?? 'default',
            config: (e.config ?? {}) as Record<string, any>,
          }));

        current.value = {
          id: '',
          name: `${templateName} - 副本`,
          description: `由模板 ${templateName} 创建`,
          category: '其他',
          status: 'draft',
          version: 1,
          nodes: migratedNodes,
          edges: migratedEdges,
          createdBy: 'current-user',
          createdAt: now,
          updatedAt: now,
          stats: { executions: 0, successRate: 0, avgDuration: 0, totalCost: 0, todayExecutions: 0, todayCost: 0 },
        };
        pushHistory();
        markSaved();
        return;
      }
    }

    // 回退：创建默认 2 节点模板
    const startId = createNodeId();
    const llmId = createNodeId();
    current.value = {
      id: '',
      name: `${templateName} - 副本`,
      description: `由模板 ${templateName} 创建`,
      category: '其他',
      status: 'draft',
      version: 1,
      nodes: [
        { id: startId, type: 'trigger.webhook', category: 'trigger', name: 'Webhook 触发器', x: 120, y: 160, config: { method: 'POST', path: '/hooks/incoming' } },
        { id: llmId, type: 'ai.llm', category: 'ai', name: 'LLM 对话', x: 420, y: 160, config: { model: 'gpt-4o-mini', prompt: '请处理输入...' } },
      ],
      edges: [{ id: createEdgeId(), source: startId, target: llmId }],
      createdBy: 'current-user',
      createdAt: now,
      updatedAt: now,
      stats: { executions: 0, successRate: 0, avgDuration: 0, totalCost: 0, todayExecutions: 0, todayCost: 0 },
    };
    pushHistory();
    markSaved();
  }

  return {
    list,
    total,
    loading,
    current,
    nodes,
    edges,
    canUndo,
    canRedo,
    isDirty,
    fetchList,
    fetchOne,
    resetCurrent,
    ensureCurrent,
    addNode,
    addNodeAt,
    removeNode,
    moveNode,
    updateNodeConfig,
    updateNodeDescription,
    addEdge,
    removeEdge,
    updateEdge,
    setNodeStatus,
    resetNodeStatuses,
    saveCurrent,
    removeById,
    publishCurrent,
    runCurrent,
    bootstrapFromTemplate,
    createWorkflow,
    createFromTemplate,
    importDsl,
    undo,
    redo,
  };
});

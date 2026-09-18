/**
 * 模板预览：将工作流的 workflowJson / workflowGraph 归一化为
 * 前端 `WorkflowNode[] / WorkflowEdge[]`，供实际画布 (ReactFlow) 渲染。
 *
 * 关键点：
 *  1. 节点 category 在编辑器枚举之外（builtin 模板使用 `'transform' / 'end' / 'action'`），
 *     这里统一映射到合法 `NodeCategory`，驱动 ReactFlow 节点颜色 / 图标。
 *  2. 兼容只含 cx/cy 的 workflowGraph 轻量结构以及完整 n.x/n.y 形式。
 *  3. 兼容 `description / config / status` 等字段透传，画布组件会读取渲染。
 *  4. 边：解析 `edgeType / label`，并按 ReactFlow 期望的 `default | condition | loop | error`
 *     归一化；其它值落在 `default`。
 */

import type { ParsedWorkflowContent } from '@/api/template';
import type {
  WorkflowNode,
  WorkflowEdge,
  NodeCategory,
} from '@/types/workflow';

const VALID_CATEGORIES: NodeCategory[] = ['trigger', 'ai', 'flow', 'tool', 'notify'];

const CATEGORY_FALLBACK: Record<string, NodeCategory> = {
  // 内置模板用的精简分类 → 编辑器分类
  transform: 'flow',
  end: 'flow',
  action: 'tool',
  data: 'tool',
  control: 'flow',
};

function normalizeCategory(raw?: string, nodeType?: string): NodeCategory {
  if (raw && (VALID_CATEGORIES as string[]).includes(raw)) {
    return raw as NodeCategory;
  }
  if (raw && CATEGORY_FALLBACK[raw]) {
    return CATEGORY_FALLBACK[raw];
  }
  // 从 type 推断
  if (!nodeType) return 'flow';
  if (nodeType.startsWith('trigger')) return 'trigger';
  if (nodeType.startsWith('ai.') || nodeType === 'knowledge.search') return 'ai';
  if (
    nodeType.startsWith('condition') ||
    nodeType.startsWith('flow') ||
    nodeType.startsWith('code') ||
    nodeType.startsWith('transform') ||
    nodeType.startsWith('end')
  ) {
    return 'flow';
  }
  if (
    nodeType.startsWith('http') ||
    nodeType.startsWith('tool') ||
    nodeType.startsWith('db')
  ) {
    return 'tool';
  }
  if (nodeType.startsWith('notify')) return 'notify';
  return 'flow';
}

const VALID_EDGE_TYPES = new Set(['default', 'condition', 'loop', 'error']);

function normalizeEdgeType(raw?: string): WorkflowEdge['edgeType'] {
  if (!raw) return 'default';
  return VALID_EDGE_TYPES.has(raw) ? (raw as WorkflowEdge['edgeType']) : 'default';
}

/**
 * 把 parseWorkflowJson（/Graph）的输出转为编辑器画布可消费的形状。
 * 当内容无效时，返回 `null` —— 调用方决定是否降级到 fallback 示例。
 */
export function toCanvasContent(parsed: ParsedWorkflowContent | null | undefined): {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
} | null {
  if (!parsed || parsed.error) return null;
  if (!parsed.nodes?.length) return null;

  const nodes: WorkflowNode[] = parsed.nodes.map((n) => {
    const x =
      typeof n.x === 'number'
        ? n.x
        : typeof (n as any).cx === 'number'
          ? ((n as any).cx as number) - (typeof n.width === 'number' ? n.width : 240) / 2
          : 0;
    const y =
      typeof n.y === 'number'
        ? n.y
        : typeof (n as any).cy === 'number'
          ? ((n as any).cy as number) - (typeof n.height === 'number' ? n.height : 60) / 2
          : 0;
    const cfg = (n as any).config;
    const node: WorkflowNode = {
      id: n.id,
      type: n.type || 'unknown',
      category: normalizeCategory(n.category, n.type),
      name: n.name || n.type || n.id,
      description: n.description,
      x,
      y,
      config: cfg && typeof cfg === 'object' ? cfg : {},
      status: 'pending',
    };
    return node;
  });

  const edges: WorkflowEdge[] = (parsed.edges || []).map((e) => ({
    id: e.id,
    source: e.source,
    target: e.target,
    label: e.label,
    edgeType: normalizeEdgeType((e as any).edgeType),
  }));

  return { nodes, edges };
}

// =============================================================================
//  fallback：演示用工作流，模拟一个真实场景：IM 工单自动分流
// =============================================================================

/**
 * 一个有设计感的"中后台 AI 分流"演示工作流。
 * 用在模板缺 workflowJson 时，给用户看到一个有意义的画布预览。
 */
export function getFallbackDemoWorkflow(): { nodes: WorkflowNode[]; edges: WorkflowEdge[] } {
  const NODE_W = 240;
  const NODE_H = 72;
  const DX = 320;
  const DY = 160;

  const nodes: WorkflowNode[] = [
    {
      id: 'fb-trigger',
      type: 'trigger.webhook',
      category: 'trigger',
      name: '接收 IM 工单',
      description: '监听企业内部 IM 工具的消息事件，作为流程入口',
      x: 40, y: 160,
      status: 'pending',
      config: {
        method: 'POST',
        path: '/hooks/im/ticket-incoming',
        authToken: '',
        timeout: 8000,
      },
    },
    {
      id: 'fb-classify',
      type: 'ai.classify',
      category: 'ai',
      name: 'AI 意图识别',
      description: '基于 LLM 把工单分类为账单 / 技术 / 投诉 / 其他',
      x: 40 + DX, y: 160,
      status: 'running',
      config: {
        model: 'gpt-4o-mini',
        temperature: 0.2,
        categories: '账单,技术,投诉,其他',
        input: '{{trigger.body.text}}',
        outputKey: 'category',
      },
    },
    {
      id: 'fb-switch',
      type: 'flow.if-else',
      category: 'flow',
      name: '按分类路由',
      description: '根据分类选择下游不同的处理路径',
      x: 40 + DX * 2, y: 160,
      status: 'pending',
      config: {
        cases: '[]',
      },
    },
    {
      id: 'fb-kb',
      type: 'ai.rag',
      category: 'ai',
      name: '知识库检索',
      description: '基于账单 FAQ 知识库匹配最佳答案',
      x: 40 + DX * 3, y: -DY + 160,
      status: 'pending',
      config: {
        knowledgeBaseId: 'kb_billing_faq',
        topK: 3,
        scoreThreshold: 0.65,
      },
    },
    {
      id: 'fb-http-create',
      type: 'tool.http',
      category: 'tool',
      name: '创建工单',
      description: '调用工单系统 API 创建一张处理单',
      x: 40 + DX * 3, y: 0 + 160,
      status: 'pending',
      config: {
        method: 'POST',
        url: 'https://ticket.internal/api/ticket/create',
        body: '{}',
        timeout: 8000,
      },
    },
    {
      id: 'fb-http-assign',
      type: 'tool.http',
      category: 'tool',
      name: '升级人工',
      description: '将投诉类型工单升级到值班人工坐席',
      x: 40 + DX * 3, y: DY + 160,
      status: 'pending',
      config: {
        method: 'POST',
        url: 'https://ticket.internal/api/ticket/escalate',
        body: '{}',
      },
    },
    {
      id: 'fb-merge',
      type: 'flow.aggregation',
      category: 'flow',
      name: '合并多条结果',
      description: '把 FAQ / 创建工单 / 升级人工 三路结果合并为统一响应',
      x: 40 + DX * 4, y: 160,
      status: 'pending',
      config: {
        strategy: 'concat',
        separator: '\n\n',
      },
    },
    {
      id: 'fb-notify',
      type: 'notify.feishu',
      category: 'notify',
      name: '飞书通知坐席',
      description: '把处理结果同步给值班 IM 群',
      x: 40 + DX * 5, y: 160,
      status: 'pending',
      config: {
        webhook: 'https://open.feishu.cn/hook/xxx',
        msgType: 'text',
        atAll: false,
      },
    },
    {
      id: 'fb-end',
      type: 'end.return',
      category: 'flow',
      name: '返回响应',
      description: '返回最终响应',
      x: 40 + DX * 6, y: 160,
      status: 'pending',
      config: {
        outputMode: 'return_last',
      },
    },
  ];

  const edges: WorkflowEdge[] = [
    { id: 'e1', source: 'fb-trigger',   target: 'fb-classify' },
    { id: 'e2', source: 'fb-classify',  target: 'fb-switch' },
    { id: 'e3', source: 'fb-switch',    target: 'fb-kb',           label: '账单', edgeType: 'condition' },
    { id: 'e4', source: 'fb-switch',    target: 'fb-http-create',  label: '其他', edgeType: 'condition' },
    { id: 'e5', source: 'fb-switch',    target: 'fb-http-assign',  label: '投诉', edgeType: 'condition' },
    { id: 'e6', source: 'fb-kb',        target: 'fb-merge' },
    { id: 'e7', source: 'fb-http-create', target: 'fb-merge' },
    { id: 'e8', source: 'fb-http-assign', target: 'fb-merge' },
    { id: 'e9', source: 'fb-merge',     target: 'fb-notify' },
    { id: 'e10', source: 'fb-notify',   target: 'fb-end' },
  ];

  // 备注宽度只是一份语义配置，不影响渲染位置
  void NODE_W; void NODE_H;
  return { nodes, edges };
}

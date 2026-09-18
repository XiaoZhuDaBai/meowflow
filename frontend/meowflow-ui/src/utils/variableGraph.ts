import type { VariableGroup } from '@/components/workflow/config/VariablePicker.vue';
import type { Workflow, WorkflowNode } from '@/types/workflow';

/**
 * 根据工作流当前节点上游产出,计算可用于 VariablePicker 的变量组。
 * - 触发器: 始终展示
 * - 上游节点: 按连线拓扑先后排序
 * - 当前节点本身及其下游不再展示
 */
export function buildVariableGroups(
  workflow: Workflow | null,
  currentNodeId: string | null,
  currentWorkflow?: Workflow | null,
): VariableGroup[] {
  if (!workflow) return [];
  const wf = currentWorkflow ?? workflow;
  const groups: VariableGroup[] = [];
  const seen = new Set<string>();

  function tryAdd(
    category: string,
    label: string,
    icon: string,
    node: WorkflowNode,
    variables: { path: string; description: string }[],
  ) {
    const list = variables.filter((v) => !seen.has(`${node.id}.${v.path}`));
    if (!list.length) return;
    list.forEach((v) => seen.add(`${node.id}.${v.path}`));
    groups.push({
      category,
      label: `${label} · ${node.name}`,
      icon,
      variables: list.map((v) => ({
        path: `${node.id}.${v.path}`,
        description: v.description,
      })),
    });
  }

  function upstream(startId: string): WorkflowNode[] {
    const result: WorkflowNode[] = [];
    const visited = new Set<string>();
    const queue = [startId];
    while (queue.length) {
      const id = queue.shift()!;
      wf.edges
        .filter((e) => e.target === id)
        .forEach((e) => {
          if (visited.has(e.source)) return;
          visited.add(e.source);
          const n = wf.nodes.find((nn) => nn.id === e.source);
          if (n) result.push(n);
          queue.push(e.source);
        });
    }
    return result;
  }

  // 1. 触发器: 所有触发器节点都可见
  for (const n of wf.nodes) {
    if (n.category !== 'trigger') continue;
    if (n.id === currentNodeId) continue;
    tryAdd('trigger', '触发器', 'fa-bolt', n, defaultTriggerVars(n));
  }

  // 2. 当前节点的上游
  if (currentNodeId) {
    const ups = upstream(currentNodeId).filter((n) => n.category !== 'trigger');
    ups.forEach((n) => {
      const cat = n.category;
      const meta = NODE_CATEGORY_LABEL[cat] ?? { label: cat, icon: 'fa-circle' };
      tryAdd(cat, meta.label, meta.icon, n, defaultOutputVars(n));
    });
  }

  return groups;
}

const NODE_CATEGORY_LABEL: Record<string, { label: string; icon: string }> = {
  trigger: { label: '触发器', icon: 'fa-bolt' },
  ai: { label: 'AI', icon: 'fa-brain' },
  flow: { label: '流程', icon: 'fa-diagram-project' },
  tool: { label: '工具', icon: 'fa-wrench' },
  notify: { label: '通知', icon: 'fa-paper-plane' },
};

function defaultTriggerVars(n: WorkflowNode) {
  switch (n.type) {
    case 'trigger.webhook':
      return [
        { path: 'body', description: '请求体' },
        { path: 'headers', description: '请求头' },
        { path: 'query', description: '查询参数' },
        { path: 'method', description: 'HTTP 方法' },
      ];
    case 'trigger.cron':
      return [
        { path: 'time', description: '执行时间' },
        { path: 'timezone', description: '时区' },
      ];
    case 'trigger.form':
      return [
        { path: 'fields', description: '表单字段' },
      ];
    case 'trigger.imessage':
      return [
        { path: 'text', description: '消息文本' },
        { path: 'sender', description: '发送者' },
      ];
    default:
      return [{ path: 'output', description: '触发数据' }];
  }
}

function defaultOutputVars(n: WorkflowNode) {
  switch (n.type) {
    case 'ai.llm':
      return [{ path: 'text', description: '生成的文本' }];
    case 'ai.classify':
      return [
        { path: 'category', description: '命中的分类' },
        { path: 'confidence', description: '置信度' },
      ];
    case 'ai.extract':
      return [{ path: 'data', description: '提取的数据' }];
    case 'ai.summarize':
      return [{ path: 'summary', description: '摘要内容' }];
    case 'ai.rag':
      return [
        { path: 'docs', description: '检索到的文档' },
        { path: 'answer', description: '回答' },
      ];
    case 'flow.condition':
      return [
        { path: 'matched', description: '是否命中' },
        { path: 'branch', description: '命中的分支' },
      ];
    case 'flow.loop':
      return [{ path: 'item', description: '当前迭代项' }];
    case 'flow.parallel':
      return [{ path: 'results', description: '并行结果' }];
    case 'tool.http':
      return [
        { path: 'status', description: 'HTTP 状态码' },
        { path: 'body', description: '响应体' },
        { path: 'headers', description: '响应头' },
      ];
    case 'tool.db':
      return [{ path: 'rows', description: '查询结果' }];
    case 'tool.code':
      return [{ path: 'result', description: '代码执行结果' }];
    case 'tool.assign':
      return []; // 用户自定义变量名
    default:
      return [{ path: 'output', description: '节点输出' }];
  }
}

/**
 * 把变量字符串中的 `{{nodeId.path}}` 替换为 `{{nodeName.path}}`，
 * 在 UI 文本里展示更友好。
 */
export function prettifyVariablePath(
  text: string,
  workflow: Workflow | null,
): string {
  if (!workflow) return text;
  return text.replace(/\{\{([^}]+)\}\}/g, (_, expr: string) => {
    const [id, ...rest] = expr.split('.');
    const n = workflow.nodes.find((nn) => nn.id === id);
    return `{{${n ? n.name : id}.${rest.join('.')}}}`;
  });
}

/**
 * Variable inspector helper - 从工作流推导上下游节点提供的变量
 */
import type { WorkflowNode, WorkflowEdge } from '@/types/workflow';
import type { Variable, VariableProvider } from '@/types/variable';
import { findNodeDefinition } from '@/mock/nodes';

/**
 * 获取节点作为上游（输出）提供的变量
 */
export function getProviderOutputs(node: WorkflowNode): Variable[] {
  const def = findNodeDefinition(node.type);
  if (!def?.outputs) return defaultOutputsFor(node);
  return def.outputs.map((o) => ({
    name: o.key,
    type: o.type,
    description: o.description,
  }));
}

/**
 * 获取节点作为下游（输入）需要的变量
 */
export function getConsumerInputs(node: WorkflowNode): Variable[] {
  const def = findNodeDefinition(node.type);
  if (!def?.inputs) return [];
  return def.inputs.map((i) => ({
    name: i.key,
    type: i.type,
    required: i.required,
    description: i.description,
  }));
}

function defaultOutputsFor(node: WorkflowNode): Variable[] {
  return [{ name: 'output', type: 'any', description: '节点输出' }];
}

/**
 * 推导所有前置节点（通过边）提供的变量
 */
export function getUpstreamProviders(
  nodes: WorkflowNode[],
  edges: WorkflowEdge[],
  targetId: string,
): VariableProvider[] {
  const nodeMap = new Map(nodes.map((n) => [n.id, n]));
  const visited = new Set<string>();
  const result: VariableProvider[] = [];

  function walk(id: string) {
    if (visited.has(id)) return;
    visited.add(id);

    const providers = edges
      .filter((e) => e.target === id)
      .map((e) => e.source);

    for (const pid of providers) {
      const provider = nodeMap.get(pid);
      if (!provider) continue;
      result.push({
        nodeId: provider.id,
        nodeName: provider.name,
        category: provider.category,
        variables: getProviderOutputs(provider),
      });
      walk(pid);
    }
  }

  walk(targetId);
  return result;
}

/**
 * 引用变量 - 生成 {{nodeName.varName}} 格式
 */
export function buildVariableReference(providerNodeName: string, varName: string): string {
  return `{{${providerNodeName}.${varName}}}`;
}

/**
 * 解析变量引用 - 从 "{{nodeName.varName}}" 中提取
 */
export function parseVariableReference(ref: string): { nodeName: string; varName: string } | null {
  const match = ref.match(/^\{\{([^.]+)\.([^}]+)\}\}$/);
  if (!match) return null;
  return { nodeName: match[1], varName: match[2] };
}

/**
 * 检查值是否为变量引用
 */
export function isVariableReference(value: any): boolean {
  return typeof value === 'string' && /^\{\{[^.]+\.[^}]+\}\}$/.test(value);
}

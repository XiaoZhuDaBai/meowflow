/**
 * Connection validation - 检查两个节点之间能否连接
 */
import type { WorkflowNode, WorkflowEdge, NodeCategory } from '@/types/workflow';

export interface ConnectionValidationResult {
  isValid: boolean;
  reason?: string;
}

export function validateConnection(
  source: WorkflowNode,
  target: WorkflowNode,
  allNodes: WorkflowNode[],
  allEdges: WorkflowEdge[],
): ConnectionValidationResult {
  // 1. 不能连接自己
  if (source.id === target.id) {
    return { isValid: false, reason: '不能连接节点到自己' };
  }

  // 2. 不能连接到触发器 (触发器是起点)
  if (target.category === 'trigger') {
    return { isValid: false, reason: '触发器节点不能作为目标' };
  }

  // 3. 不能从通知出发 (通知是终点)
  if (source.category === 'notify') {
    return { isValid: false, reason: '通知节点不能作为起始' };
  }

  // 4. 不能重复连接
  if (allEdges.some((e) => e.source === source.id && e.target === target.id)) {
    return { isValid: false, reason: '已经存在该连线' };
  }

  // 5. 不能有循环
  if (wouldCreateCycle(source.id, target.id, allEdges)) {
    return { isValid: false, reason: '不能创建循环' };
  }

  return { isValid: true };
}

function wouldCreateCycle(source: string, target: string, edges: WorkflowEdge[]): boolean {
  const adj = new Map<string, string[]>();
  for (const e of edges) {
    if (!adj.has(e.source)) adj.set(e.source, []);
    adj.get(e.source)!.push(e.target);
  }
  // 从 target 做 BFS，看能否到达 source
  const visited = new Set<string>();
  const queue = [target];
  while (queue.length > 0) {
    const current = queue.shift()!;
    if (current === source) return true;
    if (visited.has(current)) continue;
    visited.add(current);
    const next = adj.get(current) || [];
    queue.push(...next);
  }
  return false;
}

/**
 * 获取画布边类型的默认颜色
 */
export function getEdgeTypeColor(type: 'default' | 'condition' | 'loop' | 'error'): string {
  switch (type) {
    case 'condition':
      return '#f59e0b';
    case 'loop':
      return '#8b5cf6';
    case 'error':
      return '#ef4444';
    default:
      return '#94a3b8';
  }
}

/**
 * 自动推断连接类型 - 根据节点类型启发式
 */
export function inferEdgeType(
  source: WorkflowNode,
  target: WorkflowNode,
): 'default' | 'condition' | 'loop' | 'error' {
  if (source.category === 'flow' && source.type === 'flow.condition') {
    return 'condition';
  }
  if (source.category === 'flow' && (source.type === 'flow.loop' || source.type === 'flow.parallel')) {
    return 'loop';
  }
  return 'default';
}

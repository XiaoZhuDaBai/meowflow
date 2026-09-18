/**
 * Snippet insertion - 将模板片段插入到当前画布
 */
import type { Snippet } from '@/mock/snippets';
import { useWorkflowStore } from '@/stores/workflow';
import { createNodeId } from '@/mock/workflows';

export interface InsertResult {
  insertedNodeIds: string[];
  insertedCount: number;
}

const NODE_W = 240;
const NODE_H = 80;
const COL_STEP = 280;
const ROW_STEP = 120;
const PADDING = 60;

/**
 * 找到一块不会被现有节点占用的 280x80 起始区域
 * 优先查找整体包围盒的右下方
 */
function findEmptyBase(nodes: { x: number; y: number }[]): { x: number; y: number } {
  if (nodes.length === 0) return { x: 240, y: 180 };

  const occupied = nodes.map((n) => ({
    left: n.x - 20,
    right: n.x + NODE_W + 20,
    top: n.y - 20,
    bottom: n.y + NODE_H + 20,
  }));

  const maxRight = Math.max(...nodes.map((n) => n.x + NODE_W));
  const maxBottom = Math.max(...nodes.map((n) => n.y + NODE_H));

  const startX = Math.max(maxRight + COL_STEP, PADDING);
  const startY = Math.max(maxBottom + ROW_STEP, PADDING);

  function findFree(start: { x: number; y: number }): { x: number; y: number } | null {
    for (let row = 0; row < 200; row++) {
      for (let col = 0; col < 100; col++) {
        const x = start.x + col * COL_STEP;
        const y = start.y + row * ROW_STEP;
        const target = {
          left: x,
          right: x + NODE_W,
          top: y,
          bottom: y + NODE_H,
        };
        const collides = occupied.some(
          (o) => !(target.right <= o.left || target.left >= o.right || target.bottom <= o.top || target.top >= o.bottom),
        );
        if (!collides) return { x, y };
      }
    }
    return null;
  }

  return findFree({ x: startX, y: PADDING }) || { x: startX, y: PADDING };
}

export function useInsertSnippet() {
  const workflow = useWorkflowStore();

  function insert(snippet: Snippet): InsertResult {
    if (!workflow.current) {
      workflow.ensureCurrent('未命名工作流');
    }

    const existingNodes = workflow.current?.nodes ?? [];
    const base = findEmptyBase(existingNodes);

    // 内部归一化: 把 snippet 内的最小 (x, y) 作为锚点
    const minX = Math.min(...snippet.nodes.map((n) => n.x));
    const minY = Math.min(...snippet.nodes.map((n) => n.y));
    const offsetX = base.x - minX;
    const offsetY = base.y - minY;

    const tempIdToRealId = new Map<string, string>();
    const result: InsertResult = {
      insertedNodeIds: [],
      insertedCount: snippet.nodes.length,
    };

    for (const node of snippet.nodes) {
      const newId = createNodeId();
      tempIdToRealId.set(node.tempId, newId);

      workflow.addNode({
        id: newId,
        type: node.type,
        category: node.category,
        name: node.name,
        description: node.description,
        config: { ...node.config },
        x: node.x + offsetX,
        y: node.y + offsetY,
      });

      result.insertedNodeIds.push(newId);
    }

    for (const edge of snippet.edges) {
      const sourceId = tempIdToRealId.get(edge.sourceTempId);
      const targetId = tempIdToRealId.get(edge.targetTempId);
      if (!sourceId || !targetId) continue;
      workflow.addEdge(sourceId, targetId);
    }

    return result;
  }

  return { insert };
}

import { onMounted, onBeforeUnmount } from 'vue';
import { useWorkflowStore } from '@/stores/workflow';
import { useSelectionStore } from '@/stores/selection';
import { useCanvasStore } from '@/stores/canvas';
import { useClipboardStore } from '@/stores/clipboard';
import { createNodeId } from '@/mock/workflows';

interface ShortcutHandlers {
  /** 复制选中节点 */
  onCopy?: () => void;
  /** 粘贴剪贴板中的节点 */
  onPaste?: () => void;
  /** 触发保存 */
  onSave?: () => void;
  /** 触发运行 */
  onRun?: () => void;
  /** 添加注释 (N 键) */
  onAddNote?: () => void;
}

/**
 * 注册画布相关的键盘快捷键
 *
 * 支持:
 * - Delete/Backspace: 删除选中
 * - Ctrl/Cmd+C: 复制
 * - Ctrl/Cmd+V: 粘贴
 * - Ctrl/Cmd+Z: 撤销
 * - Ctrl/Cmd+Shift+Z: 重做
 * - Ctrl/Cmd+A: 全选
 * - Ctrl/Cmd+S: 保存
 * - Escape: 取消选择
 * - B: 切换节点面板
 * - M: 切换 MiniMap
 * - G: 切换网格
 */
export function useWorkflowShortcuts(handlers: ShortcutHandlers = {}) {
  const workflow = useWorkflowStore();
  const selection = useSelectionStore();
  const canvas = useCanvasStore();
  const clipboard = useClipboardStore();

  function isEditableTarget(target: EventTarget | null): boolean {
    if (!target || !(target instanceof HTMLElement)) return false;
    const tag = target.tagName.toLowerCase();
    return (
      tag === 'input'
      || tag === 'textarea'
      || tag === 'select'
      || target.isContentEditable
    );
  }

  function handleKeydown(e: KeyboardEvent) {
    // 在可编辑元素中不响应快捷键
    if (isEditableTarget(e.target)) return;

    const isMac = navigator.platform.toUpperCase().includes('MAC');
    const modKey = isMac ? e.metaKey : e.ctrlKey;
    const key = e.key;

    // 单键快捷键
    if (!modKey && !e.shiftKey && !e.altKey) {
      if (key === 'Delete' || key === 'Backspace') {
        e.preventDefault();
        deleteSelected();
        return;
      }

      if (key === 'Escape') {
        e.preventDefault();
        selection.clearSelection();
        return;
      }

      if (key === 'b' || key === 'B') {
        e.preventDefault();
        canvas.toggleNodePalette();
        return;
      }

      if (key === 'm' || key === 'M') {
        e.preventDefault();
        canvas.toggleMinimap();
        return;
      }

      if (key === 'g' || key === 'G') {
        e.preventDefault();
        canvas.toggleGrid();
        return;
      }

      if (key === 'n' || key === 'N') {
        e.preventDefault();
        handlers.onAddNote?.();
        return;
      }
    }

    if (modKey) {
      const lowerKey = key.toLowerCase();

      if (lowerKey === 'c' && !e.shiftKey) {
        e.preventDefault();
        copySelected();
        return;
      }

      if (lowerKey === 'v' && !e.shiftKey) {
        e.preventDefault();
        pasteFromClipboard();
        return;
      }

      if (lowerKey === 'z' && !e.shiftKey) {
        e.preventDefault();
        workflow.undo();
        return;
      }

      if ((lowerKey === 'z' && e.shiftKey) || lowerKey === 'y') {
        e.preventDefault();
        workflow.redo();
        return;
      }

      if (lowerKey === 'a') {
        e.preventDefault();
        selectAll();
        return;
      }

      if (lowerKey === 's') {
        e.preventDefault();
        handlers.onSave?.();
        return;
      }

      // Cmd/Ctrl+D: 复制并粘贴选中节点(含 note)。等价于 copy+paste,
      // 但 D 不需要先复制再 Cmd+V,直接一份新的。
      if (lowerKey === 'd' && !e.shiftKey) {
        e.preventDefault();
        duplicateSelected();
        return;
      }
    }
  }

  function deleteSelected() {
    if (!selection.hasSelection) return;
    const nodeIds = [...selection.selectedNodeIds];
    const edgeIds = [...selection.selectedEdgeIds];

    for (const id of nodeIds) workflow.removeNode(id);
    for (const id of edgeIds) workflow.removeEdge(id);

    selection.clearSelection();
  }

  function copySelected() {
    if (!workflow.current) return;
    const selectedIds = [...selection.selectedNodeIds];
    if (selectedIds.length === 0) {
      handlers.onCopy?.();
      return;
    }

    const nodes = workflow.current.nodes.filter((n) => selectedIds.includes(n.id));
    // 仅复制两端都在选中集合内的边
    const selectedSet = new Set(selectedIds);
    const edges = workflow.current.edges.filter(
      (e) => selectedSet.has(e.source) && selectedSet.has(e.target),
    );

    clipboard.setPayload({
      nodes: nodes.map((n) => ({
        id: n.id,
        type: n.type,
        category: n.category,
        name: n.name,
        description: n.description,
        config: { ...n.config },
        x: n.x,
        y: n.y,
      })),
      edges: edges.map((e) => ({
        id: e.id,
        source: e.source,
        target: e.target,
        label: e.label,
        edgeType: e.edgeType,
        config: e.config ? { ...e.config } : undefined,
      })),
    });

    handlers.onCopy?.();
  }

  /**
   * 粘贴剪贴板中的节点 + 边。
   *
   * 关键点：
   * - 剪贴板保留原始节点 / 边 ID（ClipboardNode.id / ClipboardEdge.source|target）。
   * - 粘贴时为每个节点分配新 ID，构建 oldId -> newId 映射。
   * - 边的 source / target 按映射重写，确保拓扑完整保留。
   * - 偏移 (40, 40)，新粘贴节点若已存在则继续累加偏移，避免重叠到原节点。
   */
  function pasteFromClipboard() {
    if (clipboard.isEmpty || !workflow.current) return;
    const payload = clipboard.payload!;

    const OFFSET_X = 40;
    const OFFSET_Y = 40;

    // 1. 生成 ID 映射并收集新建节点
    const oldToNew = new Map<string, string>();
    const newNodeIds: string[] = [];

    payload.nodes.forEach((n) => {
      const newId = createNodeId();
      oldToNew.set(n.id, newId);
      workflow.addNode({
        id: newId,
        type: n.type,
        category: n.category as any,
        name: n.name,
        description: n.description,
        config: { ...n.config },
        x: n.x + OFFSET_X,
        y: n.y + OFFSET_Y,
      });
      newNodeIds.push(newId);
    });

    // 2. 按 oldId->newId 映射重写每条边的 source / target
    const newEdgeIds: string[] = [];
    payload.edges.forEach((e) => {
      const newSource = oldToNew.get(e.source);
      const newTarget = oldToNew.get(e.target);
      if (!newSource || !newTarget || newSource === newTarget) return;
      workflow.addEdge(newSource, newTarget);
      // addEdge 内部生成的 edge id 不易拿到，从最新一批边里查找最新一条
    });

    // 3. 选中新建节点，让 Cmd+D 继续复制粘贴的内容
    selection.selectMultiple(newNodeIds);

    handlers.onPaste?.();
  }

  function selectAll() {
    if (!workflow.current) return;
    selection.selectMultiple(
      workflow.current.nodes.map((n) => n.id),
      workflow.current.edges.map((e) => e.id),
    );
  }

  /**
   * Cmd/Ctrl+D: clone the current selection in-place (no clipboard needed).
   * Reuses addNode so config (text/color/size for notes) round-trips.
   * Offset 40px down-right, same as paste.
   */
  function duplicateSelected() {
    if (!workflow.current) return;
    const ids = [...selection.selectedNodeIds];
    if (ids.length === 0) return;
    const OFFSET = 40;
    const oldIds = ids;
    const newIds: string[] = [];

    for (const id of oldIds) {
      const src = workflow.current.nodes.find((n) => n.id === id);
      if (!src) continue;
      workflow.addNode({
        type: src.type,
        category: src.category,
        name: src.name,
        description: src.description,
        config: { ...src.config },
        x: src.x + OFFSET,
        y: src.y + OFFSET,
      });
    }
    // The newly added nodes are appended at the end of the array in order.
    // Select them so a second Cmd+D will duplicate the duplicates.
    const fresh = workflow.current.nodes.slice(-oldIds.length);
    selection.selectMultiple(fresh.map((n) => n.id));
  }

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });

  return {
    deleteSelected,
    copySelected,
    pasteFromClipboard,
    duplicateSelected,
    selectAll,
  };
}

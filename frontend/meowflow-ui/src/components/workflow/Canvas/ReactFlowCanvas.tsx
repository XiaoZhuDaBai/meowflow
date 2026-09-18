import React, { useCallback, useEffect, useMemo, memo, useRef } from 'react';
import {
  BaseEdge,
  EdgeLabelRenderer,
  getBezierPath,
  Handle,
  MarkerType,
  Position,
  ReactFlow,
  Background,
  MiniMap,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
  type Connection,
  type Edge,
  type Node,
  type NodeDragHandler,
  type NodeMouseHandler,
  type OnNodesChange,
  type OnSelectionChangeParams,
} from 'reactflow';
import 'reactflow/dist/style.css';

import type { WorkflowNode, WorkflowEdge } from '@/types/workflow';
import NoteNode from './NoteNode';

const NODE_W = 240;
const NODE_H = 72;

export type RFNodeData = WorkflowNode & {
  __debugMode?: boolean;
  __breakpoint?: boolean;
  __pausedHere?: boolean;
  __onToggleBreakpoint?: (nodeId: string) => void;
};

type DebugNodeOptions = {
  debugMode?: boolean;
  breakpointNodeIds?: string[];
  pausedAtNodeId?: string;
  onToggleBreakpoint?: (nodeId: string) => void;
};

function toRFNode(n: WorkflowNode, debug?: DebugNodeOptions): Node<RFNodeData> {
  return {
    id: n.id,
    // Notes use the dedicated NoteNode component; everything else uses the
    // generic workflow node renderer.
    type: n.type === 'note' ? 'note' : 'workflow',
    position: { x: n.x, y: n.y },
    data: {
      ...n,
      __debugMode: Boolean(debug?.debugMode),
      __breakpoint: Boolean(debug?.breakpointNodeIds?.includes(n.id)),
      __pausedHere: debug?.pausedAtNodeId === n.id,
      __onToggleBreakpoint: debug?.onToggleBreakpoint,
    },
  };
}

function toRFEdge(e: WorkflowEdge): Edge {
  const edgeType = e.edgeType || 'default';
  const stroke = edgeType === 'condition'
    ? '#f59e0b'
    : edgeType === 'loop'
      ? '#8b5cf6'
      : edgeType === 'error'
        ? '#ef4444'
        : '#94a3b8';
  return {
    id: e.id,
    source: e.source,
    target: e.target,
    type: 'custom',
    label: e.label,
    data: {
      edgeType,
      label: e.label,
      config: e.config,
    },
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 10,
      height: 10,
      color: stroke,
    },
  };
}

// Custom edge: bezier between the actual handle positions. Used to be
// forced horizontal (sourceRight → targetLeft) which only works when the
// user can't drag from top/bottom handles. Now we honor whatever
// `sourcePosition` / `targetPosition` React Flow passes — that's also
// where the connection line will visually start/end.
function CustomEdge({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  selected,
  label,
  data,
}: any) {
  const edgeType = data?.edgeType || 'default';
  const typeColor = edgeType === 'condition'
    ? '#f59e0b'
    : edgeType === 'loop'
      ? '#8b5cf6'
      : edgeType === 'error'
        ? '#ef4444'
        : '#94a3b8';

  const [path, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    // If you set sourcePosition/targetPosition to anything other than
    // Left/Right the bezier control points adapt automatically.
    sourcePosition: sourcePosition ?? Position.Right,
    targetX,
    targetY,
    targetPosition: targetPosition ?? Position.Left,
    curvature: 0.25,
  });

  const stroke = selected ? '#6366f1' : typeColor;
  const isDashed = edgeType === 'condition';

  return (
    <>
      <BaseEdge
        id={id}
        path={path}
        style={{
          stroke,
          strokeWidth: selected ? 2.5 : 2,
          strokeDasharray: isDashed ? '6 4' : undefined,
        }}
      />
      <EdgeLabelRendererAny>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
            pointerEvents: 'all',
            background: selected ? '#eef2ff' : '#fff',
            border: `1px solid ${selected ? '#6366f1' : typeColor}`,
            color: selected ? '#6366f1' : typeColor,
            borderRadius: 4,
            padding: '2px 6px',
            fontSize: 11,
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            gap: 4,
          }}
          className="nodrag nopan"
        >
          {edgeType !== 'default' && (
            <i
              className={
                edgeType === 'condition'
                  ? 'fa-solid fa-code-branch'
                  : edgeType === 'loop'
                    ? 'fa-solid fa-rotate'
                    : 'fa-solid fa-circle-exclamation'
              }
              style={{ fontSize: 10 }}
            />
          )}
          {label}
        </div>
      </EdgeLabelRendererAny>
    </>
  );
}

// Custom Connection Line - shows while user is dragging a new connection
function CustomConnectionLine({
  fromX,
  fromY,
  toX,
  toY,
  fromPosition,
  toPosition,
}: any) {
  // React Flow passes the live cursor position as `toX/toY` and the chosen
  // handle position as `fromPosition`. Drawing a real bezier (instead of
  // a hardcoded horizontal stub) is what makes the preview line actually
  // visible and tracks the cursor from any side of the source node.
  const [path] = getBezierPath({
    sourceX: fromX,
    sourceY: fromY,
    sourcePosition: fromPosition ?? Position.Right,
    targetX: toX,
    targetY: toY,
    targetPosition: toPosition ?? Position.Left,
    curvature: 0.25,
  });

  return (
    <g>
      <path
        d={path}
        fill="none"
        stroke="#6366f1"
        strokeWidth={2}
        strokeDasharray="4 4"
        className="react-flow__connection-path"
      />
    </g>
  );
}

// Node component
const WorkflowNodeComp = memo(({ data, selected }: { data: RFNodeData; selected: boolean }) => {
  const [hovered, setHovered] = React.useState(false);
  const STATUS_COLORS: Record<string, string> = {
    running: '#f59e0b',
    success: '#10b981',
    failed: '#ef4444',
    pending: '#94a3b8',
    skipped: '#64748b',
  };
  const STATUS_LABELS: Record<string, string> = {
    running: '运行中',
    success: '成功',
    failed: '失败',
    pending: '等待',
    skipped: '已跳过',
  };
  const CATEGORY_COLORS: Record<string, string> = {
    trigger: '#8b5cf6',
    ai: '#3b82f6',
    flow: '#10b981',
    tool: '#f59e0b',
    notify: '#ec4899',
  };

  // 节点 type → FontAwesome 图标（精简版，与 mock/nodes.ts 中 NODE_CATALOG 镜像）
  const TYPE_ICONS: Record<string, string> = {
    'trigger.webhook': 'fa-bolt',
    'trigger.cron': 'fa-clock',
    'trigger.form': 'fa-rectangle-list',
    'trigger.imessage': 'fa-comment',
    'ai.llm': 'fa-brain',
    'ai.classify': 'fa-tags',
    'ai.extract': 'fa-magnifying-glass',
    'ai.summarize': 'fa-compress',
    'ai.rag': 'fa-book',
    'knowledge.search': 'fa-book',
    'flow.condition': 'fa-code-branch',
    'condition.if': 'fa-code-branch',
    'condition.switch': 'fa-code-branch',
    'flow.if-else': 'fa-code-branch',
    'flow.loop': 'fa-rotate',
    'flow.iteration': 'fa-repeat',
    'flow.parallel': 'fa-arrows-split',
    'flow.wait': 'fa-hourglass-half',
    'flow.aggregation': 'fa-compress-arrows-alt',
    'transform.aggregator': 'fa-compress-arrows-alt',
    'tool.http': 'fa-globe',
    'http.request': 'fa-globe',
    'tool.db': 'fa-database',
    'tool.code': 'fa-code',
    'code.transform': 'fa-code',
    'tool.assign': 'fa-pen-to-square',
    'notify.feishu': 'fa-paper-plane',
    'notify.dingtalk': 'fa-paper-plane',
    'notify.wxwork': 'fa-comment-dots',
    'notify.email': 'fa-envelope',
    'notify.sms': 'fa-mobile-screen',
    'end.aggregator': 'fa-flag-checkered',
    'end.return': 'fa-flag-checkered',
    'note': 'fa-note-sticky',
  };

  const status = data.status ?? '';
  const statusColor = STATUS_COLORS[status] ?? null;
  const statusLabel = STATUS_LABELS[status] ?? null;
  const iconBg = CATEGORY_COLORS[data.category ?? ''] ?? '#6366f1';
  const iconClass = TYPE_ICONS[data.type ?? ''] ?? 'fa-gear';
  const debugMode = Boolean(data.__debugMode);
  const hasBreakpoint = Boolean(data.__breakpoint);
  const pausedHere = Boolean(data.__pausedHere);

  // 从 config 中抽出最多 2 个简短的关键参数作为可视 hint
  const cfg = (data.config || {}) as Record<string, unknown>;
  const KEY_PARAM_KEYS = ['method', 'path', 'cron', 'model', 'url', 'expression', 'topic'];
  const keyChips = KEY_PARAM_KEYS
    .filter((k) => cfg[k] !== undefined && cfg[k] !== null && String(cfg[k]).length > 0)
    .slice(0, 2)
    .map((k) => {
      const v = String(cfg[k]);
      const trimmed = v.length > 22 ? `${v.slice(0, 22)}…` : v;
      return { key: k, value: trimmed };
    });

  const isConditionNode = data.type === 'flow.condition' || data.type === 'condition.if';
  const isIfElseNode = data.type === 'flow.if-else' || data.type === 'condition.switch';
  const branchCases = isIfElseNode && Array.isArray((data.config as any)?.cases)
    ? ((data.config as any).cases as Array<{ id?: string; name?: string }>)
    : [];
  const sourceHandles = isConditionNode
    ? [{ id: 'true', label: 'T' }, { id: 'false', label: 'F' }]
    : isIfElseNode
      ? [
          ...branchCases.map((item, index) => ({
            id: item.name || item.id || `case-${index}`,
            label: item.name || item.id || `分支${index + 1}`,
          })),
          { id: 'default', label: '默认' },
        ]
      : [{ id: 'source', label: '' }];

  const borderStyle = status === 'running'
    ? { borderColor: '#f59e0b', backgroundColor: '#fffbeb' }
    : status === 'success'
      ? { borderColor: '#10b981', backgroundColor: '#f0fdf4' }
      : status === 'failed'
        ? { borderColor: '#ef4444', backgroundColor: '#fef2f2' }
        : {};

  const selectedStyle = selected
    ? { borderColor: '#6366f1', boxShadow: '0 0 0 3px rgba(99,102,241,0.25)' }
    : {};

  const nodeStyle = {
    width: NODE_W,
    minHeight: NODE_H,
    borderStyle: 'solid' as const,
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: '12px',
    backgroundColor: '#fff',
    boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
    transition: 'box-shadow 0.15s, border-color 0.15s',
    ...borderStyle,
    ...selectedStyle,
  };

  return (
    <div
      style={nodeStyle as any}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {(debugMode || hasBreakpoint) && (
        <button
          type="button"
          className="nodrag nopan"
          title={hasBreakpoint ? '点击移除断点' : '点击设置断点'}
          onClick={(event) => {
            event.stopPropagation();
            data.__onToggleBreakpoint?.(data.id);
          }}
          style={{
            position: 'absolute',
            top: -8,
            right: -8,
            width: 18,
            height: 18,
            borderRadius: '50%',
            border: hasBreakpoint ? '2px solid #b91c1c' : '2px solid #cbd5e1',
            backgroundColor: pausedHere ? '#f59e0b' : hasBreakpoint ? '#ef4444' : '#fff',
            cursor: 'pointer',
            boxShadow: pausedHere ? '0 0 0 5px rgba(245,158,11,.22)' : 'none',
            zIndex: 12,
            padding: 0,
          }}
        />
      )}
      <Handle
        type="target"
        position={Position.Left}
        id="target"
        style={{
          position: 'absolute',
          left: 0,
          top: '50%',
          transform: 'translateY(-50%)',
          width: 14,
          height: 14,
          borderRadius: '50%',
          border: '2px solid #6366f1',
          backgroundColor: '#fff',
          opacity: hovered || selected ? 1 : 0,
          transition: 'opacity 0.15s',
        }}
        className="react-flow__handle"
      />

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 12px 4px' }}>
        <div
          style={{
            width: 36,
            height: 36,
            borderRadius: 8,
            backgroundColor: iconBg,
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 14,
            flexShrink: 0,
            position: 'relative',
            boxShadow: '0 2px 6px rgba(15, 23, 42, 0.1)',
          }}
        >
          {/* 顶部反光 */}
          <span
            style={{
              position: 'absolute',
              inset: 0,
              borderRadius: 8,
              background: 'linear-gradient(180deg, rgba(255,255,255,0.22), transparent 55%)',
              pointerEvents: 'none',
            }}
          />
          <i className={`fa-solid ${iconClass}`} style={{ position: 'relative' }} />
        </div>

        <div style={{ flex: 1, minWidth: 0 }}>
          <div
            style={{
              fontSize: 12,
              fontWeight: 600,
              color: '#1e293b',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              maxWidth: 150,
            }}
            title={data.name}
          >
            {data.name}
          </div>
          <div
            style={{
              fontSize: 10,
              color: '#94a3b8',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              maxWidth: 150,
            }}
            title={data.type}
          >
            {data.type}
          </div>
        </div>
      </div>

      {keyChips.length > 0 && (
        <div
          style={{
            display: 'flex',
            gap: 4,
            padding: '0 12px 8px',
            flexWrap: 'wrap',
          }}
        >
          {keyChips.map((c) => (
            <span
              key={c.key}
              title={`${c.key}: ${c.value}`}
              style={{
                fontSize: 10,
                lineHeight: 1.4,
                padding: '1px 7px',
                borderRadius: 999,
                backgroundColor: '#f1f5f9',
                color: '#475569',
                fontFamily:
                  'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
                maxWidth: 200,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {c.value}
            </span>
          ))}
        </div>
      )}

      {sourceHandles.map((handle, index) => {
        const top = sourceHandles.length === 1
          ? '50%'
          : `${20 + (index * 60) / Math.max(sourceHandles.length - 1, 1)}%`;
        return (
          <Handle
            key={handle.id}
            type="source"
            position={Position.Right}
            id={handle.id}
            style={{
              position: 'absolute',
              right: 0,
              top,
              transform: 'translateY(-50%)',
              width: 14,
              height: 14,
              borderRadius: '50%',
              border: '2px solid #6366f1',
              backgroundColor: '#fff',
              opacity: hovered || selected ? 1 : 0,
              transition: 'opacity 0.15s',
            }}
            className="react-flow__handle"
          >
            {handle.label && (
              <span
                style={{
                  position: 'absolute',
                  right: 18,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  fontSize: 9,
                  color: '#6366f1',
                  whiteSpace: 'nowrap',
                  opacity: hovered || selected ? 1 : 0,
                  pointerEvents: 'none',
                }}
              >
                {handle.label}
              </span>
            )}
          </Handle>
        );
      })}
    </div>
  );
});

const nodeTypes = { workflow: WorkflowNodeComp, note: NoteNode } as const;
const edgeTypes = { custom: CustomEdge } as const;
const EdgeLabelRendererAny = EdgeLabelRenderer as any;

type Props = {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  readOnly?: boolean;
  onNodesChange: (updates: { id: string; x: number; y: number }[]) => void;
  onConnect: (source: string, target: string, sourceHandle?: string, targetHandle?: string) => void;
  onSelectNode: (node: WorkflowNode | null) => void;
  onSelectEdge: (edge: { id: string; source: string; target: string; label?: string; edgeType?: string; config?: Record<string, any> } | null) => void;
  onSelectionChange?: (selection: { nodeIds: string[]; edgeIds: string[] }) => void;
  showMinimap?: boolean;
  showGrid?: boolean;
  onFitView?: (fit: () => void) => void;
  /** Called when the user drops a NodeDefinition JSON payload onto the canvas.
   * The handler receives the parsed def and the flow-space coordinates. */
  onDropNode?: (def: { type: string; name: string; category: string; color?: string; icon?: string }, flowPos: { x: number; y: number }) => void;
  /** Separate hook for note drops so the parent can route to addNote. */
  onDropNote?: (def: { type: string; name: string; color?: string; icon?: string }, flowPos: { x: number; y: number }) => void;
  /** Note-node bridge events from NoteNode → Vue store. */
  onNoteTextChange?: (id: string, text: string) => void;
  onNoteColorChange?: (id: string, color: string) => void;
  onNoteResize?: (id: string, width: number, height: number) => void;
  debugMode?: boolean;
  breakpointNodeIds?: string[];
  pausedAtNodeId?: string;
  onToggleBreakpoint?: (nodeId: string) => void;
};

function CanvasCore({
  nodes, edges, readOnly, onNodesChange, onConnect,
  onSelectNode, onSelectEdge, onSelectionChange,
  showMinimap, showGrid, onFitView, onDropNode, onDropNote,
  onNoteTextChange, onNoteColorChange, onNoteResize,
  debugMode, breakpointNodeIds, pausedAtNodeId, onToggleBreakpoint,
}: Props) {
  const rfNodes = useMemo(
    () => nodes.map((node) => toRFNode(node, { debugMode, breakpointNodeIds, pausedAtNodeId, onToggleBreakpoint })),
    [nodes, debugMode, breakpointNodeIds, pausedAtNodeId, onToggleBreakpoint],
  );
  const rfEdges = useMemo(() => edges.map(toRFEdge), [edges]);
  const [currNodes, setCurrNodes, handleNodesChange] = useNodesState<RFNodeData>(rfNodes);
  const [currEdges, setCurrEdges] = useEdgesState(rfEdges);

  const reactFlow = useReactFlow();
  const fitViewRef = useRef<(() => void) | null>(null);

  // External props are the persisted source of truth. Keep React Flow's
  // measured dimensions and selection flags, while replacing data/positions
  // with the latest Vue values. Mapping from rfNodes guarantees one output
  // entry per incoming id; stale nodes are discarded.
  useEffect(() => {
    setCurrNodes((previous) => {
      const previousById = new Map<string, Node<RFNodeData>>(previous.map((node) => [node.id, node]));
      return rfNodes.map((node) => {
        const existing = previousById.get(node.id);
        if (!existing) return node;
        return {
          ...node,
          width: existing.width,
          height: existing.height,
          selected: existing.selected,
        };
      });
    });
  }, [rfNodes, setCurrNodes]);

  useEffect(() => {
    setCurrEdges(rfEdges);
  }, [rfEdges, setCurrEdges]);

  const handleNodeDragStop: NodeDragHandler = useCallback(
    (_, node, selectedNodes) => {
      const moved = selectedNodes.length > 0 ? selectedNodes : [node];
      onNodesChange(moved.map((item) => ({
        id: item.id,
        x: item.position.x,
        y: item.position.y,
      })));
    },
    [onNodesChange],
  );

  // 向外暴露 fitView
  useEffect(() => {
    if (onFitView) {
      fitViewRef.current = () => reactFlow.fitView({ padding: 0.2 });
      onFitView(fitViewRef.current!);
    }
  }, [onFitView, reactFlow]);

  // NoteNode lives inside the React subtree but writes must reach Vue's
  // store. We bridge through window CustomEvents so the Editor Vue layer
  // can listen once and update the store. Each handler is a separate event
  // to avoid coupling unrelated updates.
  useEffect(() => {
    const onText = (e: Event) => {
      const detail = (e as CustomEvent<{ id: string; text: string }>).detail;
      if (!detail?.id) return;
      onNoteTextChange?.(detail.id, detail.text);
    };
    const onColor = (e: Event) => {
      const detail = (e as CustomEvent<{ id: string; color: string }>).detail;
      if (!detail?.id) return;
      onNoteColorChange?.(detail.id, detail.color);
    };
    const onResize = (e: Event) => {
      const detail = (e as CustomEvent<{ id: string; width: number; height: number }>).detail;
      if (!detail?.id) return;
      onNoteResize?.(detail.id, detail.width, detail.height);
    };
    window.addEventListener('meowflow:note-text-changed', onText);
    window.addEventListener('meowflow:note-color-changed', onColor);
    window.addEventListener('meowflow:note-resized', onResize);
    return () => {
      window.removeEventListener('meowflow:note-text-changed', onText);
      window.removeEventListener('meowflow:note-color-changed', onColor);
      window.removeEventListener('meowflow:note-resized', onResize);
    };
  }, [onNoteTextChange, onNoteColorChange, onNoteResize]);

  const handleConnect = useCallback(
    (params: Connection) => {
      if (params.source && params.target && params.source !== params.target) {
        onConnect(params.source, params.target, params.sourceHandle ?? undefined, params.targetHandle ?? undefined);
      }
    },
    [onConnect],
  );

  const handleNodeClick: NodeMouseHandler = useCallback(
    (_, node) => {
      onSelectNode(node.data as WorkflowNode);
    },
    [onSelectNode],
  );

  const handleEdgeClick: NodeMouseHandler = useCallback(
    (_, edge) => {
      onSelectEdge({
        id: edge.id,
        source: edge.source,
        target: edge.target,
        label: (edge.data as any)?.label ?? (edge.label as any),
        edgeType: (edge.data as any)?.edgeType ?? undefined,
        config: (edge.data as any)?.config ?? undefined,
      });
    },
    [onSelectEdge],
  );

  const handlePaneClick = useCallback(() => {
    onSelectNode(null);
    onSelectEdge(null);
  }, [onSelectNode, onSelectEdge]);

  // Drag-and-drop support: BlockSelector serializes the node def into the
  // dataTransfer, we read it here, convert screen→flow coordinates, and
  // hand it back through `onDropNode` for the Vue-side store to commit.
  const [isDragOver, setIsDragOver] = React.useState(false);
  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
    if (!isDragOver) setIsDragOver(true);
  }, [isDragOver]);
  const handleDragLeave = useCallback(() => setIsDragOver(false), []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragOver(false);
      const raw = e.dataTransfer.getData('application/x-meowflow-node');
      if (!raw) return;
      let def: { type: string; name: string; category?: string; color?: string; icon?: string };
      try {
        def = JSON.parse(raw);
      } catch {
        return;
      }
      const flowPos = reactFlow.screenToFlowPosition({ x: e.clientX, y: e.clientY });
      // Note drops use a dedicated handler so the parent can route through
      // addNote (which sets config.text/color) instead of addNode.
      if (def.type === 'note') {
        onDropNote?.(def, flowPos);
        return;
      }
      onDropNode?.(
        { type: def.type, name: def.name, category: def.category ?? 'flow', color: def.color, icon: def.icon },
        flowPos,
      );
    },
    [reactFlow, onDropNode, onDropNote],
  );

  const handleSelectionChange = useCallback(
    (params: OnSelectionChangeParams) => {
      if (onSelectionChange) {
        onSelectionChange({
          nodeIds: params.nodes.map((n) => n.id),
          edgeIds: params.edges.map((e) => e.id),
        });
      }
    },
    [onSelectionChange],
  );

  return (
    <ReactFlow
      className={`workflow-canvas${isDragOver ? ' is-drop-target' : ''}`}
      nodes={currNodes}
      edges={currEdges}
      nodeTypes={nodeTypes}
      edgeTypes={edgeTypes}
      onNodesChange={handleNodesChange}
      onNodeDragStop={handleNodeDragStop}
      onConnect={handleConnect}
      onNodeClick={handleNodeClick}
      onEdgeClick={handleEdgeClick}
      onPaneClick={handlePaneClick}
      onSelectionChange={handleSelectionChange}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      connectionLineComponent={CustomConnectionLine}
      nodesDraggable={!readOnly}
      nodesConnectable={!readOnly}
      elementsSelectable={!readOnly}
      multiSelectionKeyCode={['Meta', 'Control']}
      selectionKeyCode={['Shift']}
      deleteKeyCode={null}
      minZoom={0.2}
      maxZoom={2}
      defaultEdgeOptions={{
        type: 'custom',
        markerEnd: { type: MarkerType.ArrowClosed, width: 10, height: 10, color: '#94a3b8' },
      }}
      fitView
      fitViewOptions={{ padding: 0.2 }}
    >
      {showGrid !== false && (
        <Background gap={14} size={1} color="#e2e8f0" />
      )}
      {showMinimap !== false && !readOnly && (
        <MiniMap
          pannable
          zoomable
          style={{ width: 102, height: 72 }}
          maskColor="rgba(241,245,249,0.8)"
        />
      )}
    </ReactFlow>
  );
}

export function CanvasWithProvider(props: Props) {
  return (
    <ReactFlowProvider>
      <CanvasCore {...props} />
    </ReactFlowProvider>
  );
}

// 导出 fitView 函数供外部触发
export function useCanvasFitView() {
  const reactFlow = useReactFlow();
  return useCallback(() => reactFlow.fitView({ padding: 0.2 }), [reactFlow]);
}

// Canvas-level styles. We can't use CSS modules with React Flow's className,
// so these are global and scoped by `.workflow-canvas` class.
const styleEl = typeof document !== 'undefined'
  ? (() => {
    const id = 'meowflow-canvas-styles';
    const existing = document.getElementById(id);
    if (existing) return existing;
    const el = document.createElement('style');
    el.id = id;
    el.textContent = `
      .workflow-canvas.is-drop-target {
        position: relative;
      }
      .workflow-canvas.is-drop-target::after {
        content: '放开鼠标即可放置节点';
        position: absolute;
        inset: 8px;
        border: 2px dashed #3b82f6;
        border-radius: 12px;
        background: rgba(59, 130, 246, 0.06);
        color: #1e40af;
        font-size: 14px;
        font-weight: 500;
        display: flex;
        align-items: center;
        justify-content: center;
        pointer-events: none;
        z-index: 50;
        animation: meowflow-dropzone-fade 120ms ease-out;
      }
      @keyframes meowflow-dropzone-fade {
        from { opacity: 0; }
        to   { opacity: 1; }
      }
    `;
    document.head.appendChild(el);
    return el;
  })()
  : null;
void styleEl;

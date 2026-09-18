import React, { useState, useRef, useEffect, useCallback } from 'react';
import { NodeResizer } from '@reactflow/node-resizer';
import type { NodeProps } from 'reactflow';
import '@reactflow/node-resizer/dist/style.css';

/** Note node on the workflow canvas (Dify-style sticky note).
 *
 * Stored as a regular WorkflowNode with `type === 'note'`. The note text
 * lives in `data.config.text`; background color in `data.config.color`;
 * size in `data.config.width` / `data.config.height`.
 *
 * Behavior:
 *  - drag from handle on hover works (React Flow built-in)
 *  - double-click to edit; click outside or Esc to commit
 *  - textarea auto-grows up to MAX_H pixels, then scrolls inside the note
 *  - drag the bottom-right handle (NodeResizer) to resize
 */

const NOTE_COLORS = [
  '#fff8c5', // sticky yellow (default)
  '#ffe4e1', // rose
  '#dfe7fd', // blue
  '#d6f5e3', // mint
  '#ede0fb', // lavender
  '#e5e7eb', // slate
];

const NOTE_DEFAULT_W = 240;
const NOTE_DEFAULT_H = 150;
const NOTE_MIN_W = 160;
const NOTE_MIN_H = 90;
const NOTE_MAX_W = 640;
const NOTE_MAX_H = 480;

export type NoteNodeData = {
  text?: string;
  color?: string;
  width?: number;
  height?: number;
  selected?: boolean;
};

export default function NoteNode({ id, data, selected }: NodeProps<NoteNodeData>) {
  const text = data?.text ?? '';
  const color = data?.color ?? NOTE_COLORS[0];
  const width = data?.width ?? NOTE_DEFAULT_W;
  const height = data?.height ?? NOTE_DEFAULT_H;
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(text);
  const taRef = useRef<HTMLTextAreaElement>(null);
  const rootRef = useRef<HTMLDivElement>(null);

  // Sync draft when external text changes (e.g., after paste, undo).
  useEffect(() => {
    if (!editing) setDraft(text);
  }, [text, editing]);

  // Auto-focus textarea when entering edit mode.
  useEffect(() => {
    if (editing && taRef.current) {
      taRef.current.focus();
      taRef.current.select();
    }
  }, [editing]);

  // Auto-grow textarea inside the note's current height.
  useEffect(() => {
    if (!editing || !taRef.current) return;
    const ta = taRef.current;
    ta.style.height = 'auto';
    const max = Math.max(40, height - 32);
    ta.style.height = Math.min(ta.scrollHeight, max) + 'px';
  }, [editing, draft, height]);

  const commit = useCallback(() => {
    if (draft !== text) {
      // Notify parent (CanvasCore) → emit update → store updates → rerender.
      window.dispatchEvent(
        new CustomEvent('meowflow:note-text-changed', {
          detail: { id, text: draft },
        }),
      );
    }
    setEditing(false);
  }, [draft, text, id]);

  const handleResizeEnd = useCallback(
    (_evt: unknown, params: { width: number; height: number }) => {
      // Persist size on commit; per-frame updates would flood the store.
      window.dispatchEvent(
        new CustomEvent('meowflow:note-resized', {
          detail: { id, width: Math.round(params.width), height: Math.round(params.height) },
        }),
      );
    },
    [id],
  );

  const handleDoubleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setDraft(text);
    setEditing(true);
  };

  const handleBlur = () => commit();

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      setDraft(text);
      setEditing(false);
      taRef.current?.blur();
    } else if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      e.preventDefault();
      taRef.current?.blur();
    }
  };

  // Cursor cycle: switching colors is a small affordance while editing.
  const cycleColor = (e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    const idx = NOTE_COLORS.indexOf(color);
    const next = NOTE_COLORS[(idx + 1) % NOTE_COLORS.length];
    window.dispatchEvent(
      new CustomEvent('meowflow:note-color-changed', { detail: { id, color: next } }),
    );
  };

  return (
    <div
      ref={rootRef}
      onDoubleClick={handleDoubleClick}
      className={'meowflow-note-node' + (selected ? ' is-selected' : '')}
      style={{
        width,
        height,
        background: color,
        border: '1px solid rgba(0,0,0,0.08)',
        borderLeft: `4px solid ${accentFor(color)}`,
        borderRadius: 8,
        boxShadow: selected
          ? '0 0 0 2px #6366f1, 0 4px 16px rgba(99,102,241,0.25)'
          : '0 2px 6px rgba(0,0,0,0.08)',
        padding: 0,
        cursor: editing ? 'text' : 'grab',
        position: 'relative',
        overflow: 'hidden',
        fontFamily:
          'ui-sans-serif, system-ui, -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif',
        transition: 'box-shadow 120ms ease',
      }}
    >
      <NodeResizer
        nodeId={id}
        isVisible={selected}
        minWidth={NOTE_MIN_W}
        minHeight={NOTE_MIN_H}
        maxWidth={NOTE_MAX_W}
        maxHeight={NOTE_MAX_H}
        lineClassName="meowflow-note-resize-line"
        handleClassName="meowflow-note-resize-handle"
        color="#6366f1"
        onResizeEnd={handleResizeEnd}
      />
      {/* Toolbar — visible on hover or in edit mode */}
      <div
        className="meowflow-note-toolbar"
        style={{
          position: 'absolute',
          top: 4,
          right: 6,
          display: 'flex',
          gap: 4,
          opacity: editing ? 1 : undefined,
        }}
      >
        <button
          type="button"
          onClick={cycleColor}
          onMouseDown={(e) => e.stopPropagation()}
          title="切换颜色"
          style={toolBtn}
        >
          <i className="fa-solid fa-palette" />
        </button>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            setDraft(text);
            setEditing((v) => !v);
          }}
          onMouseDown={(e) => e.stopPropagation()}
          title={editing ? '退出编辑' : '编辑'}
          style={toolBtn}
        >
          <i className={editing ? 'fa-solid fa-check' : 'fa-solid fa-pen'} />
        </button>
      </div>

      {editing ? (
        <textarea
          ref={taRef}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          onMouseDown={(e) => e.stopPropagation()}
          placeholder="添加注释…"
          style={{
            display: 'block',
            width: '100%',
            height: '100%',
            resize: 'none',
            background: 'transparent',
            border: 'none',
            outline: 'none',
            padding: '14px 16px',
            fontSize: 13,
            lineHeight: 1.55,
            color: '#1f2937',
            fontFamily: 'inherit',
            boxSizing: 'border-box',
          }}
        />
      ) : (
        <div
          style={{
            padding: '14px 16px',
            fontSize: 13,
            lineHeight: 1.55,
            color: text ? '#1f2937' : '#9ca3af',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            height: '100%',
            boxSizing: 'border-box',
            overflow: 'auto',
            userSelect: 'none',
          }}
        >
          {text || '双击编辑注释…'}
        </div>
      )}
    </div>
  );
}

const toolBtn: React.CSSProperties = {
  width: 22,
  height: 22,
  border: 'none',
  borderRadius: 4,
  background: 'rgba(255,255,255,0.7)',
  color: '#374151',
  cursor: 'pointer',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: 11,
};

function accentFor(bg: string): string {
  // Slightly darker shade of the background for the left bar.
  return bg
    .replace('#fff8c5', '#facc15')
    .replace('#ffe4e1', '#fb7185')
    .replace('#dfe7fd', '#60a5fa')
    .replace('#d6f5e3', '#34d399')
    .replace('#ede0fb', '#a78bfa')
    .replace('#e5e7eb', '#94a3b8');
}

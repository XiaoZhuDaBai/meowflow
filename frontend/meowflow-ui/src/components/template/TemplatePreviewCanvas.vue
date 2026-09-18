<!--
  模板预览画布（v2）
  ----------------------------------------------------------------------------
  - 同步加载 WorkflowCanvas，避免异步组件挂载时机不一致导致的 React root 异常。
  - 通过 :key 强制重建，切换模板时 React 树整体重建，无残留 store 副作用。
  - Toolbar 只保留 fit / legend，缩放交给画布自带的滚轮。
  - 卸载时主动清空 selection store 的临时选中，避免污染编辑器。

  重要修复：
  - 旧的 `.canvas-empty` 用 `inset:0` + 全屏背景覆盖整个画布容器，会
    把右上角的 toolbar 按钮遮住，导致"画布没有按钮"现象。
    现在把 empty 放在 `canvas-host` 内部并降低 z-index，绝对不会盖住
    toolbar / legend / dialog footer 按钮。
-->
<template>
  <div class="template-preview-canvas" :key="resetKey">
    <div class="canvas-host">
      <WorkflowCanvas
        :nodes="canvasNodes"
        :edges="canvasEdges"
        read-only
      />

      <div v-if="!hasContent" class="canvas-empty">
        <i class="fa-solid fa-diagram-project"></i>
        <span>当前模板暂无工作流数据</span>
      </div>
    </div>

    <div v-if="showToolbar" class="canvas-toolbar">
      <el-button size="small" plain @click="triggerFitView" title="重置视图">
        <i class="fa-solid fa-expand"></i>
        <span>重置视图</span>
      </el-button>
      <el-button
        size="small"
        :plain="!showLegend"
        :type="showLegend ? 'primary' : 'default'"
        :title="showLegend ? '隐藏图例' : '显示图例'"
        @click="showLegend = !showLegend"
      >
        <i class="fa-solid fa-circle-info"></i>
        <span>{{ showLegend ? '隐藏图例' : '显示图例' }}</span>
      </el-button>
      <span class="node-count" :title="`${canvasNodes.length} 个节点 / ${canvasEdges.length} 条连线`">
        <i class="fa-solid fa-sitemap"></i>
        {{ canvasNodes.length }} · {{ canvasEdges.length }}
      </span>
    </div>

    <div v-if="showLegend" class="canvas-legend">
      <span class="legend-item"><span class="dot" style="background:#8b5cf6"></span>触发器</span>
      <span class="legend-item"><span class="dot" style="background:#3b82f6"></span>AI</span>
      <span class="legend-item"><span class="dot" style="background:#10b981"></span>流程控制</span>
      <span class="legend-item"><span class="dot" style="background:#f59e0b"></span>工具</span>
      <span class="legend-item"><span class="dot" style="background:#ec4899"></span>通知</span>
      <span class="legend-item legend-line"><span class="line-bar"></span>数据流</span>
      <span class="legend-item legend-line condition"><span class="line-bar dashed"></span>条件分支</span>
      <span class="legend-item legend-line loop"><span class="line-bar purple"></span>循环</span>
      <span class="legend-item legend-line error"><span class="line-bar red"></span>异常处理</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 模板预览画布：复用编辑器 WorkflowCanvas 但强制 readOnly。
 *
 * 实现要点：
 *  1. 同步 import 编辑器的 WorkflowCanvas，避免异步挂载带来的 React root
 *     反复 mount/unmount 异常。
 *  2. 卸载时清空 selection / canvas store 的临时状态，避免污染编辑器。
 *  3. fitView 通过 window 事件触发（WorkflowCanvas 内部监听
 *     'meowflow:fit-view' 全局事件）。
 */

import { computed, onBeforeUnmount, ref } from 'vue';
import type { ParsedWorkflowContent } from '@/api/template';
import { toCanvasContent, getFallbackDemoWorkflow } from '@/utils/templatePreviewAdapter';
import type { WorkflowNode, WorkflowEdge } from '@/types/workflow';
import WorkflowCanvas from '@/components/workflow/Canvas/WorkflowCanvas.vue';
import { useSelectionStore } from '@/stores/selection';

const props = defineProps<{
  parsed: ParsedWorkflowContent;
  /** 切换 parsed 时强制重建 React 树（避免节点 id 复用导致 store 残留） */
  resetKey?: string | number;
  /** 关闭 toolbar（嵌入更小区域时使用） */
  showToolbar?: boolean;
  /**
   * 卸载时是否清空 selection store 的临时选中。
   * 默认 true（保持独立预览时的干净状态）。
   * 编辑器嵌入场景下设为 false，避免误清当前正在编辑的工作流选中态。
   */
  clearSelectionOnUnmount?: boolean;
}>();

const showLegend = ref(true);

const selectionStore = useSelectionStore();

const canvasNodes = computed<WorkflowNode[]>(() => {
  const content = toCanvasContent(props.parsed);
  if (content) return content.nodes;
  return getFallbackDemoWorkflow().nodes;
});
const canvasEdges = computed<WorkflowEdge[]>(() => {
  const content = toCanvasContent(props.parsed);
  if (content) return content.edges;
  return getFallbackDemoWorkflow().edges;
});

const hasContent = computed(() => canvasNodes.value.length > 0);

function triggerFitView() {
  window.dispatchEvent(new Event('meowflow:fit-view'));
}

onBeforeUnmount(() => {
  // 仅在独立预览场景下清空 selection，避免污染编辑器选中态
  if (props.clearSelectionOnUnmount !== false) {
    try {
      selectionStore.clearSelection();
    } catch {
      /* 防御 */
    }
  }
});
</script>

<style scoped>
.template-preview-canvas {
  position: relative;
  width: 100%;
  height: 520px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border);
  background: #f8fafc;
}

.canvas-host {
  position: relative;
  width: 100%;
  height: 100%;
}

.canvas-toolbar {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 5;
  padding: 6px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(8px);
}

.canvas-toolbar :deep(.el-button) {
  font-size: 12px;
}

.canvas-toolbar :deep(.el-button span) {
  margin-left: 4px;
}

.node-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  margin-left: 4px;
}

.canvas-legend {
  position: absolute;
  left: 12px;
  bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  z-index: 4;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(8px);
  font-size: 11.5px;
  color: var(--text-secondary);
  max-width: calc(100% - 24px);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend-item .dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.12);
}

.legend-line .line-bar {
  display: inline-block;
  width: 18px;
  height: 2px;
  background: #94a3b8;
  border-radius: 2px;
}

.legend-line .line-bar.dashed {
  background: repeating-linear-gradient(
    to right,
    #f59e0b 0,
    #f59e0b 4px,
    transparent 4px,
    transparent 7px
  );
}

.legend-line .line-bar.purple {
  background: #8b5cf6;
}

.legend-line .line-bar.red {
  background: #ef4444;
}

/*
 * 空状态：放在 canvas-host 内部、position:absolute 覆盖 host 区域，
 * 因为 canvas-host 是 inset:0 覆盖整个容器，所以这里只是子区域。
 * 关键点：它绝对无法越过 host 边界，因此不可能遮住 toolbar / legend。
 */
.canvas-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  font-size: 13px;
  background: rgba(248, 250, 252, 0.88);
  backdrop-filter: blur(2px);
  /* 不响应鼠标，避免吃掉下方画布的拖动事件 */
  pointer-events: none;
  z-index: 1;
}

.canvas-empty i {
  font-size: 32px;
}
</style>

<style>
/* 让 reactflow 在只读画布中也能呈现我们模板预览里的精致感 */
.template-preview-canvas .react-flow__edge-text {
  font-size: 10.5px !important;
}

.template-preview-canvas .react-flow__edge-textbg {
  fill: #fff !important;
}
</style>

<template>
  <div ref="editorRootRef" class="editor" :class="{ 'log-resizing': isLogResizing }">
    <CanvasToolbar
      :can-undo="store.canUndo"
      :can-redo="store.canRedo"
      :dirty="store.isDirty"
      :show-minimap="canvasStore.minimapVisible"
      :show-grid="canvasStore.gridVisible"
      @test-run="onTestRunClick"
      @open-palette="openPalette"
      @add-note="onAddNoteAtCenter"
      @undo="store.undo()"
      @redo="store.redo()"
      @publish="onPublish"
      @share-template="shareTemplateOpen = true"
      @toggle-minimap="canvasStore.toggleMinimap()"
      @toggle-grid="canvasStore.toggleGrid()"
      @preview="onPreview"
      @save="store.saveCurrent()"
      @back="onBack"
      @update-name="onUpdateName"
    />

    <!-- 调试控制条 (仅在运行时或调试模式下显示) -->
    <Transition name="fade">
      <DebugBar 
        v-if="(runningId !== null && debugStore.isDebugMode) || debugStore.isPaused"
        class="debug-bar-floating"
        @exit="debugStore.exitDebugMode()"
      />
    </Transition>

    <div ref="editorBodyRef" class="editor-body">
      <div class="canvas-area">
        <WorkflowCanvas
          ref="canvasRef"
          :nodes="store.current?.nodes ?? []"
          :edges="store.current?.edges ?? []"
          :read-only="false"
          :debug-mode="debugStore.isDebugMode"
          :breakpoint-node-ids="debugBreakpointIds"
          :paused-at-node-id="debugStore.pausedAtNodeId"
          @toggle-breakpoint="debugStore.toggleBreakpoint"
          @select-node="onSelectNode"
          @select-edge="onSelectEdge"
          @connect="onConnect"
          @update:nodes="onUpdateNodes"
          @drop-node="onDropNode"
          @drop-note="onDropNote"
          @note-text="onNoteText"
          @note-color="onNoteColor"
          @note-resize="onNoteResize"
          @fit-view="onFitView"
          @save="store.saveCurrent()"
        />
        <Transition name="slide">
          <NodePalette
            v-if="canvasStore.nodePaletteVisible && showPalette"
            :collapsed="paletteCollapsed"
            :featured-types="featuredTypes"
            @select="onAddNode"
            @close="showPalette = false"
            @toggle-collapse="paletteCollapsed = !paletteCollapsed"
          />
        </Transition>

        <!-- 画布操作按钮 -->
        <div class="canvas-actions">
          <el-tooltip content="添加节点 (B)" placement="left">
            <el-button circle type="primary" @click="openPalette">
              <i class="fa-solid fa-plus"></i>
            </el-button>
          </el-tooltip>
          <el-tooltip content="自适应视图" placement="left">
            <el-button circle @click="onFitView">
              <i class="fa-solid fa-expand"></i>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="canvasStore.minimapVisible ? '隐藏小地图 (M)' : '显示小地图 (M)'" placement="left">
            <el-button circle @click="canvasStore.toggleMinimap()">
              <i class="fa-solid fa-map"></i>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="canvasStore.gridVisible ? '隐藏网格 (G)' : '显示网格 (G)'" placement="left">
            <el-button circle @click="canvasStore.toggleGrid()">
              <i class="fa-solid fa-border-all"></i>
            </el-button>
          </el-tooltip>
          <el-tooltip content="变量检查器" placement="left">
            <el-button circle @click="showVariableInspect = !showVariableInspect">
              <i class="fa-solid fa-list-tree"></i>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="debugStore.isDebugMode ? '退出调试模式' : '进入调试模式'" placement="left">
            <el-button circle :type="debugStore.isDebugMode ? 'warning' : ''" @click="debugStore.toggleDebugMode()">
              <i class="fa-solid fa-bug"></i>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <ConfigPanel
        :node="selectedNode"
        :edge="selectedEdge"
        :def="selectedDef"
        :template-id="store.current?.templateId"
        @close="clearSelection"
        @update-node-config="onNodeConfigChange"
        @update-node-name="onNodeNameChange"
        @update-node-description="onNodeDescriptionChange"
        @update-edge="onEdgeConfigChange"
      />
    </div>

    <!-- 调试快照 -->
    <DebugSnapshotPanel v-if="debugStore.isDebugMode" />

    <!-- 变量检查器 -->
    <Transition name="slide-right">
      <VariableInspectPanel
        v-if="showVariableInspect"
        :node="selectedNode"
        @close="showVariableInspect = false"
      />
    </Transition>

    <!-- 底部日志面板 (折叠 / 展开 / 调整高度) -->
    <div
      class="bottom-log"
      :class="{ collapsed: logCollapsed }"
      :style="logPanelStyle"
    >
      <div
        class="log-resizer"
        role="separator"
        aria-label="调整日志面板高度"
        aria-orientation="horizontal"
        :aria-valuemin="0"
        :aria-valuemax="maxLogHeight"
        :aria-valuenow="logCollapsed ? 0 : logPanelHeight"
        tabindex="0"
        title="向上拖动调整日志面板高度，双击恢复默认高度"
        @pointerdown="startLogResize"
        @pointermove="resizeLogPanel"
        @pointerup="stopLogResize"
        @pointercancel="stopLogResize"
        @keydown="onLogResizerKeydown"
        @dblclick="resetLogHeight"
      >
        <span class="log-resizer-grip"></span>
      </div>
      <div class="log-toggle" @click="toggleLogPanel">
        <i :class="logCollapsed ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
        <span>{{ logCollapsed ? '展开日志' : '收起日志' }}</span>
        <span v-if="logStore.running" class="running-tag">
          <span class="dot"></span>
          运行中
        </span>
      </div>
      <div v-show="!logCollapsed" class="log-host">
        <LogPanel
          :entries="logStore.entries"
          :running="logStore.running"
          @clear="logStore.clear()"
          @pause-toggle="logStore.setRunning(false)"
          @resume="logStore.setRunning(true)"
        />
      </div>
    </div>

    <!-- 测试运行输入弹窗 -->
    <TestInputDialog
      v-model="testInputOpen"
      :running="testRunning"
      @submit="onRunTest"
    />

    <!-- 测试运行结果弹窗 -->
    <TestResultDialog
      v-if="testResult"
      v-model="testResultOpen"
      :execution-id="testResult.executionId"
      :status="testResult.status"
      :duration="testResult.duration"
      :cost="testResult.cost"
      :payload="testResult.payload"
    />

    <!-- 工作流预览弹窗（与模板市场共享 WorkflowPreview 组件） -->
    <el-dialog
      v-model="previewOpen"
      :title="store.current?.name ? `${store.current.name} - 预览` : '工作流预览'"
      width="920px"
      class="editor-preview-dialog"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="!hasPreviewContent" class="preview-empty">
        <i class="fa-solid fa-circle-info"></i>
        <span>当前工作流还没有节点，请先添加节点后再预览。</span>
      </div>
      <WorkflowPreview
        v-else
        :parsed="previewParsed"
        :reset-key="previewResetKey"
        :meta="previewMeta"
        :clear-selection-on-unmount="false"
      />
      <template #footer>
        <el-button @click="previewOpen = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Webhook 发布后弹窗 -->
    <WebhookUrlDialog
      v-model="publishDialogOpen"
      :workflow-id="store.current?.id ?? ''"
      :workflow-name="store.current?.name ?? ''"
      :triggers="publishTriggers"
      :method="webhookMethod"
      :cron="cronExpr"
      :timezone="cronTimezone"
    />

    <!-- 分享到模板市场 -->
    <ShareToTemplateDialog
      v-model="shareTemplateOpen"
      :nodes="store.current?.nodes ?? []"
      :edges="store.current?.edges ?? []"
      :workflow-name="store.current?.name"
      :version="String(store.current?.version ?? 'v1')"
      @success="onShareTemplateSuccess"
    />

    <!-- 命令面板 (Ctrl+K 唤起) -->
    <Teleport to="body">
      <NodeCommandPalette
        v-if="showCommandPalette"
        v-model="showCommandPalette"
        @select="onCommandPaletteSelect"
      />
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router';
import { useWorkflowStore } from '@/stores/workflow';
import { useLogStore } from '@/stores/log';
import { useCanvasStore } from '@/stores/canvas';
import { useSelectionStore } from '@/stores/selection';
import { useWorkflowShortcuts } from '@/composables/useWorkflowShortcuts';
import CanvasToolbar from '@/components/workflow/Canvas/CanvasToolbar.vue';
import NodePalette from '@/components/workflow/Canvas/NodePalette.vue';
import ConfigPanel, { type SelectedEdge } from '@/components/workflow/Canvas/ConfigPanel.vue';
import WorkflowCanvas from '@/components/workflow/Canvas/WorkflowCanvas.vue';
import VariableInspectPanel from '@/components/workflow/VariableInspect/VariableInspectPanel.vue';
import LogPanel from '@/components/log/LogPanel.vue';
import TestInputDialog from '@/components/workflow/TestInputDialog.vue';
import TestResultDialog from '@/components/workflow/TestResultDialog.vue';
import WebhookUrlDialog from '@/components/workflow/WebhookUrlDialog.vue';
import WorkflowPreview, { type WorkflowPreviewMeta } from '@/components/common/WorkflowPreview.vue';
import DebugBar from '@/components/workflow/DebugBar/DebugBar.vue';
import DebugSnapshotPanel from '@/components/workflow/DebugBar/DebugSnapshotPanel.vue';
import NodeCommandPalette from '@/components/workflow/NodePalette/NodeCommandPalette.vue';
import ShareToTemplateDialog from '@/components/workflow/ShareToTemplateDialog.vue';
import { findNodeDefinition } from '@/mock/nodes';
import { ElMessage, ElMessageBox } from '@/utils/notify';
import type { WorkflowNode } from '@/types/workflow';
import type { NodeDefinition } from '@/types/node';
import { executionApi } from '@/api/execution';
import { workflowApi } from '@/api/workflow';
import { useWorkflowRun } from '@/composables/useWorkflowRun';
import { useDebugStore } from '@/stores/debug';
const route = useRoute();
const router = useRouter();
const store = useWorkflowStore();
const logStore = useLogStore();
const canvasStore = useCanvasStore();
const selectionStore = useSelectionStore();
const debugStore = useDebugStore();
const debugBreakpointIds = computed(() => Array.from(debugStore.breakpointNodeIds));

// 命令面板状态
const showCommandPalette = ref(false);

// 注册快捷键
useWorkflowShortcuts({
  onSave: () => {
    store.saveCurrent();
  },
  onAddNote: () => {
    addNote();
  },
});

// 注册全局快捷键 (Ctrl+K 唤起命令面板)
onMounted(() => {
  const handleKeyDown = (e: KeyboardEvent) => {
    // Ctrl+K 或 Cmd+K 唤起命令面板
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
      e.preventDefault();
      showCommandPalette.value = !showCommandPalette.value;
    }
    // Esc 关闭命令面板
    if (e.key === 'Escape' && showCommandPalette.value) {
      showCommandPalette.value = false;
    }
  };
  
  window.addEventListener('keydown', handleKeyDown);
  
  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeyDown);
  });
});

function onBack() {
  router.push('/workflows');
}

// 离开编辑器的变更检查（路由跳转 / 关闭 tab / 刷新）
let bypassGuard = false;
onBeforeRouteLeave(async (to) => {
  if (bypassGuard) return true;
  if (!store.isDirty) return true;

  // Use a confirm-style box with three outcomes so the user can:
  //   - save + leave  (confirm)
  //   - discard + leave (cancel button)
  //   - stay         (close button / X / Esc)
  // Throwing from the close path keeps the route on the editor so the
  // user has a chance to keep editing.
  try {
    await ElMessageBox.confirm(
      '当前工作流有未保存的修改，是否保存后离开？',
      '未保存的更改',
      {
        type: 'warning',
        confirmButtonText: '保存并离开',
        cancelButtonText: '不保存',
        showClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        distinguishCancelAndClose: true,
      },
    );
    // Confirmed → save first, then allow navigation.
    try {
      await store.saveCurrent();
      bypassGuard = true;
      return true;
    } catch {
      await ElMessageBox.alert('保存失败，请稍后重试', '保存失败', { type: 'error' });
      return false;
    }
  } catch (action) {
    // distinguishCancelAndClose: 'cancel' (不保存) | 'close' (X / Esc)
    if (action === 'cancel') {
      bypassGuard = true;
      return true;
    }
    // 'close' → 留在编辑器
    return false;
  }
});

// 拦截浏览器级别的关闭 / 刷新
function beforeUnloadHandler(e: BeforeUnloadEvent) {
  if (store.isDirty) {
    e.preventDefault();
    e.returnValue = '';
  }
}

const canvasRef = ref<InstanceType<typeof WorkflowCanvas>>();
const editorRootRef = ref<HTMLDivElement>();
const editorBodyRef = ref<HTMLDivElement>();
const selectedNode = ref<WorkflowNode | null>(null);
const selectedEdge = ref<SelectedEdge | null>(null);
const showPalette = ref(true);
const paletteCollapsed = ref(false);
const logCollapsed = ref(true);
const showVariableInspect = ref(false);

const DEFAULT_LOG_HEIGHT = 220;
const MIN_LOG_HEIGHT = 120;
const MIN_CANVAS_HEIGHT = 160;
const LOG_RESIZE_STEP = 20;
const LOG_COLLAPSE_THRESHOLD = 48;
const logPanelHeight = ref(DEFAULT_LOG_HEIGHT);
const maxLogHeight = ref(DEFAULT_LOG_HEIGHT);
const isLogResizing = ref(false);
const logPanelStyle = computed<Record<string, string>>(() => ({
  '--log-panel-height': `${logPanelHeight.value}px`,
  '--log-panel-max-height': `${maxLogHeight.value}px`,
}));

let resizeStartY = 0;
let resizeStartHeight = DEFAULT_LOG_HEIGHT;
let editorResizeObserver: ResizeObserver | undefined;

function refreshMaxLogHeight() {
  if (!editorBodyRef.value) return;
  const visibleLogHeight = logCollapsed.value ? 0 : logPanelHeight.value;
  maxLogHeight.value = Math.max(
    MIN_LOG_HEIGHT,
    Math.floor(editorBodyRef.value.clientHeight + visibleLogHeight - MIN_CANVAS_HEIGHT),
  );
  if (!logCollapsed.value) {
    logPanelHeight.value = clampLogHeight(logPanelHeight.value);
  }
}

function clampLogHeight(height: number) {
  return Math.min(maxLogHeight.value, Math.max(MIN_LOG_HEIGHT, height));
}

function toggleLogPanel() {
  if (logCollapsed.value) {
    refreshMaxLogHeight();
    logPanelHeight.value = clampLogHeight(logPanelHeight.value);
    logCollapsed.value = false;
  } else {
    logCollapsed.value = true;
  }
}

function startLogResize(event: PointerEvent) {
  if (event.button !== 0) return;

  refreshMaxLogHeight();
  resizeStartY = event.clientY;
  resizeStartHeight = logCollapsed.value ? 0 : logPanelHeight.value;
  isLogResizing.value = true;
  (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  event.preventDefault();
}

function resizeLogPanel(event: PointerEvent) {
  if (!isLogResizing.value) return;

  const nextHeight = resizeStartHeight + resizeStartY - event.clientY;
  if (nextHeight < LOG_COLLAPSE_THRESHOLD) {
    logCollapsed.value = true;
    return;
  }

  logCollapsed.value = false;
  logPanelHeight.value = clampLogHeight(nextHeight);
}

function stopLogResize(event: PointerEvent) {
  if (!isLogResizing.value) return;

  const handle = event.currentTarget as HTMLElement;
  if (handle.hasPointerCapture(event.pointerId)) {
    handle.releasePointerCapture(event.pointerId);
  }
  isLogResizing.value = false;
}

function resetLogHeight() {
  refreshMaxLogHeight();
  logCollapsed.value = false;
  logPanelHeight.value = clampLogHeight(DEFAULT_LOG_HEIGHT);
}

function onLogResizerKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowUp') {
    event.preventDefault();
    if (logCollapsed.value) {
      resetLogHeight();
    } else {
      logPanelHeight.value = clampLogHeight(logPanelHeight.value + LOG_RESIZE_STEP);
    }
  } else if (event.key === 'ArrowDown') {
    event.preventDefault();
    if (logCollapsed.value) return;
    if (logPanelHeight.value <= MIN_LOG_HEIGHT) {
      logCollapsed.value = true;
    } else {
      logPanelHeight.value = clampLogHeight(logPanelHeight.value - LOG_RESIZE_STEP);
    }
  } else if (event.key === 'Home') {
    event.preventDefault();
    logCollapsed.value = false;
    logPanelHeight.value = clampLogHeight(MIN_LOG_HEIGHT);
  } else if (event.key === 'End') {
    event.preventDefault();
    refreshMaxLogHeight();
    logCollapsed.value = false;
    logPanelHeight.value = maxLogHeight.value;
  } else if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    toggleLogPanel();
  }
}

// 推荐节点（顶部突出显示）
const featuredTypes = ['ai.llm', 'tool.http', 'notify.dingtalk'];

function openPalette() {
  showPalette.value = true;
  paletteCollapsed.value = false;
}

// 测试运行
const testInputOpen = ref(false);
const testRunning = ref(false);
const testResultOpen = ref(false);
const testResult = ref<any>(null);

// 试运行状态机（替代本地 streamMockExecution）
const { run: runWorkflow, stop: stopRun, runningId } = useWorkflowRun();

// 工作流预览弹窗
const previewOpen = ref(false);

// 发布弹窗
const publishDialogOpen = ref(false);
const publishTriggers = ref<any[]>([]);
const webhookMethod = ref<'GET' | 'POST' | 'PUT'>('POST');
const cronExpr = ref('');
const cronTimezone = ref('Asia/Shanghai');

// 分享到模板市场弹窗
const shareTemplateOpen = ref(false);

const selectedDef = computed<NodeDefinition | null>(() => {
  if (!selectedNode.value) return null;
  return findNodeDefinition(selectedNode.value.type) ?? null;
});

function clearSelection() {
  selectedNode.value = null;
  selectedEdge.value = null;
  selectionStore.clearSelection();
}

function onNodeConfigChange(config: Record<string, any>) {
  if (selectedNode.value) {
    store.updateNodeConfig(selectedNode.value.id, config);
  }
}

function onNodeNameChange(name: string) {
  if (selectedNode.value) {
    store.updateNodeConfig(selectedNode.value.id, {}, name);
  }
}

function onNodeDescriptionChange(description: string) {
  if (selectedNode.value) {
    // 把描述写入节点顶层 description 字段（toDefinition 会一并序列化到 definition.data.description）
    const node = selectedNode.value;
    store.updateNodeDescription(node.id, description);
  }
}

function onUpdateName(name: string) {
  if (store.current) {
    store.current.name = name;
    store.saveCurrent();
  }
}

function onSelectNode(n: WorkflowNode | null) {
  selectedNode.value = n;
  selectedEdge.value = null;
  if (n) {
    selectionStore.selectNode(n.id);
    showPalette.value = false;
  } else {
    selectionStore.clearSelection();
  }
}

function onSelectEdge(e: SelectedEdge | null) {
  selectedEdge.value = e;
  selectedNode.value = null;
  if (e) {
    selectionStore.selectEdge(e.id);
  } else {
    selectionStore.clearSelection();
  }
}

function onEdgeConfigChange(patch: Partial<SelectedEdge>) {
  if (!selectedEdge.value) return;
  store.updateEdge(selectedEdge.value.id, patch);
  // 同步本地选中态（让用户看到刚修改的字段）
  selectedEdge.value = { ...selectedEdge.value, ...patch };
}

function onConnect(payload: {
  source: string;
  target: string;
  sourcePort?: string;
  targetPort?: string;
}) {
  if (payload.source === payload.target) {
    ElMessage.warning('不能连接节点到自己');
    return;
  }
  const sourceNode = store.current?.nodes.find((n) => n.id === payload.source);
  const targetNode = store.current?.nodes.find((n) => n.id === payload.target);
  const patch: { edgeType?: 'condition'; label?: string } = {};
  if (sourceNode?.type === 'flow.condition') {
    patch.edgeType = 'condition';
    patch.label = payload.sourcePort === 'false'
      ? 'false'
      : payload.sourcePort === 'true'
        ? 'true'
        : (store.current?.edges.filter((e) => e.source === payload.source).length ?? 0) === 0
          ? 'true'
          : 'false';
  } else if (sourceNode?.type === 'flow.if-else') {
    patch.edgeType = 'condition';
    patch.label = payload.sourcePort || targetNode?.name || payload.target;
  }
  store.addEdge(payload.source, payload.target, patch);
  ElMessage.success('连线已创建');
}

function onUpdateNodes(updates: { id: string; x: number; y: number }[]) {
  if (updates.length > 0) {
    store.moveNode(updates[0].id, updates[0].x, updates[0].y, true);
  }
  for (let i = 1; i < updates.length; i++) {
    store.moveNode(updates[i].id, updates[i].x, updates[i].y, false);
  }
}

/**
 * Add a node at a specific flow position.
 * Used by both:
 *  - onAddNode (click on palette → no anchor, falls back to findEmptyPosition)
 *  - onDropNode (drag from palette onto canvas → user-anchored position)
 */
function addNodeAt(
  def: { type: string; category: string; name: string; config?: Record<string, unknown>; [k: string]: unknown },
  anchor: { x: number; y: number },
) {
  const pos = findPositionNear(anchor);
  store.addNode({
    type: def.type,
    category: def.category as any,
    name: def.name,
    x: pos.x,
    y: pos.y,
    config: {},
  });
  ElMessage.success(`已添加节点: ${def.name}`);
}

function onAddNode(def: { type: string; category: string; name: string; color?: string; icon?: string }) {
  // Notes click-add through palette: route through addNote so config is set.
  if (def.type === 'note') {
    addNote(null, def.color);
    return;
  }
  // No anchor for click-add — start from a free grid cell.
  addNodeAt(def, findEmptyPosition());
}

/**
 * 命令面板选择节点后的处理
 */
function onCommandPaletteSelect(nodeType: string) {
  showCommandPalette.value = false;
  
  // 查找节点定义
  const nodeDef = findNodeDefinition(nodeType);
  if (!nodeDef) {
    ElMessage.error(`未找到节点类型: ${nodeType}`);
    return;
  }
  
  // 添加节点到画布中心
  addNodeAt({
    type: nodeDef.type,
    category: nodeDef.category,
    name: nodeDef.name,
  }, findEmptyPosition());
}

function onDropNode(payload: {
  def: { type: string; name: string; category: string };
  x: number;
  y: number;
}) {
  addNodeAt(payload.def, { x: payload.x, y: payload.y });
}

function addNote(anchor: { x: number; y: number } | null = null, color?: string) {
  // Notes are WorkflowNodes with type='note'; config carries text + color + size.
  const pos = findPositionNear(anchor);
  return store.addNode({
    type: 'note',
    category: 'flow',
    name: '注释',
    x: pos.x,
    y: pos.y,
    config: {
      text: '',
      color: color ?? '#fff8c5',
      width: 240,
      height: 150,
    },
  });
}

function onAddNoteAtCenter() {
  // No anchor → findEmptyPosition returns bottom-right of the bbox.
  const id = addNote();
  if (id) ElMessage.success('已添加注释');
}

function onDropNote(payload: {
  def: { type: string; name: string; color?: string; icon?: string };
  x: number;
  y: number;
}) {
  addNote({ x: payload.x, y: payload.y }, payload.def.color);
}

function onNoteText(payload: { id: string; text: string }) {
  store.updateNodeConfig(payload.id, { text: payload.text });
}

function onNoteColor(payload: { id: string; color: string }) {
  store.updateNodeConfig(payload.id, { color: payload.color });
}

function onNoteResize(payload: { id: string; width: number; height: number }) {
  store.updateNodeConfig(payload.id, { width: payload.width, height: payload.height });
}

/**
 * Compute a position that doesn't collide with any existing node.
 * Used as the anchor for click-add when no mouse position is available.
 */
function findEmptyPosition(): { x: number; y: number } {
  return findPositionNear(null);
}

/**
 * Find a free position, optionally anchored to a preferred spot.
 * - anchor=null: scan from the bottom-right corner of the bounding box.
 * - anchor=({x,y}): try that exact cell, then spiral outward by COL_STEP/ROW_STEP.
 *
 * Returns the first non-colliding cell. Falls back to the anchor
 * (or a default origin) if every cell is occupied.
 */
function findPositionNear(anchor: { x: number; y: number } | null): { x: number; y: number } {
  const nodes = store.current?.nodes ?? [];
  const NODE_W = 240;
  const NODE_H = 80;
  const COL_STEP = 280;
  const ROW_STEP = 120;
  const PADDING = 60;

  if (nodes.length === 0) {
    if (anchor) return { x: anchor.x, y: anchor.y };
    return { x: 240, y: 180 };
  }

  // Snap anchor to grid (so the drop position feels natural).
  let baseX: number, baseY: number;
  if (anchor) {
    baseX = Math.max(PADDING, Math.round((anchor.x - PADDING) / COL_STEP) * COL_STEP + PADDING);
    baseY = Math.max(PADDING, Math.round((anchor.y - PADDING) / ROW_STEP) * ROW_STEP + PADDING);
  } else {
    const maxRight = Math.max(...nodes.map((n) => n.x + NODE_W));
    const maxBottom = Math.max(...nodes.map((n) => n.y + NODE_H));
    baseX = Math.max(maxRight + COL_STEP, PADDING);
    baseY = Math.max(maxBottom + ROW_STEP, PADDING);
  }

  const occupied = nodes.map((n) => ({
    left: n.x - 20,
    right: n.x + NODE_W + 20,
    top: n.y - 20,
    bottom: n.y + NODE_H + 20,
  }));

  function collidesAt(x: number, y: number): boolean {
    const target = {
      left: x,
      right: x + NODE_W,
      top: y,
      bottom: y + NODE_H,
    };
    return occupied.some(
      (o) => !(target.right <= o.left || target.left >= o.right || target.bottom <= o.top || target.top >= o.bottom),
    );
  }

  if (!collidesAt(baseX, baseY)) return { x: baseX, y: baseY };

  // Spiral search: small ring around the anchor (radius 1..5 cells).
  for (let ring = 1; ring <= 5; ring++) {
    for (let dr = -ring; dr <= ring; dr++) {
      for (let dc = -ring; dc <= ring; dc++) {
        if (Math.abs(dr) !== ring && Math.abs(dc) !== ring) continue;
        const x = baseX + dc * COL_STEP;
        const y = baseY + dr * ROW_STEP;
        if (!collidesAt(x, y)) return { x, y };
      }
    }
  }

  // Fallback: staggered right-down offset (avoid double-stacking).
  if (anchor) return { x: baseX + COL_STEP, y: baseY };
  return { x: baseX, y: baseY };
}

function onFitView() {
  canvasRef.value?.fitView?.();
}

// Boot
async function boot() {
  const id = route.params.id as string | undefined;
  store.resetCurrent();
  if (id) {
    try {
      await store.fetchOne(id);
    } catch {
      ElMessage.error('工作流不存在');
      router.replace('/workflows');
    }
  } else {
    if (!store.current) {
      router.replace('/workflows');
    }
  }
}

watch(
  () => store.current?.id,
  () => {
    clearSelection();
  },
);

function onTestRunClick() {
  if (!store.current?.nodes?.length) {
    ElMessage.warning('工作流为空');
    return;
  }
  testInputOpen.value = true;
}

async function onRunTest(input: Record<string, any>) {
  testInputOpen.value = false;
  testRunning.value = true;
  logCollapsed.value = false;

  if (!store.current?.id) {
    try {
      await store.saveCurrent();
    } catch {
      ElMessage.error('请先保存工作流后再测试');
      testRunning.value = false;
      return;
    }
  }

  try {
    // 触发执行 + 订阅 SSE（SSE 在 mock 模式下由 composable 内部降级到本地定时器）
    const result = await runWorkflow(store.current!.id!, input);

    testResult.value = {
      executionId: result.executionId,
      status: result.status,
      duration: result.duration,
      cost: result.cost,
      payload: {
        input,
        output: result.output ?? { ok: true },
        nodes: (result.nodes ?? []).map((n: any) => ({
          ...n,
          duration: n.duration ?? 100,
          status: n.status ?? 'success',
        })) as any,
        error: result.error,
      },
    };
    testResultOpen.value = true;
  } catch (e: any) {
    logStore.push({ level: 'error', message: `测试运行失败: ${e?.message ?? e}` });
  } finally {
    testRunning.value = false;
    logStore.setRunning(false);
  }
}

// ===================== 工作流预览（编辑器入口） =====================
const hasPreviewContent = computed(
  () => !!store.current && Array.isArray(store.current.nodes) && store.current.nodes.length > 0,
);

/**
 * 把当前 store.current 转换为 ParsedWorkflowContent 形状，
 * 与模板预览共享同一组 Tab UI。
 * - 节点 x/y 直接复用 store 中的画布坐标
 * - 边附带 edgeType / label / config
 */
const previewParsed = computed(() => {
  const wf = store.current;
  if (!wf) {
    return { nodes: [], edges: [], raw: null as any };
  }
  const nodes = (wf.nodes ?? []).map((n) => ({
    id: n.id,
    type: n.type,
    category: n.category,
    name: n.name,
    description: n.description,
    x: n.x,
    y: n.y,
    cx: n.x + 120,
    cy: n.y + 30,
    w: 240,
    h: 60,
    config: n.config ?? {},
    raw: n,
  }));
  const edges = (wf.edges ?? []).map((e) => ({
    id: e.id,
    source: e.source,
    target: e.target,
    label: e.label,
    edgeType: (e.edgeType ?? 'default') as string,
    config: (e.config ?? {}) as Record<string, any>,
    raw: e,
  }));
  return {
    nodes,
    edges,
    raw: { nodes: wf.nodes, edges: wf.edges },
  };
});

const previewMeta = computed<WorkflowPreviewMeta | null>(() => {
  const wf = store.current;
  if (!wf) return null;
  return {
    name: wf.name,
    description: wf.description,
    category: wf.category,
    emoji: wf.icon || '🐾',
    author: wf.createdBy,
    createdAt: wf.createdAt?.slice(0, 10),
  };
});

/**
 * 「图形数据」Tab：图形数据已来源于当前 store 节点坐标，这里无需额外字符串。
 * 共享组件在没有 graphRaw 时会优雅跳过该 Tab，不影响浏览。
 */
const previewResetKey = computed(() => {
  const wf = store.current;
  if (!wf) return 'preview-empty';
  // 时间戳或节点/连线数量变化时强制 ReactFlow 重建
  return `${wf.id ?? 'wf'}:${(wf.nodes?.length ?? 0)}:${(wf.edges?.length ?? 0)}:${wf.updatedAt ?? ''}`;
});

function onPreview() {
  previewOpen.value = true;
}

async function onPublish() {
  const validationErrors = validatePublishNodes(store.current?.nodes ?? []);
  if (validationErrors.length) {
    ElMessage.error(validationErrors[0]);
    return;
  }

  await store.saveCurrent();
  await store.publishCurrent();

  const triggers: any[] = [];
  const nodes = store.current?.nodes ?? [];
  let method: 'GET' | 'POST' | 'PUT' = 'POST';
  let cron = '';
  let tz = 'Asia/Shanghai';

  const workflowId = store.current?.id;

  // 1. Webhook 节点 -> 调后端 registerWebhook 拿到真实 webhookUrl
  for (const n of nodes) {
    if (n.type === 'trigger.webhook') {
      const path = n.config?.path ?? '/hooks/incoming';
      method = (n.config?.method ?? 'POST') as 'GET' | 'POST' | 'PUT';
      let url = '';
      try {
        if (workflowId) {
          const resp = await workflowApi.registerWebhook({
            workflowId,
            path,
            method,
          });
          url = resp.webhookUrl;
        }
      } catch (e: any) {
        ElMessage.warning(`Webhook 注册失败（仅展示本地 URL）: ${e?.message ?? e}`);
      }
      if (!url) {
        url = `${window.location.origin}/api/webhook/wf_${workflowId}${path}`;
      }
      const example = `curl -X ${method} "${url}" \\
  -H "Content-Type: application/json" \\
  -d '{"query": "hello"}'`;
      triggers.push({ type: 'webhook', label: 'Webhook', url, example });
    }

    // 2. Cron 节点 -> 调后端 registerSchedule
    if (n.type === 'trigger.cron') {
      cron = n.config?.cron ?? '0 0 * * *';
      tz = n.config?.timezone ?? 'Asia/Shanghai';
      try {
        if (workflowId && n.id) {
          await workflowApi.registerSchedule({
            workflowId,
            nodeId: n.id,
            cronExpression: cron,
          });
        }
      } catch (e: any) {
        ElMessage.warning(`定时任务注册失败: ${e?.message ?? e}`);
      }
      triggers.push({
        type: 'cron',
        label: '定时触发',
        example: `# Cron: ${cron} (${tz})\n# 系统将按表达式定时执行此工作流`,
      });
    }
  }
  if (!triggers.length) {
    triggers.push({
      type: 'manual',
      label: '手动触发',
      example: '# 当前工作流没有触发器,可在执行日志页面手动触发',
    });
  }

  publishTriggers.value = triggers;
  webhookMethod.value = method;
  cronExpr.value = cron;
  cronTimezone.value = tz;
  publishDialogOpen.value = true;
}

function onShareTemplateSuccess(_templateId: number) {
  ElMessage.success('模板已提交审核，审核通过后将展示在模板市场');
}

function validatePublishNodes(nodes: WorkflowNode[]): string[] {
  const errors: string[] = [];
  for (const node of nodes) {
    const def = findNodeDefinition(node.type);
    if (!def) continue;

    for (const param of def.params.filter((p) => p.required)) {
      const value = node.config?.[param.key];
      const empty = value === undefined || value === null || value === ''
        || (Array.isArray(value) && value.length === 0);
      if (empty) {
        errors.push(`${node.name || node.type}：${param.label}为必填项`);
        break;
      }
    }

    if (node.type === 'ai.llm'
        && !node.config?.modelId
        && !node.config?.model) {
      errors.push(`${node.name || node.type}：AI 模型为必填项`);
    }
    if (node.type === 'trigger.webhook' && !node.config?.path) {
      errors.push(`${node.name || node.type}：路径为必填项`);
    }
  }
  return errors;
}

onMounted(async () => {
  window.addEventListener('beforeunload', beforeUnloadHandler);
  editorResizeObserver = new ResizeObserver(refreshMaxLogHeight);
  if (editorRootRef.value) {
    editorResizeObserver.observe(editorRootRef.value);
  }
  if (editorBodyRef.value) {
    editorResizeObserver.observe(editorBodyRef.value);
  }
  refreshMaxLogHeight();
  await boot();
});

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnloadHandler);
  editorResizeObserver?.disconnect();
  stopRun();
  store.resetCurrent();
  logStore.clear();
});
</script>

<style scoped>
.editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-secondary);
}
.editor-body {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}
.canvas-area {
  flex: 1;
  position: relative;
  min-width: 0;
}

.canvas-actions {
  position: absolute;
  right: 16px;
  bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 20;
}

.connect-tip {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 100;
  box-shadow: var(--shadow-md);
  max-width: 500px;
}

.bottom-log {
  --log-panel-height: 220px;
  --log-panel-max-height: 220px;
  position: relative;
  background: var(--bg-dark);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}
.bottom-log .log-resizer {
  height: 7px;
  flex: 0 0 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--border);
  cursor: ns-resize;
  touch-action: none;
  outline: none;
  transition: background 0.15s ease, border-color 0.15s ease;
}
.bottom-log .log-resizer-grip {
  width: 44px;
  height: 3px;
  border-radius: 999px;
  background: rgba(203, 213, 225, 0.35);
  transition: width 0.15s ease, background 0.15s ease;
}
.bottom-log .log-resizer:hover,
.bottom-log .log-resizer:focus-visible,
.editor.log-resizing .log-resizer {
  border-top-color: var(--primary);
  background: rgba(96, 165, 250, 0.08);
}
.bottom-log .log-resizer:hover .log-resizer-grip,
.bottom-log .log-resizer:focus-visible .log-resizer-grip,
.editor.log-resizing .log-resizer-grip {
  width: 56px;
  background: var(--primary);
}
.editor.log-resizing,
.editor.log-resizing * {
  cursor: ns-resize !important;
  user-select: none !important;
}
.bottom-log .log-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  cursor: pointer;
  font-size: 12px;
  color: #cbd5e1;
  user-select: none;
}
.bottom-log .log-toggle:hover { background: rgba(255,255,255,0.05); }
.bottom-log .log-host {
  height: min(var(--log-panel-height), var(--log-panel-max-height));
  min-height: 0;
  overflow: hidden;
}
.bottom-log.collapsed .log-host { display: none; }

.running-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--success);
  font-size: 11px;
  margin-left: 8px;
}
.running-tag .dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--success);
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.4; }
}

.slide-enter-active, .slide-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.slide-enter-from, .slide-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.25s ease, opacity 0.2s ease;
}
.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* 工作流预览弹窗内容 */
.editor-preview-dialog :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
  padding-top: 12px;
  padding-bottom: 12px;
}

.preview-empty {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}
.preview-empty i {
  font-size: 28px;
  color: var(--text-tertiary);
}

/* 调试控制条浮动样式 */
.debug-bar-floating {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 8px;
}
</style>

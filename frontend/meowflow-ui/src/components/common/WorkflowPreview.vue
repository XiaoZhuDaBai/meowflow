<!--
  WorkflowPreview - 共享工作流预览组件
  -----------------------------------------------------------------------------
  - 同时服务于「模板市场」和「我的工作流编辑器」两处入口：
      · TemplatePreview：用模板的 workflowJson / workflowGraph 作为数据源
      · Editor.vue：用当前 store.current（已就地的真实工作流节点+连线）作为数据源
  - 不依赖任何上游业务概念（模板、模板市场、模板 API 等），仅消费通用数据。
  - 默认 Tab：画布 → 节点列表 → 配置 → 连线 → JSON 图形数据。
  - 提供统一的 `mode`: 'template' | 'editor'，影响 Cover Header 渲染策略。
-->
<template>
  <div class="workflow-preview">
    <!-- 顶部概览卡（可由调用方控制是否显示） -->
    <div v-if="meta" class="preview-header">
      <div v-if="meta.cover || meta.emoji" class="cover" :style="coverStyle">
        <span v-if="meta.emoji" class="emoji">{{ meta.emoji }}</span>
        <i v-else-if="meta.cover" class="fa-solid fa-puzzle-piece fallback"></i>
      </div>
      <div class="meta">
        <div class="name">{{ meta.name || '工作流预览' }}</div>
        <div v-if="meta.description" class="desc">{{ meta.description }}</div>
        <div v-if="chips.length" class="tag-row">
          <span
            v-for="(c, idx) in chips"
            :key="idx"
            class="meta-chip"
            :class="c.kind"
          >
            <i v-if="c.icon" :class="c.icon"></i>
            <span>{{ c.label }}</span>
          </span>
        </div>
      </div>
    </div>

    <!-- 自定义额外信息（如评分 / 标签 / 备注），由调用方决定是否填充 -->
    <slot name="header-extra" />

    <el-tabs v-model="activeTab" class="preview-tabs">
      <!-- 真实画布（ReactFlow 只读） -->
      <el-tab-pane :label="`画布`" name="canvas">
        <TemplatePreviewCanvas
          :parsed="parsed"
          :reset-key="resetKey"
          :show-toolbar="true"
          :clear-selection-on-unmount="clearSelectionOnUnmount"
        />
      </el-tab-pane>

      <!-- 节点列表 -->
      <el-tab-pane :label="`节点 (${parsed.nodes.length})`" name="nodes">
        <TemplateNodeList :parsed="parsed" />
      </el-tab-pane>

      <!-- 配置预览（全量参数） -->
      <el-tab-pane label="参数配置" name="config">
        <TemplateConfigPreview :parsed="parsed" />
      </el-tab-pane>

      <!-- 连线图 -->
      <el-tab-pane :label="`连线 (${parsed.edges.length})`" name="edges">
        <div v-if="parsed.edges.length" class="edges-list">
          <div
            v-for="(e, idx) in parsed.edges"
            :key="e.id ?? idx"
            class="edge-item"
          >
            <span class="edge-source">{{ nodeName(e.source) }}</span>
            <span v-if="e.label" class="edge-label">{{ e.label }}</span>
            <i class="fa-solid fa-arrow-right edge-arrow"></i>
            <span class="edge-target">{{ nodeName(e.target) }}</span>
          </div>
        </div>
        <div v-else class="empty-block">
          <i class="fa-solid fa-circle-info"></i>
          <span>暂无连线数据</span>
        </div>
      </el-tab-pane>

      <!-- 通用 JSON 视图 -->
      <el-tab-pane label="图形数据" name="graph">
        <JsonViewer :data="graphData" />
        <div v-if="!hasGraph" class="muted" style="margin-top:8px">
          当前工作流未提供图形数据
        </div>
      </el-tab-pane>

      <el-tab-pane label="JSON 配置" name="json">
        <JsonViewer :data="parsed.raw ?? parsed" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { ParsedWorkflowContent } from '@/api/template';
import JsonViewer from '@/components/common/JsonViewer.vue';
import TemplatePreviewCanvas from '@/components/template/TemplatePreviewCanvas.vue';
import TemplateNodeList from '@/components/template/TemplateNodeList.vue';
import TemplateConfigPreview from '@/components/template/TemplateConfigPreview.vue';

/**
 * 顶部概览卡所需的最少信息，让调用方按需传入。
 * 若全部为空（meta === null 或 undefined），则不展示 Header。
 */
export interface WorkflowPreviewMeta {
  name?: string;
  description?: string;
  category?: string;
  emoji?: string;
  cover?: string;
  author?: string;
  usageCount?: number;
  createdAt?: string;
  reviewStatus?: string;
}

export interface MetaChip {
  label: string;
  icon?: string;
  kind?: 'default' | 'category' | 'warn' | 'author';
}

const props = defineProps<{
  /**
   * 通用画布数据，可以来自：
   *  - 模板 API 的 workflowJson / workflowGraph
   *  - store.current.{nodes, edges}（从内存工作流构造）
   */
  parsed: ParsedWorkflowContent;
  /**
   * 切换数据时用于触发 ReactFlow 重建的 key
   */
  resetKey?: string | number;
  /** 顶部概览元数据（可选） */
  meta?: WorkflowPreviewMeta | null;
  /** 自定义 chips，覆盖默认生成 */
  chips?: MetaChip[];
  /** 原始 graph 字符串（可选，用于「图形数据」Tab） */
  graphRaw?: string | null;
  /** 画布 Tab 名称，默认「画布」 */
  canvasTabLabel?: string;
  /**
   * 预览画布卸载时是否清空 selection store。
   * 模板市场场景：true（独立预览，关闭后不能影响列表其他卡片）。
   * 编辑器场景：false（预览关闭后不能误清当前工作流的节点选中态）。
   * 默认 true 与模板市场保持一致。
   */
  clearSelectionOnUnmount?: boolean;
}>();

const activeTab = ref('canvas');
watch(
  () => props.resetKey,
  () => {
    activeTab.value = 'canvas';
  },
);

const hasGraph = computed(() => !!props.graphRaw);

const graphData = computed(() => {
  if (!props.graphRaw) return null;
  try {
    return JSON.parse(props.graphRaw);
  } catch {
    return props.graphRaw;
  }
});

const DEFAULT_COVER_GRADIENTS: Record<string, string> = {
  客服: 'linear-gradient(135deg,#818cf8,#c084fc)',
  HR: 'linear-gradient(135deg,#34d399,#10b981)',
  运营: 'linear-gradient(135deg,#fbbf24,#f59e0b)',
  电商: 'linear-gradient(135deg,#f472b6,#ec4899)',
  团队: 'linear-gradient(135deg,#60a5fa,#3b82f6)',
  数据: 'linear-gradient(135deg,#22d3ee,#06b6d4)',
};

const coverStyle = computed(() => {
  const c = props.meta?.cover || props.meta?.category;
  if (c && c.startsWith('linear-gradient')) return { background: c };
  return {
    background: c ? (DEFAULT_COVER_GRADIENTS[c] ?? 'linear-gradient(135deg,#a78bfa,#6366f1)') : '#6366f1',
  };
});

const defaultChips = computed<MetaChip[]>(() => {
  const m = props.meta;
  const chips: MetaChip[] = [];
  if (m?.category) chips.push({ label: m.category, icon: 'fa-solid fa-folder-open', kind: 'category' });
  if (m?.author)   chips.push({ label: m.author,   icon: 'fa-solid fa-user' });
  if (typeof m?.usageCount === 'number')
    chips.push({ label: `${m.usageCount} 次使用`, icon: 'fa-solid fa-eye' });
  if (m?.createdAt) chips.push({ label: `创建于 ${m.createdAt}`, icon: 'fa-solid fa-calendar' });
  if (m?.reviewStatus && m.reviewStatus !== 'approved')
    chips.push({ label: reviewLabel(m.reviewStatus), icon: 'fa-solid fa-circle-info', kind: 'warn' });
  return chips;
});

const chips = computed<MetaChip[]>(() => {
  if (props.chips?.length) return props.chips;
  return defaultChips.value;
});

function reviewLabel(s: string): string {
  switch (s) {
    case 'pending': return '审核中，暂不可使用';
    case 'rejected': return '审核未通过';
    default: return s;
  }
}

function nodeName(id: string): string {
  const found = props.parsed.nodes.find((n) => n.id === id);
  return found?.name ?? id;
}
</script>

<style scoped>
.workflow-preview {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.preview-header {
  display: flex;
  gap: 14px;
  align-items: stretch;
}
.preview-header .cover {
  width: 96px;
  height: 96px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}
.preview-header .cover .emoji { font-size: 40px; }
.preview-header .cover .fallback { font-size: 32px; color: #fff; opacity: 0.85; }
.preview-header .meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.preview-header .name {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
}
.preview-header .desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}
.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  padding: 3px 8px;
  border-radius: 999px;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meta-chip.category {
  background: var(--primary-bg);
  color: var(--primary);
}
.meta-chip.warn {
  background: var(--warning-bg);
  color: var(--warning);
}
.meta-chip.author {
  background: #f1f5f9;
  color: #475569;
}

.edge-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: var(--bg-secondary);
  border-radius: 8px;
  font-size: 12.5px;
}
.edge-item .edge-source,
.edge-item .edge-target {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: var(--text-primary);
  background: var(--bg-primary);
  padding: 3px 10px;
  border-radius: 6px;
  border: 1px solid var(--border-light);
}
.edge-item .edge-arrow { color: var(--primary); }
.edge-item .edge-label {
  font-size: 11px;
  color: var(--text-tertiary);
  background: var(--primary-bg);
  padding: 1px 8px;
  border-radius: 4px;
}

.edges-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow: auto;
}

.empty-block {
  padding: 24px;
  text-align: center;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.muted { color: var(--text-tertiary); }
</style>

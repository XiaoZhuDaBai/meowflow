<template>
  <div class="block-selector">
    <!-- 顶部搜索框 -->
    <div class="selector-header">
      <el-input
        v-model="searchText"
        placeholder="搜索节点"
        clearable
        size="small"
      >
        <template #prefix>
          <i class="fa-solid fa-magnifying-glass" />
        </template>
      </el-input>
    </div>

    <!-- 标签页 -->
    <div class="selector-tabs">
      <div
        v-for="tab in availableTabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 节点列表 -->
    <div class="selector-body">
      <!-- 注释: 固定项,作为画布注释,搜索时不参与过滤 -->
      <div v-if="!searchText.trim()" class="group group-note">
        <div class="group-label">
          <i class="fa-solid fa-note-sticky" />
          注释
        </div>
        <div class="group-items">
          <div
            class="block-item note-item"
            :draggable="true"
            @dragstart="onNoteDragStart($event)"
            @click="onNoteClick"
            @mouseenter="hoveredNode = NOTE_ITEM.type"
            @mouseleave="hoveredNode = null"
          >
            <div class="item-icon note-icon">
              <i :class="NOTE_ITEM.icon" />
            </div>
            <div class="item-info">
              <div class="item-name">{{ NOTE_ITEM.name }}</div>
              <div class="item-desc">{{ NOTE_ITEM.description }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="groupedNodes.length === 0 && searchText.trim()" class="empty">
        <i class="fa-solid fa-inbox" />
        <span>没有匹配的节点</span>
      </div>

      <div v-for="group in groupedNodes" :key="group.label" class="group">
        <div class="group-label">{{ group.label }}</div>
        <div class="group-items">
          <div
            v-for="def in group.items"
            :key="def.type"
            class="block-item"
            :draggable="true"
            :class="{ 'is-featured': isFeatured(def) }"
            @click="onClick(def)"
            @dragstart="onDragStart($event, def)"
            @mouseenter="hoveredNode = def.type"
            @mouseleave="hoveredNode = null"
          >
            <div class="item-icon" :style="{ background: def.color, color: '#fff' }">
              <i :class="def.icon" />
            </div>
            <div class="item-info">
              <div class="item-name">
                {{ def.name }}
                <el-tag
                  v-if="isFeatured(def)"
                  size="small"
                  type="warning"
                  effect="plain"
                  class="featured-tag"
                >推荐</el-tag>
              </div>
              <div class="item-desc">{{ def.description }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 节点预览卡片 -->
    <PreviewCard
      :visible="hoveredNode !== null"
      :node="hoveredDefinition"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElInput, ElTag } from 'element-plus';
import { NODE_CATALOG } from '@/mock/nodes';
import type { NodeDefinition, BlockClassification } from '@/types/node';
import PreviewCard from './PreviewCard.vue';

/** Note (canvas comment) definition. Not a workflow node — lives outside
 * NODE_CATALOG so it's never executed, only used as a UI affordance.
 *
 * We use `type: 'note'` and a custom `noteColor` field that the drop
 * handler reads to set the sticky-note background.
 */
const NOTE_ITEM = {
  type: 'note',
  name: '注释',
  icon: 'fa-note-sticky',
  color: '#fff8c5',
  description: '画布注释,用于标记 / 分组 / 注释说明',
};

const props = defineProps<{
  // 可用节点类型过滤 (空 = 全部可用)
  availableBlocksTypes?: string[];
  // 显示在顶部的推荐节点
  featuredTypes?: string[];
}>();

const emit = defineEmits<{
  (e: 'select', def: NodeDefinition): void;
}>();

const searchText = ref('');
const activeTab = ref('all');
const hoveredNode = ref<string | null>(null);

const BLOCK_TABS: BlockClassification[] = [
  { key: 'all', label: '全部', tabsKey: 'all' },
  { key: 'input', label: '输入', tabsKey: 'input' },
  { key: 'processing', label: '处理', tabsKey: 'processing' },
  { key: 'logic', label: '逻辑', tabsKey: 'logic' },
  { key: 'output', label: '输出', tabsKey: 'output' },
];

const availableTabs = computed(() => BLOCK_TABS);

const hoveredDefinition = computed<NodeDefinition | null>(() => {
  if (!hoveredNode.value) return null;
  return NODE_CATALOG.find((n) => n.type === hoveredNode.value) || null;
});

const filteredNodes = computed<NodeDefinition[]>(() => {
  const text = searchText.value.trim().toLowerCase();
  return NODE_CATALOG.filter((node) => {
    if (props.availableBlocksTypes && props.availableBlocksTypes.length > 0
      && !props.availableBlocksTypes.includes(node.type)) {
      return false;
    }
    if (activeTab.value !== 'all' && node.classification !== activeTab.value) {
      return false;
    }
    if (text) {
      const haystack = `${node.name} ${node.description} ${node.type}`.toLowerCase();
      if (!haystack.includes(text)) return false;
    }
    return true;
  });
});

const groupedNodes = computed(() => {
  const groups: Record<string, { label: string; color: string; items: NodeDefinition[] }> = {};
  const categoryLabels: Record<string, { label: string; color: string }> = {
    trigger: { label: '触发器', color: '#8b5cf6' },
    ai: { label: 'AI', color: '#3b82f6' },
    flow: { label: '流程', color: '#10b981' },
    tool: { label: '工具', color: '#f59e0b' },
    notify: { label: '通知', color: '#ec4899' },
  };

  // 按 sort 排序
  const sorted = [...filteredNodes.value].sort((a, b) => {
    const sa = a.sort ?? 999;
    const sb = b.sort ?? 999;
    return sa - sb;
  });

  for (const node of sorted) {
    if (!groups[node.category]) {
      groups[node.category] = {
        label: categoryLabels[node.category]?.label || node.category,
        color: categoryLabels[node.category]?.color || '#64748b',
        items: [],
      };
    }
    groups[node.category].items.push(node);
  }

  return Object.entries(groups).map(([key, value]) => ({
    key,
    ...value,
  }));
});

function isFeatured(def: NodeDefinition): boolean {
  return props.featuredTypes?.includes(def.type) ?? false;
}

function onClick(def: NodeDefinition) {
  emit('select', def);
}

function onNoteClick() {
  // Click also adds a note (use default anchor; findEmptyPosition on Editor side).
  emit('select', NOTE_ITEM as unknown as NodeDefinition);
}

function onNoteDragStart(e: DragEvent) {
  if (!e.dataTransfer) return;
  // Same payload shape as onDragStart — the drop handler reads `type === 'note'`
  // to know to route through addNote instead of addNode.
  e.dataTransfer.setData(
    'application/x-meowflow-node',
    JSON.stringify({
      type: NOTE_ITEM.type,
      name: NOTE_ITEM.name,
      category: 'flow',
      color: NOTE_ITEM.color,
      icon: NOTE_ITEM.icon,
      classification: 'logic',
    }),
  );
  e.dataTransfer.setData('text/plain', NOTE_ITEM.name);
  e.dataTransfer.effectAllowed = 'copy';

  // Custom drag image for visual feedback.
  const dragImage = document.createElement('div');
  dragImage.className = 'drag-preview';
  dragImage.style.cssText = `
    position: fixed; top: -100px; left: -100px;
    padding: 8px 12px; background: ${NOTE_ITEM.color}; color: #92400e;
    border-radius: 8px; font-size: 12px; font-weight: 500;
    display: flex; align-items: center; gap: 6px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.2);
    border: 1px solid rgba(146, 64, 14, 0.3);
  `;
  dragImage.innerHTML = `<i class="fa-solid ${NOTE_ITEM.icon}"></i> ${NOTE_ITEM.name}`;
  document.body.appendChild(dragImage);
  e.dataTransfer.setDragImage(dragImage, 20, 20);
  setTimeout(() => document.body.removeChild(dragImage), 0);
}

function onDragStart(e: DragEvent, def: NodeDefinition) {
  if (!e.dataTransfer) return;
  // Serialize the full def so the drop handler can render the node without
  // a second lookup. The payload is small (a few strings/numbers).
  e.dataTransfer.setData(
    'application/x-meowflow-node',
    JSON.stringify({
      type: def.type,
      category: def.category,
      name: def.name,
      color: def.color,
      icon: def.icon,
      classification: def.classification,
    }),
  );
  e.dataTransfer.setData('text/plain', def.name);
  e.dataTransfer.effectAllowed = 'copy';

  // 自定义拖拽预览
  const dragImage = document.createElement('div');
  dragImage.className = 'drag-preview';
  dragImage.style.cssText = `
    position: fixed; top: -100px; left: -100px;
    padding: 8px 12px; background: ${def.color}; color: #fff;
    border-radius: 8px; font-size: 12px; font-weight: 500;
    display: flex; align-items: center; gap: 6px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  `;
  dragImage.innerHTML = `<i class="fa-solid ${def.icon}"></i> ${def.name}`;
  document.body.appendChild(dragImage);
  e.dataTransfer.setDragImage(dragImage, 20, 20);
  setTimeout(() => document.body.removeChild(dragImage), 0);
}

watch(searchText, () => {
  // 重置到第一个 tab 以避免过滤后空结果
});
</script>

<style scoped>
.block-selector {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: #fff;
  overflow: hidden;
}

.selector-header {
  padding: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.selector-tabs {
  display: flex;
  padding: 8px 12px 0;
  border-bottom: 1px solid #f1f5f9;
  gap: 2px;
  flex-shrink: 0;
}

.tab-item {
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  border-radius: 6px 6px 0 0;
  transition: all 0.15s;
  white-space: nowrap;
}
.tab-item:hover {
  background: #f8fafc;
  color: #1e293b;
}
.tab-item.active {
  background: #f1f5f9;
  color: #6366f1;
  font-weight: 600;
}

.selector-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: #94a3b8;
  font-size: 13px;
  gap: 8px;
}
.empty i {
  font-size: 24px;
  color: #cbd5e1;
}

.group {
  margin-bottom: 12px;
}

.group-label {
  padding: 6px 8px;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.group-label i {
  margin-right: 4px;
}
.group-note .group-label {
  color: #b45309;
}

.group-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.block-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.15s;
  user-select: none;
}
.block-item:hover {
  background: #f8fafc;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.block-item:active {
  cursor: grabbing;
}
.block-item.is-featured {
  background: linear-gradient(135deg, #fef3c7 0%, transparent 100%);
}

.item-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}
.note-icon {
  background: linear-gradient(135deg, #fff8c5 0%, #fde68a 100%);
  color: #92400e;
  border: 1px solid rgba(146, 64, 14, 0.2);
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-name {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.featured-tag {
  font-size: 10px !important;
  height: 18px !important;
  padding: 0 4px !important;
}

.item-desc {
  font-size: 11px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

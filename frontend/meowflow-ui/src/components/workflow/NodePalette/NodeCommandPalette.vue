<script setup lang="ts">
/**
 * 节点命令面板 (NodeCommandPalette)
 *
 * 对标 Coze Studio / Dify 的 Ctrl+K 命令面板:
 * - 顶部搜索框
 * - 节点列表按相关度排序
 * - 支持键盘导航 (↑↓ Enter Esc)
 * - 显示节点所属分类、图标、描述
 * - 选中后通过事件回调插入到画布
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { ElIcon } from 'element-plus';
import { NODE_CATALOG } from '@/mock/nodes';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'select', nodeType: string): void;
}>();

const searchText = ref('');
const inputRef = ref<HTMLInputElement | null>(null);
const activeIndex = ref(0);

interface SearchItem {
  type: string;
  name: string;
  category: string;
  categoryLabel: string;
  icon: string;
  color: string;
  description: string;
  matchedFields: string[];
}

const CATEGORY_LABEL: Record<string, string> = {
  trigger: '触发器',
  ai: 'AI',
  flow: '逻辑控制',
  tool: '工具',
  notify: '通知',
  transform: '转换',
  control: '控制',
  end: '结束',
};

const items = computed<SearchItem[]>(() => {
  const q = searchText.value.trim().toLowerCase();
  const results: { item: SearchItem; score: number }[] = [];

  for (const node of NODE_CATALOG) {
    const item: SearchItem = {
      type: node.type,
      name: node.name,
      category: node.category,
      categoryLabel: CATEGORY_LABEL[node.category] || node.category,
      icon: node.icon,
      color: node.color,
      description: node.description,
      matchedFields: [],
    };

    let score = 0;
    if (!q) {
      score = 100;
    } else {
      if (node.name.toLowerCase().includes(q)) {
        score += 80;
        item.matchedFields.push('name');
      }
      if (node.type.toLowerCase().includes(q)) {
        score += 50;
        item.matchedFields.push('type');
      }
      if (node.description && node.description.toLowerCase().includes(q)) {
        score += 30;
        item.matchedFields.push('description');
      }
      if (CATEGORY_LABEL[node.category]?.toLowerCase().includes(q)) {
        score += 20;
        item.matchedFields.push('category');
      }
      // 模糊匹配每个字
      const chars = q.split('').filter(c => c.trim());
      for (const c of chars) {
        if (node.name.toLowerCase().includes(c)) score += 5;
      }
    }

    if (score > 0) {
      results.push({ item, score });
    }
  }

  results.sort((a, b) => b.score - a.score);
  return results.slice(0, 50).map(r => r.item);
});

watch(() => props.modelValue, (open) => {
  if (open) {
    searchText.value = '';
    activeIndex.value = 0;
    nextTick(() => inputRef.value?.focus());
  }
});

function handleKeydown(e: KeyboardEvent) {
  if (!props.modelValue) return;
  if (e.key === 'ArrowDown') {
    e.preventDefault();
    activeIndex.value = Math.min(activeIndex.value + 1, items.value.length - 1);
  } else if (e.key === 'ArrowUp') {
    e.preventDefault();
    activeIndex.value = Math.max(activeIndex.value - 1, 0);
  } else if (e.key === 'Enter') {
    e.preventDefault();
    selectActive();
  } else if (e.key === 'Escape') {
    e.preventDefault();
    close();
  }
}

function selectActive() {
  const item = items.value[activeIndex.value];
  if (item) {
    emit('select', item.type);
    close();
  }
}

function close() {
  emit('update:modelValue', false);
}

function highlight(text: string): string {
  const q = searchText.value.trim();
  if (!q) return text;
  const idx = text.toLowerCase().indexOf(q.toLowerCase());
  if (idx === -1) return text;
  return (
    text.slice(0, idx)
    + `<mark>${text.slice(idx, idx + q.length)}</mark>`
    + text.slice(idx + q.length)
  );
}

function categoryColor(category: string): string {
  const map: Record<string, string> = {
    trigger: '#8b5cf6',
    ai: '#6366f1',
    flow: '#0ea5e9',
    tool: '#10b981',
    notify: '#f59e0b',
    transform: '#ec4899',
    control: '#64748b',
    end: '#dc2626',
  };
  return map[category] || '#64748b';
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
});
</script>

<template>
  <Teleport to="body">
    <Transition name="command-fade">
      <div v-if="modelValue" class="command-overlay" @click.self="close">
        <div class="command-panel">
          <!-- 搜索框 -->
          <div class="search-bar">
            <i class="fa-solid fa-magnifying-glass"></i>
            <input
              ref="inputRef"
              v-model="searchText"
              placeholder="搜索节点 (Ctrl+K) — 试试 'LLM'、'HTTP'、'知识库'…"
              @input="activeIndex = 0"
            />
            <kbd>ESC</kbd>
          </div>

          <!-- 结果列表 -->
          <div class="result-list">
            <div
              v-for="(item, idx) in items"
              :key="item.type"
              class="result-item"
              :class="{ active: idx === activeIndex }"
              @mouseenter="activeIndex = idx"
              @click="selectActive"
            >
              <div
                class="node-icon"
                :style="{ background: item.color + '20', color: item.color }"
              >
                <i :class="`fa-solid ${item.icon}`"></i>
              </div>
              <div class="node-info">
                <div class="node-name" v-html="highlight(item.name)"></div>
                <div class="node-desc">{{ item.description }}</div>
              </div>
              <div class="node-cat" :style="{ color: categoryColor(item.category) }">
                {{ item.categoryLabel }}
              </div>
            </div>
            <div v-if="items.length === 0" class="no-results">
              <i class="fa-solid fa-circle-question"></i>
              <p>未找到匹配的节点</p>
              <p class="hint">试试搜索：LLM、知识库、HTTP、定时、分类、循环</p>
            </div>
          </div>

          <!-- 底部快捷键提示 -->
          <div class="footer">
            <span><kbd>↑↓</kbd> 选择</span>
            <span><kbd>↵</kbd> 插入</span>
            <span><kbd>ESC</kbd> 关闭</span>
            <span class="count">{{ items.length }} 个结果</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.command-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 12vh;
  z-index: 9999;
}

.command-panel {
  width: 640px;
  max-width: 90vw;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 70vh;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.search-bar i {
  color: #94a3b8;
  font-size: 16px;
}

.search-bar input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: #1e293b;
}

.search-bar input::placeholder {
  color: #94a3b8;
}

.search-bar kbd {
  font-size: 11px;
  padding: 2px 8px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  color: #64748b;
  font-family: monospace;
}

.result-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.1s;
}

.result-item.active {
  background: #eff6ff;
}

.result-item:hover {
  background: #f8fafc;
}

.node-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.node-info {
  flex: 1;
  min-width: 0;
}

.node-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.node-name :deep(mark) {
  background: #fef3c7;
  color: #92400e;
  padding: 0 2px;
  border-radius: 2px;
}

.node-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-cat {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(99, 102, 241, 0.08);
  border-radius: 10px;
  font-weight: 500;
  flex-shrink: 0;
}

.no-results {
  padding: 60px 20px;
  text-align: center;
  color: #94a3b8;
}

.no-results i {
  font-size: 32px;
  margin-bottom: 12px;
}

.no-results .hint {
  font-size: 11px;
  color: #cbd5e1;
  margin-top: 4px;
}

.footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 20px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
  font-size: 11px;
  color: #64748b;
}

.footer kbd {
  font-size: 10px;
  padding: 1px 6px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 3px;
  font-family: monospace;
  margin-right: 4px;
}

.footer .count {
  margin-left: auto;
  color: #94a3b8;
}

/* Transitions */
.command-fade-enter-active,
.command-fade-leave-active {
  transition: opacity 0.15s;
}
.command-fade-enter-active .command-panel,
.command-fade-leave-active .command-panel {
  transition: transform 0.15s, opacity 0.15s;
}
.command-fade-enter-from,
.command-fade-leave-to {
  opacity: 0;
}
.command-fade-enter-from .command-panel,
.command-fade-leave-to .command-panel {
  transform: scale(0.96) translateY(-10px);
  opacity: 0;
}
</style>

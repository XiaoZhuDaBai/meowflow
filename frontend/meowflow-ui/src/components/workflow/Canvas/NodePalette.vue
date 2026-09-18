<template>
  <div class="palette-sidebar" :class="{ collapsed }">
    <button class="toggle-btn" @click.stop="emit('toggle-collapse')" :title="collapsed ? '展开节点' : '收起'">
      <i :class="collapsed ? 'fa-solid fa-chevron-right' : 'fa-solid fa-chevron-left'" />
    </button>

    <Transition name="panel">
      <div v-if="!collapsed" class="palette-panel">
        <div class="palette-header">
          <span>{{ activeTab === 'snippets' ? '模板片段' : '节点组件' }}</span>
          <span class="header-shortcut">B</span>
        </div>
        <div class="palette-tabs">
          <div
            class="palette-tab"
            :class="{ active: activeTab === 'blocks' }"
            @click="activeTab = 'blocks'"
          >
            <i class="fa-solid fa-cubes" />
            节点
          </div>
          <div
            class="palette-tab"
            :class="{ active: activeTab === 'snippets' }"
            @click="activeTab = 'snippets'"
          >
            <i class="fa-solid fa-shapes" />
            模板
          </div>
        </div>
        <div class="palette-body">
          <BlockSelector
            v-if="activeTab === 'blocks'"
            :featured-types="featuredTypes"
            @select="(def: NodeDefinition) => emit('select', def)"
          />
          <SnippetSelector
            v-else
            @insert="onInsertSnippet"
          />
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import BlockSelector from '@/components/workflow/selector/BlockSelector.vue';
import SnippetSelector from '@/components/workflow/snippets/SnippetSelector.vue';
import type { NodeDefinition } from '@/types/node';
import type { Snippet } from '@/mock/snippets';
import { useInsertSnippet } from '@/composables/useInsertSnippet';
import { ElMessage } from '@/utils/notify';

defineProps<{
  collapsed?: boolean;
  featuredTypes?: string[];
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'toggle-collapse'): void;
  (e: 'select', def: NodeDefinition): void;
  (e: 'insertSnippet', snippet: Snippet): void;
}>();

const activeTab = ref<'blocks' | 'snippets'>('blocks');
const { insert } = useInsertSnippet();

function onInsertSnippet(snippet: Snippet) {
  // 让 store 自己计算合适的偏移
  const result = insert(snippet);
  ElMessage.success(`已插入片段「${snippet.name}」(共 ${result.insertedNodeIds.length} 个节点)`);
  emit('insertSnippet', snippet);
}
</script>

<style scoped>
.palette-sidebar {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 30;
  display: flex;
  align-items: stretch;
  max-height: calc(100% - 32px);
}

.toggle-btn {
  width: 24px;
  height: 48px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-left: none;
  border-radius: 0 8px 8px 0;
  cursor: pointer;
  color: #64748b;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.06);
}
.toggle-btn:hover {
  background: #f8fafc;
  color: #6366f1;
}

.palette-panel {
  width: 280px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-left: none;
  border-radius: 0 12px 12px 0;
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 600px;
  height: 80vh;
}

.palette-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  border-bottom: 1px solid #f1f5f9;
}

.header-shortcut {
  font-size: 10px;
  padding: 2px 6px;
  background: #f1f5f9;
  border-radius: 4px;
  color: #64748b;
  font-family: monospace;
}

.palette-tabs {
  display: flex;
  padding: 0 8px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
  background: #f8fafc;
}

.palette-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 4px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
  border-bottom: 2px solid transparent;
}
.palette-tab i {
  font-size: 11px;
}
.palette-tab:hover {
  color: #6366f1;
}
.palette-tab.active {
  color: #6366f1;
  border-bottom-color: #6366f1;
  background: #fff;
  font-weight: 600;
}

.palette-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-enter-active,
.panel-leave-active {
  transition: all 0.2s ease;
  transform-origin: left center;
}
.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: scaleX(0.6) translateY(-50%);
}
</style>

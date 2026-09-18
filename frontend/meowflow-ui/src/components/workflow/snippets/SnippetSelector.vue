<template>
  <div class="snippet-selector">
    <div class="snippet-header">
      <el-input
        v-model="searchText"
        placeholder="搜索片段"
        clearable
        size="small"
      >
        <template #prefix>
          <i class="fa-solid fa-magnifying-glass" />
        </template>
      </el-input>
    </div>

    <div class="snippet-body">
      <div v-if="filteredSnippets.length === 0" class="empty">
        <i class="fa-solid fa-inbox" />
        <span>没有匹配的片段</span>
      </div>

      <div
        v-for="snippet in filteredSnippets"
        :key="snippet.id"
        class="snippet-card"
        @click="emit('insert', snippet)"
      >
        <div class="snippet-icon">
          <i :class="snippet.icon" />
        </div>
        <div class="snippet-info">
          <div class="snippet-name">
            {{ snippet.name }}
            <el-tag v-if="snippet.isOfficial" size="small" type="success">官方</el-tag>
          </div>
          <div class="snippet-desc">{{ snippet.description }}</div>
          <div class="snippet-tags">
            <el-tag
              v-for="tag in snippet.tags"
              :key="tag"
              size="small"
              effect="plain"
              class="tag"
            >
              {{ tag }}
            </el-tag>
            <span class="snippet-count">{{ snippet.nodes.length }} 节点</span>
          </div>
        </div>
        <div class="snippet-action">
          <i class="fa-solid fa-plus" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElInput, ElTag } from 'element-plus';
import { BUILTIN_SNIPPETS } from '@/mock/snippets';
import type { Snippet } from '@/mock/snippets';

const props = defineProps<{
  snippets?: Snippet[];
}>();

const emit = defineEmits<{
  (e: 'insert', snippet: Snippet): void;
}>();

const searchText = ref('');

const allSnippets = computed(() => props.snippets || BUILTIN_SNIPPETS);

const filteredSnippets = computed(() => {
  const text = searchText.value.trim().toLowerCase();
  if (!text) return allSnippets.value;
  return allSnippets.value.filter((s) => {
    const haystack = `${s.name} ${s.description} ${s.tags.join(' ')}`.toLowerCase();
    return haystack.includes(text);
  });
});
</script>

<style scoped>
.snippet-selector {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  overflow: hidden;
}

.snippet-header {
  padding: 12px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.snippet-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  color: #94a3b8;
  gap: 8px;
  font-size: 13px;
}
.empty i {
  font-size: 24px;
  color: #cbd5e1;
}

.snippet-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.snippet-card:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99,102,241,0.1);
  transform: translateY(-1px);
}

.snippet-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.snippet-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.snippet-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.snippet-desc {
  font-size: 11px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.snippet-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.tag {
  font-size: 10px !important;
  height: 18px !important;
  padding: 0 4px !important;
}

.snippet-count {
  font-size: 10px;
  color: #94a3b8;
  margin-left: 4px;
}

.snippet-action {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  transition: all 0.15s;
  flex-shrink: 0;
}

.snippet-card:hover .snippet-action {
  background: #6366f1;
  color: #fff;
}
</style>

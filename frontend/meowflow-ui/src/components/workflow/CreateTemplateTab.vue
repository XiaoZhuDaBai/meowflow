<template>
  <div class="create-template-tab">
    <!-- 搜索 -->
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索模板..." clearable style="width: 280px">
        <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
      </el-input>
    </div>

    <!-- 分类标签 -->
    <div class="category-chips">
      <span
        v-for="c in categoryChips"
        :key="c.value"
        class="chip"
        :class="{ active: category === c.value }"
        @click="category = c.value"
      >
        {{ c.label }}
      </span>
    </div>

    <!-- 模板网格 -->
    <div class="tpl-grid">
      <div
        v-for="t in filtered"
        :key="t.id"
        class="tpl-card"
        :class="{ selected: selectedId === t.id }"
        @click="selectedId = selectedId === t.id ? '' : t.id"
      >
        <div class="tpl-cover" :style="{ background: coverGradient(t.category) }">
          <div class="emoji">{{ t.coverEmoji }}</div>
          <span v-if="t.isOfficial" class="official-tag">官方</span>
          <div v-if="selectedId === t.id" class="selected-badge">
            <i class="fa-solid fa-check"></i>
          </div>
        </div>
        <div class="tpl-body">
          <div class="tpl-name">{{ t.name }}</div>
          <div class="tpl-desc">{{ t.description }}</div>
          <div class="tpl-tags">
            <span class="tag" v-for="tag in (t.tags ?? []).slice(0, 2)" :key="tag"># {{ tag }}</span>
          </div>
          <div class="tpl-meta">
            <span><i class="fa-solid fa-fire"></i> {{ formatNumber(t.usageCount) }}</span>
            <span><i class="fa-solid fa-star"></i> {{ t.rating.toFixed(1) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="filtered.length === 0" class="empty-tip">
      <i class="fa-solid fa-inbox"></i>
      <span>暂无模板</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { Template } from '@/api/types';

const props = defineProps<{
  templates: Template[];
}>();

const emit = defineEmits<{
  (e: 'submit', data: { id: string; name: string }): void;
}>();

const keyword = ref('');
const category = ref('all');
const selectedId = ref('');

const CATEGORY_COVER: Record<string, string> = {
  客服: 'linear-gradient(135deg,#818cf8,#c084fc)',
  HR: 'linear-gradient(135deg,#34d399,#10b981)',
  运营: 'linear-gradient(135deg,#fbbf24,#f59e0b)',
  电商: 'linear-gradient(135deg,#f472b6,#ec4899)',
  团队: 'linear-gradient(135deg,#60a5fa,#3b82f6)',
  数据: 'linear-gradient(135deg,#22d3ee,#06b6d4)',
};

const categoryChips = computed(() => {
  const cats = Array.from(new Set(props.templates.map((t) => t.category)));
  return [
    { label: '全部', value: 'all' },
    ...cats.map((c) => ({ label: c, value: c })),
  ];
});

const filtered = computed(() => {
  let list = props.templates;
  if (category.value !== 'all') list = list.filter((t) => t.category === category.value);
  if (keyword.value) {
    const k = keyword.value.toLowerCase();
    list = list.filter(
      (t) => t.name.toLowerCase().includes(k) || t.description.toLowerCase().includes(k),
    );
  }
  return list;
});

function coverGradient(c: string) {
  return CATEGORY_COVER[c] ?? 'linear-gradient(135deg,#a78bfa,#6366f1)';
}

function formatNumber(n: number) {
  return n >= 1000 ? `${(n / 1000).toFixed(1)}k` : String(n);
}

function getSubmitData() {
  if (!selectedId.value) return null;
  const t = props.templates.find((x) => x.id === selectedId.value);
  return t ? { id: t.id, name: t.name } : null;
}

defineExpose({ getSubmitData, selectedId });
</script>

<style scoped>
.create-template-tab {
  padding: 8px 0;
}

.search-bar {
  margin-bottom: 12px;
}

.category-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.chip {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.chip:hover {
  color: var(--primary);
  border-color: var(--primary-light);
}

.chip.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.tpl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;
}

.tpl-card {
  background: var(--bg-primary);
  border: 2px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  flex-direction: column;
}

.tpl-card:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.tpl-card.selected {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-bg);
}

.tpl-cover {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.tpl-cover .emoji {
  font-size: 36px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.15));
}

.official-tag {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(255,255,255,0.85);
  color: var(--primary);
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
  font-weight: 600;
}

.selected-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

.tpl-body {
  padding: 10px 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tpl-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
}

.tpl-desc {
  font-size: 11px;
  color: var(--text-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tpl-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  margin-top: 2px;
}

.tag {
  font-size: 10px;
  color: var(--primary);
  background: var(--primary-bg);
  padding: 1px 5px;
  border-radius: 3px;
}

.tpl-meta {
  display: flex;
  gap: 8px;
  font-size: 10px;
  color: var(--text-tertiary);
  margin-top: auto;
  padding-top: 4px;
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px;
  color: var(--text-tertiary);
  font-size: 13px;
}
</style>

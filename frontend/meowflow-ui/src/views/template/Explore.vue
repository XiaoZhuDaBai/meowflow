<script setup lang="ts">
/**
 * 模板浏览页 (Explore Templates)
 *
 * 参考 Dify /explore 页面设计：
 * - 顶部搜索 + 分类筛选
 * - 主区域：模板卡片网格
 * - 支持难度筛选、模型筛选
 * - 点击卡片进入预览 → 一键使用
 */
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { templateApi } from '@/api/template';
import { useWorkflowStore } from '@/stores/workflow';
import { ElMessage } from '@/utils/notify';
import { ALL_BUILTIN_TEMPLATES } from '@/mock/templates/index';
import { TEMPLATE_CATEGORIES } from '@/mock/templateCategories';
import type { BuiltinTemplate } from '@/mock/builtinTemplates';

const router = useRouter();
const workflowStore = useWorkflowStore();

const searchText = ref('');
const activeCategory = ref<string>('all');
const activeDifficulty = ref<string>('all');

const filteredTemplates = computed(() => {
  let list = ALL_BUILTIN_TEMPLATES;

  // 分类
  if (activeCategory.value !== 'all') {
    list = list.filter(t => t.category === activeCategory.value);
  }

  // 难度
  if (activeDifficulty.value !== 'all') {
    list = list.filter(t => (t as any).difficulty === activeDifficulty.value);
  }

  // 搜索
  const q = searchText.value.trim().toLowerCase();
  if (q) {
    list = list.filter(t =>
      t.name.toLowerCase().includes(q)
      || t.description.toLowerCase().includes(q)
      || (t.tags || []).some(tag => tag.toLowerCase().includes(q))
    );
  }

  return list;
});

const featuredTemplates = computed(() => {
  return ALL_BUILTIN_TEMPLATES
    .slice()
    .sort((a, b) => (b.usageCount || 0) - (a.usageCount || 0))
    .slice(0, 6);
});

function selectCategory(code: string) {
  activeCategory.value = code;
}

async function useTemplate(template: BuiltinTemplate) {
  try {
    await templateApi.use(template.id, template.name);
    const detail = await templateApi.getById(template.id);
    const workflowJson = (detail as any)?.workflowJson || (template as any).workflowJson;
    workflowStore.bootstrapFromTemplate(template.id, template.name, workflowJson);
    router.push('/editor');
  } catch (error: any) {
    ElMessage.error(error?.message || '使用模板失败');
  }
}

function previewTemplate(template: BuiltinTemplate) {
  router.push({
    path: '/templates',
    query: { preview: template.id },
  });
}

onMounted(() => {
  // 自动滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' });
});

function difficultyLabel(d?: string) {
  if (!d) return '';
  return { easy: '简单', medium: '中等', hard: '高级' }[d] || d;
}

function difficultyColor(d?: string) {
  if (!d) return '#94a3b8';
  return { easy: '#10b981', medium: '#f59e0b', hard: '#ef4444' }[d] || '#94a3b8';
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
</script>

<template>
  <div class="explore-page">
    <!-- 头部 -->
    <header class="hero">
      <div class="hero-content">
        <h1>📚 模板市场</h1>
        <p class="hero-subtitle">{{ ALL_BUILTIN_TEMPLATES.length }}+ 行业模板，开箱即用</p>

        <!-- 搜索 -->
        <div class="search-bar">
          <i class="fa-solid fa-magnifying-glass"></i>
          <input
            v-model="searchText"
            placeholder="搜索模板：客服、营销、SQL、OCR…"
            class="search-input"
          />
          <span class="search-count">{{ filteredTemplates.length }} 个结果</span>
        </div>
      </div>
    </header>

    <div class="page-body">
      <!-- 分类侧边栏 -->
      <aside class="sidebar">
        <div class="filter-group">
          <h3 class="filter-title">分类</h3>
          <div class="filter-list">
            <button
              class="filter-item"
              :class="{ active: activeCategory === 'all' }"
              @click="selectCategory('all')"
            >
              <span class="emoji">🌟</span>
              <span>全部</span>
              <span class="count">{{ ALL_BUILTIN_TEMPLATES.length }}</span>
            </button>
            <button
              v-for="cat in TEMPLATE_CATEGORIES"
              :key="cat.code"
              class="filter-item"
              :class="{ active: activeCategory === cat.code }"
              @click="selectCategory(cat.code)"
            >
              <span class="emoji">{{ cat.emoji }}</span>
              <span>{{ cat.name }}</span>
              <span class="count">
                {{ ALL_BUILTIN_TEMPLATES.filter(t => t.category === cat.code).length }}
              </span>
            </button>
          </div>
        </div>

        <div class="filter-group">
          <h3 class="filter-title">难度</h3>
          <div class="difficulty-list">
            <button
              class="diff-item"
              :class="{ active: activeDifficulty === 'all' }"
              @click="activeDifficulty = 'all'"
            >
              全部
            </button>
            <button
              v-for="d in ['easy', 'medium', 'hard']"
              :key="d"
              class="diff-item"
              :class="{ active: activeDifficulty === d }"
              @click="activeDifficulty = d"
              :style="{ '--diff-color': difficultyColor(d) }"
            >
              {{ difficultyLabel(d) }}
            </button>
          </div>
        </div>
      </aside>

      <!-- 主内容 -->
      <main class="main-content">
        <!-- 精选模板 (仅在全部 + 无搜索时显示) -->
        <section v-if="activeCategory === 'all' && !searchText" class="featured-section">
          <h2 class="section-title">
            <i class="fa-solid fa-fire" style="color: #f59e0b"></i>
            热门模板
          </h2>
          <div class="featured-grid">
            <div
              v-for="tpl in featuredTemplates"
              :key="tpl.id"
              class="featured-card"
              @click="previewTemplate(tpl)"
            >
              <div class="featured-cover" :style="{ background: 'linear-gradient(135deg, #6366f1, #8b5cf6)' }">
                <span class="cover-emoji">{{ tpl.coverEmoji }}</span>
              </div>
              <div class="featured-info">
                <div class="featured-name">{{ tpl.name }}</div>
                <div class="featured-meta">
                  <span>⭐ {{ tpl.rating }}</span>
                  <span>📊 {{ tpl.usageCount }} 次使用</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 模板列表 -->
        <section class="list-section">
          <h2 class="section-title">
            {{ activeCategory === 'all' ? '所有模板' : TEMPLATE_CATEGORIES.find(c => c.code === activeCategory)?.name }}
            <span class="count-badge">{{ filteredTemplates.length }}</span>
          </h2>

          <div v-if="filteredTemplates.length === 0" class="empty">
            <i class="fa-solid fa-inbox"></i>
            <p>没有找到匹配的模板</p>
            <p class="hint">试试其他关键词或分类</p>
          </div>

          <div v-else class="template-grid">
            <article
              v-for="tpl in filteredTemplates"
              :key="tpl.id"
              class="template-card"
            >
              <!-- 卡片头部 -->
              <div class="card-header">
                <span class="cover-emoji">{{ tpl.coverEmoji }}</span>
                <div class="card-tags">
                  <span
                    v-if="(tpl as any).difficulty"
                    class="difficulty-tag"
                    :style="{ background: difficultyColor((tpl as any).difficulty) + '20', color: difficultyColor((tpl as any).difficulty) }"
                  >
                    {{ difficultyLabel((tpl as any).difficulty) }}
                  </span>
                  <span class="official-tag">官方</span>
                </div>
              </div>

              <!-- 卡片主体 -->
              <h3 class="card-name" v-html="highlight(tpl.name)"></h3>
              <p class="card-desc">{{ tpl.description }}</p>

              <!-- 标签 -->
              <div class="tag-list">
                <span v-for="tag in (tpl.tags || []).slice(0, 4)" :key="tag" class="tag">
                  {{ tag }}
                </span>
              </div>

              <!-- 卡片底部 -->
              <div class="card-footer">
                <div class="card-stats">
                  <span>⭐ {{ tpl.rating }}</span>
                  <span>📊 {{ tpl.usageCount }}</span>
                  <span v-if="(tpl as any).estimatedSetupMinutes">
                    ⏱️ {{ (tpl as any).estimatedSetupMinutes }}min
                  </span>
                </div>
                <div class="card-actions">
                  <button class="btn-preview" @click="previewTemplate(tpl)">
                    预览
                  </button>
                  <button class="btn-use" @click="useTemplate(tpl)">
                    使用 →
                  </button>
                </div>
              </div>
            </article>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.explore-page {
  min-height: 100vh;
  background: #f8fafc;
}

/* Hero */
.hero {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%);
  padding: 48px 32px 56px;
  color: #fff;
}

.hero-content {
  max-width: 1200px;
  margin: 0 auto;
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: 700;
}

.hero-subtitle {
  margin: 0 0 24px;
  opacity: 0.9;
  font-size: 16px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.15);
  max-width: 720px;
  height: 52px;
}

.search-bar i {
  color: #94a3b8;
  font-size: 16px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  background: transparent;
  color: #1e293b;
}

.search-count {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 10px;
  border-radius: 10px;
}

/* Page body */
.page-body {
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  gap: 24px;
  padding: 32px;
}

/* Sidebar */
.sidebar {
  width: 240px;
  flex-shrink: 0;
}

.filter-group {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.filter-title {
  margin: 0 0 12px;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.filter-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #475569;
  transition: all 0.15s;
  text-align: left;
}

.filter-item:hover {
  background: #f1f5f9;
}

.filter-item.active {
  background: #eef2ff;
  color: #6366f1;
  font-weight: 600;
}

.filter-item .emoji {
  font-size: 16px;
}

.filter-item .count {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 8px;
}

.difficulty-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.diff-item {
  padding: 4px 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  color: #64748b;
}

.diff-item.active {
  background: var(--diff-color, #6366f1);
  color: #fff;
  border-color: var(--diff-color, #6366f1);
}

/* Main */
.main-content {
  flex: 1;
  min-width: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.count-badge {
  font-size: 12px;
  background: #eef2ff;
  color: #6366f1;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}

/* Featured */
.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 32px;
}

.featured-card {
  display: flex;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  transition: all 0.2s;
}

.featured-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.featured-cover {
  width: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cover-emoji {
  font-size: 36px;
}

.featured-info {
  padding: 12px;
  flex: 1;
  min-width: 0;
}

.featured-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.featured-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #94a3b8;
}

/* Template Grid */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.template-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
}

.template-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.card-tags {
  display: flex;
  gap: 4px;
}

.difficulty-tag,
.official-tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.official-tag {
  background: #fef3c7;
  color: #92400e;
}

.card-name {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.4;
}

.card-name :deep(mark) {
  background: #fef3c7;
  color: #92400e;
  padding: 0 2px;
  border-radius: 2px;
}

.card-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  flex: 1;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 12px;
}

.tag {
  font-size: 11px;
  padding: 2px 8px;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 10px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f5f9;
  padding-top: 12px;
}

.card-stats {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #94a3b8;
}

.card-actions {
  display: flex;
  gap: 6px;
}

.btn-preview,
.btn-use {
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px solid;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-preview {
  background: #fff;
  border-color: #e2e8f0;
  color: #64748b;
}

.btn-preview:hover {
  border-color: #94a3b8;
}

.btn-use {
  background: #6366f1;
  border-color: #6366f1;
  color: #fff;
  font-weight: 500;
}

.btn-use:hover {
  background: #4f46e5;
}

.empty {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty i {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty .hint {
  font-size: 12px;
  color: #cbd5e1;
  margin-top: 4px;
}
</style>

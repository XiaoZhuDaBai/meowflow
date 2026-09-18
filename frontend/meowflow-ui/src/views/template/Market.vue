<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">模板市场</div>
        <div class="page-subtitle">从行业模板一键创建工作流 · 共 {{ filtered.length }} 个模板</div>
      </div>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索模板" clearable style="width: 280px">
        <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
      </el-input>
      <div class="flex gap-8" style="flex-wrap: wrap;">
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
    </div>

    <div class="tpl-grid">
      <div v-for="t in filtered" :key="t.id" class="tpl-card">
        <div class="tpl-cover" :style="{ background: coverGradient(t.category) }">
          <div class="emoji">{{ t.coverEmoji }}</div>
          <span v-if="t.isOfficial" class="official-tag">官方</span>
        </div>
        <div class="tpl-body">
          <div class="tpl-name">{{ t.name }}</div>
          <div class="tpl-desc">{{ t.description }}</div>
          <div class="tpl-tags">
            <span class="tag" v-for="tag in t.tags" :key="tag"># {{ tag }}</span>
          </div>
          <div class="tpl-meta">
            <span><i class="fa-solid fa-fire"></i> {{ formatNumber(t.usageCount) }} 次使用</span>
            <span><i class="fa-solid fa-star"></i> {{ t.rating.toFixed(1) }}</span>
            <span class="muted">{{ t.author }}</span>
          </div>
        </div>
        <div class="tpl-footer">
          <div class="tpl-actions-left">
            <el-tooltip content="点赞" placement="top">
              <el-button size="small" @click="onLike(t)" :type="t.isLiked ? 'warning' : ''">
                <i :class="t.isLiked ? 'fa-solid fa-star' : 'fa-regular fa-star'"></i>
                <span class="btn-count">{{ t.likes ?? 0 }}</span>
              </el-button>
            </el-tooltip>
            <el-tooltip content="收藏" placement="top">
              <el-button size="small" @click="onFavorite(t)" :type="t.isFavorited ? 'primary' : ''">
                <i :class="t.isFavorited ? 'fa-solid fa-bookmark' : 'fa-regular fa-bookmark'"></i>
              </el-button>
            </el-tooltip>
          </div>
          <div class="tpl-actions-right">
            <el-tooltip content="预览" placement="top">
              <el-button size="small" @click="onPreview(t)">
                <i class="fa-solid fa-eye"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="使用模板" placement="top">
              <el-button size="small" type="primary" :loading="loadingId === t.id" @click="onUse(t)">
                <i class="fa-solid fa-rocket"></i>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>

    <TemplatePreview
      v-model="previewOpen"
      :template="previewing"
      :loading="previewLoading"
      :use-loading="previewing ? loadingId === previewing.id : false"
      @use="onUse"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { templateApi } from '@/api/template';
import type { Template } from '@/api/types';
import { useWorkflowStore } from '@/stores/workflow';
import { ElMessage } from '@/utils/notify';
import { formatNumber } from '@/utils/format';
import TemplatePreview from '@/components/template/TemplatePreview.vue';

const route = useRoute();
const router = useRouter();
const store = useWorkflowStore();
const all = ref<Template[]>([]);
const keyword = ref('');
const category = ref('all');
const previewOpen = ref(false);
const previewing = ref<Template | null>(null);
const previewLoading = ref(false);
const loadingId = ref<string | null>(null);

const categoryChips = computed(() => {
  const set = new Set(all.value.map((t) => t.category));
  return [{ label: '全部', value: 'all' }, ...Array.from(set).map((c) => ({ label: c, value: c }))];
});

const filtered = computed(() => {
  let list = all.value;
  if (category.value !== 'all') list = list.filter((t) => t.category === category.value);
  if (keyword.value) {
    const k = keyword.value.toLowerCase();
    list = list.filter((t) => t.name.toLowerCase().includes(k) || t.description.toLowerCase().includes(k));
  }
  return list;
});

function coverGradient(c: string) {
  const map: Record<string, string> = {
    客服: 'linear-gradient(135deg,#818cf8,#c084fc)',
    HR: 'linear-gradient(135deg,#34d399,#10b981)',
    运营: 'linear-gradient(135deg,#fbbf24,#f59e0b)',
    电商: 'linear-gradient(135deg,#f472b6,#ec4899)',
    团队: 'linear-gradient(135deg,#60a5fa,#3b82f6)',
    数据: 'linear-gradient(135deg,#22d3ee,#06b6d4)',
  };
  return map[c] ?? 'linear-gradient(135deg,#a78bfa,#6366f1)';
}

async function load() {
  const res = await templateApi.search({});
  all.value = res.items;
  const requestedId = String(route.query.preview ?? '');
  if (requestedId) {
    const target = all.value.find((item) => String(item.id) === requestedId);
    if (target) await onPreview(target);
  }
}

async function onPreview(t: Template) {
  previewLoading.value = true;
  previewOpen.value = true;
  previewing.value = null;
  try {
    // 列表接口可能不带 workflowJson / workflowGraph，这里拉一次详情
    // 以保证预览画布能渲染真实节点。
    let detail = await templateApi.getById(t.id);
    // 兜底：mock 缓存中可能存在旧版本，detail 不带 workflowJson。
    // 此时用列表里原始对象里残留的字段补全，避免 TemplatePreview 退化为 fallback。
    if (detail && (!detail.workflowJson || !detail.workflowGraph)) {
      const tAny = t as any;
      const merged: Template = {
        ...detail,
        ...(tAny.workflowJson ? { workflowJson: tAny.workflowJson } : {}),
        ...(tAny.workflowGraph ? { workflowGraph: tAny.workflowGraph } : {}),
      } as Template;
      // 双下划线字段（__workflowJson / __workflowGraph）由 mapTemplateDTO 写入，
      // 用于让 TemplatePreview 拿到原始字符串（即使上层已展开成对象）。
      (merged as any).__workflowJson = tAny.__workflowJson ?? tAny.workflowJson ?? detail.workflowJson;
      (merged as any).__workflowGraph = tAny.__workflowGraph ?? tAny.workflowGraph ?? detail.workflowGraph;
      detail = merged;
    }
    previewing.value = detail ?? t;
  } catch (e) {
    // 拉取失败时仍打开预览，让 TemplatePreview 显示"暂无工作流数据"
    previewing.value = t;
  } finally {
    previewLoading.value = false;
  }
}

async function onUse(t: Template) {
  loadingId.value = t.id;
  try {
    await templateApi.use(t.id, `${t.name} - 副本`);
    ElMessage.success('已创建工作流,跳转编辑器');
    previewOpen.value = false;
    const workflowJson = (t as any).__workflowJson || (t as any).workflowJson;
    store.bootstrapFromTemplate(t.id, t.name, workflowJson);
    router.push('/editor');
  } finally {
    loadingId.value = null;
  }
}

async function onLike(t: Template) {
  try {
    if (t.isLiked) {
      await templateApi.unlike(t.id);
      t.likes = Math.max(0, (t.likes ?? 1) - 1);
      t.isLiked = false;
    } else {
      await templateApi.like(t.id);
      t.likes = (t.likes ?? 0) + 1;
      t.isLiked = true;
    }
  } catch {
    ElMessage.error('操作失败');
  }
}

async function onFavorite(t: Template) {
  try {
    if (t.isFavorited) {
      await templateApi.unfavorite(t.id);
      t.isFavorited = false;
      ElMessage.success('已取消收藏');
    } else {
      await templateApi.favorite(t.id);
      t.isFavorited = true;
      ElMessage.success('已收藏');
    }
  } catch {
    ElMessage.error('操作失败');
  }
}

onMounted(load);
</script>

<style scoped>
.tpl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.tpl-card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: box-shadow 0.15s, transform 0.15s;
}
.tpl-card:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); }
.tpl-cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.tpl-cover .emoji { font-size: 48px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.15)); }
.official-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255,255,255,0.85);
  color: var(--primary);
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}
.tpl-body { padding: 12px 14px; flex: 1; display: flex; flex-direction: column; gap: 6px; }
.tpl-name { font-weight: 600; font-size: 14px; }
.tpl-desc { font-size: 12px; color: var(--text-secondary); line-height: 1.5; min-height: 36px; }
.tpl-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.tag {
  font-size: 11px;
  color: var(--primary);
  background: var(--primary-bg);
  padding: 1px 6px;
  border-radius: 4px;
}
.tpl-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: var(--text-tertiary);
}
.tpl-meta .muted { color: var(--text-tertiary); }
.tpl-footer {
  border-top: 1px solid var(--border-light);
  padding: 8px 12px;
  display: flex;
  gap: 8px;
  justify-content: space-between;
  align-items: center;
}
.tpl-actions-left,
.tpl-actions-right {
  display: flex;
  gap: 4px;
}
.btn-count {
  font-size: 11px;
  margin-left: 2px;
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
.chip:hover { color: var(--primary); border-color: var(--primary-light); }
.chip.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
</style>
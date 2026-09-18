<template>
  <div class="tpl-card" @click="emit('preview', template)">
    <div class="tpl-cover" :style="{ background: coverGradient(template.category) }">
      <div v-if="template.coverEmoji" class="emoji">{{ template.coverEmoji }}</div>
      <i v-else class="fa-solid fa-puzzle-piece fallback-icon"></i>
      <span v-if="template.isOfficial" class="official-tag">官方</span>
      <span v-if="template.reviewStatus === 'pending'" class="review-tag">审核中</span>
      <span v-else-if="template.reviewStatus && template.reviewStatus !== 'approved'" class="review-tag warn">
        {{ reviewLabel(template.reviewStatus) }}
      </span>
    </div>
    <div class="tpl-body">
      <div class="tpl-name" :title="template.name">{{ template.name }}</div>
      <div class="tpl-desc" :title="template.description">{{ template.description || '暂无描述' }}</div>
      <div v-if="template.tags && template.tags.length" class="tpl-tags">
        <span class="tag" v-for="tag in template.tags.slice(0, 4)" :key="tag"># {{ tag }}</span>
        <span v-if="template.tags.length > 4" class="tag more">+{{ template.tags.length - 4 }}</span>
      </div>
      <div class="tpl-meta">
        <span class="meta-item">
          <i class="fa-solid fa-fire"></i>
          {{ formatNumber(template.usageCount) }} 次使用
        </span>
        <span class="meta-item rating">
          <i class="fa-solid fa-star"></i>
          {{ template.rating ? template.rating.toFixed(1) : '-' }}
        </span>
        <span class="meta-item muted" :title="template.author">{{ template.author }}</span>
      </div>
    </div>
    <div class="tpl-footer">
      <el-button size="small" @click.stop="emit('preview', template)">
        <i class="fa-solid fa-eye"></i><span style="margin-left:4px">预览</span>
      </el-button>
      <el-button
        size="small"
        type="primary"
        :loading="loading"
        :disabled="template.reviewStatus === 'pending' || template.reviewStatus === 'rejected'"
        @click.stop="emit('use', template)"
      >
        <i class="fa-solid fa-rocket"></i><span style="margin-left:4px">使用模板</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Template } from '@/api/types';
import { formatNumber } from '@/utils/format';

defineProps<{
  template: Template;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'preview', template: Template): void;
  (e: 'use', template: Template): void;
}>();

function reviewLabel(status?: string) {
  switch (status) {
    case 'pending':
      return '审核中';
    case 'rejected':
      return '未通过';
    case 'approved':
      return '已审核';
    default:
      return status || '';
  }
}

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
</script>

<style scoped>
.tpl-card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: box-shadow 0.15s, transform 0.15s, border-color 0.15s;
  cursor: pointer;
}
.tpl-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
  border-color: var(--primary-light);
}
.tpl-cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.tpl-cover .emoji {
  font-size: 48px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.15));
}
.tpl-cover .fallback-icon {
  font-size: 40px;
  color: rgba(255,255,255,0.85);
}
.official-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255,255,255,0.92);
  color: var(--primary);
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 999px;
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}
.review-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(255,255,255,0.92);
  color: var(--warning);
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 999px;
  font-weight: 500;
}
.review-tag.warn { color: var(--danger); }

.tpl-body {
  padding: 12px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 0;
}
.tpl-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tpl-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  min-height: 36px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tpl-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.tag {
  font-size: 11px;
  color: var(--primary);
  background: var(--primary-bg);
  padding: 1px 6px;
  border-radius: 4px;
  line-height: 1.4;
}
.tag.more {
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
}
.tpl-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: var(--text-tertiary);
  align-items: center;
  flex-wrap: wrap;
}
.tpl-meta .meta-item { display: inline-flex; gap: 4px; align-items: center; }
.tpl-meta .rating { color: var(--warning); }
.tpl-meta .muted {
  color: var(--text-tertiary);
  max-width: 90px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tpl-footer {
  border-top: 1px solid var(--border-light);
  padding: 8px 12px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  background: var(--bg-secondary);
}
</style>
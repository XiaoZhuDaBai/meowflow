<template>
  <el-dialog
    :model-value="modelValue"
    :title="template?.name || '模板预览'"
    width="920px"
    class="template-preview-dialog"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div v-if="loading" class="preview-loading">
      <i class="fa-solid fa-spinner fa-spin"></i>
      <span>正在加载模板详情...</span>
    </div>

    <div v-else-if="!template" class="preview-empty">
      <el-empty description="未找到模板数据" />
    </div>

    <div v-else>
      <WorkflowPreview
        :parsed="parsed"
        :reset-key="resetKey"
        :meta="meta"
        :graph-raw="workflowGraphRaw"
      >
        <template #header-extra>
          <!-- 评分 -->
          <div class="rating-row">
            <el-rate
              :model-value="Math.round(template.rating || 0)"
              disabled
              show-score
              :score-template="`{value} / 5`"
              text-color="#ff9900"
            />
            <span class="meta-count">共 {{ template.reviewCount ?? 0 }} 条评价</span>
          </div>

          <!-- 标签 -->
          <div v-if="template.tags && template.tags.length" class="tags-section">
            <div class="section-title">
              <i class="fa-solid fa-tags"></i>
              <span>标签</span>
            </div>
            <div class="tags-list">
              <span v-for="t in template.tags" :key="t" class="tag-pill"># {{ t }}</span>
            </div>
          </div>

          <!-- 备注 -->
          <div v-if="template.remark" class="remark">
            <i class="fa-solid fa-circle-info"></i>
            <span>{{ template.remark }}</span>
          </div>
        </template>
      </WorkflowPreview>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button
        size="small"
        :type="template?.isLiked ? 'warning' : ''"
        @click="onLike"
        :loading="actionLoading"
      >
        <i :class="template?.isLiked ? 'fa-solid fa-star' : 'fa-regular fa-star'"></i>
        <span style="margin-left:4px">{{ template?.likes ?? 0 }}</span>
      </el-button>
      <el-button
        size="small"
        :type="template?.isFavorited ? 'primary' : ''"
        @click="onFavorite"
        :loading="actionLoading"
      >
        <i :class="template?.isFavorited ? 'fa-solid fa-bookmark' : 'fa-regular fa-bookmark'"></i>
        <span style="margin-left:4px">{{ template?.isFavorited ? '已收藏' : '收藏' }}</span>
      </el-button>
      <el-button
        type="primary"
        :loading="useLoading"
        :disabled="disabledUse"
        @click="onUse"
      >
        <i class="fa-solid fa-rocket"></i>
        <span style="margin-left:4px">使用此模板</span>
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { Template } from '@/api/types';
import { parseWorkflowJson, templateApi } from '@/api/template';
import WorkflowPreview, { type WorkflowPreviewMeta } from '@/components/common/WorkflowPreview.vue';
import { formatDate } from '@/utils/format';
import { ElMessage } from '@/utils/notify';

const props = defineProps<{
  modelValue: boolean;
  template: Template | null;
  loading?: boolean;
  useLoading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'use', template: Template): void;
  (e: 'like', template: Template): void;
  (e: 'favorite', template: Template): void;
}>();

const actionLoading = ref(false);

const workflowJsonRaw = computed<string | null>(() => {
  const t: any = props.template;
  return t?.workflowJson ?? t?.__workflowJson ?? null;
});

const workflowGraphRaw = computed<string | null>(() => {
  const t: any = props.template;
  return t?.workflowGraph ?? t?.__workflowGraph ?? null;
});

const parsed = computed(() => parseWorkflowJson(workflowJsonRaw.value));

const resetKey = computed(() => props.template?.id ?? 'preview');

const meta = computed<WorkflowPreviewMeta | null>(() => {
  const t = props.template;
  if (!t) return null;
  return {
    name: t.name,
    description: t.description,
    category: (t as any).category,
    emoji: (t as any).coverEmoji,
    author: t.author,
    usageCount: typeof t.usageCount === 'number' ? t.usageCount : undefined,
    createdAt: t.createdAt ? formatDate(t.createdAt) : undefined,
    reviewStatus: (t as any).reviewStatus,
  };
});

const disabledUse = computed(() => {
  const t = props.template as any;
  return t?.reviewStatus === 'pending' || t?.reviewStatus === 'rejected';
});

function onUse() {
  if (props.template) emit('use', props.template);
}

async function onLike() {
  if (!props.template) return;
  actionLoading.value = true;
  try {
    if (props.template.isLiked) {
      await templateApi.unlike(props.template.id);
      props.template.likes = Math.max(0, (props.template.likes ?? 1) - 1);
      props.template.isLiked = false;
    } else {
      await templateApi.like(props.template.id);
      props.template.likes = (props.template.likes ?? 0) + 1;
      props.template.isLiked = true;
    }
    emit('like', props.template);
  } catch {
    ElMessage.error('操作失败');
  } finally {
    actionLoading.value = false;
  }
}

async function onFavorite() {
  if (!props.template) return;
  actionLoading.value = true;
  try {
    if (props.template.isFavorited) {
      await templateApi.unfavorite(props.template.id);
      props.template.isFavorited = false;
      ElMessage.success('已取消收藏');
    } else {
      await templateApi.favorite(props.template.id);
      props.template.isFavorited = true;
      ElMessage.success('已收藏');
    }
    emit('favorite', props.template);
  } catch {
    ElMessage.error('操作失败');
  } finally {
    actionLoading.value = false;
  }
}

watch(
  () => props.modelValue,
  () => {
    // 关闭后会由内部 Tab 重置
  },
);
</script>

<style scoped>
.preview-loading,
.preview-empty {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}
.meta-count {
  font-size: 12px;
  color: var(--text-tertiary);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag-pill {
  font-size: 12px;
  color: var(--primary);
  background: var(--primary-bg);
  padding: 2px 10px;
  border-radius: 999px;
}

.remark {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--info-bg);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid #bfdbfe;
}
</style>
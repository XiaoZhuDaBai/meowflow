<template>
  <div class="create-blank-tab">
    <!-- 触发器类型选择 -->
    <div class="section-label">选择触发器类型（可选）</div>
    <div class="trigger-cards">
      <div
        v-for="t in TRIGGER_OPTIONS"
        :key="t.type"
        class="trigger-card"
        :class="{ active: selectedTrigger === t.type }"
        @click="selectedTrigger = t.type"
      >
        <div class="trigger-icon-wrap">
          <i :class="`fa-solid ${t.icon}`" :style="{ color: t.color }"></i>
        </div>
        <div class="trigger-info">
          <div class="trigger-name">{{ t.name }}</div>
          <div class="trigger-desc">{{ t.desc }}</div>
        </div>
        <div v-if="selectedTrigger === t.type" class="trigger-check">
          <i class="fa-solid fa-check"></i>
        </div>
      </div>
    </div>

    <el-divider style="margin: 20px 0" />

    <!-- 基本信息表单 -->
    <el-form :model="form" label-position="top" @submit.prevent>
      <el-form-item label="工作流名称" required>
        <el-input
          v-model="form.name"
          placeholder="给你的工作流起个名字"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="描述这个工作流做什么（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      <div class="row-2col">
        <el-form-item label="分类" class="flex-1">
          <el-select v-model="form.category" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="图标" class="flex-1">
          <WorkflowIconPicker v-model="form.icon" />
        </el-form-item>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import WorkflowIconPicker from '@/components/common/WorkflowIconPicker.vue';

const emit = defineEmits<{
  (e: 'submit', data: { name: string; description: string; category: string; icon: any; triggerType: string }): void;
}>();

const CATEGORIES = ['客服', 'HR', '运营', '电商', '团队', '数据', '其他'];

const TRIGGER_OPTIONS = [
  { type: 'trigger.webhook', name: 'Webhook', desc: '接收外部 HTTP 请求触发', icon: 'fa-bolt', color: '#8b5cf6' },
  { type: 'trigger.cron', name: '定时触发', desc: '按 Cron 表达式定时执行', icon: 'fa-clock', color: '#8b5cf6' },
  { type: 'trigger.form', name: '表单', desc: '用户提交表单时触发', icon: 'fa-rectangle-list', color: '#8b5cf6' },
  { type: 'trigger.imessage', name: '消息', desc: '接收钉钉/企微/飞书消息', icon: 'fa-comment', color: '#8b5cf6' },
];

const selectedTrigger = ref('');

const form = reactive({
  name: '',
  description: '',
  category: '',
  icon: { icon: '🤖', color: '#6366f1' } as any,
});

function getSubmitData() {
  return {
    name: form.name.trim(),
    description: form.description.trim(),
    category: form.category,
    icon: form.icon,
    triggerType: selectedTrigger.value,
  };
}

defineExpose({ getSubmitData });
</script>

<style scoped>
.create-blank-tab {
  padding: 8px 0;
  max-width: 600px;
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.trigger-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.trigger-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
  flex: 1;
  min-width: 160px;
  background: var(--bg-primary);
  position: relative;
}

.trigger-card:hover {
  border-color: var(--primary-light);
  background: var(--primary-bg);
}

.trigger-card.active {
  border-color: var(--primary);
  background: var(--primary-bg);
}

.trigger-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.trigger-info {
  flex: 1;
  min-width: 0;
}

.trigger-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.trigger-desc {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.trigger-check {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  flex-shrink: 0;
}

.row-2col {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.flex-1 {
  flex: 1;
}
</style>

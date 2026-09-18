<template>
  <div class="icon-picker">
    <el-popover :width="260" trigger="click" placement="bottom-start">
      <template #reference>
        <div class="picker-trigger">
          <span class="preview-icon" :style="{ background: modelValue?.color ?? '#6366f1' }">
            {{ modelValue?.icon ?? '🤖' }}
          </span>
          <span class="picker-label">选择图标</span>
          <i class="fa-solid fa-chevron-down picker-arrow"></i>
        </div>
      </template>

      <div class="picker-body">
        <div class="picker-section">
          <div class="picker-section-label">图标</div>
          <div class="icon-grid">
            <button
              v-for="emoji in EMOJIS"
              :key="emoji"
              class="icon-btn"
              :class="{ active: modelValue?.icon === emoji }"
              @click="selectIcon(emoji)"
            >{{ emoji }}</button>
          </div>
        </div>

        <el-divider style="margin: 10px 0" />

        <div class="picker-section">
          <div class="picker-section-label">颜色</div>
          <div class="color-grid">
            <button
              v-for="c in COLORS"
              :key="c"
              class="color-btn"
              :class="{ active: modelValue?.color === c }"
              :style="{ background: c }"
              @click="selectColor(c)"
            >
              <i v-if="modelValue?.color === c" class="fa-solid fa-check" style="color:#fff"></i>
            </button>
          </div>
        </div>

        <el-divider style="margin: 10px 0" />

        <div class="picker-section">
          <div class="picker-section-label">自定义颜色</div>
          <el-input
            v-model="customColor"
            size="small"
            placeholder="#6366f1"
            maxlength="7"
            @change="applyCustomColor"
          >
            <template #prepend>
              <div class="color-swatch" :style="{ background: customColorValid ? customColor : '#ccc' }"></div>
            </template>
          </el-input>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

export interface WorkflowIconValue {
  icon: string;
  color: string;
}

const props = defineProps<{
  modelValue?: WorkflowIconValue | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: WorkflowIconValue): void;
}>();

const EMOJIS = ['🤖', '📋', '📊', '📧', '💬', '📝', '🔧', '⚙️', '🚀', '✨', '💡', '🔔', '🌟', '🎯', '📱', '🎨'];

const COLORS = [
  '#8b5cf6', // 触发器紫
  '#3b82f6', // AI蓝
  '#10b981', // 流程绿
  '#f59e0b', // 工具橙
  '#ec4899', // 通知粉
  '#ef4444', // 危险红
  '#06b6d4', // 青色
  '#6366f1', // 主色
];

const customColor = ref('');

const customColorValid = computed(() => /^#[0-9a-fA-F]{6}$/.test(customColor.value));

function selectIcon(icon: string) {
  emit('update:modelValue', { icon, color: props.modelValue?.color ?? COLORS[0] });
}

function selectColor(color: string) {
  emit('update:modelValue', { icon: props.modelValue?.icon ?? EMOJIS[0], color });
}

function applyCustomColor() {
  if (customColorValid.value) {
    emit('update:modelValue', { icon: props.modelValue?.icon ?? EMOJIS[0], color: customColor.value });
  }
}
</script>

<style scoped>
.icon-picker {
  display: inline-block;
}

.picker-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-primary);
  transition: border-color 0.15s;
  user-select: none;
}

.picker-trigger:hover {
  border-color: var(--primary-light);
}

.preview-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.picker-label {
  flex: 1;
}

.picker-arrow {
  font-size: 10px;
  color: var(--text-tertiary);
}

.picker-body {
  padding: 4px;
}

.picker-section-label {
  font-size: 11px;
  color: var(--text-tertiary);
  font-weight: 500;
  margin-bottom: 6px;
  padding-left: 2px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 3px;
}

.icon-btn {
  width: 28px;
  height: 28px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: none;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.1s;
}

.icon-btn:hover {
  background: var(--bg-tertiary);
}

.icon-btn.active {
  background: var(--primary-bg);
  border-color: var(--primary-light);
}

.color-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
}

.color-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  transition: transform 0.1s;
}

.color-btn:hover {
  transform: scale(1.15);
}

.color-btn.active {
  border-color: #fff;
  box-shadow: 0 0 0 2px var(--text-primary);
}

.color-swatch {
  width: 14px;
  height: 14px;
  border-radius: 3px;
}
</style>

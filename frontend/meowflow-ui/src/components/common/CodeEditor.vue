<template>
  <div class="code-editor">
    <textarea
      ref="textareaRef"
      :value="modelValue"
      :placeholder="placeholder"
      :readonly="readonly"
      class="code-area"
      :style="{
        minHeight: typeof height === 'number' ? `${height}px` : height,
      }"
      @input="onInput"
      @blur="onBlur"
    />
    <div v-if="error" class="error-tip">
      <i class="fa-solid fa-circle-exclamation"></i>
      <span>{{ error }}</span>
    </div>
    <div v-else-if="hint" class="hint">
      <i class="fa-solid fa-circle-info"></i>
      <span>{{ hint }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{
  modelValue: string;
  placeholder?: string;
  readonly?: boolean;
  height?: number | string;
  language?: 'json' | 'javascript' | 'python' | 'text';
  validate?: 'json' | ((value: string) => string | null);
  hint?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void;
  (e: 'validate', ok: boolean, message?: string): void;
}>();

const textareaRef = ref<HTMLTextAreaElement>();
const error = ref<string | null>(null);

function validate(value: string): string | null {
  if (!props.validate) return null;
  if (props.validate === 'json') {
    if (!value.trim()) return null;
    try {
      JSON.parse(value);
      return null;
    } catch (e: any) {
      return `JSON 格式错误: ${e.message}`;
    }
  }
  if (typeof props.validate === 'function') {
    return props.validate(value);
  }
  return null;
}

function onInput(e: Event) {
  const v = (e.target as HTMLTextAreaElement).value;
  emit('update:modelValue', v);
}

function onBlur() {
  error.value = validate(props.modelValue);
  emit('validate', error.value === null, error.value ?? undefined);
}

watch(
  () => props.modelValue,
  () => {
    error.value = validate(props.modelValue);
  },
  { immediate: true },
);
</script>

<style scoped>
.code-editor {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}
.code-area {
  width: 100%;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  color: var(--text-primary);
  resize: vertical;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.code-area:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-bg);
}
.code-area:read-only {
  background: var(--bg-tertiary);
  cursor: default;
}
.error-tip,
.hint {
  display: flex;
  gap: 4px;
  align-items: center;
  font-size: 11px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
}
.error-tip {
  background: #fef2f2;
  color: var(--danger);
}
.hint {
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
}
</style>

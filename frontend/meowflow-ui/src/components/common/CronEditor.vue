<template>
  <div class="cron-editor">
    <div class="quick">
      <span
        v-for="p in presets"
        :key="p.label"
        class="chip"
        :class="{ active: modelValue === p.expr }"
        @click="applyPreset(p.expr)"
      >
        {{ p.label }}
      </span>
    </div>
    <el-input
      :model-value="modelValue"
      :placeholder="placeholder ?? 'Cron 表达式,例如 0 9 * * *'"
      @update:model-value="(v) => emit('update:modelValue', v ?? '')"
    >
      <template #prefix><i class="fa-regular fa-clock"></i></template>
    </el-input>
    <div v-if="parsed.length" class="preview">
      <div class="preview-title">接下来 5 次执行 ({{ timezone }})</div>
      <ul class="preview-list">
        <li v-for="(t, i) in parsed" :key="i">
          <i class="fa-solid fa-circle-check"></i>
          <span>{{ t }}</span>
        </li>
      </ul>
    </div>
    <div v-else-if="modelValue" class="error">
      <i class="fa-solid fa-circle-exclamation"></i>
      <span>Cron 表达式无效</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

const props = defineProps<{
  modelValue: string;
  placeholder?: string;
  timezone?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void;
}>();

const timezone = computed(() => props.timezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone);

const presets = [
  { label: '每分钟', expr: '* * * * *' },
  { label: '每小时', expr: '0 * * * *' },
  { label: '每天0点', expr: '0 0 * * *' },
  { label: '每天9点', expr: '0 9 * * *' },
  { label: '工作日9点', expr: '0 9 * * 1-5' },
  { label: '每周一', expr: '0 9 * * 1' },
];

function applyPreset(expr: string) {
  emit('update:modelValue', expr);
}

// 简易 Cron 解析 - 仅支持 5 字段格式,精度按分钟
function parseCron(expr: string): string[] {
  const parts = expr.trim().split(/\s+/);
  if (parts.length !== 5) return [];
  const [minute, hour, day, month, weekday] = parts;
  const result: string[] = [];
  const now = new Date();
  let cursor = new Date(now.getTime() + 60_000 - (now.getTime() % 60_000));

  function match(field: string, _min: number, _max: number, value: number): boolean {
    if (field === '*') return true;
    if (field.includes('/')) {
      const [, step] = field.split('/');
      return value % Number(step) === 0;
    }
    if (field.includes(',')) return field.split(',').some((p) => Number(p) === value);
    if (field.includes('-')) {
      const [a, b] = field.split('-').map(Number);
      return value >= a && value <= b;
    }
    return Number(field) === value;
  }

  while (result.length < 5 && cursor.getTime() < now.getTime() + 60 * 24 * 3600_000) {
    const m = cursor.getMinutes();
    const h = cursor.getHours();
    const d = cursor.getDate();
    const mo = cursor.getMonth() + 1;
    const w = cursor.getDay();
    const ok =
      match(minute, 0, 59, m) &&
      match(hour, 0, 23, h) &&
      match(day, 1, 31, d) &&
      match(month, 1, 12, mo) &&
      match(weekday, 0, 6, w);
    if (ok) {
      result.push(cursor.toISOString().replace('T', ' ').slice(0, 16));
    }
    cursor = new Date(cursor.getTime() + 60_000);
  }
  return result;
}

const parsed = ref<string[]>([]);

watch(
  () => [props.modelValue, timezone.value],
  () => {
    parsed.value = props.modelValue ? parseCron(props.modelValue) : [];
  },
  { immediate: true },
);
</script>

<style scoped>
.cron-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  font-size: 11px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.15s;
}
.chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.chip.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
.preview {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
}
.preview-title {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}
.preview-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  list-style: none;
  padding: 0;
  margin: 0;
}
.preview-list li {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-primary);
  font-family: ui-monospace, monospace;
}
.preview-list i {
  color: var(--success);
  font-size: 10px;
}
.error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #fef2f2;
  color: var(--danger);
  border-radius: var(--radius-sm);
  font-size: 12px;
}
</style>

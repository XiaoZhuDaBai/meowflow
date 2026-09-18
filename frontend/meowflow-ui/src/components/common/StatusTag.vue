<template>
  <span class="status-tag" :style="{ '--c': color }">
    <span class="dot" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { STATUS_COLOR, STATUS_LABEL } from '@/utils/constants';

const props = defineProps<{ status: string }>();
const color = computed(() => STATUS_COLOR[props.status] ?? '#94a3b8');
const label = computed(() => STATUS_LABEL[props.status] ?? props.status);
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--c) 12%, white);
  color: var(--c);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;
  white-space: nowrap;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--c);
}
</style>
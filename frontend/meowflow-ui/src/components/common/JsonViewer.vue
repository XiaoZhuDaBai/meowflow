<template>
  <pre class="json-viewer" v-if="visible">{{ formatted }}</pre>
  <span v-else class="muted">-</span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ data: any }>();
const visible = computed(() => props.data !== undefined && props.data !== null && props.data !== '');
const formatted = computed(() => {
  try {
    return JSON.stringify(props.data, null, 2);
  } catch {
    return String(props.data);
  }
});
</script>

<style scoped>
.json-viewer {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 8px;
  color: var(--text-primary);
  max-height: 240px;
  overflow: auto;
}
.muted { color: var(--text-tertiary); font-size: 12px; }
</style>
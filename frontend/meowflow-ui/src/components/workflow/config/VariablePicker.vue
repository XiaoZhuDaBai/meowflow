<template>
  <div class="variable-picker" :class="{ inline }">
    <el-popover :width="360" placement="bottom-start" trigger="click">
      <template #reference>
        <el-button size="small" link :class="{ 'is-trigger': !inline }">
          <i class="fa-solid fa-code-branch"></i>
          <span style="margin-left: 4px">插入变量</span>
        </el-button>
      </template>
      <div class="picker-popup">
        <el-input v-model="keyword" placeholder="搜索变量" clearable size="small">
          <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
        </el-input>
        <div class="groups">
          <div v-for="g in groups" :key="g.category" class="group">
            <div class="group-title">{{ g.label }}</div>
            <div class="group-items">
              <div
                v-for="v in filteredGroup(g)"
                :key="v.path"
                class="var-item"
                @click="insert(v.path)"
              >
                <code class="var-path">{{ varToken(v.path) }}</code>
                <span class="var-desc">{{ v.description }}</span>
              </div>
            </div>
          </div>
          <el-empty v-if="!filteredGroups.length" description="无可用变量" :image-size="60" />
        </div>
      </div>
    </el-popover>
    <div v-if="inline && recent && recent.length" class="recent">
      <span class="recent-label">最近使用:</span>
      <code
        v-for="p in recent.slice(0, 4)"
        :key="p"
        class="chip-var"
        @click="emit('insert', p)"
      >{{ varToken(p) }}</code>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

export interface VariableItem {
  path: string;          // e.g. "trigger.query"
  description: string;
}

export interface VariableGroup {
  category: string;      // matches WorkflowNode.category
  label: string;
  icon?: string;
  variables: VariableItem[];
}

const props = defineProps<{
  groups: VariableGroup[];
  inline?: boolean;
  recent?: string[];
}>();

const emit = defineEmits<{
  (e: 'insert', path: string): void;
}>();

const keyword = ref('');

function filteredGroup(g: VariableGroup) {
  if (!keyword.value) return g.variables;
  const k = keyword.value.toLowerCase();
  return g.variables.filter(
    (v) => v.path.toLowerCase().includes(k) || v.description.toLowerCase().includes(k),
  );
}

const filteredGroups = computed(() =>
  props.groups
    .map((g) => ({ ...g, variables: filteredGroup(g) }))
    .filter((g) => g.variables.length > 0),
);

function insert(path: string) {
  emit('insert', path);
}

function varToken(path: string) {
  return '{{' + path + '}}';
}
</script>

<style scoped>
.variable-picker {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.variable-picker.inline {
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
}
.is-trigger {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: var(--primary);
}
.picker-popup {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.groups {
  max-height: 320px;
  overflow-y: auto;
}
.group {
  margin-bottom: 8px;
}
.group-title {
  font-size: 11px;
  color: var(--text-tertiary);
  font-weight: 600;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}
.group-items {
  display: flex;
  flex-direction: column;
}
.var-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
}
.var-item:hover {
  background: var(--primary-bg);
}
.var-path {
  font-family: ui-monospace, monospace;
  font-size: 11px;
  color: var(--primary);
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 4px;
}
.var-desc {
  font-size: 11px;
  color: var(--text-tertiary);
  text-align: right;
  max-width: 50%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.recent-label {
  font-size: 11px;
  color: var(--text-tertiary);
}
.chip-var {
  font-family: ui-monospace, monospace;
  font-size: 11px;
  padding: 2px 6px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 4px;
  cursor: pointer;
  color: var(--primary);
}
.chip-var:hover {
  border-color: var(--primary);
}
</style>

<template>
  <div class="if-else-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="条件列表">
        <div class="cases">
          <div
            v-for="(c, idx) in cases"
            :key="c.id"
            class="case-row"
          >
            <div class="case-head">
              <span class="case-no">分支 {{ idx + 1 }}</span>
              <el-button size="small" link @click="removeCase(idx)">
                <i class="fa-solid fa-xmark"></i>
              </el-button>
            </div>
            <el-form-item label="分支名称" label-position="top">
              <el-input v-model="c.name" placeholder="如 优秀" size="small" />
            </el-form-item>
            <el-form-item label="条件表达式" label-position="top">
              <el-input
                v-model="c.expression"
                type="textarea"
                :rows="2"
                placeholder="{{input.score}} >= 80"
                size="small"
              />
              <VariablePicker inline :groups="variableGroups" @insert="(p) => insertToExpr(c, p)" />
            </el-form-item>
          </div>
          <el-button size="small" link @click="addCase">
            <i class="fa-solid fa-plus"></i>
            <span style="margin-left:4px">添加分支</span>
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="默认分支">
        <el-switch v-model="hasDefault" />
        <span class="tip">无分支命中时,执行默认分支 (false)</span>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

interface Case {
  id: string;
  name: string;
  expression: string;
}

const DEFAULT_CASES: Case[] = [
  { id: 'case-1', name: '优秀', expression: '{{input.score}} >= 80' },
  { id: 'case-2', name: '及格', expression: '{{input.score}} >= 60' },
];

function ensureCases(): Case[] {
  const arr = props.modelValue?.cases;
  if (Array.isArray(arr) && arr.length) return arr as Case[];
  return DEFAULT_CASES;
}

const cases = computed({
  get: () => ensureCases(),
  set: (v: Case[]) => emit('update:modelValue', { ...props.modelValue, cases: v }),
});

const hasDefault = computed({
  get: () => props.modelValue?.hasDefault ?? true,
  set: (v: boolean) => emit('update:modelValue', { ...props.modelValue, hasDefault: v }),
});

function addCase() {
  cases.value = [
    ...cases.value,
    { id: `case-${Date.now()}`, name: '', expression: '' },
  ];
}

function removeCase(idx: number) {
  const arr = cases.value.slice();
  arr.splice(idx, 1);
  cases.value = arr;
}

function insertToExpr(c: Case, path: string) {
  c.expression = (c.expression ?? '') + `{{${path}}}`;
}
</script>

<style scoped>
.cases {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.case-row {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 10px;
}
.case-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.case-no {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
}
.tip {
  margin-left: 12px;
  font-size: 11px;
  color: var(--text-tertiary);
}
</style>

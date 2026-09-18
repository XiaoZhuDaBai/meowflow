<template>
  <div class="assigner-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="赋值列表" required>
        <div class="assignments">
          <div
            v-for="(a, idx) in assignments"
            :key="a.id ?? idx"
            class="row"
          >
            <el-input v-model="a.name" placeholder="变量名" size="small" />
            <el-input v-model="a.value" placeholder="变量值 (支持 {{...}})" size="small" />
            <el-input-number v-model="a.type" :min="0" :max="3" size="small" :controls="false" placeholder="类型" />
            <el-button size="small" link @click="remove(idx)">
              <i class="fa-solid fa-xmark"></i>
            </el-button>
          </div>
          <el-button size="small" link @click="add">
            <i class="fa-solid fa-plus"></i>
            <span style="margin-left:4px">添加赋值</span>
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="变量">
        <VariablePicker inline :groups="variableGroups" @insert="insertAtLast" />
      </el-form-item>

      <el-form-item label="覆盖已存在">
        <el-switch v-model="overwrite" />
        <span class="tip">允许覆盖已有同名变量</span>
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

interface Assignment {
  id: string;
  name: string;
  value: string;
  type?: number;
}

const DEFAULT: Assignment[] = [
  { id: 'a1', name: 'foo', value: '{{llm.text}}', type: 0 },
  { id: 'a2', name: 'bar', value: '42', type: 1 },
];

function ensureAssignments(): Assignment[] {
  const arr = props.modelValue?.assignments;
  if (Array.isArray(arr) && arr.length) return arr as Assignment[];
  return DEFAULT;
}

const assignments = computed({
  get: () => ensureAssignments(),
  set: (v: Assignment[]) => emit('update:modelValue', { ...props.modelValue, assignments: v }),
});

const overwrite = computed({
  get: () => props.modelValue?.overwrite ?? true,
  set: (v: boolean) => emit('update:modelValue', { ...props.modelValue, overwrite: v }),
});

function add() {
  assignments.value = [
    ...assignments.value,
    { id: `a${Date.now()}`, name: '', value: '', type: 0 },
  ];
}

function remove(idx: number) {
  const arr = assignments.value.slice();
  arr.splice(idx, 1);
  assignments.value = arr;
}

function insertAtLast(path: string) {
  const last = assignments.value[assignments.value.length - 1];
  if (last) last.value = (last.value ?? '') + `{{${path}}}`;
}
</script>

<style scoped>
.assignments {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.row {
  display: grid;
  grid-template-columns: 1fr 2fr 60px 28px;
  gap: 6px;
  align-items: center;
}
.tip {
  margin-left: 12px;
  font-size: 11px;
  color: var(--text-tertiary);
}
</style>

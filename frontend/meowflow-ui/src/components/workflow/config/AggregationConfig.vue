<template>
  <div class="aggregation-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="聚合策略">
        <el-radio-group v-model="strategy">
          <el-radio-button value="concat">拼接</el-radio-button>
          <el-radio-button value="merge">合并</el-radio-button>
          <el-radio-button value="first">取首个</el-radio-button>
          <el-radio-button value="last">取最后</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="strategy === 'concat'" label="分隔符">
        <el-input v-model="separator" placeholder="默认换行" />
      </el-form-item>

      <el-form-item label="输入分支">
        <div class="branches">
          <div v-for="(b, idx) in branches" :key="b.id" class="branch-row">
            <span class="branch-name">{{ b.name }}</span>
            <el-input v-model="b.source" placeholder="{{branchA.output}}" />
            <el-button size="small" link @click="removeBranch(idx)">
              <i class="fa-solid fa-xmark"></i>
            </el-button>
          </div>
          <el-button size="small" link @click="addBranch">
            <i class="fa-solid fa-plus"></i>
            <span style="margin-left:4px">添加分支输入</span>
          </el-button>
        </div>
        <div class="tip">上游多分支节点的输出变量路径,按策略合并</div>
      </el-form-item>

      <el-form-item label="变量">
        <VariablePicker inline :groups="variableGroups" @insert="insertAtBranch" />
      </el-form-item>

      <el-form-item label="输出变量">
        <el-input v-model="outputName" placeholder="result" />
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

interface Branch { id: string; name: string; source: string }

const DEFAULT_BRANCHES: Branch[] = [
  { id: 'b1', name: '分支 A', source: '{{branchA.text}}' },
  { id: 'b2', name: '分支 B', source: '{{branchB.text}}' },
];

function ensureBranches(): Branch[] {
  const arr = props.modelValue?.branches;
  if (Array.isArray(arr) && arr.length) return arr as Branch[];
  return DEFAULT_BRANCHES;
}

const strategy = computed({
  get: () => props.modelValue?.strategy ?? 'concat',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, strategy: v }),
});

const separator = computed({
  get: () => props.modelValue?.separator ?? '\n\n',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, separator: v }),
});

const branches = computed({
  get: () => ensureBranches(),
  set: (v: Branch[]) => emit('update:modelValue', { ...props.modelValue, branches: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'result',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, outputName: v }),
});

function addBranch() {
  branches.value = [
    ...branches.value,
    { id: `b${Date.now()}`, name: `分支 ${String.fromCharCode(65 + branches.value.length)}`, source: '' },
  ];
}

function removeBranch(idx: number) {
  const arr = branches.value.slice();
  arr.splice(idx, 1);
  branches.value = arr;
}

function insertAtBranch(path: string) {
  // 插入到最后一个空分支
  const last = branches.value[branches.value.length - 1];
  if (last) last.source = (last.source ?? '') + `{{${path}}}`;
}
</script>

<style scoped>
.branches {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.branch-row {
  display: grid;
  grid-template-columns: 80px 1fr 28px;
  gap: 6px;
  align-items: center;
}
.branch-name {
  font-size: 12px;
  color: var(--text-secondary);
}
.tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
</style>

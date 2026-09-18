<template>
  <div class="condition-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="条件模式">
        <el-radio-group v-model="mode">
          <el-radio-button value="expression">表达式</el-radio-button>
          <el-radio-button value="builder">可视化构建</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <template v-if="mode === 'expression'">
        <el-form-item label="条件表达式">
          <CodeEditor
            v-model="expression"
            :rows="3"
            placeholder="{{ai.score}} >= 80"
          />
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item label="左侧变量">
          <el-input v-model="builder.left" placeholder="{{ai.score}}" />
          <VariablePicker inline :groups="variableGroups" @insert="insertToVar" />
        </el-form-item>
        <el-form-item label="比较运算符">
          <el-select v-model="builder.op">
            <el-option value="==" label="等于 (==)" />
            <el-option value="!=" label="不等于 (!=)" />
            <el-option value=">" label="大于 (>)" />
            <el-option value=">=" label="大于等于 (>=)" />
            <el-option value="<" label="小于 (<)" />
            <el-option value="<=" label="小于等于 (<=)" />
            <el-option value="contains" label="包含" />
            <el-option value="startsWith" label="开头是" />
            <el-option value="endsWith" label="结尾是" />
          </el-select>
        </el-form-item>
        <el-form-item label="比较值">
          <el-input v-model="builder.right" placeholder="80 或 'pending'" />
        </el-form-item>
        <el-form-item label="预览">
          <code class="expr-preview">{{ expressionPreview }}</code>
        </el-form-item>
      </template>

      <el-form-item label="分支">
        <div class="branches">
          <el-radio-group v-model="branchMode">
            <el-radio-button value="binary">是 / 否 (2 路)</el-radio-button>
            <el-radio-button value="switch">多路分支</el-radio-button>
          </el-radio-group>
        </div>
      </el-form-item>

      <template v-if="branchMode === 'switch'">
        <el-form-item label="分支列表">
          <div class="branch-list">
            <div
              v-for="(b, idx) in switchBranches"
              :key="idx"
              class="branch-row"
            >
              <el-input v-model="b.label" placeholder="分支显示名称" size="small" />
              <el-input v-model="b.value" placeholder="期望值" size="small" />
              <el-button size="small" link @click="removeBranch(idx)">
                <i class="fa-solid fa-xmark"></i>
              </el-button>
            </div>
            <el-button size="small" link @click="addBranch">
              <i class="fa-solid fa-plus"></i><span style="margin-left:4px">添加分支</span>
            </el-button>
          </div>
        </el-form-item>
      </template>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';
import CodeEditor from '@/components/common/CodeEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';

interface Branch { label: string; value: string }

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

function patch(extra: Record<string, any>) {
  emit('update:modelValue', { ...props.modelValue, ...extra });
}

const mode = computed({
  get: () => props.modelValue?.mode ?? 'expression',
  set: (v: 'expression' | 'builder') => patch({ mode: v }),
});

const expression = computed({
  get: () => props.modelValue?.expression ?? '',
  set: (v: string) => patch({ expression: v }),
});

const builder = computed({
  get: () => props.modelValue?.builder ?? { left: '', op: '==', right: '' },
  set: (v: any) => patch({ builder: v }),
});

const branchMode = computed({
  get: () => props.modelValue?.branchMode ?? 'binary',
  set: (v: 'binary' | 'switch') => patch({ branchMode: v }),
});

const switchBranches = computed({
  get: () => (props.modelValue?.branches as Branch[]) ?? [
    { label: '是', value: 'true' },
    { label: '否', value: 'false' },
  ],
  set: (v: Branch[]) => patch({ branches: v }),
});

const expressionPreview = computed(() => {
  const b = builder.value;
  if (!b.left) return '请设置左侧变量';
  if (b.op === 'contains') return `${b.left}.includes(${b.right})`;
  if (b.op === 'startsWith') return `${b.left}.startsWith(${b.right})`;
  if (b.op === 'endsWith') return `${b.left}.endsWith(${b.right})`;
  return `${b.left} ${b.op} ${b.right}`;
});

function insertToVar(path: string) {
  if (mode.value === 'builder') {
    builder.value = { ...builder.value, left: `{{${path}}}` };
  } else {
    expression.value = `{{${path}}}`;
  }
}

function addBranch() {
  const arr = switchBranches.value.slice();
  arr.push({ label: `分支${arr.length + 1}`, value: '' });
  switchBranches.value = arr;
}

function removeBranch(idx: number) {
  const arr = switchBranches.value.slice();
  arr.splice(idx, 1);
  switchBranches.value = arr;
}
</script>

<style scoped>
.branches { display: flex; gap: 12px; }
.branch-list { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.branch-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 6px;
  align-items: center;
}
.expr-preview {
  display: block;
  padding: 6px 10px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  font-family: ui-monospace, monospace;
  font-size: 12px;
  color: var(--primary);
}
</style>

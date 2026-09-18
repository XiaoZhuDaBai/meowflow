<template>
  <div class="list-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="输入数组" required>
        <el-input v-model="source" placeholder="{{upstream.list}}" />
      </el-form-item>

      <el-form-item label="操作类型" required>
        <el-select v-model="operation" style="width: 100%">
          <el-option value="filter" label="过滤 (filter)" />
          <el-option value="map" label="映射 (map)" />
          <el-option value="reduce" label="规约 (reduce)" />
          <el-option value="sort" label="排序 (sort)" />
          <el-option value="first" label="取首项" />
          <el-option value="unique" label="去重" />
          <el-option value="count" label="计数" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="['filter','map','reduce'].includes(operation)" label="表达式" required>
        <CodeEditor
          v-model="expression"
          :rows="3"
          placeholder="item.score > 0.5"
        />
        <div class="tip">
          <span v-if="operation === 'filter'">return true 保留项</span>
          <span v-else-if="operation === 'map'">return 新对象</span>
          <span v-else-if="operation === 'reduce'">return 累加值</span>
        </div>
      </el-form-item>

      <el-form-item v-if="operation === 'sort'" label="排序键">
        <el-input v-model="sortKey" placeholder="score" />
        <el-radio-group v-model="sortOrder" style="margin-top: 6px;">
          <el-radio-button value="asc">升序</el-radio-button>
          <el-radio-button value="desc">降序</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="变量">
        <VariablePicker inline :groups="variableGroups" @insert="insertAtSource" />
      </el-form-item>

      <el-form-item label="输出变量">
        <el-input v-model="outputName" placeholder="result" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import CodeEditor from '@/components/common/CodeEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';

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

const source = computed({
  get: () => props.modelValue?.source ?? '',
  set: (v: string) => patch({ source: v }),
});

const operation = computed({
  get: () => props.modelValue?.operation ?? 'filter',
  set: (v: string) => patch({ operation: v }),
});

const expression = computed({
  get: () => props.modelValue?.expression ?? '',
  set: (v: string) => patch({ expression: v }),
});

const sortKey = computed({
  get: () => props.modelValue?.sortKey ?? '',
  set: (v: string) => patch({ sortKey: v }),
});

const sortOrder = computed({
  get: () => props.modelValue?.sortOrder ?? 'asc',
  set: (v: string) => patch({ sortOrder: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'result',
  set: (v: string) => patch({ outputName: v }),
});

function insertAtSource(path: string) {
  source.value = `{{${path}}`;
}
</script>

<style scoped>
.tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
</style>

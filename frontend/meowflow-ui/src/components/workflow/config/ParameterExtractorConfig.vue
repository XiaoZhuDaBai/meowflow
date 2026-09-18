<template>
  <div class="param-extractor-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="AI 模型" required>
        <el-select
          v-model="modelId"
          :loading="loadingModels"
          placeholder="选择 AI 模型"
          style="width: 100%"
          filterable
        >
          <el-option
            v-for="m in enabledModels"
            :key="m.id"
            :value="m.id"
            :label="`${m.name} (${m.provider})`"
          />
          <template #empty>
            <div class="empty-tip">暂无可用模型</div>
          </template>
        </el-select>
      </el-form-item>

      <el-form-item label="提取 Schema" required>
        <CodeEditor
          v-model="schemaText"
          language="json"
          :rows="8"
          placeholder='{"name":"string","age":"number"}'
        />
        <div class="tip">JSON 对象,key 为字段名,value 为字段类型 (string/number/boolean/array)</div>
      </el-form-item>

      <el-form-item label="输入文本">
        <div class="input-row">
          <el-input v-model="text" placeholder="{{input.text}}" />
          <VariablePicker inline :groups="variableGroups" @insert="insertAtText" />
        </div>
      </el-form-item>

      <el-form-item label="提取指令">
        <CodeEditor
          v-model="instruction"
          :rows="3"
          placeholder="从以下文本中提取结构化参数"
        />
      </el-form-item>

      <el-form-item label="输出变量">
        <el-input v-model="outputName" placeholder="params" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import CodeEditor from '@/components/common/CodeEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';
import { systemApi } from '@/api/system';

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const enabledModels = ref<{ id: number | string; name: string; provider: string }[]>([]);
const loadingModels = ref(false);

async function loadModels() {
  loadingModels.value = true;
  try {
    enabledModels.value = await systemApi.enabledModels();
  } catch {
    enabledModels.value = [];
  } finally {
    loadingModels.value = false;
  }
}

onMounted(loadModels);

function patch(extra: Record<string, any>) {
  emit('update:modelValue', { ...props.modelValue, ...extra });
}

const modelId = computed({
  get: () => props.modelValue?.modelId ?? props.modelValue?.model ?? '',
  set: (v: number | string) => patch({ modelId: v }),
});

const schemaText = computed({
  get: () => {
    if (typeof props.modelValue?.schema === 'string') return props.modelValue.schema;
    if (props.modelValue?.schema) return JSON.stringify(props.modelValue.schema, null, 2);
    return '';
  },
  set: (v: string) => patch({ schema: v }),
});

const text = computed({
  get: () => props.modelValue?.text ?? '{{input.text}}',
  set: (v: string) => patch({ text: v }),
});

const instruction = computed({
  get: () => props.modelValue?.instruction ?? '从以下文本中提取结构化参数,严格按 Schema 输出 JSON:',
  set: (v: string) => patch({ instruction: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'params',
  set: (v: string) => patch({ outputName: v }),
});

function insertAtText(path: string) {
  text.value = `{{${path}}}`;
}
</script>

<style scoped>
.tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
.input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.empty-tip {
  padding: 6px 10px;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>

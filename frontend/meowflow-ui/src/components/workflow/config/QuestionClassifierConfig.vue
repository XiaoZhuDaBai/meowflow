<template>
  <div class="qc-config">
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

      <el-form-item label="分类标签" required>
        <CodeEditor
          v-model="categoriesText"
          language="json"
          :rows="5"
          placeholder='["技术支持","销售咨询","投诉建议"]'
        />
        <div class="tip">JSON 字符串数组,每个元素为一个分类标签</div>
      </el-form-item>

      <el-form-item label="输入问题">
        <div class="input-row">
          <el-input v-model="query" placeholder="{{trigger.query}}" />
          <VariablePicker inline :groups="variableGroups" @insert="insertAtQuery" />
        </div>
      </el-form-item>

      <el-form-item label="分类指令">
        <CodeEditor
          v-model="instruction"
          :rows="3"
          placeholder="你是文本分类助手,根据用户问题,选择最匹配的分类标签"
        />
      </el-form-item>

      <el-form-item label="输出变量">
        <el-input v-model="outputName" placeholder="category" />
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

const categoriesText = computed({
  get: () => {
    if (typeof props.modelValue?.categories === 'string') return props.modelValue.categories;
    if (Array.isArray(props.modelValue?.categories)) {
      return JSON.stringify(props.modelValue.categories, null, 2);
    }
    return '';
  },
  set: (v: string) => patch({ categories: v }),
});

const query = computed({
  get: () => props.modelValue?.query ?? '{{trigger.query}}',
  set: (v: string) => patch({ query: v }),
});

const instruction = computed({
  get: () => props.modelValue?.instruction ?? '',
  set: (v: string) => patch({ instruction: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'category',
  set: (v: string) => patch({ outputName: v }),
});

function insertAtQuery(path: string) {
  query.value = `{{${path}}}`;
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

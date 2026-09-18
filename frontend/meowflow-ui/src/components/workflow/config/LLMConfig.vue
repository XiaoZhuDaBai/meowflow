<template>
  <div class="llm-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="AI 模型" required>
        <el-select
          v-model="modelId"
          :loading="loadingModels"
          style="width: 100%"
          placeholder="选择已配置的 AI 模型"
          filterable
        >
          <el-option
            v-for="m in enabledModels"
            :key="m.id"
            :value="m.id"
            :label="`${m.name} (${m.provider})`"
          >
            <div class="model-option">
              <span class="m-name">{{ m.name }}</span>
              <span class="m-provider">{{ m.provider }}</span>
            </div>
          </el-option>
          <template #empty>
            <div class="empty-tip">
              暂无可用模型,请到
              <el-link type="primary" @click="openSettings">系统设置</el-link>
              中配置
            </div>
          </template>
        </el-select>
        <div class="model-info">
          <code v-if="selectedModel" class="model-key">{{ selectedModel.modelKey }}</code>
          <code v-else-if="modelRaw" class="model-key model-raw" title="模型不在已配置列表中，显示模板原始值">{{ modelRaw }}</code>
          <i v-if="selectedModel?.isDefault" class="fa-solid fa-star" style="color: var(--warning)"></i>
        </div>
      </el-form-item>

      <el-form-item label="系统提示词">
        <CodeEditor
          v-model="systemPrompt"
          placeholder="你是一个有帮助的助手"
          :rows="3"
        />
      </el-form-item>

      <el-form-item label="用户 Prompt">
        <CodeEditor
          v-model="prompt"
          placeholder="请根据以下输入回答：{{trigger.query}}"
          :rows="5"
        />
      </el-form-item>

      <el-form-item label="变量">
        <VariablePicker
          inline
          :groups="variableGroups"
          @insert="onInsertToPrompt"
        />
      </el-form-item>

      <el-form-item label="温度">
        <div class="slider-row">
          <el-slider v-model="temperature" :min="0" :max="2" :step="0.1" style="flex: 1" />
          <span class="slider-value">{{ temperature.toFixed(1) }}</span>
        </div>
        <div class="tip">数值越高,回答越发散有创意;数值越低,回答越确定稳定</div>
      </el-form-item>

      <el-form-item label="最大 Token">
        <el-input-number v-model="maxTokens" :min="100" :max="32000" :step="100" style="width: 100%" />
      </el-form-item>

      <el-form-item label="输出到">
        <el-input v-model="outputName" placeholder="result" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import CodeEditor from '@/components/common/CodeEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';
import { systemApi, type AiModelOption } from '@/api/system';

const router = useRouter();

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const enabledModels = ref<AiModelOption[]>([]);
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

/** 当前选中的模型 ID(优先读 modelId,兼容旧 modelKey 字段) */
const modelId = computed({
  get: () => {
    const v = props.modelValue?.modelId ?? props.modelValue?.model;
    if (v !== undefined && v !== null) return v;
    // 默认选择第一个 isDefault 或第一项
    const def = enabledModels.value.find((m) => m.isDefault) ?? enabledModels.value[0];
    return def?.id ?? '';
  },
  set: (v: number | string) =>
    emit('update:modelValue', { ...props.modelValue, modelId: v }),
});

const selectedModel = computed(() => enabledModels.value.find((m) => m.id === modelId.value));

/** 模板原始的 model 字符串（当 selectedModel 为 undefined 时，用于提示用户） */
const modelRaw = computed(() => props.modelValue?.model as string | undefined);

const systemPrompt = computed({
  get: () => props.modelValue?.systemPrompt ?? '',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, systemPrompt: v }),
});

const prompt = computed({
  get: () => props.modelValue?.prompt ?? '',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, prompt: v }),
});

const temperature = computed({
  get: () => Number(props.modelValue?.temperature ?? 0.7),
  set: (v: number) => emit('update:modelValue', { ...props.modelValue, temperature: v }),
});

const maxTokens = computed({
  get: () => Number(props.modelValue?.maxTokens ?? 2048),
  set: (v: number) => emit('update:modelValue', { ...props.modelValue, maxTokens: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'result',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, outputName: v }),
});

function onInsertToPrompt(path: string) {
  prompt.value = (prompt.value ?? '') + `{{${path}}}`;
}

function openSettings() {
  router.push('/settings');
}
</script>

<style scoped>
.llm-config { padding: 4px 0; }
.slider-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.slider-value {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  color: var(--text-secondary);
  min-width: 32px;
  text-align: right;
}
.tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
.model-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.m-name { font-weight: 500; }
.m-provider {
  font-size: 11px;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 4px;
}
.model-info {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--text-tertiary);
}
.model-key {
  font-family: ui-monospace, monospace;
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 4px;
}
.model-key.model-raw {
  color: var(--warning);
  background: var(--warning-bg);
}
.empty-tip {
  padding: 6px 10px;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>

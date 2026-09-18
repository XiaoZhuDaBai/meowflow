<template>
  <div class="agent-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="AI 模型" required>
        <el-select
          v-model="modelId"
          :loading="loadingModels"
          placeholder="选择驱动 Agent 的模型"
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
            <div class="empty-tip">暂无可用模型,请先到系统设置中配置</div>
          </template>
        </el-select>
      </el-form-item>

      <el-form-item label="推理策略" required>
        <el-radio-group v-model="strategy">
          <el-radio-button value="function-calling">
            <i class="fa-solid fa-bolt"></i>
            <span style="margin-left:4px">Function Calling</span>
          </el-radio-button>
          <el-radio-button value="react">
            <i class="fa-solid fa-diagram-project"></i>
            <span style="margin-left:4px">ReAct</span>
          </el-radio-button>
        </el-radio-group>
        <div class="strategy-tip">
          <span v-if="strategy === 'function-calling'">
            通过模型原生工具调用,推荐 GPT-4 / Claude 3.5+
          </span>
          <span v-else>
            显式 Thought → Action → Observation,适合不支持工具调用的模型
          </span>
        </div>
      </el-form-item>

      <el-form-item label="可用工具">
        <el-input
          v-model="tools"
          type="textarea"
          :rows="2"
          placeholder="tool-id-1,tool-id-2,tool-id-3 (逗号分隔)"
        />
        <div class="tip">引用后端 tool-mcp 注册的工具 ID,多个用逗号分隔</div>
      </el-form-item>

      <el-form-item label="指令" required>
        <CodeEditor
          v-model="instruction"
          :rows="5"
          placeholder="你是 {{agent_role}}, 使用工具帮助用户解决问题。支持 {{user_query}}"
        />
        <VariablePicker inline :groups="variableGroups" @insert="insertToInstruction" />
        <div class="tip">支持 Jinja 语法引用上游变量</div>
      </el-form-item>

      <el-form-item label="查询输入">
        <el-input v-model="query" placeholder="{{trigger.query}}" />
      </el-form-item>

      <el-form-item label="最大迭代">
        <el-input-number v-model="maxIterations" :min="1" :max="20" />
        <div class="tip">Agent 最多推理次数,避免无限循环</div>
      </el-form-item>

      <el-form-item label="温度">
        <div class="slider-row">
          <el-slider v-model="temperature" :min="0" :max="2" :step="0.1" style="flex: 1" />
          <span class="slider-value">{{ temperature.toFixed(1) }}</span>
        </div>
      </el-form-item>

      <el-form-item label="输出变量">
        <el-input v-model="outputName" placeholder="result" />
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

const strategy = computed({
  get: () => props.modelValue?.strategy ?? 'function-calling',
  set: (v: string) => patch({ strategy: v }),
});

const tools = computed({
  get: () => props.modelValue?.tools ?? '',
  set: (v: string) => patch({ tools: v }),
});

const instruction = computed({
  get: () => props.modelValue?.instruction ?? '',
  set: (v: string) => patch({ instruction: v }),
});

const query = computed({
  get: () => props.modelValue?.query ?? '{{trigger.query}}',
  set: (v: string) => patch({ query: v }),
});

const maxIterations = computed({
  get: () => Number(props.modelValue?.maxIterations ?? 5),
  set: (v: number) => patch({ maxIterations: v }),
});

const temperature = computed({
  get: () => Number(props.modelValue?.temperature ?? 0.7),
  set: (v: number) => patch({ temperature: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'result',
  set: (v: string) => patch({ outputName: v }),
});

function insertToInstruction(path: string) {
  instruction.value = (instruction.value ?? '') + `{{${path}}}`;
}
</script>

<style scoped>
.agent-config { padding: 4px 0; }
.tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
.strategy-tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
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
.empty-tip {
  padding: 6px 10px;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>

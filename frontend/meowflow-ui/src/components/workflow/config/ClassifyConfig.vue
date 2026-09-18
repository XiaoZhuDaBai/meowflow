<template>
  <div class="classify-config">
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
      </el-form-item>

      <el-form-item label="输入变量">
        <div class="input-row">
          <el-input v-model="input" placeholder="{{trigger.text}}" />
          <VariablePicker inline :groups="variableGroups" @insert="insertAtInput" />
        </div>
      </el-form-item>

      <el-form-item label="候选类别">
        <div class="categories">
          <div
            v-for="(cat, idx) in categories"
            :key="cat.id ?? idx"
            class="category-row"
          >
            <el-input v-model="cat.name" placeholder="分类名称" size="small" />
            <el-input v-model="cat.description" placeholder="分类说明 (帮助 AI 更好理解)" size="small" />
            <el-button size="small" link @click="removeCategory(idx)">
              <i class="fa-solid fa-xmark"></i>
            </el-button>
          </div>
          <el-button size="small" link @click="addCategory">
            <i class="fa-solid fa-plus"></i>
            <span style="margin-left:4px">添加分类</span>
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="系统提示词">
        <CodeEditor
          v-model="systemPrompt"
          :rows="3"
          placeholder="你是一个分类助手..."
        />
      </el-form-item>

      <el-form-item label="允许 fallback">
        <el-switch v-model="allowFallback" />
        <span class="tip">当所有分类都不匹配时,返回其他</span>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import CodeEditor from '@/components/common/CodeEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';
import { systemApi } from '@/api/system';

const router = useRouter();

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

interface Category { id: string; name: string; description: string }

const DEFAULT_CATS: Category[] = [
  { id: 'cat-1', name: '问题', description: '客户咨询的技术问题' },
  { id: 'cat-2', name: '建议', description: '客户提出的改进建议' },
  { id: 'cat-3', name: '投诉', description: '客户的不满与投诉' },
];

function ensureCats(): Category[] {
  const arr = props.modelValue?.categories;
  if (Array.isArray(arr) && arr.length) {
    return arr.map((item, idx) => {
      if (typeof item === 'string') {
        return { id: `cat-${idx}`, name: item, description: '' };
      }
      return item as Category;
    });
  }
  return DEFAULT_CATS;
}

const modelId = computed({
  get: () => {
    const v = props.modelValue?.modelId ?? props.modelValue?.model;
    if (v !== undefined && v !== null) return v;
    const def = enabledModels.value[0];
    return def?.id ?? '';
  },
  set: (v: number | string) => emit('update:modelValue', { ...props.modelValue, modelId: v }),
});

const input = computed({
  get: () => props.modelValue?.input ?? '{{trigger.text}}',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, input: v }),
});

const categories = computed({
  get: () => ensureCats(),
  set: (v: Category[]) => emit('update:modelValue', {
    ...props.modelValue,
    categories: v.map((item) => item.name.trim()).filter(Boolean),
  }),
});

const systemPrompt = computed({
  get: () => props.modelValue?.systemPrompt ?? '你是一个专业的文本分类助手,根据输入内容,选择最匹配的分类标签并说明理由。',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, systemPrompt: v }),
});

const allowFallback = computed({
  get: () => props.modelValue?.allowFallback ?? true,
  set: (v: boolean) => emit('update:modelValue', { ...props.modelValue, allowFallback: v }),
});

function addCategory() {
  categories.value = [
    ...categories.value,
    { id: `cat-${Date.now()}`, name: '', description: '' },
  ];
}

function removeCategory(idx: number) {
  const arr = categories.value.slice();
  arr.splice(idx, 1);
  categories.value = arr;
}

function insertAtInput(path: string) {
  input.value = `{{${path}}}`;
}

function openSettings() {
  router.push('/settings');
}
</script>

<style scoped>
.categories {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.category-row {
  display: grid;
  grid-template-columns: 1fr 2fr auto;
  gap: 6px;
  align-items: center;
}
.input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.tip {
  margin-left: 12px;
  font-size: 11px;
  color: var(--text-tertiary);
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
.empty-tip {
  padding: 6px 10px;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>

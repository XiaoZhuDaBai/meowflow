<template>
  <el-dialog
    :model-value="modelValue"
    title="测试运行"
    width="560"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <p class="hint">
      <i class="fa-solid fa-circle-info"></i>
      <span>设置测试输入 (Mock 环境,可填写任意值)</span>
    </p>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="表单输入" name="form">
        <el-form label-width="100px" size="small">
          <el-form-item
            v-for="(f, idx) in fields"
            :key="idx"
            :label="f.label ?? f.key"
          >
            <el-input
              v-if="f.type === 'string'"
              :model-value="formValues[f.key] ?? ''"
              @update:model-value="(v) => setValue(f.key, v)"
              :placeholder="f.placeholder"
            />
            <el-input-number
              v-else-if="f.type === 'number'"
              :model-value="Number(formValues[f.key] ?? 0)"
              @update:model-value="(v: any) => setValue(f.key, v)"
              style="width: 100%;"
            />
            <el-switch
              v-else-if="f.type === 'boolean'"
              :model-value="!!formValues[f.key]"
              @update:model-value="(v) => setValue(f.key, v)"
            />
            <CodeEditor
              v-else-if="f.type === 'json'"
              v-model="rawJson"
              language="json"
              validate="json"
              :rows="6"
              @update:model-value="onJsonChange"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="原始 JSON" name="raw">
        <CodeEditor
          v-model="rawJson"
          language="json"
          validate="json"
          :rows="10"
          placeholder='{ "query": "hello" }'
        />
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="resetDefaults">
        <i class="fa-solid fa-rotate-right"></i><span style="margin-left:4px">使用示例</span>
      </el-button>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="running" @click="onSubmit">
        <i class="fa-solid fa-play"></i><span style="margin-left:4px">开始测试</span>
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import CodeEditor from '@/components/common/CodeEditor.vue';

interface Field {
  key: string;
  label?: string;
  type: 'string' | 'number' | 'boolean' | 'json';
  placeholder?: string;
}

const props = defineProps<{
  modelValue: boolean;
  fields?: Field[];
  defaultJson?: string;
  running?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'submit', payload: Record<string, any>): void;
}>();

const activeTab = ref<'form' | 'raw'>('raw');
const formValues = ref<Record<string, any>>({});
const rawJson = ref(props.defaultJson ?? '{\n  "query": "hello"\n}');

const fields = computed<Field[]>(() =>
  props.fields?.length
    ? props.fields
    : [
        { key: 'query', label: '查询内容', type: 'string', placeholder: '请输入查询' },
        { key: 'user', label: '用户', type: 'string', placeholder: '可选' },
      ],
);

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      resetDefaults();
    }
  },
);

function resetDefaults() {
  formValues.value = { query: 'hello', user: 'demo' };
  rawJson.value = props.defaultJson ?? '{\n  "query": "hello",\n  "user": "demo"\n}';
}

function setValue(k: string, v: any) {
  formValues.value[k] = v;
  rawJson.value = JSON.stringify(formValues.value, null, 2);
}

function onJsonChange(v: string) {
  try {
    formValues.value = JSON.parse(v);
  } catch {
    // ignore
  }
}

function onSubmit() {
  let payload: Record<string, any>;
  try {
    payload = JSON.parse(rawJson.value);
  } catch (e: any) {
    payload = { error: e.message, raw: rawJson.value };
  }
  emit('submit', payload);
}
</script>

<style scoped>
.hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--warning);
  background: var(--warning-bg);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  margin: 4px 0 12px;
}
</style>

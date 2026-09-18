<template>
  <div class="webhook-trigger-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="请求方法">
        <el-radio-group v-model="method">
          <el-radio-button value="POST">POST</el-radio-button>
          <el-radio-button value="GET">GET</el-radio-button>
          <el-radio-button value="PUT">PUT</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="路径">
        <el-input v-model="path" placeholder="/hooks/incoming">
          <template #prepend>{{ originPrefix }}</template>
        </el-input>
        <div class="tip">路径必须以 / 开头,推荐使用 Hooks 风格</div>
      </el-form-item>

      <el-form-item label="认证 Token">
        <el-input v-model="authToken" placeholder="可选,用于验证请求来源" />
      </el-form-item>

      <el-form-item label="入参 Schema">
        <CodeEditor
          v-model="schemaStr"
          language="json"
          validate="json"
          :rows="6"
          placeholder='{ "type": "object", "properties": { "query": { "type": "string" } } }'
        />
      </el-form-item>

      <el-form-item label="输出变量">
        <div class="output-preview">
          <div class="var-row" v-for="v in outputs" :key="v.path">
            <code>{{ varToken(v.path) }}</code>
            <span>{{ v.description }}</span>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <el-divider />

    <WebhookTester
      :default-url="fullUrl"
      :default-method="method"
      :default-body="sampleBody"
      @sent="onTestSent"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import CodeEditor from '@/components/common/CodeEditor.vue';
import WebhookTester from './WebhookTester.vue';

const props = defineProps<{
  modelValue: Record<string, any>;
  workflowId?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

function patch(extra: Record<string, any>) {
  emit('update:modelValue', { ...props.modelValue, ...extra });
}

const method = computed({
  get: () => props.modelValue?.method ?? 'POST',
  set: (v: string) => patch({ method: v }),
});

const path = computed({
  get: () => props.modelValue?.path ?? '/hooks/incoming',
  set: (v: string) => patch({ path: v }),
});

const authToken = computed({
  get: () => props.modelValue?.authToken ?? '',
  set: (v: string) => patch({ authToken: v }),
});

const schemaStr = computed({
  get: () => props.modelValue?.schema
    ? (typeof props.modelValue.schema === 'string' ? props.modelValue.schema : JSON.stringify(props.modelValue.schema, null, 2))
    : '',
  set: (v: string) => {
    try {
      patch({ schema: v ? JSON.parse(v) : undefined });
    } catch {
      patch({ schema: v });
    }
  },
});

const originPrefix = computed(() => {
  if (typeof window !== 'undefined') {
    const code = props.workflowId ? `wf_${props.workflowId}` : 'wf_<workflowId>';
    return `${window.location.origin}/workflow/api/webhook/${code}`;
  }
  return '/workflow/api/webhook/wf_<workflowId>';
});

const fullUrl = computed(() => `${originPrefix.value}${path.value}`);
const sampleBody = ref('{\n  "query": "hello"\n}');

const outputs = computed(() => [
  { path: 'body', description: '请求体 (已解析的 JSON 对象)' },
  { path: 'headers', description: '请求头' },
  { path: 'query', description: 'URL 查询参数' },
  { path: 'method', description: 'HTTP 方法' },
]);

function varToken(path: string) {
  return '{{' + path + '}}';
}

function onTestSent(payload: { status: number; time: number }) {
  // no-op: 父组件可监听测试结果
}
</script>

<style scoped>
.tip { font-size: 11px; color: var(--text-tertiary); margin-top: 4px; }
.output-preview {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}
.var-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.var-row code {
  font-family: ui-monospace, monospace;
  background: var(--bg-primary);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--primary);
}
.var-row span { color: var(--text-tertiary); }
</style>

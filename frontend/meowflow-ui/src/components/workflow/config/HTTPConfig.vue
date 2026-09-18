<template>
  <div class="http-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="请求方法">
        <el-select v-model="method">
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
          <el-option label="PUT" value="PUT" />
          <el-option label="DELETE" value="DELETE" />
          <el-option label="PATCH" value="PATCH" />
        </el-select>
      </el-form-item>

      <el-form-item label="请求 URL">
        <el-input v-model="url" placeholder="https://api.example.com/endpoint?q={{trigger.id}}" />
        <VariablePicker inline :groups="variableGroups" @insert="(p) => insertAt('url', p)" />
      </el-form-item>

      <el-form-item label="认证方式">
        <el-select v-model="authType">
          <el-option value="none" label="无认证" />
          <el-option value="bearer" label="Bearer Token" />
          <el-option value="apiKey" label="API Key (Header)" />
          <el-option value="basic" label="Basic Auth" />
        </el-select>
      </el-form-item>

      <template v-if="authType !== 'none'">
        <el-form-item v-if="authType === 'bearer'" label="Token">
          <el-input v-model="auth.token" show-password type="password" />
        </el-form-item>
        <template v-else-if="authType === 'apiKey'">
          <el-form-item label="Header 名">
            <el-input v-model="auth.headerName" placeholder="X-API-Key" />
          </el-form-item>
          <el-form-item label="Value">
            <el-input v-model="auth.value" show-password type="password" />
          </el-form-item>
        </template>
        <template v-else-if="authType === 'basic'">
          <el-form-item label="用户名">
            <el-input v-model="auth.username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="auth.password" show-password type="password" />
          </el-form-item>
        </template>
      </template>

      <el-form-item label="请求头">
        <KeyValueEditor v-model="headersList" />
      </el-form-item>

      <el-form-item v-if="method !== 'GET'" label="请求体">
        <el-radio-group v-model="bodyType">
          <el-radio-button value="json">JSON</el-radio-button>
          <el-radio-button value="form">Form</el-radio-button>
          <el-radio-button value="raw">Raw</el-radio-button>
        </el-radio-group>
        <CodeEditor
          v-if="bodyType === 'json' || bodyType === 'raw'"
          v-model="bodyStr"
          :validate="bodyType === 'json' ? 'json' : undefined"
          :rows="6"
          :placeholder="bodyPlaceholder"
        />
        <KeyValueEditor v-else v-model="bodyForm" />
        <VariablePicker
          v-if="bodyType === 'json' || bodyType === 'raw'"
          inline
          :groups="variableGroups"
          @insert="(p) => insertAt('bodyStr', p)"
        />
      </el-form-item>

      <el-form-item label="超时 (ms)">
        <el-input-number v-model="timeoutMs" :min="1000" :max="60000" :step="1000" />
      </el-form-item>

      <el-form-item label="失败重试">
        <el-input-number v-model="retries" :min="0" :max="5" />
        <span class="tip">重试次数 (0 表示不重试)</span>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import CodeEditor from '@/components/common/CodeEditor.vue';
import KeyValueEditor from './KeyValueEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';

interface KVItem { key: string; value: string }

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const method = computed({
  get: () => props.modelValue?.method ?? 'GET',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, method: v }),
});

const url = computed({
  get: () => props.modelValue?.url ?? '',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, url: v }),
});

const authType = computed({
  get: () => props.modelValue?.auth?.type ?? 'none',
  set: (v: string) => {
    emit('update:modelValue', {
      ...props.modelValue,
      auth: { ...(props.modelValue?.auth ?? {}), type: v },
    });
  },
});

const auth = computed({
  get: () => props.modelValue?.auth ?? {},
  set: (v: Record<string, any>) =>
    emit('update:modelValue', { ...props.modelValue, auth: v }),
});

const headersList = computed({
  get: () => {
    const h = props.modelValue?.headers;
    if (Array.isArray(h)) return h as KVItem[];
    if (h && typeof h === 'object') return Object.entries(h).map(([key, value]) => ({ key, value: String(value) }));
    return [];
  },
  set: (v: KVItem[]) => {
    const obj: Record<string, string> = {};
    v.forEach((kv) => {
      if (kv.key) obj[kv.key] = kv.value;
    });
    emit('update:modelValue', { ...props.modelValue, headers: obj });
  },
});

const bodyType = computed({
  get: () => props.modelValue?.bodyType ?? 'json',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, bodyType: v }),
});

const bodyPlaceholder = computed(() =>
  bodyType.value === 'json' ? '{ "key": "value" }' : 'raw body...',
);

const bodyStr = computed({
  get: () => props.modelValue?.body ?? '',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, body: v }),
});

const bodyForm = computed({
  get: () => {
    const f = props.modelValue?.bodyForm;
    if (Array.isArray(f)) return f as KVItem[];
    if (f && typeof f === 'object')
      return Object.entries(f).map(([key, value]) => ({ key, value: String(value) }));
    return [];
  },
  set: (v: KVItem[]) => {
    const obj: Record<string, string> = {};
    v.forEach((kv) => {
      if (kv.key) obj[kv.key] = kv.value;
    });
    emit('update:modelValue', { ...props.modelValue, bodyForm: obj });
  },
});

const timeoutMs = computed({
  get: () => Number(props.modelValue?.timeoutMs ?? 10000),
  set: (v: number) => emit('update:modelValue', { ...props.modelValue, timeoutMs: v }),
});

const retries = computed({
  get: () => Number(props.modelValue?.retries ?? 0),
  set: (v: number) => emit('update:modelValue', { ...props.modelValue, retries: v }),
});

function insertAt(target: 'url' | 'bodyStr', path: string) {
  if (target === 'url') {
    url.value = (url.value ?? '') + `{{${path}}}`;
  } else {
    bodyStr.value = (bodyStr.value ?? '') + `{{${path}}}`;
  }
}
</script>

<style scoped>
.tip {
  margin-left: 12px;
  font-size: 11px;
  color: var(--text-tertiary);
}
.http-config {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>

<template>
  <div class="webhook-tester">
    <div class="head">
      <span class="title">Webhook 自检</span>
      <el-tag size="small" :type="resultTag.type">{{ resultTag.label }}</el-tag>
    </div>
    <el-form label-width="80px" size="small">
      <el-form-item label="请求方法">
        <el-radio-group v-model="method" size="small">
          <el-radio-button value="GET">GET</el-radio-button>
          <el-radio-button value="POST">POST</el-radio-button>
          <el-radio-button value="PUT">PUT</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="请求地址">
        <el-input v-model="url" placeholder="https://example.com/webhook" />
      </el-form-item>
      <el-form-item label="Headers">
        <KeyValueEditor v-model="headers" />
      </el-form-item>
      <el-form-item v-if="method !== 'GET'" label="Body">
        <CodeEditor
          v-model="body"
          language="json"
          validate="json"
          :rows="6"
          placeholder='{ "key": "value" }'
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="running" @click="onSend">
          <i class="fa-solid fa-paper-plane"></i>
          <span style="margin-left:4px">发送测试请求</span>
        </el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div v-if="response" class="response">
      <div class="resp-head">
        <span class="resp-status" :class="{ ok: response.ok }">
          {{ response.status }} {{ response.statusText }}
        </span>
        <span class="resp-time">{{ response.time }} ms</span>
      </div>
      <el-tabs>
        <el-tab-pane label="Body">
          <pre class="resp-body">{{ response.body }}</pre>
        </el-tab-pane>
        <el-tab-pane label="Headers">
          <div class="resp-headers">
            <div v-for="(v, k) in response.headers" :key="k" class="kv">
              <span class="k">{{ k }}</span>
              <span class="v">{{ v }}</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import KeyValueEditor from './KeyValueEditor.vue';
import CodeEditor from '@/components/common/CodeEditor.vue';

interface KVItem { key: string; value: string }

const props = defineProps<{
  defaultUrl?: string;
  defaultMethod?: 'GET' | 'POST' | 'PUT';
  defaultBody?: string;
}>();

const emit = defineEmits<{
  (e: 'sent', payload: { status: number; body: string; time: number }): void;
}>();

const method = ref<'GET' | 'POST' | 'PUT'>(props.defaultMethod ?? 'POST');
const url = ref(props.defaultUrl ?? '');
const headers = ref<KVItem[]>([
  { key: 'Content-Type', value: 'application/json' },
]);
const body = ref(props.defaultBody ?? '{\n  "query": "hello"\n}');
const running = ref(false);

interface ResponsePayload {
  status: number;
  statusText: string;
  time: number;
  ok: boolean;
  body: string;
  headers: Record<string, string>;
}

const response = ref<ResponsePayload | null>(null);

const resultTag = computed<{ type: 'success' | 'primary' | 'warning' | 'info' | 'danger'; label: string }>(() => {
  if (!response.value) return { type: 'info', label: '未发送' };
  if (response.value.ok) return { type: 'success', label: '成功' };
  return { type: 'danger', label: '失败' };
});

async function onSend() {
  if (!url.value) return;
  running.value = true;
  const started = Date.now();
  try {
    const hdrs: Record<string, string> = {};
    headers.value.forEach((h) => {
      if (h.key) hdrs[h.key] = h.value;
    });
    const opts: RequestInit = { method: method.value, headers: hdrs };
    if (method.value !== 'GET' && body.value) {
      opts.body = body.value;
    }
    const r = await fetch(url.value, opts);
    const elapsed = Date.now() - started;
    const text = await r.text();
    response.value = {
      status: r.status,
      statusText: r.statusText,
      ok: r.ok,
      time: elapsed,
      body: text,
      headers: Object.fromEntries(r.headers.entries()),
    };
    emit('sent', { status: r.status, body: text, time: elapsed });
  } catch (e: any) {
    response.value = {
      status: 0,
      statusText: 'NETWORK_ERROR',
      ok: false,
      time: Date.now() - started,
      body: e?.message ?? String(e),
      headers: {},
    };
  } finally {
    running.value = false;
  }
}

function reset() {
  response.value = null;
  if (props.defaultUrl) url.value = props.defaultUrl;
  if (props.defaultBody) body.value = props.defaultBody;
}
</script>

<style scoped>
.webhook-tester {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.response {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.resp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
  font-size: 12px;
}
.resp-status {
  font-family: ui-monospace, monospace;
  color: var(--danger);
  font-weight: 600;
}
.resp-status.ok {
  color: var(--success);
}
.resp-time {
  color: var(--text-tertiary);
}
.resp-body {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  padding: 12px;
  margin: 0;
  background: var(--bg-secondary);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 280px;
  overflow: auto;
}
.resp-headers {
  padding: 8px 12px;
  max-height: 240px;
  overflow: auto;
}
.kv {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  padding: 4px 0;
  border-bottom: 1px dashed var(--border-light);
}
.kv:last-child { border-bottom: none; }
.kv .k {
  color: var(--text-tertiary);
  font-family: ui-monospace, monospace;
}
.kv .v {
  color: var(--text-primary);
  font-family: ui-monospace, monospace;
}
</style>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div class="result-summary" :class="statusClass">
      <div class="icon">
        <i :class="iconClass"></i>
      </div>
      <div class="meta">
        <div class="title">{{ statusLabel }}</div>
        <div class="subtitle">{{ subtitle }}</div>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="输入" name="input">
        <pre class="code-block">{{ prettyJson(payload.input) }}</pre>
      </el-tab-pane>
      <el-tab-pane label="输出" name="output">
        <pre class="code-block">{{ prettyJson(payload.output) }}</pre>
      </el-tab-pane>
      <el-tab-pane label="节点结果" name="nodes">
        <div class="nodes">
          <div
            v-for="(n, idx) in payload.nodes"
            :key="idx"
            class="node-row"
            :class="`status-${n.status}`"
          >
            <div class="node-head">
              <span class="node-name">「{{ n.nodeName }}」</span>
              <el-tag size="small" :type="statusTagType(n.status)">{{ statusText[n.status] }}</el-tag>
              <span class="node-time">{{ n.duration ?? '-' }}</span>
            </div>
            <div v-if="n.error" class="node-error">
              <i class="fa-solid fa-circle-exclamation"></i>
              {{ n.error }}
            </div>
            <div v-else-if="n.output" class="node-output">
              <div class="out-title">输出</div>
              <pre class="out-body">{{ prettyJson(n.output) }}</pre>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="onViewDetail">
        <i class="fa-solid fa-list-ul"></i><span style="margin-left:4px">查看完整详情</span>
      </el-button>
      <el-button type="primary" @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

interface NodeResult {
  nodeId: string;
  nodeName: string;
  status: 'pending' | 'running' | 'success' | 'failed' | 'skipped';
  duration?: number | string;
  output?: any;
  error?: string;
}

const props = defineProps<{
  modelValue: boolean;
  executionId?: string;
  status: 'success' | 'failed' | 'running' | 'cancelled';
  duration?: number;
  cost?: number;
  payload: {
    input: any;
    output: any;
    nodes: NodeResult[];
    error?: string;
  };
  width?: string;
  title?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'view-detail'): void;
}>();

const router = useRouter();
const activeTab = ref<'input' | 'output' | 'nodes'>('output');

const width = computed(() => props.width ?? '720');

const title = computed(() => props.title ?? '测试运行结果');

const subtitle = computed(() => {
  if (props.payload.error) return props.payload.error;
  if (props.status === 'running') return '执行中...';
  if (props.status === 'success') return `耗时 ${props.duration ?? 0} ms · 成本 ¥ ${(props.cost ?? 0).toFixed(3)}`;
  return '查看各节点结果';
});

const statusText: Record<NodeResult['status'], string> = {
  pending: '等待',
  running: '运行中',
  success: '成功',
  failed: '失败',
  skipped: '跳过',
};

const statusLabel = computed(() =>
  ({ success: '执行成功', failed: '执行失败', running: '运行中', cancelled: '已取消' })[props.status],
);
const iconClass = computed(() =>
  ({ success: 'fa-solid fa-circle-check', failed: 'fa-solid fa-circle-xmark', running: 'fa-solid fa-spinner fa-spin', cancelled: 'fa-solid fa-ban' })[props.status],
);

const statusClass = computed(() => props.status);

function statusTagType(s: NodeResult['status']) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    success: 'success',
    failed: 'danger',
    running: 'warning',
    pending: 'info',
    skipped: 'info',
  };
  return map[s] ?? 'info';
}

function prettyJson(obj: any) {
  if (obj === undefined) return '(空)';
  try {
    return JSON.stringify(obj, null, 2);
  } catch {
    return String(obj);
  }
}

function onViewDetail() {
  if (props.executionId) {
    emit('update:modelValue', false);
    router.push(`/executions/${props.executionId}`);
  } else {
    emit('view-detail');
  }
}
</script>

<style scoped>
.result-summary {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
}
.result-summary.success .icon { color: var(--success); }
.result-summary.failed .icon { color: var(--danger); }
.result-summary.running .icon { color: var(--warning); }
.result-summary.cancelled .icon { color: var(--text-tertiary); }
.result-summary .icon i { font-size: 32px; }
.result-summary .title { font-size: 16px; font-weight: 600; }
.result-summary .subtitle { font-size: 12px; color: var(--text-tertiary); }

.code-block {
  background: var(--bg-dark);
  color: #e2e8f0;
  padding: 12px;
  border-radius: var(--radius-sm);
  font-family: ui-monospace, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow: auto;
}

.nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.node-row {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.node-row.status-success { border-color: var(--success); }
.node-row.status-failed  { border-color: var(--danger); }
.node-row.status-running { border-color: var(--warning); }

.node-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.node-name { font-weight: 600; flex: 1; }
.node-time { color: var(--text-tertiary); font-family: ui-monospace, monospace; font-size: 12px; }
.node-error {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fef2f2;
  color: var(--danger);
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
}
.out-title {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}
.out-body {
  font-family: ui-monospace, monospace;
  font-size: 11px;
  background: var(--bg-secondary);
  padding: 8px;
  border-radius: var(--radius-sm);
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}
</style>

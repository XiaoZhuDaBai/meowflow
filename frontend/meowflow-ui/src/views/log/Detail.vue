<template>
  <div class="page execution-detail" v-loading="loading">
    <div class="page-header">
      <div>
        <div class="page-title">执行详情</div>
        <div class="page-subtitle">
          <code class="exec-id">{{ execution?.id }}</code>
          <span class="muted">·</span>
          <span class="muted">{{ execution?.workflowName }}</span>
        </div>
      </div>
      <div class="flex gap-8">
        <el-button @click="$router.back()">
          <i class="fa-solid fa-arrow-left"></i><span style="margin-left:4px">返回</span>
        </el-button>
        <el-button
          v-if="execution?.status === 'failed' && execution?.workflowId"
          type="warning"
          :loading="retrying"
          @click="onRetry"
        >
          <i class="fa-solid fa-rotate-right"></i><span style="margin-left:4px">重试</span>
        </el-button>
        <el-button
          v-if="execution?.status === 'running'"
          type="danger"
          :loading="cancelling"
          @click="onCancel"
        >
          <i class="fa-solid fa-ban"></i><span style="margin-left:4px">取消执行</span>
        </el-button>
        <el-button
          v-if="execution?.workflowId"
          type="primary"
          @click="$router.push(`/editor/${execution.workflowId}`)"
        >
          <i class="fa-solid fa-pen-to-square"></i><span style="margin-left:4px">编辑工作流</span>
        </el-button>
      </div>
    </div>

    <div v-if="execution" class="cards">
      <div class="kv-card">
        <div class="k">状态</div>
        <div class="v"><StatusTag :status="execution.status" /></div>
      </div>
      <div class="kv-card">
        <div class="k">触发方式</div>
        <div class="v">{{ triggerLabel(execution.trigger) }}</div>
      </div>
      <div class="kv-card">
        <div class="k">开始时间</div>
        <div class="v">{{ formatDate(execution.startTime, 'YYYY-MM-DD HH:mm:ss') }}</div>
      </div>
      <div class="kv-card">
        <div class="k">结束时间</div>
        <div class="v">{{ execution.endTime ? formatDate(execution.endTime) : '-' }}</div>
      </div>
      <div class="kv-card">
        <div class="k">耗时</div>
        <div class="v">{{ formatDuration(execution.duration) }}</div>
      </div>
      <div class="kv-card">
        <div class="k">成本</div>
        <div class="v">{{ formatCost(execution.cost) }}</div>
      </div>
    </div>

    <div v-if="execution?.error" class="error-banner">
      <i class="fa-solid fa-circle-exclamation"></i>
      <span>{{ execution.error }}</span>
    </div>

    <div v-if="waitingNode" class="human-input-band">
      <div class="human-title">
        <i class="fa-solid fa-user-pen"></i>
        <span>等待人工输入：{{ waitingNode.nodeName || waitingNode.nodeId }}</span>
      </div>
      <el-input
        v-model="humanText"
        type="textarea"
        :rows="2"
        placeholder="输入需要提交的内容"
      />
      <el-button type="primary" :loading="submittingHuman" @click="submitHuman">
        <i class="fa-solid fa-paper-plane"></i><span style="margin-left:4px">提交并继续</span>
      </el-button>
    </div>

    <div class="layout">
      <div class="card">
        <div class="card-title">
          <i class="fa-solid fa-diagram-project"></i>
          <span>节点时间线</span>
        </div>
        <div v-if="!nodeExecs.length" class="empty-mini">
          <i class="fa-solid fa-spinner fa-spin"></i>
          <span>暂无节点执行结果</span>
        </div>
        <div v-else class="timeline">
          <div
            v-for="(n, idx) in nodeExecs"
            :key="idx"
            class="timeline-row"
            :class="`status-${n.status}`"
          >
            <div class="dot"></div>
            <div class="content">
              <div class="head">
                <span class="name">「{{ n.nodeName }}」</span>
                <el-tag size="small" :type="tagType(n.status)">{{ statusLabel(n.status) }}</el-tag>
                <span class="muted">{{ formatDuration(n.duration) }}</span>
              </div>
              <div v-if="n.error" class="err">
                <i class="fa-solid fa-circle-exclamation"></i>
                {{ n.error }}
              </div>
              <div v-if="n.output" class="out">
                <div class="out-title">输出</div>
                <pre class="out-body">{{ prettyJson(n.output) }}</pre>
              </div>
              <div v-if="n.input" class="in">
                <div class="in-title">输入</div>
                <pre class="in-body">{{ prettyJson(n.input) }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card log-card">
        <LogPanel
          :entries="logs"
          :running="execution?.status === 'running'"
          @clear="onClearLogs"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import StatusTag from '@/components/common/StatusTag.vue';
import LogPanel from '@/components/log/LogPanel.vue';
import { executionApi } from '@/api/execution';
import { ElMessage } from '@/utils/notify';
import { formatCost, formatDate, formatDuration } from '@/utils/format';
import type { Execution, ExecutionLog, NodeExecution } from '@/types/execution';
import type { NodeExecutionStatus } from '@/types/execution';

const route = useRoute();
const execution = ref<Execution | null>(null);
const nodeExecs = ref<NodeExecution[]>([]);
const logs = ref<ExecutionLog[]>([]);
const loading = ref(false);
const cancelling = ref(false);
const retrying = ref(false);
const submittingHuman = ref(false);
const humanText = ref('');
let sseHandle: (() => void) | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;

const waitingNode = computed<NodeExecution | null>(() => {
  return nodeExecs.value.find((n) => {
    const type = String(n.nodeType ?? '').toLowerCase();
    return n.status === 'running' && (type === 'human_input' || type === 'human-input');
  }) ?? null;
});

const TRIGGER_MAP: Record<string, string> = {
  manual: '手动触发',
  webhook: 'Webhook',
  schedule: '定时',
  form: '表单',
};

function triggerLabel(t: any): string {
  return TRIGGER_MAP[t as string] ?? String(t);
}

const STATUS_LABEL: Record<string, string> = {
  pending: '等待',
  running: '运行中',
  success: '成功',
  failed: '失败',
  skipped: '跳过',
};
function statusLabel(s: string) { return STATUS_LABEL[s] ?? s; }

function tagType(s: string): 'success' | 'danger' | 'warning' | 'info' {
  if (s === 'success') return 'success';
  if (s === 'failed') return 'danger';
  if (s === 'running') return 'warning';
  return 'info';
}

function prettyJson(obj: any) {
  if (obj === undefined || obj === null) return '(空)';
  try {
    return JSON.stringify(obj, null, 2);
  } catch {
    return String(obj);
  }
}

async function load() {
  loading.value = true;
  try {
    const id = String(route.params.id);
    const [exec, nodes, logList] = await Promise.all([
      executionApi.getById(id),
      executionApi.getNodeExecutions(id).catch(() => []),
      executionApi.getLogs(id).catch(() => []),
    ]);
    execution.value = exec;
    nodeExecs.value = nodes ?? [];
    logs.value = logList ?? [];

    // 若仍在运行,订阅实时事件流并启动轮询
    if (exec.status === 'running') {
      startSse(id);
      startPolling(id);
    }
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载执行详情失败');
  } finally {
    loading.value = false;
  }
}

function startPolling(execId: string) {
  if (pollTimer) return;
  pollTimer = setInterval(async () => {
    if (!execution.value || execution.value.status !== 'running') {
      stopPolling();
      return;
    }
    try {
      const updated = await executionApi.getById(execId);
      execution.value = updated;
      if (updated.status !== 'running') {
        stopPolling();
        // 重新拉取节点和日志
        const [nodes, logList] = await Promise.all([
          executionApi.getNodeExecutions(execId).catch(() => []),
          executionApi.getLogs(execId).catch(() => []),
        ]);
        nodeExecs.value = nodes ?? [];
        logs.value = logList ?? [];
      }
    } catch {
      // 忽略轮询错误
    }
  }, 3000);
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

function startSse(execId: string) {
  sseHandle = executionApi.subscribeEvents(execId, {
    onEvent: (eventType, data) => {
      const nodeName = data?.node_name;
      const nodeId = data?.node_id;
      let level: ExecutionLog['level'] = 'info';
      let message = '';
      switch (eventType) {
        case 'execution_started':
          message = '工作流开始执行';
          break;
        case 'execution_succeeded':
          level = 'success';
          message = '工作流执行成功';
          break;
        case 'execution_failed':
          level = 'error';
          message = `工作流执行失败: ${data?.data?.error ?? ''}`;
          break;
        case 'execution_cancelled':
          level = 'warning';
          message = '工作流已取消';
          break;
        case 'node_started':
          message = `节点「${nodeName || nodeId}」开始执行`;
          break;
        case 'node_finished': {
          const st = (data?.data?.status || 'success').toLowerCase();
          level = st === 'failed' || st === 'error' ? 'error' : 'success';
          message = `节点「${nodeName || nodeId}」执行${st === 'failed' ? '失败' : '成功'}`;
          break;
        }
        case 'node_stream_delta':
          message = String(data?.data?.delta ?? '');
          break;
        default:
          message = `${eventType}`;
      }
      logs.value = [
        ...logs.value,
        {
          id: `sse-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
          timestamp: data?.timestamp ?? new Date().toISOString(),
          level,
          nodeId,
          nodeName,
          message,
        },
      ];
    },
    onClose: () => {
      sseHandle = null;
    },
  });
}

function onClearLogs() {
  logs.value = [];
}

async function onCancel() {
  if (!execution.value) return;
  cancelling.value = true;
  try {
    await executionApi.cancel(execution.value.id);
    ElMessage.success('已取消');
    stopPolling();
    sseHandle?.();
    sseHandle = null;
    await load();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '取消失败');
  } finally {
    cancelling.value = false;
  }
}

async function onRetry() {
  if (!execution.value?.workflowId) return;
  retrying.value = true;
  try {
    const result = await executionApi.execute({
      workflowId: execution.value.workflowId,
      version: execution.value.version,
      trigger: 'manual',
      input: execution.value.input,
    });
    ElMessage.success(`重试成功，执行ID: ${result.id}`);
    // 跳转到新执行详情
    window.location.href = `/executions/${result.id}`;
  } catch (e: any) {
    ElMessage.error('重试失败: ' + (e?.message ?? ''));
  } finally {
    retrying.value = false;
  }
}

async function submitHuman() {
  if (!execution.value || !waitingNode.value || !humanText.value.trim()) return;
  submittingHuman.value = true;
  try {
    await executionApi.submitHumanInput(
      execution.value.id,
      waitingNode.value.nodeId,
      { value: humanText.value.trim() },
    );
    humanText.value = '';
    ElMessage.success('已提交，执行继续');
    await load();
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败');
  } finally {
    submittingHuman.value = false;
  }
}

onMounted(load);
onBeforeUnmount(() => {
  sseHandle?.();
  sseHandle = null;
  stopPolling();
});
</script>

<style scoped>
.execution-id, .exec-id {
  font-family: ui-monospace, monospace;
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--primary);
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}
.kv-card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kv-card .k { font-size: 11px; color: var(--text-tertiary); }
.kv-card .v { font-size: 14px; font-weight: 600; color: var(--text-primary); }

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fef2f2;
  color: var(--danger);
  border-radius: var(--radius-md);
  font-size: 13px;
  margin-bottom: 12px;
}

.human-input-band {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-primary);
  margin-bottom: 12px;
}
.human-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.human-input-band :deep(.el-textarea__inner) {
  font-size: 13px;
}

.layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
@media (max-width: 1100px) {
  .layout { grid-template-columns: 1fr; }
}

.card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.log-card {
  height: 600px;
  padding: 0;
  overflow: hidden;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty-mini {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--text-tertiary);
  font-size: 12px;
  padding: 16px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}
.timeline-row {
  display: flex;
  gap: 10px;
  position: relative;
  padding-bottom: 12px;
}
.timeline-row:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 14px;
  left: 6px;
  width: 1px;
  bottom: -8px;
  background: var(--border);
}
.timeline-row .dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: var(--bg-tertiary);
  border: 2px solid var(--border);
  flex-shrink: 0;
  margin-top: 6px;
}
.timeline-row.status-success .dot { background: var(--success); border-color: var(--success); }
.timeline-row.status-failed  .dot { background: var(--danger);  border-color: var(--danger);  }
.timeline-row.status-running .dot { background: var(--warning); border-color: var(--warning); animation: pulse 1s infinite; }
.timeline-row .content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.timeline-row .head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.timeline-row .name { font-weight: 600; }
.timeline-row .muted { font-size: 11px; color: var(--text-tertiary); }
.timeline-row .err {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fef2f2;
  color: var(--danger);
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
}
.timeline-row .out,
.timeline-row .in {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 6px 8px;
}
.timeline-row .out-title,
.timeline-row .in-title {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}
.timeline-row .out-body,
.timeline-row .in-body {
  font-family: ui-monospace, monospace;
  font-size: 11px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>

<template>
  <div class="log-panel" :class="{ running: running }">
    <div class="head">
      <div class="left">
        <i class="fa-solid fa-terminal"></i>
        <span>执行日志</span>
        <span v-if="running" class="running-tag">
          <span class="dot"></span>
          运行中 ({{ entries.length }})
        </span>
        <span v-else class="muted">共 {{ entries.length }} 条</span>
      </div>
      <div class="center">
        <el-input
          v-model="keyword"
          size="small"
          placeholder="过滤关键字"
          clearable
          style="width: 180px;"
        >
          <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
        </el-input>
        <el-select v-model="level" size="small" clearable placeholder="级别" style="width: 100px;">
          <el-option value="debug" label="调试" />
          <el-option value="info" label="信息" />
          <el-option value="warning" label="警告" />
          <el-option value="error" label="错误" />
          <el-option value="success" label="成功" />
        </el-select>
        <el-checkbox-group v-model="autoScroll" class="auto-scroll">
          <el-checkbox value="yes">跟随</el-checkbox>
        </el-checkbox-group>
      </div>
      <div class="right">
        <el-button size="small" link @click="$emit('pause-toggle')" v-if="running">
          <i class="fa-solid fa-pause"></i><span style="margin-left:4px">暂停</span>
        </el-button>
        <el-button size="small" link @click="$emit('resume')" v-else>
          <i class="fa-solid fa-play"></i><span style="margin-left:4px">继续</span>
        </el-button>
        <el-button size="small" link @click="$emit('clear')">
          <i class="fa-solid fa-broom"></i><span style="margin-left:4px">清空</span>
        </el-button>
        <el-button size="small" link @click="download">
          <i class="fa-solid fa-download"></i><span style="margin-left:4px">导出</span>
        </el-button>
      </div>
    </div>

    <div class="body" ref="bodyRef">
      <div v-if="!filtered.length" class="empty">
        <i class="fa-solid fa-inbox"></i>
        <div>暂无日志</div>
      </div>
      <div
        v-for="entry in filtered"
        :key="entry.id"
        class="line"
        :class="`level-${entry.level}`"
      >
        <span class="ts">{{ formatDate(entry.timestamp, 'HH:mm:ss.SSS') }}</span>
        <span class="lv">[{{ LABEL_MAP[entry.level] || entry.level }}]</span>
        <span v-if="entry.nodeName" class="node">「{{ entry.nodeName }}」</span>
        <span class="msg">{{ entry.message }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import type { ExecutionLog } from '@/types/execution';
import { formatDate } from '@/utils/format';

const props = defineProps<{
  entries: ExecutionLog[];
  running?: boolean;
  autoScrollDefault?: boolean;
}>();

const emit = defineEmits<{
  (e: 'clear'): void;
  (e: 'pause-toggle'): void;
  (e: 'resume'): void;
}>();

const keyword = ref('');
const level = ref<ExecutionLog['level'] | ''>('');
const autoScroll = ref<string[]>(props.autoScrollDefault === false ? [] : ['yes']);

const LABEL_MAP: Record<ExecutionLog['level'], string> = {
  debug: 'DEBUG',
  info: 'INFO',
  warning: 'WARN',
  error: 'ERROR',
  success: 'OK',
};

const filtered = computed(() => {
  let list = props.entries;
  if (level.value) list = list.filter((e) => e.level === level.value);
  if (keyword.value) {
    const k = keyword.value.toLowerCase();
    list = list.filter(
      (e) =>
        e.message.toLowerCase().includes(k) ||
        (e.nodeName ?? '').toLowerCase().includes(k),
    );
  }
  return list;
});

const bodyRef = ref<HTMLDivElement>();

watch(
  () => props.entries.length,
  () => {
    if (!autoScroll.value.includes('yes')) return;
    nextTick(() => {
      if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight;
    });
  },
);

function download() {
  const text = props.entries
    .map((e) => `${formatDate(e.timestamp, 'YYYY-MM-DD HH:mm:ss')} [${e.level.toUpperCase()}] ${e.nodeName ? '<' + e.nodeName + '> ' : ''}${e.message}`)
    .join('\n');
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `execution-log-${Date.now()}.txt`;
  a.click();
  URL.revokeObjectURL(url);
}
</script>

<style scoped>
.log-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-dark);
  color: #e2e8f0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  font-size: 12px;
  background: rgba(0,0,0,0.15);
}
.head .left,
.head .center,
.head .right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.head .center { flex: 1; justify-content: center; }
.head :deep(.el-input__wrapper),
.head :deep(.el-select__wrapper) {
  background: rgba(255,255,255,0.06);
  box-shadow: none;
}
.head :deep(.el-input__inner),
.head :deep(.el-select__placeholder) {
  color: #cbd5e1;
  font-size: 12px;
}
.auto-scroll { margin-left: 8px; }
.auto-scroll :deep(.el-checkbox__label) {
  color: #cbd5e1;
  font-size: 12px;
}
.running-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--success);
  font-size: 11px;
}
.running-tag .dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--success);
  animation: pulse 1s infinite;
}
.muted { color: var(--text-tertiary); font-size: 11px; }

.body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
  font-size: 12px;
  line-height: 1.7;
}
.empty {
  text-align: center;
  color: #64748b;
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.empty i { font-size: 28px; }

.line { display: flex; align-items: baseline; gap: 6px; }
.line .ts { color: #64748b; }
.line .lv { color: #94a3b8; }
.line .node { color: #a78bfa; }
.line .msg { color: #cbd5e1; word-break: break-all; }
.line.level-success .lv { color: #10b981; }
.line.level-error   .lv { color: #ef4444; }
.line.level-warning .lv { color: #f59e0b; }
.line.level-info    .lv { color: #60a5fa; }
.line.level-debug   .lv { color: #94a3b8; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.4; }
}
</style>

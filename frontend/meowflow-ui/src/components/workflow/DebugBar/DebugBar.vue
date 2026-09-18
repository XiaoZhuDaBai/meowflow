<script setup lang="ts">
/**
 * 调试控制条 (DebugBar)
 *
 * 对标 Coze Studio 调试控制条:
 * - 暂停时显示 Resume / Step / Stop
 * - 运行中显示 Stop + 当前节点
 * - 提供"调试模式"开关（仅在调试模式下设置断点才生效）
 */
import { computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useDebugStore } from '@/stores/debug';

const debug = useDebugStore();

const emit = defineEmits<{
  (e: 'exit'): void;
}>();

const stateLabel = computed(() => {
  switch (debug.debugState) {
    case 'IDLE': return '空闲';
    case 'RUNNING': return debug.pausedAtNodeId ? '运行中(断点暂停)' : '运行中';
    case 'PAUSED': return `已暂停 @ ${debug.pausedAtNodeId}`;
    case 'STEPPING': return `单步中 @ ${debug.pausedAtNodeId}`;
    case 'STOPPED': return '已停止';
    default: return debug.debugState;
  }
});

const stateColor = computed(() => {
  switch (debug.debugState) {
    case 'PAUSED': return '#f59e0b';
    case 'STEPPING': return '#8b5cf6';
    case 'RUNNING': return '#10b981';
    case 'STOPPED': return '#ef4444';
    default: return '#94a3b8';
  }
});

async function handleResume() {
  if (!debug.isPaused) return;
  await debug.resume();
  ElMessage.success('继续执行');
}

async function handleStep() {
  if (!debug.isPaused) return;
  await debug.step();
  ElMessage.info('单步执行中…');
}

async function handleStop() {
  await debug.stop();
  ElMessage.warning('已停止调试');
  emit('exit');
}

function handleToggleDebugMode() {
  debug.toggleDebugMode();
  ElMessage.info(debug.isDebugMode ? '已进入调试模式（可设置断点）' : '已退出调试模式');
}
</script>

<template>
  <div class="debug-bar" v-if="debug.isDebugMode || debug.currentExecutionId">
    <!-- 左侧：调试模式开关 + 状态 -->
    <div class="bar-section left">
      <el-button
        size="small"
        :type="debug.isDebugMode ? 'warning' : 'default'"
        @click="handleToggleDebugMode"
      >
        <span class="dot" :style="{ background: debug.isDebugMode ? '#f59e0b' : '#cbd5e1' }"></span>
        {{ debug.isDebugMode ? '调试模式 ON' : '调试模式 OFF' }}
      </el-button>

      <div class="state-badge" :style="{ borderColor: stateColor, color: stateColor }">
        <span class="dot" :style="{ background: stateColor }"></span>
        {{ stateLabel }}
      </div>

      <div class="bp-count" v-if="debug.breakpointCount > 0">
        🔴 {{ debug.breakpointCount }} 个断点
      </div>
    </div>

    <!-- 中间：执行控制 -->
    <div class="bar-section center" v-if="debug.currentExecutionId">
      <el-button-group>
        <el-button
          size="small"
          type="primary"
          :disabled="!debug.isPaused"
          @click="handleResume"
        >
          ▶ 继续
        </el-button>
        <el-button
          size="small"
          :disabled="!debug.isPaused"
          @click="handleStep"
        >
          ⏭ 单步
        </el-button>
        <el-button
          size="small"
          type="danger"
          :disabled="debug.isStopped"
          @click="handleStop"
        >
          ⏹ 停止
        </el-button>
      </el-button-group>
    </div>

    <!-- 右侧：快捷提示 -->
    <div class="bar-section right">
      <el-tooltip content="在画布上点击节点设置/取消断点" placement="top">
        <span class="hint">💡 点击节点设置断点</span>
      </el-tooltip>
    </div>
  </div>
</template>

<style scoped>
.debug-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: linear-gradient(180deg, #fef3c7 0%, #fef9e7 100%);
  border-bottom: 2px solid #f59e0b;
  font-size: 13px;
  gap: 16px;
  height: 44px;
}

.bar-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar-section.center {
  flex: 1;
  justify-content: center;
}

.state-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid;
  border-radius: 4px;
  font-weight: 500;
  font-family: ui-monospace, monospace;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.bp-count {
  font-size: 12px;
  color: #dc2626;
  font-weight: 500;
}

.hint {
  font-size: 12px;
  color: #78716c;
  font-style: italic;
}
</style>

<template>
  <div class="create-dsl-tab">
    <!-- 拖拽上传区 -->
    <div
      class="upload-zone"
      :class="{ 'drag-over': dragging }"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
      @click="triggerFileInput"
    >
      <input
        ref="fileInputRef"
        type="file"
        accept=".json,.yml,.yaml"
        style="display:none"
        @change="onFileChange"
      />
      <div class="upload-icon">
        <i class="fa-solid fa-file-import"></i>
      </div>
      <div class="upload-text">
        <span class="upload-primary">拖拽文件到此处，或 <em>点击选择文件</em></span>
        <span class="upload-hint">支持 .json / .yml / .yaml 格式</span>
      </div>
    </div>

    <!-- 解析结果预览 -->
    <div v-if="parsedResult" class="parse-result">
      <div class="result-header">
        <i class="fa-solid fa-check-circle" style="color: var(--success)"></i>
        <span>文件解析成功</span>
      </div>
      <div class="result-body">
        <div class="result-item">
          <span class="result-label">工作流名称</span>
          <span class="result-value">{{ parsedResult.name }}</span>
        </div>
        <div class="result-item">
          <span class="result-label">描述</span>
          <span class="result-value">{{ parsedResult.description || '—' }}</span>
        </div>
        <div class="result-item">
          <span class="result-label">节点数</span>
          <span class="result-value">{{ parsedResult.nodeCount }} 个</span>
        </div>
        <div class="result-item">
          <span class="result-label">连线数</span>
          <span class="result-value">{{ parsedResult.edgeCount }} 条</span>
        </div>
      </div>
    </div>

    <!-- 解析错误 -->
    <div v-if="parseError" class="parse-error">
      <i class="fa-solid fa-circle-exclamation"></i>
      <span>{{ parseError }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const emit = defineEmits<{
  (e: 'submit', data: { json: string }): void;
}>();

const dragging = ref(false);
const parseError = ref('');
const parsedResult = ref<{ name: string; description: string; nodeCount: number; edgeCount: number } | null>(null);
let rawJson = '';

const fileInputRef = ref<HTMLInputElement | null>(null);

function triggerFileInput() {
  fileInputRef.value?.click();
}

function onDrop(e: DragEvent) {
  dragging.value = false;
  const file = e.dataTransfer?.files[0];
  if (file) readFile(file);
}

function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (file) readFile(file);
}

function readFile(file: File) {
  parseError.value = '';
  parsedResult.value = null;

  const reader = new FileReader();
  reader.onload = (ev) => {
    try {
      rawJson = ev.target?.result as string;
      const data = JSON.parse(rawJson);
      const workflow = data.workflow ?? data;
      parsedResult.value = {
        name: workflow.name ?? '未知工作流',
        description: workflow.description ?? '',
        nodeCount: Array.isArray(workflow.nodes) ? workflow.nodes.length : 0,
        edgeCount: Array.isArray(workflow.edges) ? workflow.edges.length : 0,
      };
    } catch {
      parseError.value = '文件格式错误，无法解析为 JSON。请检查文件内容。';
    }
  };
  reader.readAsText(file);
}

function getSubmitData() {
  if (!rawJson || parseError.value) return null;
  return { json: rawJson };
}

defineExpose({ getSubmitData });
</script>

<style scoped>
.create-dsl-tab {
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 500px;
}

.upload-zone {
  border: 2px dashed var(--border);
  border-radius: var(--radius-lg);
  padding: 40px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-secondary);
}

.upload-zone:hover,
.upload-zone.drag-over {
  border-color: var(--primary);
  background: var(--primary-bg);
}

.upload-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--primary-bg);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.upload-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.upload-primary {
  font-size: 14px;
  color: var(--text-primary);
}

.upload-primary em {
  color: var(--primary);
  font-style: normal;
  text-decoration: underline;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-tertiary);
}

.parse-result {
  border: 1px solid var(--success);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  background: var(--success-bg);
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--success);
  margin-bottom: 10px;
}

.result-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.result-label {
  font-size: 11px;
  color: var(--text-tertiary);
}

.result-value {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.parse-error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--danger);
  font-size: 13px;
  padding: 10px 14px;
  background: var(--danger-bg);
  border-radius: var(--radius-md);
}
</style>

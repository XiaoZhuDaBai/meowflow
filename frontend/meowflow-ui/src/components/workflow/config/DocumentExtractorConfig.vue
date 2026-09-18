<template>
  <div class="doc-extractor-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="文件 URL" required>
        <el-input v-model="fileUrl" placeholder="https://example.com/doc.pdf" />
        <VariablePicker inline :groups="variableGroups" @insert="insertAtFile" />
      </el-form-item>

      <el-form-item label="文件类型">
        <el-radio-group v-model="fileType">
          <el-radio-button value="auto">自动</el-radio-button>
          <el-radio-button value="pdf">PDF</el-radio-button>
          <el-radio-button value="docx">Word</el-radio-button>
          <el-radio-button value="xlsx">Excel</el-radio-button>
          <el-radio-button value="md">Markdown</el-radio-button>
          <el-radio-button value="txt">文本</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="提取模式">
        <el-radio-group v-model="extractMode">
          <el-radio-button value="full">全文</el-radio-button>
          <el-radio-button value="by-page">按页</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="OCR 识别">
        <el-switch v-model="ocr" />
        <span class="tip">对扫描型 PDF 进行 OCR (依赖 Tesseract)</span>
      </el-form-item>

      <el-form-item label="最大字符">
        <el-input-number v-model="maxChars" :min="100" :step="1000" />
      </el-form-item>

      <el-form-item label="输出变量">
        <el-input v-model="outputName" placeholder="text" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

function patch(extra: Record<string, any>) {
  emit('update:modelValue', { ...props.modelValue, ...extra });
}

const fileUrl = computed({
  get: () => props.modelValue?.fileUrl ?? '',
  set: (v: string) => patch({ fileUrl: v }),
});

const fileType = computed({
  get: () => props.modelValue?.fileType ?? 'auto',
  set: (v: string) => patch({ fileType: v }),
});

const extractMode = computed({
  get: () => props.modelValue?.extractMode ?? 'full',
  set: (v: string) => patch({ extractMode: v }),
});

const ocr = computed({
  get: () => props.modelValue?.ocr ?? false,
  set: (v: boolean) => patch({ ocr: v }),
});

const maxChars = computed({
  get: () => Number(props.modelValue?.maxChars ?? 10000),
  set: (v: number) => patch({ maxChars: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'text',
  set: (v: string) => patch({ outputName: v }),
});

function insertAtFile(path: string) {
  fileUrl.value = `{{${path}}`;
}
</script>

<style scoped>
.tip {
  margin-left: 12px;
  font-size: 11px;
  color: var(--text-tertiary);
}
</style>

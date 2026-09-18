<template>
  <div class="template-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="模板内容" required>
        <CodeEditor
          v-model="template"
          :rows="8"
          placeholder="你好, {{user.name}}! 今天是 {{date}}."
        />
        <div class="tip">支持 <code>&#123;&#123;var&#125;&#125;</code> 引用上游变量,可使用 VariablePicker 插入</div>
      </el-form-item>

      <el-form-item label="变量">
        <VariablePicker inline :groups="variableGroups" @insert="insertAtTemplate" />
      </el-form-item>

      <el-form-item label="输出预览" v-if="template">
        <div class="preview">{{ renderPreview }}</div>
      </el-form-item>

      <el-form-item label="输出变量">
        <el-input v-model="outputName" placeholder="text" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import CodeEditor from '@/components/common/CodeEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';
import { useWorkflowStore } from '@/stores/workflow';
import { prettifyVariablePath } from '@/utils/variableGraph';

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const store = useWorkflowStore();

const template = computed({
  get: () => props.modelValue?.template ?? '',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, template: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'text',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, outputName: v }),
});

function insertAtTemplate(path: string) {
  template.value = (template.value ?? '') + `{{${path}}}`;
}

const renderPreview = computed(() => {
  return prettifyVariablePath(template.value ?? '', store.current);
});
</script>

<style scoped>
.tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
.preview {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
  font-family: ui-monospace, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}
</style>

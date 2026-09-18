<template>
  <div class="generic-config">
    <el-form label-width="100px" size="small">
      <el-form-item
        v-for="p in params"
        :key="p.key"
        :label="p.label"
        :error="errorFor(p)"
      >
        <el-input
          v-if="p.type === 'string' || p.type === 'model'"
          :model-value="modelValue?.[p.key] ?? ''"
          :placeholder="p.placeholder ?? `请输入 ${p.label}`"
          @update:model-value="(v) => set(p.key, v)"
        />
        <el-input
          v-else-if="p.type === 'text'"
          type="textarea"
          :rows="4"
          :model-value="modelValue?.[p.key] ?? ''"
          :placeholder="p.placeholder ?? ''"
          @update:model-value="(v) => set(p.key, v)"
        />
        <CodeEditor
          v-else-if="p.type === 'code' || p.type === 'json'"
          v-model="codes[p.key]"
          :rows="6"
          :language="p.type === 'json' ? 'json' : undefined"
          :validate="p.type === 'json' ? 'json' : undefined"
          :placeholder="p.placeholder ?? ''"
          @update:model-value="(v) => set(p.key, v)"
        />
        <el-input-number
          v-else-if="p.type === 'number'"
          :model-value="Number(modelValue?.[p.key] ?? p.default ?? 0)"
          @update:model-value="(v: any) => set(p.key, v)"
          style="width: 100%;"
        />
        <el-switch
          v-else-if="p.type === 'boolean'"
          :model-value="!!modelValue?.[p.key]"
          @update:model-value="(v) => set(p.key, v)"
        />
        <el-select
          v-else-if="p.type === 'select'"
          :model-value="modelValue?.[p.key] ?? p.default"
          @update:model-value="(v) => set(p.key, v)"
          style="width: 100%;"
        >
          <el-option
            v-for="o in p.options ?? []"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <span v-if="p.description" class="desc">{{ p.description }}</span>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { NodeParamSchema } from '@/types/node';
import CodeEditor from '@/components/common/CodeEditor.vue';

const props = defineProps<{
  params: NodeParamSchema[];
  modelValue: Record<string, any>;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const codes = ref<Record<string, string>>({});

watch(
  () => props.modelValue,
  (val) => {
    const next: Record<string, string> = {};
    for (const p of props.params) {
      if (p.type === 'code' || p.type === 'json') {
        next[p.key] =
          typeof val?.[p.key] === 'string'
            ? val[p.key]
            : val?.[p.key] != null
              ? JSON.stringify(val[p.key], null, 2)
              : '';
      }
    }
    if (JSON.stringify(next) !== JSON.stringify(codes.value)) {
      codes.value = next;
    }
  },
  { immediate: true, deep: true },
);

function set(key: string, value: any) {
  const param = props.params.find((p) => p.key === key);
  let nextValue = value;
  if (param?.type === 'json' && typeof value === 'string' && value.trim()) {
    try {
      nextValue = JSON.parse(value);
    } catch {
      nextValue = value;
    }
  }
  emit('update:modelValue', { ...props.modelValue, [key]: nextValue });
}

function errorFor(p: NodeParamSchema): string {
  if (!p.required) return '';
  const value = props.modelValue?.[p.key];
  if (value === undefined || value === null || value === '') {
    return `${p.label}为必填项`;
  }
  if (Array.isArray(value) && value.length === 0) {
    return `${p.label}为必填项`;
  }
  return '';
}
</script>

<style scoped>
.desc {
  margin-left: 8px;
  font-size: 11px;
  color: var(--text-tertiary);
}
</style>

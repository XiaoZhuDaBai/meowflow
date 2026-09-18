<template>
  <div class="key-value-editor">
    <div v-for="(item, index) in items" :key="index" class="row">
      <el-input v-model="item.key" placeholder="Key" size="small" />
      <el-input v-model="item.value" placeholder="Value" size="small" />
      <el-button size="small" link @click="remove(index)">
        <i class="fa-solid fa-xmark"></i>
      </el-button>
    </div>
    <el-button size="small" link @click="add">
      <i class="fa-solid fa-plus"></i><span style="margin-left:4px">添加</span>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

interface KVItem { key: string; value: string }

const props = defineProps<{
  modelValue: KVItem[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: KVItem[]): void;
}>();

const items = ref<KVItem[]>([...props.modelValue]);

watch(items, (v) => emit('update:modelValue', v), { deep: true });

watch(
  () => props.modelValue,
  (v) => {
    if (JSON.stringify(v) !== JSON.stringify(items.value)) {
      items.value = [...v];
    }
  },
  { deep: true },
);

function add() {
  items.value.push({ key: '', value: '' });
}

function remove(i: number) {
  items.value.splice(i, 1);
}
</script>

<style scoped>
.key-value-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 6px;
  align-items: center;
}
.row :deep(.el-input) {
  width: 100%;
}
</style>

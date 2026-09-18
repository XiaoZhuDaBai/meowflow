<template>
  <div class="canvas-toolbar">
    <div class="left">
      <el-button-group>
        <el-button size="small" @click="$emit('back')">
          <i class="fa-solid fa-arrow-left"></i><span style="margin-left:4px">返回</span>
        </el-button>
      </el-button-group>
      <span v-if="!editingName" class="title" @click="startEditName" title="点击修改名称">
        {{ store.current?.name || '未命名工作流' }}
      </span>
      <el-input
        v-else
        v-model="nameInput"
        size="small"
        class="name-input"
        @blur="saveName"
        @keyup.enter="saveName"
        ref="nameInputRef"
        maxlength="50"
      />
      <span v-if="dirty" class="dirty-badge" title="有未保存的修改">
        <span class="dirty-dot"></span>未保存
      </span>
    </div>
    <div class="center">
      <el-button-group>
        <el-tooltip content="添加节点 (B)" placement="bottom">
          <el-button size="small" @click="$emit('open-palette')">
            <i class="fa-solid fa-plus"></i>
          </el-button>
        </el-tooltip>
        <el-tooltip content="添加注释 (N)" placement="bottom">
          <el-button size="small" @click="$emit('add-note')">
            <i class="fa-solid fa-note-sticky"></i>
          </el-button>
        </el-tooltip>
        <el-tooltip content="撤销 (⌘Z)" placement="bottom">
          <el-button size="small" :disabled="!canUndo" @click="$emit('undo')">
            <i class="fa-solid fa-rotate-left"></i>
          </el-button>
        </el-tooltip>
        <el-tooltip content="重做 (⌘⇧Z)" placement="bottom">
          <el-button size="small" :disabled="!canRedo" @click="$emit('redo')">
            <i class="fa-solid fa-rotate-right"></i>
          </el-button>
        </el-tooltip>
        <el-tooltip :content="showMinimap ? '隐藏小地图 (M)' : '显示小地图 (M)'" placement="bottom">
          <el-button size="small" @click="$emit('toggle-minimap')">
            <i class="fa-solid fa-map"></i>
          </el-button>
        </el-tooltip>
        <el-tooltip :content="showGrid ? '隐藏网格 (G)' : '显示网格 (G)'" placement="bottom">
          <el-button size="small" @click="$emit('toggle-grid')">
            <i class="fa-solid fa-border-all"></i>
          </el-button>
        </el-tooltip>
      </el-button-group>
    </div>
    <div class="right">
      <el-button
        size="small"
        @click="onSave"
        :loading="saving"
        :type="dirty ? 'warning' : ''"
      >
        <i class="fa-solid fa-floppy-disk"></i>
        <span style="margin-left:4px">{{ dirty ? '保存修改' : '保存' }}</span>
      </el-button>
      <el-button size="small" plain @click="$emit('preview')">
        <i class="fa-solid fa-eye"></i><span style="margin-left:4px">预览</span>
      </el-button>
      <el-button
        size="small"
        type="success"
        @click="$emit('publish')"
      >
        <i class="fa-solid fa-rocket"></i><span style="margin-left:4px">激活</span>
      </el-button>
      <el-button size="small" plain @click="$emit('share-template')">
        <i class="fa-solid fa-share-nodes"></i><span style="margin-left:4px">分享</span>
      </el-button>
      <el-button size="small" type="primary" @click="$emit('test-run')" :loading="false">
        <i class="fa-solid fa-play"></i><span style="margin-left:4px">测试运行</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useWorkflowStore } from '@/stores/workflow';
import { ElMessage } from '@/utils/notify';

const props = defineProps<{
  canUndo?: boolean;
  canRedo?: boolean;
  showMinimap?: boolean;
  showGrid?: boolean;
  dirty?: boolean;
}>();

const emit = defineEmits<{
  (e: 'test-run'): void;
  (e: 'open-palette'): void;
  (e: 'add-note'): void;
  (e: 'undo'): void;
  (e: 'redo'): void;
  (e: 'share-template'): void;
  (e: 'publish'): void;
  (e: 'preview'): void;
  (e: 'toggle-minimap'): void;
  (e: 'toggle-grid'): void;
  (e: 'save'): void;
  (e: 'back'): void;
  (e: 'update-name', name: string): void;
}>();

const store = useWorkflowStore();
const router = useRouter();
const saving = ref(false);
const editingName = ref(false);
const nameInput = ref('');
const nameInputRef = ref<{ focus: () => void } | null>(null);

function startEditName() {
  nameInput.value = store.current?.name || '';
  editingName.value = true;
  nextTick(() => {
    nameInputRef.value?.focus();
  });
}

function saveName() {
  const newName = nameInput.value.trim();
  if (!newName) {
    ElMessage.warning('名称不能为空');
    nameInput.value = store.current?.name || '';
    editingName.value = false;
    return;
  }
  emit('update-name', newName);
  editingName.value = false;
}

function onBack() {
  router.push('/workflows');
}

async function onSave() {
  if (props.dirty === false) return;
  saving.value = true;
  try {
    emit('save');
    await store.saveCurrent();
    ElMessage.success('保存成功');
  } catch {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.canvas-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
  gap: 16px;
}
.left, .center, .right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
  margin-left: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.15s;
}
.title:hover {
  background: #f1f5f9;
}
.name-input {
  width: 200px;
  margin-left: 8px;
}
.dirty-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fed7aa;
  margin-left: 8px;
  user-select: none;
}
.dirty-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #f97316;
  box-shadow: 0 0 0 0 rgba(249, 115, 22, 0.5);
  animation: dirtyPulse 1.6s infinite;
}
@keyframes dirtyPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(249, 115, 22, 0.5); }
  50%      { box-shadow: 0 0 0 5px rgba(249, 115, 22, 0); }
}
</style>

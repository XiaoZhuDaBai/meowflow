<template>
  <ContextMenu :visible="visible" :x="x" :y="y" @close="emit('close')">
    <!-- 画布右键菜单 -->
    <template v-if="mode === 'pane'">
      <div class="menu-label">画布操作</div>
      <div class="menu-item" @click="emit('action', { type: 'paste' })">
        <i class="fa-solid fa-paste" />
        <span>粘贴</span>
        <span class="menu-shortcut">⌘V</span>
      </div>
      <div class="menu-item" @click="emit('action', { type: 'fit-view' })">
        <i class="fa-solid fa-expand" />
        <span>适应窗口</span>
      </div>
      <div class="menu-divider" />
      <div class="menu-item" @click="emit('action', { type: 'toggle-minimap' })">
        <i class="fa-solid fa-map" />
        <span>{{ canvasStore.minimapVisible ? '隐藏' : '显示' }} MiniMap</span>
      </div>
      <div class="menu-item" @click="emit('action', { type: 'toggle-grid' })">
        <i class="fa-solid fa-border-all" />
        <span>{{ canvasStore.gridVisible ? '隐藏' : '显示' }} 网格</span>
      </div>
    </template>

    <!-- 节点右键菜单 -->
    <template v-else-if="mode === 'node'">
      <div class="menu-item" @click="emit('action', { type: 'copy' })">
        <i class="fa-solid fa-copy" />
        <span>复制节点</span>
        <span class="menu-shortcut">⌘C</span>
      </div>
      <div class="menu-item" @click="emit('action', { type: 'duplicate' })">
        <i class="fa-solid fa-clone" />
        <span>创建副本</span>
        <span class="menu-shortcut">⌘D</span>
      </div>
      <div class="menu-divider" />
      <div class="menu-item" @click="emit('action', { type: 'rename' })">
        <i class="fa-solid fa-pen" />
        <span>重命名</span>
      </div>
      <div class="menu-item" @click="emit('action', { type: 'disconnect' })">
        <i class="fa-solid fa-link-slash" />
        <span>断开连接</span>
      </div>
      <div class="menu-divider" />
      <div class="menu-item danger" @click="emit('action', { type: 'delete' })">
        <i class="fa-solid fa-trash" />
        <span>删除节点</span>
        <span class="menu-shortcut">Del</span>
      </div>
    </template>

    <!-- 边右键菜单 -->
    <template v-else-if="mode === 'edge'">
      <div class="menu-item" @click="emit('action', { type: 'edit-label' })">
        <i class="fa-solid fa-pen" />
        <span>编辑标签</span>
      </div>
      <div class="menu-item" @click="emit('action', { type: 'set-type' })">
        <i class="fa-solid fa-sliders" />
        <span>边类型</span>
        <span class="menu-shortcut">›</span>
      </div>
      <div class="menu-divider" />
      <div class="menu-item danger" @click="emit('action', { type: 'delete' })">
        <i class="fa-solid fa-trash" />
        <span>删除连线</span>
        <span class="menu-shortcut">Del</span>
      </div>
    </template>
  </ContextMenu>
</template>

<script setup lang="ts">
import ContextMenu from './ContextMenu.vue';
import { useCanvasStore } from '@/stores/canvas';

type MenuMode = 'pane' | 'node' | 'edge';
type MenuAction =
  | { type: 'copy' }
  | { type: 'duplicate' }
  | { type: 'paste' }
  | { type: 'delete' }
  | { type: 'rename' }
  | { type: 'disconnect' }
  | { type: 'edit-label' }
  | { type: 'set-type' }
  | { type: 'fit-view' }
  | { type: 'toggle-minimap' }
  | { type: 'toggle-grid' };

defineProps<{
  visible: boolean;
  x: number;
  y: number;
  mode: MenuMode;
}>();

const emit = defineEmits<{
  (e: 'action', action: MenuAction): void;
  (e: 'close'): void;
}>();

const canvasStore = useCanvasStore();
</script>

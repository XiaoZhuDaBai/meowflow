<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="menuRef"
      class="context-menu"
      :style="position"
      @click.stop
    >
      <slot />
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps<{
  visible: boolean;
  x: number;
  y: number;
  /** 视口宽度，超出则左对齐 */
  viewportWidth?: number;
  /** 视口高度 */
  viewportHeight?: number;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const menuRef = ref<HTMLDivElement | null>(null);

const MENU_WIDTH = 200;
const MENU_ESTIMATED_HEIGHT = 240;

const position = computed(() => {
  let x = props.x;
  let y = props.y;
  if (props.viewportWidth && x + MENU_WIDTH > props.viewportWidth) {
    x = Math.max(0, props.viewportWidth - MENU_WIDTH - 8);
  }
  if (props.viewportHeight && y + MENU_ESTIMATED_HEIGHT > props.viewportHeight) {
    y = Math.max(0, y - MENU_ESTIMATED_HEIGHT);
  }
  return {
    left: `${x}px`,
    top: `${y}px`,
  };
});

function handleDocumentClick(e: MouseEvent) {
  if (!menuRef.value) return;
  if (!menuRef.value.contains(e.target as Node)) {
    emit('close');
  }
}

function handleEscape(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close');
}

watch(
  () => props.visible,
  (v) => {
    if (v) {
      // 用 nextTick 确保 DOM 已挂载
      setTimeout(() => {
        document.addEventListener('click', handleDocumentClick);
        document.addEventListener('keydown', handleEscape);
      }, 0);
    } else {
      document.removeEventListener('click', handleDocumentClick);
      document.removeEventListener('keydown', handleEscape);
    }
  },
);

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick);
  document.removeEventListener('keydown', handleEscape);
});

onMounted(() => {
  // 组件直接挂载时不自动注册（由 visible 变化触发）
});
</script>

<style scoped>
.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 180px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  display: flex;
  flex-direction: column;
}

:slotted(.menu-item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  font-size: 13px;
  color: #334155;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: all 0.15s;
}
:slotted(.menu-item:hover) {
  background: #f1f5f9;
  color: #6366f1;
}
:slotted(.menu-item.danger) {
  color: #ef4444;
}
:slotted(.menu-item.danger:hover) {
  background: #fef2f2;
  color: #ef4444;
}
:slotted(.menu-item.disabled) {
  color: #cbd5e1;
  pointer-events: none;
}
:slotted(.menu-item i) {
  width: 14px;
  font-size: 12px;
}
:slotted(.menu-divider) {
  height: 1px;
  margin: 4px 0;
  background: #e2e8f0;
}
:slotted(.menu-label) {
  padding: 6px 10px;
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
}
:slotted(.menu-shortcut) {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
  font-family: monospace;
}
</style>

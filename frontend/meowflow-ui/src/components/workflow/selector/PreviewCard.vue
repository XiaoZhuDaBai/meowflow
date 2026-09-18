<template>
  <Teleport to="body">
    <Transition name="preview">
      <div
        v-if="visible && node"
        class="preview-card"
        :style="cardStyle"
      >
        <div class="preview-icon" :style="{ background: node.color, color: '#fff' }">
          <i :class="node.icon" />
        </div>
        <div class="preview-content">
          <div class="preview-title">{{ node.name }}</div>
          <div class="preview-type">{{ node.type }}</div>
          <div class="preview-desc">{{ node.description }}</div>
          <div v-if="node.outputs && node.outputs.length > 0" class="preview-section">
            <div class="section-label">输出</div>
            <div class="output-list">
              <span v-for="output in node.outputs" :key="output.key" class="output-tag">
                {{ output.label }} ({{ output.type }})
              </span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { NodeDefinition } from '@/types/node';

const props = defineProps<{
  visible: boolean;
  node: NodeDefinition | null;
}>();

const mouseX = ref(0);
const mouseY = ref(0);

watch(
  () => props.visible,
  (val) => {
    if (val) {
      window.addEventListener('mousemove', trackMouse);
    } else {
      window.removeEventListener('mousemove', trackMouse);
    }
  },
);

function trackMouse(e: MouseEvent) {
  mouseX.value = e.clientX;
  mouseY.value = e.clientY;
}

const cardStyle = computed(() => ({
  left: `${Math.min(mouseX.value + 16, window.innerWidth - 320)}px`,
  top: `${Math.min(mouseY.value + 16, window.innerHeight - 200)}px`,
}));
</script>

<style scoped>
.preview-card {
  position: fixed;
  z-index: 9999;
  width: 280px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  pointer-events: none;
  display: flex;
  gap: 12px;
}

.preview-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.preview-content {
  flex: 1;
  min-width: 0;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
}

.preview-type {
  font-size: 11px;
  color: #94a3b8;
  font-family: monospace;
  margin-bottom: 6px;
}

.preview-desc {
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
}

.preview-section {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}

.section-label {
  font-size: 10px;
  color: #64748b;
  text-transform: uppercase;
  margin-bottom: 4px;
  font-weight: 600;
}

.output-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.output-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: #f1f5f9;
  border-radius: 4px;
  color: #475569;
}

.preview-enter-active,
.preview-leave-active {
  transition: all 0.15s ease;
}
.preview-enter-from,
.preview-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

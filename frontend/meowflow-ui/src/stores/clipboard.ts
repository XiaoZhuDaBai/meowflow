import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface ClipboardNode {
  /** 复制时的原始节点 ID（用于粘贴时构建边映射） */
  id: string;
  type: string;
  category: string;
  name: string;
  description?: string;
  config: Record<string, any>;
  x: number;
  y: number;
}

export interface ClipboardEdge {
  /** 复制时的原始边 ID（粘贴时生成新 ID） */
  id: string;
  source: string;
  target: string;
  label?: string;
  edgeType?: 'default' | 'condition' | 'loop' | 'error';
  config?: Record<string, any>;
}

export interface ClipboardPayload {
  nodes: ClipboardNode[];
  edges: ClipboardEdge[];
}

/**
 * Clipboard store - 复制/粘贴节点管理
 */
export const useClipboardStore = defineStore('clipboard', () => {
  const payload = ref<ClipboardPayload | null>(null);

  const isEmpty = computed(() => !payload.value || payload.value.nodes.length === 0);

  function setPayload(p: ClipboardPayload) {
    payload.value = p;
  }

  function clear() {
    payload.value = null;
  }

  return {
    payload,
    isEmpty,
    setPayload,
    clear,
  };
});

import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { ExecutionLog } from '@/types/execution';

export const useLogStore = defineStore('log', () => {
  const entries = ref<ExecutionLog[]>([]);
  const running = ref(false);

  function clear() {
    entries.value = [];
  }

  function push(entry: Omit<ExecutionLog, 'id' | 'timestamp'>) {
    entries.value = [
      ...entries.value,
      {
        ...entry,
        id: `log-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
        timestamp: new Date().toISOString(),
      },
    ];
  }

  function setRunning(v: boolean) {
    running.value = v;
  }

  return { entries, running, clear, push, setRunning };
});
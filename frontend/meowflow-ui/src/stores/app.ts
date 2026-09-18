import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false);
  const useMock = ref(import.meta.env.VITE_USE_MOCK === 'true');

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }
  function setUseMock(v: boolean) {
    useMock.value = v;
  }

  return { sidebarCollapsed, useMock, toggleSidebar, setUseMock };
});
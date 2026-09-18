<template>
  <div class="app-shell">
    <AppHeader v-if="!isEditor" />
    <div class="app-body">
      <NavMenu v-if="!isEditor" class="app-side" :class="{ collapsed: appStore.sidebarCollapsed }" />
      <main class="app-main" :class="{ 'editor-main': isEditor }">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppHeader from '@/components/layout/AppHeader.vue';
import NavMenu from '@/components/layout/NavMenu.vue';
import { useAppStore } from '@/stores/app';

const route = useRoute();
const appStore = useAppStore();
const isEditor = computed(() => route.name === 'WorkflowEditor');
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
}
.app-body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.app-side {
  width: var(--sidebar-width);
  background: var(--bg-primary);
  border-right: 1px solid var(--border);
  flex-shrink: 0;
  transition: width 0.2s ease;
}
.app-side.collapsed {
  width: 64px;
}
.app-main {
  flex: 1;
  min-width: 0;
  background: var(--bg-secondary);
  overflow: auto;
}
.app-main.editor-main {
  overflow: hidden;
}
</style>
<template>
  <header class="app-header">
    <div class="left">
      <div class="logo" @click="$router.push('/workflows')">
        <span class="logo-mark">🐱</span>
        <span class="logo-text">喵流</span>
      </div>
      <div class="env-tag" v-if="useMock">MOCK</div>
    </div>
    <nav class="center">
      <router-link v-for="m in mainMenu" :key="m.path" :to="m.path" class="nav-link">
        <i :class="m.icon"></i>
        <span>{{ m.label }}</span>
      </router-link>
    </nav>
    <div class="right">
      <button class="icon-btn" title="刷新" @click="reload">
        <i class="fa-solid fa-rotate-right"></i>
      </button>
      <button class="primary-btn" @click="$router.push('/editor')">
        <i class="fa-solid fa-plus"></i>
        <span>新建工作流</span>
      </button>
      <UserMenu />
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import UserMenu from './UserMenu.vue';
import { useAppStore } from '@/stores/app';
import { storeToRefs } from 'pinia';

const appStore = useAppStore();
const { useMock } = storeToRefs(appStore);

const mainMenu = computed(() => [
  { path: '/workflows',  label: '工作流', icon: 'fa-solid fa-diagram-project' },
  { path: '/templates',  label: '模板',   icon: 'fa-solid fa-puzzle-piece' },
  { path: '/statistics', label: '统计',   icon: 'fa-solid fa-chart-line' },
  { path: '/settings',   label: '设置',   icon: 'fa-solid fa-gear' },
]);

function reload() {
  location.reload();
}
</script>

<style scoped>
.app-header {
  height: var(--header-height);
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 16px;
  flex-shrink: 0;
  z-index: 10;
}
.left, .right { display: flex; align-items: center; gap: 12px; }
.center { flex: 1; display: flex; gap: 4px; }

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
.logo-mark { font-size: 22px; }
.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: 1px;
}
.env-tag {
  background: var(--warning-bg);
  color: var(--warning);
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 14px;
}
.nav-link:hover { background: var(--bg-tertiary); color: var(--text-primary); }
.nav-link.router-link-exact-active { color: var(--primary); background: var(--primary-bg); }

.icon-btn, .primary-btn {
  border: 1px solid var(--border);
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  padding: 6px 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
}
.icon-btn:hover { color: var(--primary); }
.primary-btn {
  background: var(--primary);
  color: var(--text-inverse);
  border-color: var(--primary);
}
.primary-btn:hover { background: var(--primary-dark); border-color: var(--primary-dark); }
</style>
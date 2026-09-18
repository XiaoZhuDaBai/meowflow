<template>
  <aside class="nav-menu">
    <div class="menu-title">导航</div>
    <ul>
      <li v-for="item in visibleItems" :key="item.path">
        <router-link :to="item.path" class="menu-item">
          <i :class="item.icon"></i>
          <span>{{ item.label }}</span>
        </router-link>
      </li>
    </ul>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useUserStore } from '@/stores/user';
import { MOCK_USER } from '@/mock/users';

const userStore = useUserStore();

const allItems = [
  { path: '/dashboard',  label: '总览',       icon: 'fa-solid fa-chart-pie',       permissions: ['dashboard'] },
  { path: '/workflows',  label: '我的工作流',   icon: 'fa-solid fa-diagram-project', permissions: ['workflow', 'workflow:list'] },
  { path: '/templates',  label: '模板市场',     icon: 'fa-solid fa-puzzle-piece',   permissions: ['template'] },
  { path: '/logs',       label: '执行日志',     icon: 'fa-solid fa-clipboard-list', permissions: ['log'] },
  { path: '/knowledge',  label: '知识库',       icon: 'fa-solid fa-brain',          permissions: ['system'] },
  { path: '/alerts',     label: '告警中心',     icon: 'fa-solid fa-bell',           permissions: ['monitor', 'log'] },
  { path: '/statistics', label: '统计看板',     icon: 'fa-solid fa-chart-line',     permissions: ['statistics'] },
  { path: '/profile',    label: '用户中心',     icon: 'fa-solid fa-user',           permissions: ['user'] },
  { path: '/team',       label: '团队管理',     icon: 'fa-solid fa-users',         permissions: ['team'] },
  { path: '/template-review', label: '模板审核', icon: 'fa-solid fa-check-to-slot', permissions: ['system'] },
  { path: '/audit-logs', label: '审计日志',     icon: 'fa-solid fa-clipboard-check', permissions: ['system'] },
  { path: '/settings',   label: '系统设置',     icon: 'fa-solid fa-gear',           permissions: ['system'] },
];

const visibleItems = computed(() => {
  // 防御性过滤:只接受像权限 code 的字符串(`workflow:list` 之类),
  // 中文 label 或其他非 code 形态视为无效。若全部无效,回退 MOCK_USER。
  const codeLike = (userStore.permissions ?? []).filter(
    (p) => typeof p === 'string' && /^[\w:.-]+$/.test(p),
  );
  const perms = codeLike.length > 0 ? codeLike : (MOCK_USER.permissions ?? []);

  return allItems.filter((item) =>
    item.permissions.some((p) => perms.includes(p)),
  );
});
</script>

<style scoped>
.nav-menu {
  height: 100%;
  padding: 16px 8px;
  overflow-y: auto;
}
.menu-title {
  padding: 0 12px 8px;
  font-size: 11px;
  color: var(--text-tertiary);
  letter-spacing: 1px;
}
ul { display: flex; flex-direction: column; gap: 2px; }
.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 13px;
  transition: background 0.15s ease;
}
.menu-item i { width: 16px; text-align: center; }
.menu-item:hover { background: var(--bg-tertiary); color: var(--text-primary); }
.menu-item.router-link-exact-active {
  background: var(--primary-bg);
  color: var(--primary);
  font-weight: 600;
}
</style>
import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { getAccessToken } from '@/utils/auth';
import { useUserStore } from '@/stores/user';

/** 路由 meta 扩展 */
declare module 'vue-router' {
  interface RouteMeta {
    /** 公开路由（无需登录） */
    public?: boolean;
    /** 页面标题 */
    title?: string;
    /** 菜单图标 */
    icon?: string;
    /** 隐藏侧边栏 */
    hideSidebar?: boolean;
    /** 允许的角色 */
    roles?: string[];
    /** 需要的权限 */
    permissions?: string[];
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/auth/Register.vue'), meta: { public: true } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('@/views/auth/ForgotPassword.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/workflows',
    children: [
      { path: 'workflows',            name: 'WorkflowList',     component: () => import('@/views/workflow/List.vue'),     meta: { title: '工作流', icon: 'fa-solid fa-diagram-project' } },
      { path: 'editor/:id?',          name: 'WorkflowEditor',   component: () => import('@/views/workflow/Editor.vue'),    meta: { title: '编辑器', hideSidebar: true } },
      { path: 'workflows/:id/detail', name: 'WorkflowDetail',   component: () => import('@/views/workflow/Detail.vue'),    meta: { title: '工作流详情' } },
      { path: 'templates',            name: 'TemplateMarket',   component: () => import('@/views/template/Market.vue'),    meta: { title: '模板市场', icon: 'fa-solid fa-puzzle-piece' } },
      { path: 'explore',              name: 'TemplateExplore',  component: () => import('@/views/template/Explore.vue'),    meta: { title: '浏览模板', icon: 'fa-solid fa-compass' } },
      { path: 'dashboard',            name: 'Dashboard',        component: () => import('@/views/Dashboard.vue'),          meta: { title: '总览', icon: 'fa-solid fa-chart-pie' } },
      { path: 'statistics',           name: 'Statistics',       component: () => import('@/views/stat/Dashboard.vue'),     meta: { title: '统计', icon: 'fa-solid fa-chart-line' } },
      { path: 'logs',                 name: 'Logs',             component: () => import('@/views/log/List.vue'),           meta: { title: '执行日志', icon: 'fa-solid fa-clipboard-list' } },
      { path: 'alerts',               name: 'Alerts',           component: () => import('@/views/monitor/Alerts.vue'),     meta: { title: '告警中心', icon: 'fa-solid fa-bell' } },
      { path: 'knowledge',            name: 'Knowledge',        component: () => import('@/views/knowledge/index.vue'),    meta: { title: '知识库', icon: 'fa-solid fa-brain', permissions: ['system'] } },
      { path: 'executions/:id',      name: 'ExecutionDetail',  component: () => import('@/views/log/Detail.vue'),         meta: { title: '执行详情' } },
      { path: 'profile',              name: 'Profile',          component: () => import('@/views/user/Profile.vue'),       meta: { title: '用户中心', icon: 'fa-solid fa-user' } },
      { path: 'team',                 name: 'Team',             component: () => import('@/views/team/Manage.vue'),        meta: { title: '团队管理', icon: 'fa-solid fa-users' } },
      { path: 'audit-logs',          name: 'AuditLogs',        component: () => import('@/views/system/AuditLogs.vue'),   meta: { title: '审计日志', icon: 'fa-solid fa-clipboard-check', permissions: ['system'] } },
      { path: 'template-review',      name: 'TemplateReview',    component: () => import('@/views/template/Review.vue'),      meta: { title: '模板审核', icon: 'fa-solid fa-check-to-slot', permissions: ['system'] } },
      { path: 'settings',             name: 'Settings',         component: () => import('@/views/system/Settings.vue'),    meta: { title: '系统设置', icon: 'fa-solid fa-gear' } },
    ],
  },
  { path: '/403', name: 'error-403', component: () => import('@/views/error/403.vue'), meta: { public: true } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/404.vue'), meta: { public: true } },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  if (to.meta?.public) return true;

  const hasToken = !!getAccessToken();
  if (!hasToken) {
    return { name: 'Login', query: { redirect: to.fullPath } };
  }

  const userStore = useUserStore();

  if (!userStore.user) {
    await userStore.bootstrap();
  }

  if (!userStore.user) {
    return { name: 'Login', query: { redirect: to.fullPath } };
  }

  const requiredRoles = to.meta.roles;
  if (requiredRoles?.length && !requiredRoles.some((r) => userStore.hasRole(r))) {
    return { name: 'error-403' };
  }

  const requiredPerms = to.meta.permissions;
  if (requiredPerms?.length && !userStore.hasAnyPermission(requiredPerms)) {
    return { name: 'error-403' };
  }

  return true;
});

export default router;
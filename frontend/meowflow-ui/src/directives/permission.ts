/**
 * v-permission 指令：按权限过滤元素
 *
 * 用法：
 *   v-permission="['workflow:edit']"          — 需要任一权限
 *   v-permission="['workflow:edit', 'workflow:publish']" — 需要任一权限
 *   v-permission.all="['workflow:edit']"     — 需要所有权限
 *   v-permission.role="['admin']"            — 需要任一角色
 *
 * 默认策略：隐藏（从 DOM 移除元素）
 * v-permission.disable="true" 时：禁用元素 + tooltip 提示
 */

import type { App, Directive, DirectiveBinding } from 'vue';
import { useUserStore } from '@/stores/user';

interface PermissionBinding extends DirectiveBinding {
  value: any;
  modifiers: {
    all?: boolean;
    role?: boolean;
    disable?: boolean;
  };
}

function doHide(el: HTMLElement) {
  if (el.parentNode) {
    el.parentNode.removeChild(el);
  }
}

function doDisable(el: HTMLElement, message = '暂无权限') {
  el.setAttribute('disabled', '');
  el.classList.add('is-disabled');
  el.setAttribute('title', message);
  el.style.pointerEvents = 'none';
}

const permissionDirective: Directive = {
  mounted(el: HTMLElement, binding: PermissionBinding) {
    const userStore = useUserStore();
    let allowed = false;

    if (binding.modifiers.role) {
      const roles = (Array.isArray(binding.value) ? binding.value : [binding.value]) as string[];
      allowed = roles.some((r) => userStore.hasRole(r));
    } else if (binding.modifiers.all) {
      const perms = (Array.isArray(binding.value) ? binding.value : [binding.value]) as string[];
      allowed = userStore.hasAllPermissions(perms);
    } else {
      const perms = (Array.isArray(binding.value) ? binding.value : [binding.value]) as string[];
      allowed = userStore.hasAnyPermission(perms);
    }

    if (!allowed) {
      if (binding.modifiers.disable) {
        doDisable(el, '暂无权限执行此操作');
      } else {
        doHide(el);
      }
    }
  },

  updated(el: HTMLElement, binding: PermissionBinding) {
    const userStore = useUserStore();
    let allowed = false;

    if (binding.modifiers.role) {
      const roles = (Array.isArray(binding.value) ? binding.value : [binding.value]) as string[];
      allowed = roles.some((r) => userStore.hasRole(r));
    } else if (binding.modifiers.all) {
      const perms = (Array.isArray(binding.value) ? binding.value : [binding.value]) as string[];
      allowed = userStore.hasAllPermissions(perms);
    } else {
      const perms = (Array.isArray(binding.value) ? binding.value : [binding.value]) as string[];
      allowed = userStore.hasAnyPermission(perms);
    }

    if (!allowed) {
      if (binding.modifiers.disable) {
        doDisable(el, '暂无权限执行此操作');
      } else {
        doHide(el);
      }
    } else {
      el.removeAttribute('disabled');
      el.classList.remove('is-disabled');
      el.removeAttribute('title');
      el.style.pointerEvents = '';
    }
  },
};

export function setupPermissionDirective(app: App) {
  app.directive('permission', permissionDirective);
}

export default permissionDirective;

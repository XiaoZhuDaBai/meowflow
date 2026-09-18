<template>
  <el-dropdown trigger="click" @command="onCommand">
    <div class="user-menu">
      <div class="avatar">{{ avatar }}</div>
      <div class="user-text">
        <div class="name">{{ user?.nickname || user?.username || '未登录' }}</div>
        <div class="role">{{ roleLabel }}</div>
      </div>
      <i class="fa-solid fa-chevron-down caret"></i>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="profile">
          <i class="fa-solid fa-user"></i> 用户中心
        </el-dropdown-item>
        <el-dropdown-item command="perms">
          <i class="fa-solid fa-key"></i> 权限说明
        </el-dropdown-item>
        <el-dropdown-item divided command="logout">
          <i class="fa-solid fa-right-from-bracket"></i> 退出登录
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <el-dialog v-model="permsOpen" title="当前账号权限 (Mock)" width="520px">
    <p class="muted">由于是 Mock 演示环境,系统已自动通过所有权限校验,仅供 UI 演示。</p>
    <ul class="perm-list">
      <li v-for="p in user?.permissions" :key="p">
        <i class="fa-solid fa-check" style="color: var(--success)"></i>
        <span>{{ p }}</span>
      </li>
    </ul>
    <template #footer>
      <el-button type="primary" @click="permsOpen = false">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { storeToRefs } from 'pinia';
import { ElMessageBox } from '@/utils/notify';

const router = useRouter();
const userStore = useUserStore();
const { user } = storeToRefs(userStore);

const permsOpen = ref(false);

const avatar = computed(() => user.value?.nickname?.slice(0, 1) || user.value?.username?.slice(0, 1) || '访');
const roleLabel = computed(() => {
  if (!user.value) return '未登录';
  if (userStore.isAdmin) return '管理员';
  return '成员';
});

function onCommand(cmd: string) {
  if (cmd === 'profile') router.push('/profile');
  if (cmd === 'perms') permsOpen.value = true;
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录?', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout();
        router.push('/login');
      })
      .catch(() => {});
  }
}
</script>

<style scoped>
.user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
}
.user-menu:hover { background: var(--bg-tertiary); }
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: var(--primary);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.user-text { display: flex; flex-direction: column; line-height: 1.2; }
.name { font-size: 13px; color: var(--text-primary); font-weight: 600; }
.role { font-size: 11px; color: var(--text-tertiary); }
.caret { font-size: 10px; color: var(--text-tertiary); }
.perm-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.perm-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}
</style>
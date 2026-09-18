<template>
  <div class="forbidden">
    <div class="big">403</div>
    <div class="title">没有访问权限</div>
    <div class="desc">抱歉，你没有权限访问该页面，请联系管理员获取相应权限。</div>
    <div class="actions">
      <el-button type="primary" @click="$router.back()">
        <i class="fa-solid fa-arrow-left"></i>
        <span style="margin-left:6px">返回上一页</span>
      </el-button>
      <el-button @click="$router.push('/')">
        <i class="fa-solid fa-house"></i>
        <span style="margin-left:6px">回到首页</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
</script>

<style scoped>
.forbidden {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  text-align: center;
}
.big {
  font-size: 96px;
  font-weight: 800;
  color: var(--el-color-danger);
  letter-spacing: 4px;
}
.title { font-size: 18px; color: var(--text-primary); margin: 8px 0 4px; }
.desc { color: var(--text-tertiary); margin-bottom: 24px; max-width: 400px; }
.actions { display: flex; gap: 12px; }
</style>

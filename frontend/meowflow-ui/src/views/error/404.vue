<template>
  <div class="not-found">
    <div class="big">404</div>
    <div class="title">页面找不到了 🐷</div>
    <div class="desc">你访问的页面不存在,或已经被移除</div>
    <el-button type="primary" @click="$router.push('/')">
      <i class="fa-solid fa-house"></i><span style="margin-left:6px">回到首页</span>
    </el-button>
  </div>
</template>

<style scoped>
.not-found {
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
  color: var(--primary);
  letter-spacing: 4px;
}
.title { font-size: 18px; color: var(--text-primary); margin: 8px 0 4px; }
.desc { color: var(--text-tertiary); margin-bottom: 16px; }
</style>
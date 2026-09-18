<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="brand-icon">🐱</div>
        <h1>喵流</h1>
        <p>让每一份工作，像猫一样优雅流动</p>
      </div>

      <el-form
        v-if="!isMockMode"
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        @submit.prevent="onLogin"
        class="login-form"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            :prefix-icon="UserIcon"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="LockIcon"
            @keyup.enter="onLogin"
          />
        </el-form-item>
        <el-form-item prop="code">
          <el-input
            v-model="loginForm.code"
            placeholder="图形验证码"
            style="width: 160px;"
            :prefix-icon="CaptchaIcon"
            maxlength="4"
            @keyup.enter="onLogin"
          />
          <div class="captcha-img" :class="{ loading: captchaLoading }" @click="refreshCaptcha" title="点击刷新验证码">
            <img v-if="captchaImg" :src="captchaImg" alt="验证码" />
            <span v-else class="captcha-placeholder">加载中</span>
          </div>
        </el-form-item>

        <div class="form-footer">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <router-link to="/forgot-password" class="link">忘记密码？</router-link>
        </div>
      </el-form>

      <!-- Mock mode avatar display (unchanged) -->
      <div class="avatar" v-if="user && isMockMode">
        <div class="avatar-circle">{{ user.nickname?.slice(0, 1) || user.username?.slice(0, 1) }}</div>
        <div class="info">
          <div class="name">{{ user.nickname || user.username }}</div>
          <div class="meta">{{ roleLabel }} · {{ user.organization?.name }}</div>
        </div>
      </div>

      <ul class="permissions" v-if="user && isMockMode">
        <li v-for="p in user.permissions" :key="p">
          <i class="fa-solid fa-shield-halved"></i>
          <span>{{ p }}</span>
        </li>
      </ul>

      <div class="notice" v-if="isMockMode">
        <i class="fa-solid fa-circle-info"></i>
        <span>权限已自动通过 (Mock 演示环境，无需配置)</span>
      </div>

      <el-button
        type="primary"
        size="large"
        class="login-btn"
        @click="onLogin"
        :loading="loading"
      >
        <i class="fa-solid fa-right-to-bracket"></i>
        <span style="margin-left: 6px;">{{ isMockMode ? '一键进入工作台' : '登录' }}</span>
      </el-button>

      <div class="bottom-link" v-if="!isMockMode">
        没有账号？
        <router-link to="/register" class="link">立即注册</router-link>
      </div>

      <div class="footer">
        <span>v0.2.0</span>
        <span>·</span>
        <span>{{ isMockMode ? 'Mock 模式' : '生产模式' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { useCaptchaStore } from '@/stores/captcha';
import { MOCK_USER } from '@/mock/users';

const userStore = useUserStore();
const captchaStore = useCaptchaStore();
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const user = ref(MOCK_USER);
const formRef = ref<FormInstance>();

const isMockMode = import.meta.env.VITE_USE_MOCK === 'true';
const loginForm = reactive({ username: '', password: '', code: '', uuid: '' });
const rememberMe = ref(false);
const captchaLoading = computed(() => captchaStore.loading);
const captchaImg = computed(() => captchaStore.getImg());

const UserIcon = { render: () => h('i', { class: 'fa-solid fa-user' }) };
const LockIcon = { render: () => h('i', { class: 'fa-solid fa-lock' }) };
const CaptchaIcon = { render: () => h('i', { class: 'fa-solid fa-shield-halved' }) };

const roleLabel = computed(() => userStore.isAdmin ? '管理员' : '成员');

// Validation rules
const validateUsername = (_rule: any, value: string, callback: any) => {
  if (!value.trim()) callback(new Error('请输入用户名'));
  else if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(value)) callback(new Error('用户名以字母开头，长度5-16位'));
  else callback();
};
const validateCode = (_rule: any, value: string, callback: any) => {
  if (!value.trim()) callback(new Error('请输入验证码'));
  else if (value.length !== 4) callback(new Error('验证码为4位'));
  else callback();
};

const rules: FormRules = {
  username: [{ required: true, validator: validateUsername, trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, validator: validateCode, trigger: 'blur' }],
};

onMounted(async () => {
  await userStore.bootstrap();
  if (!isMockMode) refreshCaptcha();
});

async function refreshCaptcha() {
  await captchaStore.refresh();
  loginForm.code = '';
  loginForm.uuid = captchaStore.getUuid();
}

async function onLogin() {
  loading.value = true;
  try {
    if (isMockMode) {
      await userStore.login();
    } else {
      const valid = await formRef.value?.validate().catch(() => false);
      if (!valid) return;
      await userStore.loginWithCredentials(loginForm.username, loginForm.password, loginForm.code, loginForm.uuid);
    }
    const redirect = (route.query.redirect as string) || '/workflows';
    router.replace(redirect);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-bg), #fdf4ff);
}
.login-card {
  background: var(--bg-primary);
  width: 420px;
  padding: 32px;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--border);
}
.brand { text-align: center; margin-bottom: 24px; }
.brand-icon { font-size: 36px; margin-bottom: 4px; }
.brand h1 { font-size: 22px; color: var(--primary); letter-spacing: 2px; margin-bottom: 4px; }
.brand p { font-size: 12px; color: var(--text-tertiary); }

.login-form { margin-bottom: 16px; }

.captcha-img {
  width: 120px;
  height: 40px;
  margin-left: 8px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border);
  flex-shrink: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.captcha-img:hover { opacity: 0.8; }
.captcha-img.loading { background: var(--bg-secondary); }
.captcha-img img { width: 100%; height: 100%; object-fit: cover; }
.captcha-placeholder { font-size: 11px; color: var(--text-tertiary); }

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 13px;
}
.link { color: var(--primary); text-decoration: none; }
.link:hover { text-decoration: underline; }

.avatar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}
.avatar-circle {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}
.avatar .name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.avatar .meta { font-size: 11px; color: var(--text-tertiary); }

.permissions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
  font-size: 12px;
  color: var(--text-secondary);
}
.permissions li { display: flex; gap: 6px; align-items: center; }
.permissions li i { color: var(--success); }

.notice {
  display: flex;
  gap: 6px;
  align-items: center;
  padding: 8px 12px;
  background: var(--warning-bg);
  color: var(--warning);
  border-radius: var(--radius-sm);
  font-size: 12px;
  margin-bottom: 16px;
}

.login-btn { width: 100%; }

.bottom-link {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
}

.footer {
  margin-top: 16px;
  font-size: 11px;
  color: var(--text-tertiary);
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 6px;
}
</style>

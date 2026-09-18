<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <div class="brand-icon">🐱</div>
        <h1>创建账号</h1>
        <p>加入喵流，自动化你的工作流</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        class="auth-form"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（字母开头，5-16位）" :prefix-icon="UserIcon" clearable />
        </el-form-item>
        <el-form-item prop="nickName">
          <el-input v-model="form.nickName" placeholder="昵称" :prefix-icon="NameIcon" clearable />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱" :prefix-icon="MailIcon" clearable />
        </el-form-item>
        <el-form-item prop="emailCode">
          <el-input v-model="form.emailCode" placeholder="邮箱验证码" style="width: 160px;" maxlength="6" />
          <el-button
            :disabled="countdown > 0 || emailSending"
            class="code-btn"
            @click="onSendEmailCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </el-button>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（8-20位，含字母+数字）" show-password :prefix-icon="LockIcon" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password :prefix-icon="LockIcon" />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <el-input v-model="form.captchaCode" placeholder="图形验证码（4位）" style="width: 120px;" maxlength="4" :prefix-icon="CaptchaIcon" />
          <div class="captcha-img" :class="{ loading: captchaLoading }" @click="refreshCaptcha" title="点击刷新验证码">
            <img v-if="captchaImg" :src="captchaImg" alt="验证码" />
            <span v-else class="captcha-placeholder">加载中</span>
          </div>
        </el-form-item>
        <el-form-item prop="agree">
          <el-checkbox v-model="form.agree">
            我已阅读并同意
            <a href="#" class="link" @click.prevent>《用户协议》</a>
            和
            <a href="#" class="link" @click.prevent>《隐私政策》</a>
          </el-checkbox>
        </el-form-item>
      </el-form>

      <el-button type="primary" size="large" class="submit-btn" @click="onSubmit" :loading="submitting">
        <i class="fa-solid fa-user-plus"></i>
        <span style="margin-left: 6px;">注册</span>
      </el-button>

      <div class="bottom-link">
        已有账号？
        <router-link to="/login" class="link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { useCaptchaStore } from '@/stores/captcha';

const userStore = useUserStore();
const captchaStore = useCaptchaStore();
const router = useRouter();
const formRef = ref<FormInstance>();
const submitting = ref(false);
const emailSending = ref(false);
const countdown = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

const isMockMode = import.meta.env.VITE_USE_MOCK === 'true';
const captchaLoading = computed(() => captchaStore.loading);
const captchaImg = computed(() => captchaStore.getImg());

const UserIcon = { render: () => h('i', { class: 'fa-solid fa-user' }) };
const NameIcon = { render: () => h('i', { class: 'fa-solid fa-id-badge' }) };
const MailIcon = { render: () => h('i', { class: 'fa-solid fa-envelope' }) };
const LockIcon = { render: () => h('i', { class: 'fa-solid fa-lock' }) };
const CaptchaIcon = { render: () => h('i', { class: 'fa-solid fa-shield-halved' }) };

const form = reactive({
  username: '',
  nickName: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
  agree: false,
});

const validateUsername = (_rule: any, value: string, callback: any) => {
  if (!value.trim()) callback(new Error('请输入用户名'));
  else if (!/^[a-zA-Z][a-zA-Z0-9_]{4,15}$/.test(value)) callback(new Error('用户名以字母开头，长度5-16位'));
  else callback();
};
const validateEmail = (_rule: any, value: string, callback: any) => {
  if (!value.trim()) callback(new Error('请输入邮箱'));
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) callback(new Error('邮箱格式不正确'));
  else callback();
};
const validateEmailCode = (_rule: any, value: string, callback: any) => {
  if (!value.trim()) callback(new Error('请输入邮箱验证码'));
  else if (value.length !== 6) callback(new Error('验证码为6位数字'));
  else callback();
};
const validatePassword = (_rule: any, value: string, callback: any) => {
  if (!value) callback(new Error('请输入密码'));
  else if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,20}$/.test(value)) callback(new Error('密码8-20位，必须含字母和数字'));
  else callback();
};
const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (!value) callback(new Error('请再次输入密码'));
  else if (value !== form.password) callback(new Error('两次密码不一致'));
  else callback();
};
const validateCaptcha = (_rule: any, value: string, callback: any) => {
  if (!value.trim()) callback(new Error('请输入图形验证码'));
  else if (value.length !== 4) callback(new Error('图形验证码为4位'));
  else callback();
};
const validateAgree = (_rule: any, value: boolean, callback: any) => {
  if (!value) callback(new Error('请先同意用户协议'));
  else callback();
};

const rules: FormRules = {
  username: [{ required: true, validator: validateUsername, trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ required: true, validator: validateEmail, trigger: 'blur' }],
  emailCode: [{ required: true, validator: validateEmailCode, trigger: 'blur' }],
  password: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }],
  captchaCode: [{ required: true, validator: validateCaptcha, trigger: 'blur' }],
  agree: [{ required: true, validator: validateAgree, trigger: 'change' }],
};

onMounted(() => { refreshCaptcha(); });
onUnmounted(() => { if (countdownTimer) clearInterval(countdownTimer); });

async function refreshCaptcha() {
  await captchaStore.refresh();
  form.captchaCode = '';
}

async function onSendEmailCode() {
  const valid = await formRef.value?.validateField('email').catch(() => false);
  if (!valid) return;
  emailSending.value = true;
  try {
    await userStore.sendEmailCode(form.email, 'REGISTER');
    startCountdown();
  } finally {
    emailSending.value = false;
  }
}

function startCountdown() {
  countdown.value = 60;
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
  }, 1000);
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickName: form.nickName,
      email: form.email,
      phone: undefined,
      code: form.captchaCode,
      uuid: captchaStore.getUuid(),
      emailCode: form.emailCode,
      agree: form.agree,
    });
    router.replace('/workflows');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.auth-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-bg), #fdf4ff);
}
.auth-card {
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

.auth-form { margin-bottom: 16px; }

.code-btn {
  margin-left: 8px;
  width: 100px;
  flex-shrink: 0;
}

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
  background: var(--bg-secondary);
}
.captcha-img:hover { opacity: 0.8; }
.captcha-img img { width: 100%; height: 100%; object-fit: cover; }
.captcha-placeholder { font-size: 11px; color: var(--text-tertiary); }

.link { color: var(--primary); text-decoration: none; }
.link:hover { text-decoration: underline; }

.submit-btn { width: 100%; margin-bottom: 16px; }

.bottom-link {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
}
</style>

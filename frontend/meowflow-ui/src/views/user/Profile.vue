<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">用户中心</div>
        <div class="page-subtitle">个人信息 · 密码修改 · 权限说明</div>
      </div>
    </div>

    <div class="layout">
      <div class="card profile">
        <div class="avatar">{{ profile?.nickname?.slice(0, 1) ?? profile?.username?.slice(0, 1) ?? '?' }}</div>
        <div class="info">
          <div class="name">{{ profile?.nickname || profile?.username }}</div>
          <div class="muted">@{{ profile?.username }} · {{ profile?.email }}</div>
          <div class="role-tag">{{ store.isAdmin ? '管理员' : '成员' }}</div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">基本资料</div>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 520px; margin-top: 12px;">
          <el-form-item label="姓名" prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="组织">
            <el-input :model-value="profile?.organization?.name" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSave" :loading="saving">保存</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card">
        <div class="card-title">修改密码</div>
        <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width: 520px; margin-top: 12px;">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onChangePwd" :loading="pwdLoading">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card">
        <div class="card-title">权限说明</div>
        <p class="muted" style="margin: 4px 0 12px;">
          {{ store.isAdmin ? '您是管理员，拥有系统全部功能权限。' : '您是成员，拥有以下功能权限：' }}
        </p>
        <ul class="perm-list">
          <li v-for="p in effectivePermissions" :key="p">
            <i class="fa-solid fa-check"></i><span>{{ p }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { userApi } from '@/api/user';
import { useUserStore } from '@/stores/user';
import { ElMessage } from '@/utils/notify';
import type { FormInstance, FormRules } from 'element-plus';
import type { UserInfo } from '@/types/user';

const store = useUserStore();
const profile = ref<UserInfo | null>(null);
const saving = ref(false);
const pwdLoading = ref(false);
const formRef = ref<FormInstance>();
const pwdRef = ref<FormInstance>();

const form = reactive({ nickname: '', email: '' });

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const validatePwdLen = (_rule: any, value: string, callback: any) => {
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,20}$/.test(value)) {
    callback(new Error('密码8-20位，必须含字母和数字'));
  } else {
    callback();
  }
};

const rules: FormRules = {
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
};

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validatePwdLen, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
};

const effectivePermissions = computed(() => {
  if (store.isAdmin) {
    return [
      'workflow:view / edit / run / publish / delete',
      'template:view / use / publish',
      'log:view / export',
      'dashboard:view / export',
      'profile:view / edit',
      'team:view / manage',
      'settings:view / edit',
    ];
  }
  return profile.value?.permissions ?? [];
});

async function loadProfile() {
  try {
    profile.value = await userApi.me();
    if (profile.value) {
      form.nickname = profile.value.nickname ?? '';
      form.email = profile.value.email ?? '';
    }
  } catch {
    profile.value = store.user;
  }
}

async function onSave() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    if (!profile.value?.id) return;
    saving.value = true;
    try {
      await userApi.updateProfile(profile.value.id, {
        nickname: form.nickname,
        email: form.email,
      });
      store.updateProfile({ nickname: form.nickname, email: form.email });
      profile.value = { ...profile.value, nickname: form.nickname, email: form.email };
      ElMessage.success('已保存');
    } catch {
      // error shown by http interceptor
    } finally {
      saving.value = false;
    }
  });
}

async function onChangePwd() {
  if (!pwdRef.value) return;
  await pwdRef.value.validate(async (valid) => {
    if (!valid) return;
    if (!profile.value?.id) return;
    pwdLoading.value = true;
    try {
      await userApi.changePassword(profile.value.id, pwdForm.oldPassword, pwdForm.newPassword);
      ElMessage.success('密码修改成功');
      pwdForm.oldPassword = '';
      pwdForm.newPassword = '';
      pwdForm.confirmPassword = '';
      pwdRef.value?.clearValidate();
    } catch {
      // error shown by http interceptor
    } finally {
      pwdLoading.value = false;
    }
  });
}

onMounted(loadProfile);
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.card-title { font-size: 14px; font-weight: 600; }
.profile {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 16px;
}
.profile .avatar {
  width: 64px; height: 64px; border-radius: 999px;
  background: var(--primary); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 700;
}
.profile .name { font-size: 16px; font-weight: 600; }
.profile .role-tag {
  display: inline-block;
  background: var(--primary-bg);
  color: var(--primary);
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 4px;
  margin-top: 6px;
}
.perm-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}
.perm-list li {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 6px 10px;
  font-size: 12px;
  display: flex;
  gap: 6px;
  align-items: center;
  color: var(--text-primary);
}
.perm-list li i { color: var(--success); }
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
}
</style>

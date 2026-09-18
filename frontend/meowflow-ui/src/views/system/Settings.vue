<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">系统设置</div>
        <div class="page-subtitle">
          {{ scope === 'personal' ? '个人空间 · 只对自己生效' : '团队配置 · 只读,仅供了解' }}
        </div>
      </div>
      <div class="header-actions">
        <el-radio-group v-model="scope" size="small">
          <el-radio-button value="personal">个人配置</el-radio-button>
          <el-radio-button value="team" :disabled="!hasTeamOrgs">团队配置</el-radio-button>
        </el-radio-group>
        <el-button @click="refresh" :loading="loading">
          <i class="fa-solid fa-rotate"></i>
          <span style="margin-left: 6px">刷新</span>
        </el-button>
      </div>
    </div>

    <div v-if="errorMessage" class="notice-bar warning">
      <i class="fa-solid fa-triangle-exclamation"></i>
      <span>{{ errorMessage }}</span>
    </div>

    <div v-if="scope === 'team'" class="readonly-banner">
      <i class="fa-solid fa-circle-info"></i>
      <span>当前为团队组织「{{ teamOrgName }}」的配置视角,仅可查看,无法修改。</span>
    </div>

    <el-tabs v-model="tab" class="settings-tabs">
      <!-- LLM 模型 -->
      <el-tab-pane label="LLM 模型" name="llm">
        <div class="card">
          <div class="card-toolbar">
            <span class="card-title">模型列表</span>
            <el-button
              size="small"
              type="primary"
              :disabled="scope === 'team'"
              @click="openModelDialog()"
            >
              <i class="fa-solid fa-plus"></i> 新增模型
            </el-button>
          </div>
          <el-table :data="models" stripe v-loading="loadingModels" empty-text="暂无模型,请新增">
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column prop="provider" label="厂商" width="120" />
            <el-table-column prop="modelKey" label="模型标识" width="160" />
            <el-table-column label="API Key" width="180">
              <template #default="{ row }">
                <code class="key-mask">{{ row.apiKeyMasked || '未设置' }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="endpoint" label="Endpoint" min-width="200">
              <template #default="{ row }">
                <span class="endpoint-text">{{ row.baseUrl || row.endpoint || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="80">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" :disabled="scope === 'team'" @change="(v) => onToggleLLM(row, v)" />
              </template>
            </el-table-column>
            <el-table-column label="默认" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault" type="success" size="small">默认</el-tag>
                <el-button v-else size="small" link :disabled="scope === 'team'" @click="onSetDefault(row)">设为默认</el-button>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link :disabled="scope === 'team'" @click="openModelDialog(row)">编辑</el-button>
                <el-button size="small" link @click="onTestModel(row)">测试</el-button>
                <el-button size="small" link type="danger" :disabled="scope === 'team'" @click="onDeleteModel(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 集成中心 -->
      <el-tab-pane label="集成中心" name="int">
        <div class="integration-grid">
          <div v-for="i in integrations" :key="i.id" class="int-card">
            <div class="int-head">
              <div class="int-icon" :style="{ background: iconBg(i.type) }">
                <i :class="iconClass(i.type)"></i>
              </div>
              <div class="flex-1">
                <div class="int-name">{{ i.name }}</div>
                <div class="int-desc">{{ typeLabelMap[i.type] ?? i.type }} · #{{ i.id }}</div>
              </div>
              <el-switch v-model="i.enabled" :disabled="scope === 'team'" @change="(v) => onToggleInt(i, v)" />
            </div>
            <div class="int-config">
              <div v-if="i.webhookUrl" class="config-row">
                <span class="k">Webhook</span>
                <span class="v">{{ mask(i.webhookUrl) }}</span>
              </div>
              <div v-if="i.secret" class="config-row">
                <span class="k">密钥</span>
                <span class="v">{{ mask(i.secret) }}</span>
              </div>
              <div v-if="i.accessKeyId" class="config-row">
                <span class="k">AccessKey</span>
                <span class="v">{{ mask(i.accessKeyId) }}</span>
              </div>
              <div v-if="i.customConfig" class="config-row">
                <span class="k">自定义</span>
                <span class="v">{{ i.customConfig.length }} 字符</span>
              </div>
            </div>
            <div class="int-footer">
              <el-button size="small" link :disabled="scope === 'team'" @click="openIntDialog(i)">编辑</el-button>
              <el-button size="small" link @click="onTestInt(i)">测试</el-button>
              <el-button size="small" link type="danger" :disabled="scope === 'team'" @click="onDeleteInt(i)">删除</el-button>
            </div>
          </div>
          <!-- 新增卡片占位 -->
          <div v-if="scope === 'personal'" class="int-card add-card" @click="openIntDialog()">
            <i class="fa-solid fa-plus"></i>
            <span>新增集成</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- LLM 模型弹窗 -->
    <el-dialog
      v-model="modelDialogVisible"
      :title="editingModel ? '编辑模型' : '新增模型'"
      width="560px"
    >
      <el-form ref="modelFormRef" :model="modelForm" :rules="modelRules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="modelForm.name" placeholder="如 GPT-4o" />
        </el-form-item>
        <el-form-item label="厂商" prop="provider">
          <el-select v-model="modelForm.provider" filterable allow-create placeholder="选择或输入厂商" style="width: 100%">
            <el-option value="openai" label="OpenAI" />
            <el-option value="anthropic" label="Anthropic" />
            <el-option value="azure" label="Azure" />
            <el-option value="ali" label="阿里云(DashScope)" />
            <el-option value="baidu" label="百度千帆" />
            <el-option value="deepseek" label="DeepSeek" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型标识" prop="modelKey">
          <el-input v-model="modelForm.modelKey" placeholder="gpt-4o / claude-3-opus / deepseek-chat" />
        </el-form-item>
        <el-form-item label="API Key" :prop="editingModel ? '' : 'apiKey'">
          <el-input
            v-model="modelForm.apiKey"
            type="password"
            show-password
            :placeholder="editingModel ? '留空表示不更新' : 'sk-...'"
          />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="modelForm.baseUrl" placeholder="可留空" />
        </el-form-item>
        <el-form-item label="能力">
          <el-input v-model="modelForm.capabilities" placeholder="chat, embedding, vision, function_call" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="modelForm.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="modelForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="modelSaving" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>

    <!-- 集成配置弹窗 -->
    <el-dialog
      v-model="intDialogVisible"
      :title="editingInt ? `编辑集成` : '新增集成'"
      width="560px"
    >
      <el-form :model="intForm" label-width="110px">
        <el-form-item label="类型" required>
          <el-select v-model="intForm.type" :disabled="!!editingInt" placeholder="选择类型" style="width: 100%">
            <el-option value="dingtalk" label="钉钉" />
            <el-option value="wxwork" label="企业微信" />
            <el-option value="feishu" label="飞书" />
            <el-option value="email" label="邮件" />
            <el-option value="sms" label="短信" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="intForm.name" placeholder="如 默认钉钉机器人" />
        </el-form-item>
        <el-form-item v-if="['dingtalk','wxwork','feishu'].includes(intForm.type)" label="Webhook URL">
          <el-input v-model="intForm.webhookUrl" placeholder="https://oapi.dingtalk.com/robot/send?access_token=..." />
        </el-form-item>
        <el-form-item v-if="intForm.type === 'dingtalk'" label="加签密钥">
          <el-input v-model="intForm.secret" type="password" show-password placeholder="SEC..." />
        </el-form-item>
        <el-form-item v-if="intForm.type === 'sms'" label="Access Key ID">
          <el-input v-model="intForm.accessKeyId" type="password" show-password />
        </el-form-item>
        <el-form-item v-if="intForm.type === 'sms'" label="Access Key Secret">
          <el-input v-model="intForm.accessKeySecret" type="password" show-password />
        </el-form-item>
        <el-form-item label="自定义配置">
          <el-input v-model="intForm.customConfig" type="textarea" :rows="3" placeholder="JSON 格式 (选填)" />
        </el-form-item>
        <el-form-item label="重试次数">
          <el-input-number v-model="intForm.retryTimes" :min="0" :max="10" />
        </el-form-item>
        <el-form-item label="超时(秒)">
          <el-input-number v-model="intForm.timeoutSeconds" :min="5" :max="300" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="intForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="intDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="intSaving" @click="saveInt">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from '@/utils/notify';
import {
  systemApi,
  type AiModelOption,
  type IntegrationOption,
} from '@/api/system';
import { useUserStore } from '@/stores/user';
import type { FormInstance, FormRules } from 'element-plus';

const userStore = useUserStore();

type Scope = 'personal' | 'team';
const scope = ref<Scope>('personal');

// 仅当用户至少有一个非个人组织时,才允许切到团队视图
const hasTeamOrgs = computed(() => {
  const personalId = userStore.user?.orgId;
  const orgIds = userStore.user?.orgIds ?? [];
  return orgIds.some((id) => id !== personalId);
});
const teamOrgName = computed(() => {
  // 预留: 后续从团队列表 store 读取。当前先用固定提示。
  return '当前团队';
});

const tab = ref<'llm' | 'int'>('llm');
const models = ref<AiModelOption[]>([]);
const integrations = ref<IntegrationOption[]>([]);
const loading = ref(false);
const loadingModels = ref(false);
const errorMessage = ref('');

const modelDialogVisible = ref(false);
const intDialogVisible = ref(false);
const modelSaving = ref(false);
const intSaving = ref(false);

const modelFormRef = ref<FormInstance>();
const editingModel = ref<AiModelOption | null>(null);
const editingInt = ref<IntegrationOption | null>(null);

const modelForm = ref({
  name: '',
  provider: '',
  modelKey: '',
  apiKey: '',
  baseUrl: '',
  enabled: true,
  capabilities: 'chat',
  remark: '',
});

const intForm = ref<Partial<IntegrationOption>>({
  type: 'dingtalk',
  name: '',
  webhookUrl: '',
  secret: '',
  accessKeyId: '',
  accessKeySecret: '',
  customConfig: '',
  enabled: true,
  retryTimes: 3,
  timeoutSeconds: 30,
});

const modelRules: FormRules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择厂商', trigger: 'change' }],
  modelKey: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
};

const typeLabelMap: Record<string, string> = {
  dingtalk: '钉钉',
  wxwork: '企业微信',
  feishu: '飞书',
  email: '邮件',
  sms: '短信',
};

// Vue2 filters 类型在 Vue3 中不直接存在,使用计算属性代替
const isLoading = computed(() => loading.value || loadingModels.value);

function refresh() {
  load();
}

async function load() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [m, i] = await Promise.all([
      loadModelsSafe(),
      loadIntegrationsSafe(),
    ]);
    models.value = m;
    integrations.value = i;
  } finally {
    loading.value = false;
  }
}

/**
 * infra 服务未启动/未注册时,网关会返回 503。
 * 这种情况不算真正的业务错误,不弹提示,直接当作空列表展示即可。
 * 只有服务本身返回的业务错误(如 4xx)才提示用户。
 */
function isServiceUnavailable(err: any): boolean {
  const status = err?.response?.status ?? err?.status;
  return status === 404 || status === 503 || status === 502 || status === 504 || !err?.response;
}

async function loadModelsSafe(): Promise<AiModelOption[]> {
  try {
    return (await systemApi.listAllModels({ scope: scope.value })) as AiModelOption[];
  } catch (err: any) {
    if (isServiceUnavailable(err)) return [];
    errorMessage.value = err?.message || '模型列表加载失败';
    ElMessage.error('加载模型列表失败');
    return [];
  }
}

async function loadIntegrationsSafe(): Promise<IntegrationOption[]> {
  try {
    return (await systemApi.listAllIntegrations({ scope: scope.value })) as IntegrationOption[];
  } catch (err: any) {
    if (isServiceUnavailable(err)) return [];
    errorMessage.value = err?.message || '集成列表加载失败';
    ElMessage.error('加载集成列表失败');
    return [];
  }
}

watch(scope, () => {
  load();
});

function openModelDialog(row?: any) {
  editingModel.value = row ?? null;
  if (row) {
    modelForm.value = {
      name: row.name ?? '',
      provider: row.provider ?? '',
      modelKey: row.modelKey ?? '',
      apiKey: '',
      baseUrl: row.baseUrl ?? '',
      enabled: row.enabled ?? true,
      capabilities: row.capabilities ?? 'chat',
      remark: (row as any).remark ?? '',
    };
  } else {
    modelForm.value = {
      name: '',
      provider: '',
      modelKey: '',
      apiKey: '',
      baseUrl: '',
      enabled: true,
      capabilities: 'chat',
      remark: '',
    };
  }
  modelDialogVisible.value = true;
}

async function saveModel() {
  if (scope.value === 'team') return;
  if (!modelFormRef.value) return;
  await modelFormRef.value.validate(async (valid) => {
    if (!valid) return;
    modelSaving.value = true;
    try {
      const payload = { ...modelForm.value };
      if (editingModel.value) {
        await systemApi.updateModel(editingModel.value.id, payload);
      } else {
        await systemApi.createModel(payload);
      }
      modelDialogVisible.value = false;
      ElMessage.success('已保存');
      await load();
    } catch {
      // handled
    } finally {
      modelSaving.value = false;
    }
  });
}

async function onToggleLLM(row: any, v: any) {
  if (scope.value === 'team') {
    row.enabled = !v;
    return;
  }
  try {
    await systemApi.updateModel(row.id, { enabled: v });
    ElMessage.success(v ? '已启用' : '已禁用');
  } catch {
    row.enabled = !v;
  }
}

async function onSetDefault(row: any) {
  if (scope.value === 'team') return;
  try {
    await systemApi.setDefaultModel(row.id);
    ElMessage.success(`已将「${row.name}」设为默认模型`);
    await load();
  } catch {
    // handled
  }
}

async function onDeleteModel(row: any) {
  if (scope.value === 'team') return;
  await ElMessageBox.confirm(`确定删除模型「${row.name}」吗?`, '提示', {
    type: 'warning',
  }).catch(() => null);
  try {
    await systemApi.deleteModel(row.id);
    ElMessage.success('已删除');
    await load();
  } catch {
    // handled
  }
}

async function onTestModel(row: any) {
  ElMessage.info(`正在测试「${row.name}」...`);
  try {
    const res = await systemApi.testModel(row.id);
    if (res.success) ElMessage.success(res.message || '模型连接正常');
    else ElMessage.warning(res.message || '连接失败');
  } catch {
    ElMessage.error('测试请求失败');
  }
}

function openIntDialog(row?: IntegrationOption) {
  editingInt.value = row ?? null;
  if (row) {
    intForm.value = {
      type: row.type,
      name: row.name ?? '',
      webhookUrl: row.webhookUrl ?? '',
      secret: row.secret ?? '',
      accessKeyId: row.accessKeyId ?? '',
      accessKeySecret: row.accessKeySecret ?? '',
      customConfig: row.customConfig ?? '',
      enabled: row.enabled ?? true,
      retryTimes: row.retryTimes ?? 3,
      timeoutSeconds: row.timeoutSeconds ?? 30,
    };
  } else {
    intForm.value = {
      type: 'dingtalk',
      name: '',
      webhookUrl: '',
      secret: '',
      accessKeyId: '',
      accessKeySecret: '',
      customConfig: '',
      enabled: true,
      retryTimes: 3,
      timeoutSeconds: 30,
    };
  }
  intDialogVisible.value = true;
}

async function saveInt() {
  if (scope.value === 'team') return;
  if (!intForm.value.type || !intForm.value.name) {
    ElMessage.warning('请填写完整');
    return;
  }
  intSaving.value = true;
  try {
    const payload = { ...intForm.value };
    if (editingInt.value) {
      await systemApi.updateIntegration(editingInt.value.id, payload);
    } else {
      await systemApi.createIntegration(payload);
    }
    intDialogVisible.value = false;
    ElMessage.success('已保存');
    await load();
  } catch {
    // handled
  } finally {
    intSaving.value = false;
  }
}

async function onToggleInt(row: any, v: any) {
  if (scope.value === 'team') {
    row.enabled = !v;
    return;
  }
  try {
    await systemApi.updateIntegration(row.id, { enabled: v });
    ElMessage.success(v ? '已启用' : '已禁用');
  } catch {
    row.enabled = !v;
  }
}

async function onDeleteInt(row: IntegrationOption) {
  if (scope.value === 'team') return;
  await ElMessageBox.confirm(`确定删除集成「${row.name}」吗?`, '提示', {
    type: 'warning',
  }).catch(() => null);
  try {
    await systemApi.deleteIntegration(row.id);
    ElMessage.success('已删除');
    await load();
  } catch {
    // handled
  }
}

async function onTestInt(row: IntegrationOption) {
  ElMessage.info(`正在测试「${row.name}」...`);
  try {
    const res = await systemApi.testIntegration(row.id);
    if (res.success) ElMessage.success(res.message || '连接成功');
    else ElMessage.warning(res.message || '连接失败');
  } catch {
    ElMessage.error('测试请求失败');
  }
}

// ---- Icons ----
function iconBg(t: string) {
  const map: Record<string, string> = {
    dingtalk: 'rgba(59,130,246,0.15)',
    wxwork: 'rgba(16,185,129,0.15)',
    feishu: 'rgba(99,102,241,0.15)',
    email: 'rgba(245,158,11,0.15)',
    sms: 'rgba(236,72,153,0.15)',
  };
  return map[t] ?? 'rgba(99,102,241,0.15)';
}
function iconClass(t: string) {
  const map: Record<string, string> = {
    dingtalk: 'fa-brands fa-dingtalk',
    wxwork: 'fa-brands fa-weixin',
    feishu: 'fa-solid fa-paper-plane',
    email: 'fa-solid fa-envelope',
    sms: 'fa-solid fa-mobile-screen',
  };
  return map[t] ?? 'fa-solid fa-plug';
}
function mask(v: string) {
  if (!v) return '-';
  if (v.length <= 8) return v;
  return v.slice(0, 4) + '****' + v.slice(-4);
}

onMounted(load);
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.readonly-banner {
  display: flex;
  gap: 8px;
  align-items: center;
  background: var(--primary-bg);
  color: var(--primary);
  padding: 8px 12px;
  border-radius: var(--radius-md);
  font-size: 12px;
  margin-bottom: 12px;
}
.page-title {
  font-size: 18px;
  font-weight: 700;
}
.page-subtitle {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
.notice-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  background: var(--info-bg);
  color: var(--info);
  padding: 8px 12px;
  border-radius: var(--radius-md);
  font-size: 12px;
  margin-bottom: 12px;
}
.notice-bar.warning {
  background: var(--warning-bg);
  color: var(--warning);
}
.card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 14px;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
}
.card-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.key-mask {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.endpoint-text {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  color: var(--text-secondary);
}
.integration-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}
.int-card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.add-card {
  align-items: center;
  justify-content: center;
  border-style: dashed;
  cursor: pointer;
  color: var(--text-tertiary);
  min-height: 140px;
  transition: all 0.2s;
}
.add-card:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.add-card i { font-size: 24px; margin-bottom: 4px; }
.int-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.int-icon {
  width: 40px; height: 40px;
  border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  color: var(--primary); font-size: 18px;
}
.int-name { font-weight: 600; font-size: 14px; }
.int-desc { font-size: 12px; color: var(--text-tertiary); margin-top: 2px; }
.int-config {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 8px;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.config-row { display: flex; justify-content: space-between; }
.config-row .k { color: var(--text-tertiary); }
.config-row .v { color: var(--text-primary); font-family: ui-monospace, monospace; }
.int-footer { display: flex; justify-content: flex-end; gap: 8px; }
.settings-tabs :deep(.el-tabs__item) { font-size: 14px; }
.flex-1 { flex: 1; min-width: 0; }
</style>

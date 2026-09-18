<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">告警中心</div>
        <div class="page-subtitle">告警规则、触发记录与处理状态</div>
      </div>
      <div class="header-actions">
        <el-button @click="loadAll"><i class="fa-solid fa-rotate"></i><span>刷新</span></el-button>
        <el-button type="primary" @click="openCreate"><i class="fa-solid fa-plus"></i><span>新建规则</span></el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card"><span>规则总数</span><strong>{{ ruleTotal }}</strong></div>
      <div class="stat-card danger"><span>触发中</span><strong>{{ stats.firing }}</strong></div>
      <div class="stat-card"><span>已解决</span><strong>{{ resolvedCount }}</strong></div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadAll">
      <el-tab-pane label="告警规则" name="rules">
        <el-table v-loading="loadingRules" :data="rules">
          <el-table-column prop="name" label="规则名称" min-width="150" />
          <el-table-column prop="metricName" label="指标" min-width="150" />
          <el-table-column label="条件" width="180">
            <template #default="{ row }">
              {{ operatorLabel(row.conditionType) }} {{ row.thresholdValue }}
            </template>
          </el-table-column>
          <el-table-column label="窗口" width="100">
            <template #default="{ row }">{{ row.timeWindowSeconds ?? 300 }}s</template>
          </el-table-column>
          <el-table-column label="通知渠道" width="120">
            <template #default="{ row }">{{ channelLabel(row.notificationChannels) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="告警记录" name="records">
        <el-table v-loading="loadingRecords" :data="records" empty-text="暂无告警记录">
          <el-table-column label="规则" min-width="150">
            <template #default="{ row }">{{ row.ruleName || row.title || '-' }}</template>
          </el-table-column>
          <el-table-column label="指标" min-width="150">
            <template #default="{ row }">{{ row.metricName || '-' }}</template>
          </el-table-column>
          <el-table-column label="触发条件" width="170">
            <template #default="{ row }">
              {{ operatorLabel(row.operator) }} {{ row.currentValue ?? row.triggerValue ?? '-' }}
              <span class="muted">/ {{ row.threshold ?? row.thresholdValue ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="严重程度" width="100">
            <template #default="{ row }">
              <el-tag :type="severityType(row.severity)" size="small">{{ row.severity || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'FIRING' ? 'danger' : 'success'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="通知" width="130">
            <template #default="{ row }">
              <el-tag :type="notifyStatusType(row.notifyStatus)" size="small">
                {{ notifyStatusLabel(row.notifyStatus) }}
              </el-tag>
              <span v-if="row.notifyCount" class="muted"> ×{{ row.notifyCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="触发时间" width="180">
            <template #default="{ row }">{{ formatTime(row.firedAt || row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'FIRING'" link type="primary" @click="resolve(row)">解决</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-drawer v-model="detailVisible" title="告警详情" size="520px">
          <el-descriptions v-if="selectedRecord" :column="1" border>
            <el-descriptions-item label="规则">{{ selectedRecord.ruleName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="消息">{{ selectedRecord.alertMessage || selectedRecord.message || '-' }}</el-descriptions-item>
            <el-descriptions-item label="指标">{{ selectedRecord.metricName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="触发条件">
              {{ operatorLabel(selectedRecord.operator) }}
              {{ selectedRecord.currentValue ?? selectedRecord.triggerValue ?? '-' }}
              / 阈值 {{ selectedRecord.threshold ?? selectedRecord.thresholdValue ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="严重程度">{{ selectedRecord.severity || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ selectedRecord.status }}</el-descriptions-item>
            <el-descriptions-item label="通知渠道">{{ channelLabel(selectedRecord.alertChannel) }}</el-descriptions-item>
            <el-descriptions-item label="通知状态">
              {{ notifyStatusLabel(selectedRecord.notifyStatus) }}
              <span v-if="selectedRecord.notifyCount">（{{ selectedRecord.notifyCount }} 次）</span>
            </el-descriptions-item>
            <el-descriptions-item label="最近通知">{{ formatTime(selectedRecord.notifyTime) }}</el-descriptions-item>
            <el-descriptions-item label="触发时间">{{ formatTime(selectedRecord.firedAt || selectedRecord.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="解决时间">{{ formatTime(selectedRecord.resolveTime || selectedRecord.resolvedAt) }}</el-descriptions-item>
            <el-descriptions-item label="解决说明">{{ selectedRecord.resolveComment || selectedRecord.resolutionNote || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-drawer>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑告警规则' : '新建告警规则'" width="560px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="规则名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="指标名称" prop="metricName"><el-input v-model="form.metricName" placeholder="workflow.failure.rate" /></el-form-item>
        <el-form-item label="比较条件" prop="operator">
          <el-select v-model="form.operator" style="width: 100%">
            <el-option label="大于" value="gt" />
            <el-option label="大于等于" value="gte" />
            <el-option label="小于" value="lt" />
            <el-option label="小于等于" value="lte" />
            <el-option label="等于" value="eq" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="threshold"><el-input-number v-model="form.threshold" :precision="2" /></el-form-item>
        <el-form-item label="时间窗口"><el-input-number v-model="form.duration" :min="30" :step="30" /> 秒</el-form-item>
        <el-form-item label="通知渠道">
          <el-select v-model="form.alertChannel" style="width: 100%">
            <el-option label="邮件" value="email" />
            <el-option label="钉钉" value="dingtalk" />
            <el-option label="短信" value="sms" />
          </el-select>
        </el-form-item>
        <el-form-item label="Webhook"><el-input v-model="form.webhook" placeholder="可选" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from '@/utils/notify';
import { monitorApi, type AlertRecord, type AlertRule, type AlertRulePayload } from '@/api/monitor';

const activeTab = ref('rules');
const loadingRules = ref(false);
const loadingRecords = ref(false);
const detailVisible = ref(false);
const selectedRecord = ref<AlertRecord | null>(null);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref<number | string | null>(null);
const formRef = ref<FormInstance>();
const rules = ref<AlertRule[]>([]);
const records = ref<AlertRecord[]>([]);
const ruleTotal = ref(0);
const stats = reactive({ firing: 0 });

const form = reactive<AlertRulePayload>({
  name: '', description: '', metricName: '', operator: 'gt',
  threshold: 0, duration: 300, alertChannel: 'email', webhook: '', status: 'active',
});
const formRules: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  metricName: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
  operator: [{ required: true, message: '请选择比较条件', trigger: 'change' }],
  threshold: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
};
const resolvedCount = computed(() => records.value.filter((item) => item.status === 'RESOLVED').length);

async function loadRules() {
  loadingRules.value = true;
  try {
    const result = await monitorApi.rules({ pageNum: 1, pageSize: 100 });
    rules.value = result.records ?? [];
    ruleTotal.value = result.total ?? rules.value.length;
  } finally {
    loadingRules.value = false;
  }
}

async function loadRecords() {
  loadingRecords.value = true;
  try {
    const result = await monitorApi.records({ pageNum: 1, pageSize: 100 });
    records.value = result.records ?? [];
  } finally {
    loadingRecords.value = false;
  }
}

async function loadAll() {
  await Promise.all([
    loadRules(),
    loadRecords(),
    monitorApi.stats().then((value) => Object.assign(stats, value)),
  ]);
}

function resetForm() {
  Object.assign(form, {
    name: '', description: '', metricName: '', operator: 'gt',
    threshold: 0, duration: 300, alertChannel: 'email', webhook: '', status: 'active',
  });
}

function openCreate() {
  editingId.value = null;
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: any) {
  editingId.value = row.id;
  Object.assign(form, {
    name: row.name,
    description: row.description ?? '',
    metricName: row.metricName,
    operator: row.conditionType ?? 'gt',
    threshold: Number(row.thresholdValue ?? 0),
    duration: row.timeWindowSeconds ?? 300,
    alertChannel: firstChannel(row.notificationChannels),
    webhook: '',
    status: row.enabled ? 'active' : 'inactive',
  });
  dialogVisible.value = true;
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    if (editingId.value) await monitorApi.updateRule(editingId.value, form);
    else await monitorApi.createRule(form);
    ElMessage.success('已保存');
    dialogVisible.value = false;
    await loadRules();
  } finally {
    saving.value = false;
  }
}

async function removeRule(row: any) {
  await ElMessageBox.confirm(`确定删除规则「${row.name}」？`, '提示', { type: 'warning' }).catch(() => null);
  await monitorApi.deleteRule(row.id);
  ElMessage.success('已删除');
  await loadRules();
}

async function resolve(row: any) {
  await monitorApi.resolve(row.id);
  ElMessage.success('告警已解决');
  await loadAll();
}

function channelLabel(value?: string) {
  if (!value) return '-';
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.join(', ') : String(parsed);
  } catch {
    return value;
  }
}
function firstChannel(value?: string) {
  if (!value) return 'email';
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed) && parsed.length > 0) return String(parsed[0]);
  } catch {
    // 兼容旧数据中的逗号分隔字符串。
  }
  return value.split(',')[0]?.trim() || 'email';
}
function operatorLabel(value?: string) {
  return ({ gt: '>', gte: '>=', lt: '<', lte: '<=', eq: '=' } as Record<string, string>)[value ?? ''] ?? value ?? '';
}
function openDetail(row: any) {
  selectedRecord.value = row;
  detailVisible.value = true;
}

function formatTime(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
}

function notifyStatusLabel(value?: string) {
  return ({
    SUCCESS: '已通知',
    PARTIAL: '部分通知',
    FAILED: '通知失败',
    SKIPPED: '未配置渠道',
    NOT_SENT: '未通知',
  } as Record<string, string>)[value ?? ''] ?? '未通知';
}

function notifyStatusType(value?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'SUCCESS') return 'success';
  if (value === 'PARTIAL' || value === 'SKIPPED') return 'warning';
  if (value === 'FAILED') return 'danger';
  return 'info';
}
function severityType(value?: string) {
  if (value === 'CRITICAL' || value === 'ERROR') return 'danger';
  if (value === 'WARNING') return 'warning';
  return 'info';
}

onMounted(loadAll);
</script>

<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:20px; }
.page-title { font-size:22px; font-weight:700; }
.page-subtitle { margin-top:6px; color:var(--text-tertiary); font-size:13px; }
.header-actions { display:flex; gap:8px; }
.stat-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px; margin-bottom:16px; }
.stat-card { padding:16px 20px; border:1px solid var(--border); border-radius:12px; background:var(--bg-primary); display:flex; justify-content:space-between; align-items:center; }
.stat-card span { color:var(--text-secondary); font-size:13px; }
.stat-card strong { font-size:24px; color:var(--primary); }
.stat-card.danger strong { color:var(--danger); }
.header-actions span { margin-left:6px; }
.muted { color: var(--text-tertiary); font-size: 12px; }
</style>

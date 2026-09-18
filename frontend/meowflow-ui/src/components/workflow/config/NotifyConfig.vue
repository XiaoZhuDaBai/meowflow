<template>
  <div class="notify-config">
    <el-form label-width="100px" size="small">
      <!-- 优先选择已配置的集成 -->
      <el-form-item label="选择集成" required>
        <el-select
          v-model="integrationId"
          :loading="loadingInts"
          placeholder="选择已配置的通知集成"
          filterable
          clearable
          style="width: 100%"
        >
          <el-option-group
            v-for="(items, type) in groupedIntegrations"
            :key="type"
            :label="typeLabel(type)"
          >
            <el-option
              v-for="i in items"
              :key="i.id"
              :value="i.id"
              :label="i.name"
              :disabled="!i.enabled"
            >
              <span>{{ i.name }}</span>
              <span v-if="!i.enabled" style="color: var(--text-tertiary); margin-left: 6px">已禁用</span>
            </el-option>
          </el-option-group>
          <template #empty>
            <div class="empty-tip">
              暂无可用集成,请到
              <el-link type="primary" @click="openSettings">系统设置</el-link>
              中新增
            </div>
          </template>
        </el-select>
      </el-form-item>

      <el-form-item label="平台">
        <el-radio-group v-model="platform">
          <el-radio-button value="dingtalk">钉钉</el-radio-button>
          <el-radio-button value="wxwork">企微</el-radio-button>
          <el-radio-button value="feishu">飞书</el-radio-button>
          <el-radio-button value="email">邮件</el-radio-button>
          <el-radio-button value="sms">短信</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- Webhook 覆盖(可选) -->
      <el-form-item v-if="['dingtalk','wxwork','feishu'].includes(platform)" label="Webhook 覆盖">
        <el-input v-model="webhook" placeholder="留空则使用所选集成配置" />
      </el-form-item>
      <el-form-item v-if="platform === 'dingtalk'" label="加签密钥">
        <el-input v-model="secret" type="password" show-password placeholder="留空则使用所选集成配置" />
      </el-form-item>
      <el-form-item v-if="platform === 'dingtalk'" label="@手机号">
        <el-input v-model="atMobiles" placeholder="13800000000,13900000000" />
      </el-form-item>

      <!-- 邮件 -->
      <template v-if="platform === 'email'">
        <el-form-item label="SMTP 主机">
          <el-input v-model="smtpHost" placeholder="smtp.example.com" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="smtpPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="发件人">
          <el-input v-model="fromAddress" placeholder="meowflow@example.com" />
        </el-form-item>
      </template>

      <!-- 短信 -->
      <template v-if="platform === 'sms'">
        <el-form-item label="服务商">
          <el-select v-model="smsProvider">
            <el-option value="aliyun" label="阿里云" />
            <el-option value="tencent" label="腾讯云" />
          </el-select>
        </el-form-item>
        <el-form-item label="签名">
          <el-input v-model="smsSign" placeholder="喵流" />
        </el-form-item>
        <el-form-item label="模板 ID">
          <el-input v-model="smsTemplate" placeholder="SMS_123456789" />
        </el-form-item>
      </template>

      <el-form-item label="消息类型">
        <el-radio-group v-model="msgType">
          <el-radio-button value="text">文本</el-radio-button>
          <el-radio-button value="markdown">Markdown</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="platform === 'email'" label="收件人">
        <el-input v-model="to" placeholder="{{trigger.email}}" />
      </el-form-item>

      <el-form-item v-if="platform === 'email'" label="主题">
        <el-input v-model="subject" />
      </el-form-item>

      <el-form-item label="消息内容">
        <CodeEditor
          v-model="text"
          :rows="6"
          placeholder="使用 {{nodeId.field}} 引用上游变量"
        />
        <VariablePicker inline :groups="variableGroups" @insert="onInsert" />
      </el-form-item>

      <el-form-item v-if="text" label="预览">
        <div class="preview">{{ prettifiedText }}</div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import CodeEditor from '@/components/common/CodeEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';
import { useWorkflowStore } from '@/stores/workflow';
import { prettifyVariablePath } from '@/utils/variableGraph';
import { systemApi, type IntegrationOption } from '@/api/system';

const router = useRouter();
const store = useWorkflowStore();

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const integrations = ref<IntegrationOption[]>([]);
const loadingInts = ref(false);

async function loadIntegrations() {
  loadingInts.value = true;
  try {
    integrations.value = await systemApi.enabledIntegrations();
  } catch {
    integrations.value = [];
  } finally {
    loadingInts.value = false;
  }
}

onMounted(loadIntegrations);

const groupedIntegrations = computed(() => {
  const result: Record<string, IntegrationOption[]> = {};
  for (const i of integrations.value) {
    if (!result[i.type]) result[i.type] = [];
    result[i.type]!.push(i);
  }
  return result;
});

function typeLabel(t: string) {
  const map: Record<string, string> = {
    dingtalk: '钉钉',
    wxwork: '企业微信',
    feishu: '飞书',
    email: '邮件',
    sms: '短信',
  };
  return map[t] ?? t;
}

function patch(extra: Record<string, any>) {
  emit('update:modelValue', { ...props.modelValue, ...extra });
}

/** 选中的集成 ID */
const integrationId = computed({
  get: () => props.modelValue?.integrationId ?? '',
  set: (v: number | string) => patch({ integrationId: v }),
});

const platform = computed({
  get: () => props.modelValue?.platform ?? 'dingtalk',
  set: (v: string) => patch({ platform: v }),
});

const webhook = computed({
  get: () => props.modelValue?.webhook ?? '',
  set: (v: string) => patch({ webhook: v }),
});

const secret = computed({
  get: () => props.modelValue?.secret ?? '',
  set: (v: string) => patch({ secret: v }),
});

const atMobiles = computed({
  get: () => props.modelValue?.atMobiles ?? '',
  set: (v: string) => patch({ atMobiles: v }),
});

const msgType = computed({
  get: () => props.modelValue?.msgType ?? 'markdown',
  set: (v: string) => patch({ msgType: v }),
});

const text = computed({
  get: () => props.modelValue?.text ?? '',
  set: (v: string) => patch({ text: v }),
});

const to = computed({
  get: () => props.modelValue?.to ?? '',
  set: (v: string) => patch({ to: v }),
});

const subject = computed({
  get: () => props.modelValue?.subject ?? '',
  set: (v: string) => patch({ subject: v }),
});

const smtpHost = computed({
  get: () => props.modelValue?.smtpHost ?? '',
  set: (v: string) => patch({ smtpHost: v }),
});
const smtpPort = computed({
  get: () => Number(props.modelValue?.smtpPort ?? 465),
  set: (v: number) => patch({ smtpPort: v }),
});
const fromAddress = computed({
  get: () => props.modelValue?.fromAddress ?? '',
  set: (v: string) => patch({ fromAddress: v }),
});

const smsProvider = computed({
  get: () => props.modelValue?.smsProvider ?? 'aliyun',
  set: (v: string) => patch({ smsProvider: v }),
});
const smsSign = computed({
  get: () => props.modelValue?.smsSign ?? '',
  set: (v: string) => patch({ smsSign: v }),
});
const smsTemplate = computed({
  get: () => props.modelValue?.smsTemplate ?? '',
  set: (v: string) => patch({ smsTemplate: v }),
});

function onInsert(path: string) {
  text.value = (text.value ?? '') + `{{${path}}}`;
}

function openSettings() {
  router.push('/settings');
}

const prettifiedText = computed(() =>
  prettifyVariablePath(text.value ?? '', store.current),
);
</script>

<style scoped>
.preview {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
  font-family: ui-monospace, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}
.notify-config :deep(.el-form-item) {
  align-items: flex-start;
}
.empty-tip {
  padding: 6px 10px;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>

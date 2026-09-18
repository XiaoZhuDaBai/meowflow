<template>
  <el-dialog
    :model-value="modelValue"
    title="工作流已发布"
    width="640"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div class="success">
      <i class="fa-solid fa-rocket"></i>
      <div class="title">工作流已成功发布</div>
      <div class="subtitle">{{ workflowName }}</div>
    </div>

    <el-tabs v-model="activeTab" class="webhook-tabs">
      <el-tab-pane
        v-for="t in triggers"
        :key="t.type"
        :label="t.label"
        :name="t.type"
      >
        <div class="trigger-block">
          <div class="kv">
            <span class="k">触发方式</span>
            <span class="v">{{ t.label }}</span>
          </div>
          <div v-if="t.type === 'webhook'" class="kv">
            <span class="k">URL</span>
            <div class="copyable">
              <code>{{ t.url }}</code>
              <el-button size="small" link @click="copy(t.url)">
                <i class="fa-regular fa-copy"></i>
              </el-button>
            </div>
          </div>
          <div v-if="t.type === 'webhook'" class="kv">
            <span class="k">Method</span>
            <span class="v">{{ method }}</span>
          </div>
          <div v-if="t.type === 'cron'" class="kv">
            <span class="k">表达式</span>
            <span class="v">{{ cron }}</span>
          </div>
          <div v-if="t.type === 'cron'" class="kv">
            <span class="k">时区</span>
            <span class="v">{{ timezone }}</span>
          </div>
        </div>

        <el-divider />

        <h4>调用示例</h4>
        <CodeEditor
          :model-value="t.example"
          language="text"
          :rows="5"
          readonly
        />

        <div class="actions">
          <el-button @click="copy(t.example)">
            <i class="fa-regular fa-copy"></i><span style="margin-left:4px">复制示例</span>
          </el-button>
          <el-button v-if="t.type === 'webhook'" type="primary" @click="onOpenTester">
            <i class="fa-solid fa-flask-vial"></i><span style="margin-left:4px">自检</span>
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-divider />
    <div class="footer-tip">
      <i class="fa-solid fa-circle-info"></i>
      <span>可将此工作流复制到团队中,或继续调整节点配置</span>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" @click="onViewDetail">
        <i class="fa-solid fa-eye"></i><span style="margin-left:4px">查看详情</span>
      </el-button>
    </template>

    <el-dialog
      v-model="testerOpen"
      title="Webhook 自检"
      width="640"
      append-to-body
    >
      <WebhookTester
        :default-url="webhookUrl"
        :default-method="method"
      />
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import CodeEditor from '@/components/common/CodeEditor.vue';
import WebhookTester from '@/components/workflow/config/WebhookTester.vue';
import { ElMessage } from '@/utils/notify';

const props = defineProps<{
  modelValue: boolean;
  workflowId: string;
  workflowName: string;
  triggers: Array<{
    type: 'webhook' | 'cron';
    label: string;
    url?: string;
    example: string;
  }>;
  method?: 'GET' | 'POST' | 'PUT';
  cron?: string;
  timezone?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
}>();

const router = useRouter();
const activeTab = ref(props.triggers[0]?.type ?? 'webhook');
const testerOpen = ref(false);

const method = computed(() => props.method ?? 'POST');
const cron = computed(() => props.cron ?? '');
const timezone = computed(() => props.timezone ?? 'Asia/Shanghai');

const webhookUrl = computed(() => props.triggers.find((t) => t.type === 'webhook')?.url ?? '');

function copy(text: string) {
  if (!text) return;
  navigator.clipboard
    ?.writeText(text)
    .then(() => ElMessage.success('已复制'))
    .catch(() => ElMessage.error('复制失败,请手动复制'));
}

function onOpenTester() {
  if (!webhookUrl.value) {
    ElMessage.warning('请先配置 Webhook 触发器');
    return;
  }
  testerOpen.value = true;
}

function onViewDetail() {
  emit('update:modelValue', false);
  router.push(`/workflows/${props.workflowId}/detail`);
}
</script>

<style scoped>
.success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 0;
}
.success i {
  font-size: 36px;
  color: var(--success);
}
.title { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.subtitle { font-size: 12px; color: var(--text-tertiary); }

.trigger-block {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 12px;
}
.kv {
  display: grid;
  grid-template-columns: 100px 1fr;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}
.kv .k { color: var(--text-tertiary); }
.kv .v {
  color: var(--text-primary);
  font-family: ui-monospace, monospace;
}
.copyable {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.copyable code {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  padding: 2px 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 4px;
  word-break: break-all;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.webhook-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
h4 { font-size: 13px; font-weight: 600; margin: 0 0 8px 0; }
.footer-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--primary-bg);
  color: var(--primary);
  border-radius: var(--radius-sm);
  font-size: 12px;
}
</style>

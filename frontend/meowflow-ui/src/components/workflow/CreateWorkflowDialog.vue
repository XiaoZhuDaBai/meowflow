<template>
  <el-dialog
    v-model="visible"
    title="新建工作流"
    width="800px"
    :close-on-click-modal="false"
    class="create-workflow-dialog"
  >
    <el-tabs v-model="activeTab" class="create-tabs">
      <el-tab-pane label="从空白创建" name="blank">
        <CreateBlankTab ref="blankTabRef" />
      </el-tab-pane>
      <el-tab-pane label="从模板创建" name="template">
        <CreateTemplateTab ref="templateTabRef" :templates="templateList" />
      </el-tab-pane>
      <el-tab-pane label="导入 DSL" name="dsl">
        <CreateDslTab ref="dslTabRef" />
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="dialog-footer">
        <div class="footer-tip">
          <template v-if="activeTab === 'blank'">
            选择触发器并填写名称即可创建空白工作流
          </template>
          <template v-else-if="activeTab === 'template'">
            <template v-if="templateTabRef?.selectedId">
              已选择模板，可直接创建
            </template>
            <template v-else>
              点击卡片选择模板
            </template>
          </template>
          <template v-else>
            上传或拖拽 DSL/JSON 文件导入工作流
          </template>
        </div>
        <div class="footer-actions">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onCreate">
            创建工作流
            <i class="fa-solid fa-arrow-right" style="margin-left: 4px"></i>
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from '@/utils/notify';
import { useWorkflowStore } from '@/stores/workflow';
import { useTemplateStore } from '@/stores/template';
import type { Template } from '@/api/types';
import { parseWorkflowJson } from '@/api/template';
import { createNodeId } from '@/mock/workflows';
import CreateBlankTab from './CreateBlankTab.vue';
import CreateTemplateTab from './CreateTemplateTab.vue';
import CreateDslTab from './CreateDslTab.vue';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'created', id: string): void;
}>();

const router = useRouter();
const store = useWorkflowStore();
const templateStore = useTemplateStore();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const activeTab = ref('blank');
const submitting = ref(false);

const blankTabRef = ref<InstanceType<typeof CreateBlankTab> | null>(null);
const templateTabRef = ref<InstanceType<typeof CreateTemplateTab> | null>(null);
const dslTabRef = ref<InstanceType<typeof CreateDslTab> | null>(null);

const templateList = computed(() => templateStore.items);

// Load templates when dialog opens
watch(visible, async (v) => {
  if (v && templateStore.items.length === 0) {
    await templateStore.search({});
  }
});

// Trigger defaults by type
const TRIGGER_DEFAULTS: Record<string, any> = {
  'trigger.webhook': {
    type: 'trigger.webhook',
    category: 'trigger',
    name: 'Webhook 触发器',
    x: 240,
    y: 180,
    config: { method: 'POST', path: '/hooks/incoming' },
  },
  'trigger.cron': {
    type: 'trigger.cron',
    category: 'trigger',
    name: '定时触发器',
    x: 240,
    y: 180,
    config: { cron: '0 * * * *', timezone: 'Asia/Shanghai' },
  },
  'trigger.form': {
    type: 'trigger.form',
    category: 'trigger',
    name: '表单触发器',
    x: 240,
    y: 180,
    config: { formId: '', fields: [] },
  },
  'trigger.imessage': {
    type: 'trigger.imessage',
    category: 'trigger',
    name: '消息触发器',
    x: 240,
    y: 180,
    config: { platform: 'dingtalk', keyword: '' },
  },
};

async function onCreate() {
  submitting.value = true;

  try {
    if (activeTab.value === 'blank') {
      await createBlank();
    } else if (activeTab.value === 'template') {
      await createFromTemplate();
    } else {
      await createFromDsl();
    }
  } finally {
    submitting.value = false;
  }
}

async function createBlank() {
  const data = blankTabRef.value?.getSubmitData();
  if (!data?.name) {
    ElMessage.warning('请填写工作流名称');
    return;
  }

  const triggerNode = TRIGGER_DEFAULTS[data.triggerType];
  const nodes = triggerNode
    ? [{ id: createNodeId(), ...triggerNode }]
    : [];

  // Create workflow via API
  const created = await store.createWorkflow({
    name: data.name,
    description: data.description,
    category: data.category,
    icon: data.icon ? `${data.icon.icon}:${data.icon.color}` : undefined,
    nodes,
    edges: [],
  });

  ElMessage.success('工作流创建成功');
  visible.value = false;
  router.push(`/editor/${created.id}`);
  emit('created', created.id);
}

async function createFromTemplate() {
  const data = templateTabRef.value?.getSubmitData();
  if (!data) {
    ElMessage.warning('请选择一个模板');
    return;
  }

  const tpl = await templateStore.fetchById(data.id);
  if (!tpl) {
    ElMessage.error('模板不存在');
    return;
  }

  const rawWorkflowJson = tpl.workflowJson || (tpl as any).__workflowJson;
  const workflowJson = typeof rawWorkflowJson === 'string'
    ? rawWorkflowJson
    : rawWorkflowJson
      ? JSON.stringify(rawWorkflowJson)
      : undefined;
  const parsed = parseWorkflowJson(workflowJson);
  if (!workflowJson || parsed.error || parsed.nodes.length === 0) {
    ElMessage.error('模板工作流定义为空，暂时无法创建');
    return;
  }

  const created = await store.createFromTemplate(data.id, data.name, workflowJson, {
    description: tpl.description,
    categoryId: tpl.categoryId,
    icon: tpl.coverEmoji,
  });
  ElMessage.success('基于模板创建成功');
  visible.value = false;
  await router.push(`/editor/${created.id}`);
  emit('created', created.id);
}

async function createFromDsl() {
  const data = dslTabRef.value?.getSubmitData();
  if (!data) {
    ElMessage.warning('请先上传有效的 DSL 文件');
    return;
  }

  const result = await store.importDsl(data.json);
  ElMessage.success('导入成功');
  visible.value = false;
  router.push(`/editor/${result.workflowId}`);
  emit('created', result.workflowId);
}
</script>

<style scoped>
:deep(.create-workflow-dialog .el-dialog__body) {
  padding: 16px 24px;
}

:deep(.create-tabs .el-tabs__header) {
  margin-bottom: 16px;
}

:deep(.create-tabs .el-tabs__item) {
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.footer-tip {
  font-size: 12px;
  color: var(--text-tertiary);
}

.footer-actions {
  display: flex;
  gap: 8px;
}
</style>

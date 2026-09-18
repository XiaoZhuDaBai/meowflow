<template>
  <el-dialog
    v-model="visible"
    title="分享到模板市场"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="onClosed"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="模板名称" prop="name">
        <el-input v-model="form.name" placeholder="给模板起个名字" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="模板描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="描述这个模板的用途和使用场景"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="行业分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="选择行业分类" style="width: 100%">
          <el-option
            v-for="c in categories"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="标签">
        <el-select
          v-model="form.tagIds"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="选择或输入标签（最多 5 个）"
          style="width: 100%"
          :max-collapse-tags="3"
        >
          <el-option
            v-for="t in tags"
            :key="t.id"
            :label="t.name"
            :value="t.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="封面图标">
        <div class="emoji-picker">
          <div
            v-for="e in emojiOptions"
            :key="e"
            class="emoji-item"
            :class="{ selected: form.coverIcon === e }"
            @click="form.coverIcon = e"
          >{{ e }}</div>
        </div>
        <div class="cover-icon-tip">选择封面 emoji，或在左侧输入框中填写</div>
        <el-input v-model="form.coverIcon" placeholder="可输入 emoji 或 FontAwesome 类名" style="margin-top: 8px" />
      </el-form-item>

      <el-form-item label="是否公开">
        <el-switch
          v-model="form.isPublic"
          active-value="Y"
          inactive-value="N"
          active-text="公开"
          inactive-text="私有"
        />
        <div class="form-tip">公开模板所有用户可见，私有模板仅自己可见</div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="onSubmit">
        提交审核
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { templateApi, templateReviewApi } from '@/api/template';

const props = defineProps<{
  modelValue: boolean;
  /** 工作流节点数组（用于生成模板定义） */
  nodes?: any[];
  /** 工作流连线数组 */
  edges?: any[];
  /** 当前工作流名称（用于预填模板名） */
  workflowName?: string;
  /** 当前工作流版本（用于生成 workflowGraph） */
  version?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'success', templateId: number): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const formRef = ref();
const submitting = ref(false);
const categories = ref<any[]>([]);
const tags = ref<any[]>([]);

// Emoji 选项
const emojiOptions = ['🚀', '💬', '📊', '🤖', '📝', '🔔', '📧', '🔍', '⚙️', '💡', '📁', '🔗', '📱', '🌐', '🛠️', '✨'];

// 表单
const form = reactive({
  name: '',
  description: '',
  categoryId: undefined as number | undefined,
  tagIds: [] as number[],
  coverIcon: '🚀',
  isPublic: 'Y' as 'Y' | 'N',
});

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入模板描述', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择行业分类', trigger: 'change' }],
};

// 加载分类和标签
async function loadMeta() {
  try {
    const [cats, tgs] = await Promise.all([
      templateApi.getCategories(),
      templateApi.getTags(),
    ]);
    categories.value = cats || [];
    tags.value = tgs || [];
  } catch {
    // ignore
  }
}

// 预填默认值
watch(visible, (v) => {
  if (v) {
    form.name = props.workflowName ? `${props.workflowName} 模板` : '';
    form.description = '';
    form.categoryId = undefined;
    form.tagIds = [];
    form.coverIcon = '🚀';
    form.isPublic = 'Y';
    loadMeta();
  }
});

function onClosed() {
  formRef.value?.resetFields();
}

// 生成 workflowGraph（从节点坐标计算中心点）
function buildWorkflowGraph(): string {
  if (!props.nodes?.length) return '{}';
  const graphNodes = props.nodes.map((n: any) => {
    const w = n.width || 240, h = n.height || 60;
    return {
      id: n.id,
      type: n.type,
      name: n.name || n.type,
      category: n.category,
      cx: (n.x ?? 0) + w / 2,
      cy: (n.y ?? 0) + h / 2,
      w,
      h,
      data: n.data ?? n.config ?? {},
    };
  });
  const graphEdges = (props.edges || []).map((e: any) => ({
    id: e.id,
    source: e.source,
    target: e.target,
    label: e.label,
    type: e.type,
  }));
  return JSON.stringify({ nodes: graphNodes, edges: graphEdges });
}

// 生成完整 definition
function buildDefinition(): string {
  return JSON.stringify({
    version: props.version || 'v1',
    nodes: props.nodes || [],
    edges: props.edges || [],
    variables: null,
    inputSchema: null,
    outputSchema: null,
    config: null,
  });
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const definition = buildDefinition();
    const workflowGraph = buildWorkflowGraph();

    // 1. 创建模板
    const template = await templateApi.createTemplate({
      name: form.name.trim(),
      description: form.description.trim(),
      definition,
      workflowGraph,
      categoryId: form.categoryId,
      tagIds: form.tagIds,
      coverIcon: form.coverIcon,
      isPublic: form.isPublic,
    } as any);

    // 2. 提交审核
    if (template?.id) {
      try {
        await templateReviewApi.submitForReview(Number(template.id));
      } catch {
        // review already submitted or not needed
      }
    }

    ElMessage.success('已提交审核，审核通过后将在模板市场展示');
    visible.value = false;
    emit('success', Number(template?.id));
  } catch (e: any) {
    ElMessage.error(e.message || e || '提交失败');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.emoji-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 120px;
  overflow-y: auto;
}

.emoji-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.15s;
}

.emoji-item:hover {
  background: var(--el-fill-color-light);
}

.emoji-item.selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.cover-icon-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">我的工作流</div>
        <div class="page-subtitle">共 {{ total }} 个工作流 · 支持搜索、筛选与快速操作</div>
      </div>
      <div class="flex gap-8">
        <el-button @click="$router.push('/templates')">
          <i class="fa-solid fa-puzzle-piece"></i><span style="margin-left:6px">使用模板</span>
        </el-button>
        <el-button type="primary" @click="onCreate">
          <i class="fa-solid fa-plus"></i><span style="margin-left:6px">新建工作流</span>
        </el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-input v-model="filters.keyword" placeholder="搜索工作流名称/描述" clearable style="width: 280px" @keyup.enter="load" @clear="load">
        <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
      </el-input>
      <div class="flex gap-8">
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="load">
          <el-option label="全部" value="" />
          <el-option label="草稿" value="draft" />
          <el-option label="运行中" value="running" />
          <el-option label="已停止" value="stopped" />
          <el-option label="已归档" value="archived" />
        </el-select>
        <el-select v-model="filters.category" placeholder="分类" clearable style="width: 140px" @change="load">
          <el-option label="全部" value="" />
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
      </div>
    </div>

    <div class="workflow-grid">
      <div v-for="wf in list" :key="wf.id" class="wf-card">
        <div class="wf-head">
          <div class="wf-icon"><i class="fa-solid fa-diagram-project"></i></div>
          <div class="wf-meta">
            <div class="wf-name">{{ wf.name }}</div>
            <div class="wf-cat">{{ wf.category }} · v{{ wf.version }}</div>
          </div>
          <StatusTag :status="wf.status" />
        </div>
        <div class="wf-desc">{{ wf.description || '—' }}</div>
        <div class="wf-stats">
          <div>
            <div class="label">总执行</div>
            <div class="value">{{ formatNumber(wf.stats.executions) }}</div>
          </div>
          <div>
            <div class="label">成功率</div>
            <div class="value">{{ formatPercent(wf.stats.successRate) }}</div>
          </div>
          <div>
            <div class="label">平均耗时</div>
            <div class="value">{{ formatDuration(wf.stats.avgDuration) }}</div>
          </div>
          <div>
            <div class="label">总成本</div>
            <div class="value">{{ formatCost(wf.stats.totalCost) }}</div>
          </div>
        </div>
        <div class="wf-footer">
          <span class="muted">更新于 {{ formatRelativeTime(wf.updatedAt) }}</span>
          <div class="flex gap-8">
            <el-button size="small" @click="$router.push(`/workflows/${wf.id}/detail`)">
              <i class="fa-solid fa-eye"></i>
              <span style="margin-left:4px">详情</span>
            </el-button>
            <el-button size="small" type="primary" @click="$router.push(`/editor/${wf.id}`)">
              <i class="fa-solid fa-pen-to-square"></i>
              <span style="margin-left:4px">编辑</span>
            </el-button>
            <el-dropdown trigger="click" @command="(cmd) => onAction(cmd, wf.id)">
              <el-button size="small" link><i class="fa-solid fa-ellipsis"></i></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="duplicate">复制</el-dropdown-item>
                  <el-dropdown-item command="publish">发布</el-dropdown-item>
                  <el-dropdown-item command="stop">停止</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !list.length" icon="fa-solid fa-folder-open" title="暂无工作流" description="点击右上角“新建工作流”开始你的第一个自动化流程" />
  </div>

  <CreateWorkflowDialog v-model="createDialogVisible" @created="onCreated" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import StatusTag from '@/components/common/StatusTag.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import CreateWorkflowDialog from '@/components/workflow/CreateWorkflowDialog.vue';
import { useWorkflowStore } from '@/stores/workflow';
import { workflowApi } from '@/api/workflow';
import { ElMessage, ElMessageBox } from '@/utils/notify';
import { formatCost, formatDuration, formatNumber, formatPercent, formatRelativeTime } from '@/utils/format';

const router = useRouter();
const store = useWorkflowStore();
const loading = ref(false);
const createDialogVisible = ref(false);

const filters = reactive({ keyword: '', status: '', category: '' });

const list = computed(() => store.list);
const total = computed(() => store.total);

const categories = computed(() => Array.from(new Set(list.value.map((w) => w.category))));

async function load() {
  loading.value = true;
  try {
    await store.fetchList({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      category: filters.category || undefined,
    });
  } finally {
    loading.value = false;
  }
}

function onCreate() {
  createDialogVisible.value = true;
}

function onCreated(id: string) {
  load();
}

async function onAction(cmd: string, id: string) {
  if (cmd === 'duplicate') {
    await workflowApi.duplicate(id);
    ElMessage.success('已复制');
    await load();
  } else if (cmd === 'publish') {
    await workflowApi.publish(id);
    ElMessage.success('已发布');
    await load();
  } else if (cmd === 'stop') {
    await workflowApi.stop(id);
    ElMessage.success('已停止');
    await load();
  } else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除该工作流?该操作不可恢复', '危险操作', { type: 'warning' });
      await store.removeById(id);
      await load();
    } catch { /* cancelled */ }
  }
}

onMounted(load);
</script>

<style scoped>
.workflow-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}
.wf-card {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.wf-card:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow-md);
}
.wf-head { display: flex; align-items: center; gap: 12px; }
.wf-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--primary-bg);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.wf-meta { flex: 1; min-width: 0; }
.wf-name { font-weight: 600; font-size: 14px; color: var(--text-primary); }
.wf-cat { font-size: 11px; color: var(--text-tertiary); margin-top: 2px; }
.wf-desc { font-size: 12px; color: var(--text-secondary); line-height: 1.6; min-height: 36px; }
.wf-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 8px 0;
  border-top: 1px dashed var(--border);
  border-bottom: 1px dashed var(--border);
}
.wf-stats .label { font-size: 11px; color: var(--text-tertiary); }
.wf-stats .value { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.wf-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
}
.muted { color: var(--text-tertiary); }
</style>
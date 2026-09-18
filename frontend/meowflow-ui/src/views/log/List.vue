<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">执行日志</div>
        <div class="page-subtitle">每次工作流执行的详细记录与节点状态</div>
      </div>
      <div class="flex gap-8">
        <el-button @click="load">
          <i class="fa-solid fa-rotate"></i>
          <span style="margin-left:4px">刷新</span>
        </el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="filters.workflowId" placeholder="工作流" clearable style="width: 220px" @change="onFilterChange">
        <el-option v-for="w in workflows" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="onFilterChange">
        <el-option label="全部" value="" />
        <el-option label="成功" value="success" />
        <el-option label="失败" value="failed" />
        <el-option label="运行中" value="running" />
        <el-option label="等待" value="pending" />
        <el-option label="已取消" value="cancelled" />
      </el-select>
      <el-select v-model="filters.trigger" placeholder="触发方式" clearable style="width: 140px" @change="onFilterChange">
        <el-option label="全部" value="" />
        <el-option label="手动" value="manual" />
        <el-option label="Webhook" value="webhook" />
        <el-option label="定时" value="schedule" />
        <el-option label="表单" value="form" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 240px"
        @change="onDateChange"
      />
    </div>

    <div class="card">
      <!-- Loading State -->
      <div v-if="loading" class="state-container">
        <i class="fa-solid fa-spinner fa-spin fa-2x"></i>
        <span>加载中...</span>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="state-container error">
        <i class="fa-solid fa-circle-exclamation fa-2x"></i>
        <span>{{ error }}</span>
        <el-button size="small" @click="load">重试</el-button>
      </div>

      <!-- Empty State -->
      <div v-else-if="!list.length" class="state-container empty">
        <i class="fa-solid fa-inbox fa-2x"></i>
        <span>暂无执行记录</span>
        <el-button size="small" @click="load">刷新</el-button>
      </div>

      <!-- Data Table -->
      <el-table v-else :data="list" stripe v-loading="loading">
        <el-table-column prop="workflowName" label="工作流" min-width="160" />
        <el-table-column label="触发方式" width="110">
          <template #default="{ row }">{{ triggerLabel(row.trigger) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
        </el-table-column>
        <el-table-column label="成本" width="100">
          <template #default="{ row }">{{ formatCost(row.cost) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openDetail(row.id)">快速</el-button>
            <el-button size="small" link type="primary" @click="gotoDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.status === 'running'"
              size="small"
              link
              type="danger"
              :loading="cancellingId === row.id"
              @click="onCancel(row as Execution)"
            >
              取消
            </el-button>
            <el-button
              v-if="row.status === 'failed'"
              size="small"
              link
              type="warning"
              :loading="retryingId === row.id"
              @click="onRetry(row as Execution)"
            >
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="list.length" class="pagination">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="filters.pageSize"
          v-model:current-page="filters.page"
          @current-change="load"
        />
      </div>
    </div>

    <el-drawer v-model="detailOpen" title="执行详情" size="520">
      <div v-if="current" class="detail">
        <div class="kv"><span class="k">工作流</span><span class="v">{{ current.workflowName }}</span></div>
        <div class="kv"><span class="k">状态</span><span class="v"><StatusTag :status="current.status" /></span></div>
        <div class="kv"><span class="k">触发方式</span><span class="v">{{ triggerLabel(current.trigger) }}</span></div>
        <div class="kv"><span class="k">耗时</span><span class="v">{{ formatDuration(current.duration) }}</span></div>
        <div class="kv"><span class="k">成本</span><span class="v">{{ formatCost(current.cost) }}</span></div>
        <div class="kv"><span class="k">开始时间</span><span class="v">{{ formatDate(current.startTime) }}</span></div>
        <div class="kv"><span class="k">结束时间</span><span class="v">{{ formatDate(current.endTime) }}</span></div>
        <div class="kv" v-if="current.error">
          <span class="k">错误</span>
          <span class="v" style="color: var(--danger)">{{ current.error }}</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import StatusTag from '@/components/common/StatusTag.vue';
import { executionApi } from '@/api/execution';
import { workflowApi } from '@/api/workflow';
import { useWorkflowStore } from '@/stores/workflow';
import { formatCost, formatDate, formatDuration } from '@/utils/format';
import { ElMessage } from '@/utils/notify';
import type { Execution } from '@/types/execution';
import type { WorkflowListItem } from '@/types/workflow';

const store = useWorkflowStore();
const list = ref<Execution[]>([]);
const total = ref(0);
const workflows = ref<WorkflowListItem[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const dateRange = ref<[string, string] | null>(null);
const cancellingId = ref<string | number | null>(null);
const retryingId = ref<string | number | null>(null);

const filters = reactive({
  workflowId: '' as string,
  status: '' as string,
  trigger: '' as string,
  page: 1,
  pageSize: 20,
});

const detailOpen = ref(false);
const current = ref<Execution | null>(null);

function triggerLabel(t: string) {
  const map: Record<string, string> = {
    manual: '手动',
    webhook: 'Webhook',
    schedule: '定时',
    form: '表单',
  };
  return map[t] ?? t;
}

async function load() {
  loading.value = true;
  error.value = null;
  try {
    const params: any = {
      page: filters.page,
      pageSize: filters.pageSize,
    };
    if (filters.workflowId) params.workflowId = filters.workflowId;
    if (filters.status) params.status = filters.status;
    if (filters.trigger) params.trigger = filters.trigger;
    if (dateRange.value) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await executionApi.page(params);
    list.value = res.items;
    total.value = res.total;
  } catch (e: any) {
    error.value = e?.message ?? '加载失败';
  } finally {
    loading.value = false;
  }
}

function onFilterChange() {
  filters.page = 1;
  load();
}

function onDateChange() {
  filters.page = 1;
  load();
}

async function openDetail(id: string | number) {
  try {
    current.value = await executionApi.getById(id);
    detailOpen.value = true;
  } catch (e: any) {
    ElMessage.error('加载详情失败: ' + (e?.message ?? ''));
  }
}

function gotoDetail(id: string | number) {
  window.location.href = `/executions/${id}`;
}

async function onCancel(row: Execution) {
  cancellingId.value = row.id;
  try {
    await executionApi.cancel(row.id);
    ElMessage.success('已取消');
    await load();
  } catch (e: any) {
    ElMessage.error('取消失败: ' + (e?.message ?? ''));
  } finally {
    cancellingId.value = null;
  }
}

async function onRetry(row: Execution) {
  retryingId.value = row.id;
  try {
    const result = await executionApi.execute({
      workflowId: row.workflowId,
      version: row.version,
      trigger: 'manual',
      input: row.input,
    });
    ElMessage.success(`重试成功，执行ID: ${result.id}`);
    await load();
  } catch (e: any) {
    ElMessage.error('重试失败: ' + (e?.message ?? ''));
  } finally {
    retryingId.value = null;
  }
}

onMounted(async () => {
  await store.fetchList();
  workflows.value = store.list;
  await load();
});
</script>

<style scoped>
.pagination { display: flex; justify-content: flex-end; margin-top: 12px; }
.detail { display: flex; flex-direction: column; gap: 10px; }
.kv { display: flex; gap: 12px; font-size: 13px; }
.kv .k { width: 80px; color: var(--text-tertiary); }
.kv .v { color: var(--text-primary); }

.state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px 0;
  color: var(--text-tertiary);
  font-size: 14px;
}
.state-container.error { color: var(--danger); }
.state-container.empty { color: var(--text-tertiary); }
.state-container i { font-size: 32px; }
</style>

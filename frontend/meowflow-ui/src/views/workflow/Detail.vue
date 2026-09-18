<template>
  <div class="page" v-loading="loading">
    <div class="page-header">
      <div>
        <div class="page-title">{{ data?.name || '工作流详情' }}</div>
        <div class="page-subtitle">{{ data?.description }}</div>
      </div>
      <div class="flex gap-8">
        <el-button @click="$router.back()"><i class="fa-solid fa-arrow-left"></i><span style="margin-left:6px">返回</span></el-button>
        <el-button v-if="data?.id" type="primary" @click="$router.push(`/editor/${data.id}`)">
          <i class="fa-solid fa-pen-to-square"></i><span style="margin-left:6px">编辑</span>
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="icon" style="background: var(--primary-bg); color: var(--primary);"><i class="fa-solid fa-bolt"></i></div>
        <div>
          <div class="label">总执行</div>
          <div class="value">{{ formatNumber(data?.stats.executions) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--success-bg); color: var(--success);"><i class="fa-solid fa-circle-check"></i></div>
        <div>
          <div class="label">成功率</div>
          <div class="value">{{ formatPercent(data?.stats.successRate) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--info-bg); color: var(--info);"><i class="fa-solid fa-clock"></i></div>
        <div>
          <div class="label">平均耗时</div>
          <div class="value">{{ formatDuration(data?.stats.avgDuration) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--warning-bg); color: var(--warning);"><i class="fa-solid fa-coins"></i></div>
        <div>
          <div class="label">总成本</div>
          <div class="value">{{ formatCost(data?.stats.totalCost) }}</div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top: 16px;">
      <div class="card-title">工作流定义</div>
      <div class="muted">共 {{ data?.nodes.length ?? 0 }} 个节点 / {{ data?.edges.length ?? 0 }} 条连线</div>
      <el-table :data="data?.nodes ?? []" stripe style="margin-top: 12px;">
        <el-table-column label="顺序" width="80" type="index" />
        <el-table-column label="节点" prop="name" />
        <el-table-column label="类型" prop="type" width="200">
          <template #default="{ row }"><code class="type-tag">{{ row.type }}</code></template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="配置">
          <template #default="{ row }"><JsonViewer :data="row.config" /></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { workflowApi } from '@/api/workflow';
import type { Workflow } from '@/types/workflow';
import JsonViewer from '@/components/common/JsonViewer.vue';
import { formatCost, formatDuration, formatNumber, formatPercent } from '@/utils/format';
import { NODE_CATEGORY_META } from '@/utils/constants';

const route = useRoute();
const data = ref<Workflow | null>(null);
const loading = ref(false);

function categoryLabel(c: string) {
  return NODE_CATEGORY_META[c]?.label ?? c;
}

onMounted(async () => {
  const id = route.params.id as string;
  loading.value = true;
  try {
    data.value = await workflowApi.getById(id);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.card-title { font-size: 14px; font-weight: 600; margin-bottom: 4px; }
.type-tag {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  background: var(--bg-secondary);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
}
</style>
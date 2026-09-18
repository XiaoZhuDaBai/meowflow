<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">总览</div>
        <div class="page-subtitle">今日工作流运行情况与系统概览</div>
      </div>
      <div class="text-secondary">{{ now }}</div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="icon" style="background: var(--primary-bg); color: var(--primary);"><i class="fa-solid fa-bolt"></i></div>
        <div>
          <div class="label">今日执行</div>
          <div class="value">{{ data?.today.executions ?? '-' }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--success-bg); color: var(--success);"><i class="fa-solid fa-circle-check"></i></div>
        <div>
          <div class="label">成功率</div>
          <div class="value">{{ formatPercent(data?.today.successRate) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--info-bg); color: var(--info);"><i class="fa-solid fa-clock"></i></div>
        <div>
          <div class="label">平均耗时</div>
          <div class="value">{{ formatDuration(data?.today.avgDuration) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--warning-bg); color: var(--warning);"><i class="fa-solid fa-coins"></i></div>
        <div>
          <div class="label">今日成本</div>
          <div class="value">{{ formatCost(data?.today.cost) }}</div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top: 16px;">
      <div class="card-title">Top 工作流</div>
      <el-table :data="topRows" stripe>
        <el-table-column prop="name" label="工作流" />
        <el-table-column prop="executions" label="执行次数" width="140" align="right">
          <template #default="{ row }">{{ formatNumber(row.executions) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" link @click="$router.push(`/workflows/${row.id}/detail`)">查看</el-button>
            <el-button size="small" link @click="$router.push(`/editor/${row.id}`)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card" style="margin-top: 16px;">
      <div class="card-title">最近 7 天执行趋势</div>
      <div ref="chartRef" style="height: 280px;"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { statApi } from '@/api/stat';
import { useAutoRefresh } from '@/composables/useAutoRefresh';
import { dayjs, formatCost, formatDuration, formatNumber, formatPercent } from '@/utils/format';
import type { DashboardStats } from '@/api/types';

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer]);

const data = ref<DashboardStats | null>(null);
const now = dayjs().format('YYYY-MM-DD HH:mm');
const topRows = ref<{ id: string; name: string; executions: number }[]>([]);
const chartRef = ref<HTMLDivElement>();
let chart: echarts.ECharts | null = null;

async function load() {
  data.value = await statApi.overview();
  topRows.value = data.value.topWorkflows;
  draw();
}

useAutoRefresh({ onTick: load });

function draw() {
  if (!chartRef.value || !data.value) return;
  if (!chart) chart = echarts.init(chartRef.value);
  const trend = data.value.trend;
  chart.setOption({
    grid: { left: 40, right: 16, top: 24, bottom: 32 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map((t) => t.date.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'line',
        smooth: true,
        data: trend.map((t) => t.executions),
        itemStyle: { color: '#6366f1' },
        areaStyle: { color: 'rgba(99,102,241,0.12)' },
      },
    ],
  });
}

onMounted(load);
watch(data, draw);

function onResize() {
  chart?.resize();
}
window.addEventListener('resize', onResize);
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  chart?.dispose();
  chart = null;
});
</script>

<style scoped>
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}
</style>
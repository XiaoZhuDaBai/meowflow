<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">统计看板</div>
        <div class="page-subtitle">执行趋势 · 成本构成 · 状态分布</div>
      </div>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
        />
        <el-button @click="load" :loading="loading"><i class="fa-solid fa-refresh"></i> 刷新</el-button>
        <el-button @click="onExport"><i class="fa-solid fa-download"></i> 导出 CSV</el-button>
      </div>
    </div>

    <!-- Stat cards -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="icon" style="background: var(--primary-bg); color: var(--primary);"><i class="fa-solid fa-bolt"></i></div>
        <div>
          <div class="label">{{ dateRange ? '区间执行' : '7 日执行' }}</div>
          <div class="value">{{ formatNumber(weekTotal) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--success-bg); color: var(--success);"><i class="fa-solid fa-circle-check"></i></div>
        <div>
          <div class="label">平均成功率</div>
          <div class="value">{{ formatPercent(avgSuccess) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--warning-bg); color: var(--warning);"><i class="fa-solid fa-coins"></i></div>
        <div>
          <div class="label">总成本</div>
          <div class="value">{{ formatCost(cost?.total) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="icon" style="background: var(--info-bg); color: var(--info);"><i class="fa-solid fa-list-check"></i></div>
        <div>
          <div class="label">活跃工作流</div>
          <div class="value">{{ activeCount }}</div>
        </div>
      </div>
    </div>

    <!-- Error / Loading state -->
    <div v-if="loadError && !loading" class="error-state">
      <i class="fa-solid fa-circle-exclamation"></i>
      <span>数据加载失败，请检查后端服务后</span>
      <el-button size="small" @click="load">重试</el-button>
    </div>
    <div v-else-if="!overview && !loading" class="empty-state">
      <i class="fa-solid fa-inbox"></i>
      <span>暂无统计数据</span>
    </div>

    <template v-else>
      <!-- Charts -->
      <div class="charts">
        <div class="card" style="grid-column: 1 / -1;">
          <div class="card-title">执行趋势</div>
          <div ref="trendRef" style="height: 260px;"></div>
        </div>
        <div class="card">
          <div class="card-title">成本构成（按工作流）</div>
          <div ref="barRef" style="height: 260px;"></div>
        </div>
        <div class="card">
          <div class="card-title">状态分布</div>
          <div ref="pieRef" style="height: 260px;"></div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { statApi } from '@/api/stat';
import { useAutoRefresh } from '@/composables/useAutoRefresh';
import { useWorkflowStore } from '@/stores/workflow';
import { formatCost, formatNumber, formatPercent } from '@/utils/format';
import type { CostStats, DashboardStats } from '@/api/types';

echarts.use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

const store = useWorkflowStore();
const overview = ref<DashboardStats | null>(null);
const cost = ref<CostStats | null>(null);
const loading = ref(false);
const loadError = ref(false);
const dateRange = ref<[string, string] | null>(null);

const trendRef = ref<HTMLDivElement>();
const barRef = ref<HTMLDivElement>();
const pieRef = ref<HTMLDivElement>();

let trendChart: echarts.ECharts | null = null;
let barChart: echarts.ECharts | null = null;
let pieChart: echarts.ECharts | null = null;

const weekTotal = computed(() =>
  (overview.value?.trend ?? []).reduce((s, t) => s + t.executions, 0),
);
const avgSuccess = computed(() => {
  const t = overview.value?.trend ?? [];
  if (!t.length) return 0;
  return t.reduce((s, p) => s + p.successRate, 0) / t.length;
});
const activeCount = computed(() => store.list.filter((w) => w.status === 'running').length);

function buildDateRangeParams() {
  if (!dateRange.value) return {};
  return { startDate: dateRange.value[0], endDate: dateRange.value[1] };
}

async function load() {
  loading.value = true;
  loadError.value = false;
  try {
    const [ov, cs] = await Promise.all([statApi.overview(), statApi.cost()]);
    overview.value = ov;
    cost.value = cs;
    await nextTick();
    drawTrend();
    drawBar();
    drawPie();
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function drawTrend() {
  if (!trendRef.value || !overview.value) return;
  if (!trendChart) trendChart = echarts.init(trendRef.value);
  trendChart.setOption({
    grid: { left: 40, right: 16, top: 16, bottom: 32 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: overview.value.trend.map((t) => t.date.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'line',
        smooth: true,
        data: overview.value.trend.map((t) => t.executions),
        itemStyle: { color: '#6366f1' },
        areaStyle: { color: 'rgba(99,102,241,0.18)' },
      },
    ],
  });
}

function drawBar() {
  if (!barRef.value || !cost.value) return;
  if (!barChart) barChart = echarts.init(barRef.value);
  const rows = cost.value.byWorkflow;
  barChart.setOption({
    grid: { left: 100, right: 24, top: 16, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: rows.map((r) => r.workflowName) },
    series: [
      {
        type: 'bar',
        data: rows.map((r) => ({ value: r.cost, itemStyle: { color: '#10b981' } })),
        barWidth: 16,
      },
    ],
  });
}

function drawPie() {
  if (!pieRef.value || !overview.value) return;
  if (!pieChart) pieChart = echarts.init(pieRef.value);
  const buckets = [
    { name: '成功', value: 0 },
    { name: '失败', value: 0 },
    { name: '运行中', value: 0 },
  ];
  const total = overview.value.today.executions;
  const successCount = Math.round((overview.value.today.successRate / 100) * total);
  buckets[0].value = successCount || 1;
  buckets[1].value = Math.max(1, total - successCount - 1);
  buckets[2].value = 1;
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        data: buckets.map((b) => ({
          name: b.name,
          value: b.value,
          itemStyle: { color: b.name === '成功' ? '#10b981' : b.name === '失败' ? '#ef4444' : '#f59e0b' },
        })),
      },
    ],
  });
}

function onDateChange() {
  load();
}

async function onExport() {
  const rows: string[] = [
    ['日期', '执行次数', '成功率', '成本（元）'].join(','),
    ...(overview.value?.trend ?? []).map((t) =>
      [t.date, t.executions, t.successRate.toFixed(1) + '%', t.cost.toFixed(4)].join(','),
    ),
  ];
  const csv = rows.join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `meowflow-stats-${new Date().toISOString().split('T')[0]}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function onResize() {
  trendChart?.resize();
  barChart?.resize();
  pieChart?.resize();
}

watch([overview, cost], () => {
  drawTrend();
  drawBar();
  drawPie();
});

useAutoRefresh({
  intervalMs: 30_000,
  // 自动 tick 与手动刷新/日期切换共用 load(),loading 时让 useAutoRefresh 直接跳过
  onTick: async () => {
    if (loading.value) return;
    await load();
  },
});

onMounted(async () => {
  await store.fetchList();
  await load();
});

window.addEventListener('resize', onResize);
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  trendChart?.dispose(); trendChart = null;
  barChart?.dispose();   barChart = null;
  pieChart?.dispose();   pieChart = null;
});
</script>

<style scoped>
.header-actions { display: flex; gap: 8px; align-items: center; }
.charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 12px;
}
.card-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.charts > .card:first-child { grid-column: 1 / -1; }
.error-state, .empty-state {
  display: flex; align-items: center; gap: 10px;
  padding: 32px;
  color: var(--text-tertiary);
  font-size: 14px;
  margin-top: 16px;
}
@media (max-width: 900px) {
  .charts { grid-template-columns: 1fr; }
  .header-actions { flex-wrap: wrap; }
}
</style>

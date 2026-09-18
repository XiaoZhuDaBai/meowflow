<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">审计日志</div>
        <div class="page-subtitle">记录用户操作、请求结果与异常信息</div>
      </div>
      <el-button :loading="loading" @click="load">
        <i class="fa-solid fa-rotate"></i>
        <span>刷新</span>
      </el-button>
    </div>

    <div class="card">
      <el-form inline>
        <el-form-item label="用户">
          <el-input v-model="filters.username" clearable placeholder="用户名" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="filters.module" clearable placeholder="模块名" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="module" label="模块" width="130" />
        <el-table-column prop="method" label="操作" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="90" />
        <el-table-column prop="requestUrl" label="请求地址" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="100" />
        <el-table-column prop="operateTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row as AuditLog)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="filters.pageNum"
          :page-size="filters.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="审计日志详情" size="560px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="用户">{{ current.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ current.module || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ current.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ current.requestMethod }} {{ current.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ current.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.costTime ?? 0 }} ms</el-descriptions-item>
        <el-descriptions-item label="状态">{{ current.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="json-block">{{ pretty(current.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果">
          <pre class="json-block">{{ pretty(current.responseResult) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="current.errorMsg" label="错误信息">
          <span class="error-text">{{ current.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { auditApi, type AuditLog } from '@/api/audit';

const loading = ref(false);
const records = ref<AuditLog[]>([]);
const total = ref(0);
const filters = reactive({ username: '', module: '', status: '', pageNum: 1, pageSize: 20 });
const detailVisible = ref(false);
const current = ref<AuditLog | null>(null);

async function load() {
  loading.value = true;
  try {
    const result = await auditApi.page({
      username: filters.username || undefined,
      module: filters.module || undefined,
      status: filters.status || undefined,
      pageNum: Number(filters.pageNum),
      pageSize: Number(filters.pageSize),
    });
    records.value = result.records ?? [];
    total.value = Number(result.total ?? 0);
  } catch (error: any) {
    ElMessage.error(error?.message || '加载审计日志失败');
  } finally {
    loading.value = false;
  }
}

function search() {
  filters.pageNum = 1;
  load();
}

function reset() {
  filters.username = '';
  filters.module = '';
  filters.status = '';
  search();
}

async function showDetail(row: AuditLog) {
  try {
    current.value = await auditApi.getById(row.id);
    detailVisible.value = true;
  } catch (error: any) {
    ElMessage.error(error?.message || '加载日志详情失败');
  }
}

function pretty(value?: string) {
  if (!value) return '-';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

onMounted(load);
</script>

<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:20px; }
.page-title { font-size:22px; font-weight:700; }
.page-subtitle { margin-top:6px; color:var(--text-tertiary); font-size:13px; }
.pagination-wrap { display:flex; justify-content:flex-end; margin-top:16px; }
.json-block { margin:0; white-space:pre-wrap; word-break:break-all; max-height:240px; overflow:auto; font-size:12px; }
.error-text { color:var(--danger); }
</style>

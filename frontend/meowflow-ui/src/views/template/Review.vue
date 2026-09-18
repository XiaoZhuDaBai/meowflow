<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">模板审核</div>
        <div class="page-subtitle">管理模板市场提交 · 共 {{ total }} 个待审核</div>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="待审核" name="pending">
        <template #label>
          <span class="tab-label">
            待审核
            <el-badge :value="pendingCount" :hidden="pendingCount === 0" type="warning" />
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="审核历史" name="history" />
    </el-tabs>

    <!-- 搜索筛选 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索模板名称/提交人"
        clearable
        style="width: 280px"
        @clear="loadData"
      >
        <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
      </el-input>
      <el-button type="primary" @click="loadData">
        <i class="fa-solid fa-search"></i> 搜索
      </el-button>
    </div>

    <!-- 待审核列表 -->
    <div v-if="activeTab === 'pending'">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="模板信息" min-width="200">
          <template #default="{ row }">
            <div class="template-info">
              <span class="template-name">{{ row.templateName || `模板 #${row.templateId}` }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交人" width="120">
          <template #default="{ row }">
            {{ row.submitterName || `用户 #${row.submitterId}` }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.submittedAt || row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="onApprove(row as TemplateReviewRecord)">
              <i class="fa-solid fa-check"></i> 通过
            </el-button>
            <el-button size="small" type="danger" @click="onReject(row as TemplateReviewRecord)">
              <i class="fa-solid fa-xmark"></i> 拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 审核历史 -->
    <div v-if="activeTab === 'history'">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="模板信息" min-width="200">
          <template #default="{ row }">
            <div class="template-info">
              <span class="template-name">{{ row.templateName || `模板 #${row.templateId}` }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交人" width="120">
          <template #default="{ row }">
            {{ row.submitterName || `用户 #${row.submitterId}` }}
          </template>
        </el-table-column>
        <el-table-column label="审核时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.reviewedAt || row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" min-width="200">
          <template #default="{ row }">
            <span class="comment-text">{{ row.comment || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="dialogVisible = false"
    >
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="模板ID">
          <el-input v-model.number="reviewForm.templateId" disabled />
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入审核意见（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          :type="dialogAction === 'approve' ? 'success' : 'danger'"
          :loading="submitting"
          @click="onSubmitReview"
        >
          {{ dialogAction === 'approve' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { templateReviewApi, type TemplateReviewRecord, type ReviewActionRequest } from '@/api/template';
import type { DefaultRow } from 'element-plus/es/components/table/src/table/defaults';

const activeTab = ref('pending');
const loading = ref(false);
const list = ref<TemplateReviewRecord[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const keyword = ref('');

// 统计数据
const pendingCount = ref(0);

// 审核对话框
const dialogVisible = ref(false);
const dialogTitle = ref('');
const dialogAction = ref<'approve' | 'reject'>('approve');
const submitting = ref(false);
const reviewForm = ref<ReviewActionRequest>({
  templateId: 0,
  comment: '',
});

const getStatusType = (status: string) => {
  switch (status) {
    case 'approved': return 'success';
    case 'rejected': return 'danger';
    case 'pending': return 'warning';
    default: return 'info';
  }
};

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'approved': return '已通过';
    case 'rejected': return '已拒绝';
    case 'pending': return '待审核';
    default: return status;
  }
};

const formatTime = (time?: string) => {
  if (!time) return '-';
  return new Date(time).toLocaleString('zh-CN');
};

const loadData = async () => {
  loading.value = true;
  try {
    if (activeTab.value === 'pending') {
      const res = await templateReviewApi.getPendingReviews(page.value, pageSize.value);
      list.value = res.items;
      total.value = Number(res.total ?? 0);
    } else {
      const res = await templateReviewApi.getMyReviewHistory(page.value, pageSize.value);
      list.value = res.items;
      total.value = res.total;
    }
  } catch (e: any) {
    ElMessage.error('加载审核列表失败: ' + (e.message || e));
  } finally {
    loading.value = false;
  }
};

const loadPendingCount = async () => {
  try {
    const res = await templateReviewApi.getPendingReviews(1, 1);
    pendingCount.value = Number(res.total ?? 0);
  } catch {
    // 忽略
  }
};

const onTabChange = () => {
  page.value = 1;
  loadData();
};

const onApprove = (row: TemplateReviewRecord) => {
  dialogTitle.value = '审核通过';
  dialogAction.value = 'approve';
  reviewForm.value = {
    templateId: row.templateId,
    comment: '',
  };
  dialogVisible.value = true;
};

const onReject = (row: TemplateReviewRecord) => {
  ElMessageBox.confirm(
    `确定要拒绝模板「${row.templateName || row.templateId}」吗？`,
    '确认拒绝',
    { type: 'warning' }
  ).then(() => {
    dialogTitle.value = '审核拒绝';
    dialogAction.value = 'reject';
    reviewForm.value = {
      templateId: row.templateId,
      comment: '',
    };
    dialogVisible.value = true;
  }).catch(() => {});
};

const onSubmitReview = async () => {
  submitting.value = true;
  try {
    if (dialogAction.value === 'approve') {
      await templateReviewApi.approve(reviewForm.value);
      ElMessage.success('审核通过');
    } else {
      await templateReviewApi.reject(reviewForm.value);
      ElMessage.success('已拒绝');
    }
    dialogVisible.value = false;
    loadData();
    loadPendingCount();
  } catch (e: any) {
    ElMessage.error('操作失败: ' + (e.message || e));
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadData();
  loadPendingCount();
});
</script>

<style scoped>
.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.template-info {
  display: flex;
  flex-direction: column;
}

.template-name {
  font-weight: 500;
}

.comment-text {
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

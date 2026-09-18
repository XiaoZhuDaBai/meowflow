<template>
  <div class="knowledge-container">
    <div class="header">
      <div>
        <h2>知识库管理</h2>
        <p>管理知识库、文档与向量检索测试</p>
      </div>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        创建知识库
      </el-button>
    </div>

    <el-table :data="knowledgeBases" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="vectorStoreType" label="向量库类型" width="140" />
      <el-table-column prop="dimension" label="向量维度" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleManageDocs(scope.row as KnowledgeBase)">管理文档</el-button>
          <el-button size="small" @click="openSearch(scope.row as KnowledgeBase)">检索测试</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row as KnowledgeBase)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showCreateDialog" title="创建知识库" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入知识库描述" />
        </el-form-item>
        <el-form-item label="向量库类型">
          <el-select v-model="createForm.vectorStoreType" placeholder="请选择">
            <el-option label="PGVector" value="pgvector" />
            <el-option label="Milvus" value="milvus" />
            <el-option label="Redis" value="redis" />
            <el-option label="Elasticsearch" value="elasticsearch" />
          </el-select>
        </el-form-item>
        <el-form-item label="向量维度">
          <el-input-number v-model="createForm.dimension" :min="128" :max="2048" :step="128" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDocsDialog" :title="`管理文档 - ${currentBase?.name}`" width="900px" destroy-on-close>
      <div class="docs-toolbar">
        <el-button size="small" @click="loadDocuments">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" size="small" @click="showUploadDialog = true">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
      </div>
      <el-table :data="documents" v-loading="loadingDocs" stripe empty-text="暂无文档">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="contentType" label="类型" width="140" />
        <el-table-column label="文件大小" width="100">
          <template #default="scope">
            <span v-if="scope.row.fileSize">{{ formatFileSize(scope.row.fileSize) }}</span>
            <span v-else style="color: var(--text-tertiary)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag size="small" :type="documentStatusType(scope.row.status)">{{ documentStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.filePath"
              size="small"
              @click="handleDownload(scope.row as Document)"
            >
              下载原文件
            </el-button>
            <el-button size="small" type="danger" @click="handleDeleteDoc(scope.row as Document)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="showUploadDialog" title="上传文档" width="700px">
      <el-tabs v-model="uploadTab">
        <el-tab-pane label="文本内容" name="text" />
        <el-tab-pane label="上传文件" name="file" />
      </el-tabs>
      <!-- 文本模式 -->
      <el-form v-if="uploadTab === 'text'" :model="uploadForm" label-width="100px" style="margin-top: 20px">
        <el-form-item label="文档标题" required>
          <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="内容类型">
          <el-select v-model="uploadForm.contentType" style="width: 100%">
            <el-option label="纯文本" value="text/plain" />
            <el-option label="HTML" value="text/html" />
            <el-option label="JSON" value="application/json" />
            <el-option label="Markdown" value="text/markdown" />
          </el-select>
        </el-form-item>
        <el-form-item label="文档内容" required>
          <el-input v-model="uploadForm.content" type="textarea" :rows="10" placeholder="请输入或粘贴文档内容" />
        </el-form-item>
      </el-form>
      <!-- 文件模式 -->
      <div v-if="uploadTab === 'file'" class="file-upload-area">
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".txt,.md,.html,.json,.pdf,.docx"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 txt/md/html/json/pdf/docx，单文件不超过 50MB</div>
          </template>
        </el-upload>
        <el-form :model="uploadForm" label-width="100px" style="margin-top: 16px">
          <el-form-item label="文档标题" required>
            <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
          </el-form-item>
        </el-form>
        <el-progress v-if="uploadProgress > 0" :percentage="uploadProgress" />
      </div>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">
          {{ uploadTab === 'file' ? '上传文件' : '提交' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSearchDialog" :title="`检索测试 - ${currentBase?.name}`" width="760px">
      <div class="search-toolbar">
        <el-input v-model="searchForm.query" placeholder="输入检索问题" clearable @keyup.enter="handleSearch" />
        <el-input-number v-model="searchForm.topK" :min="1" :max="20" />
        <el-button type="primary" :loading="searching" @click="handleSearch">检索</el-button>
      </div>
      <el-empty v-if="!searchResults.length" description="暂无检索结果" />
      <el-table v-else :data="searchResults" stripe>
        <el-table-column prop="content" label="匹配内容" min-width="360" show-overflow-tooltip />
        <el-table-column prop="documentTitle" label="文档" width="150" />
        <el-table-column prop="score" label="相似度" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh, Search, Upload } from '@element-plus/icons-vue';
import { UploadFilled } from '@element-plus/icons-vue';
import { knowledgeApi, type Document, type KnowledgeBase, type SearchResult } from '@/api/knowledge';

const knowledgeBases = ref<KnowledgeBase[]>([]);
const loading = ref(false);
const creating = ref(false);
const showCreateDialog = ref(false);
const createForm = ref({
  name: '',
  description: '',
  vectorStoreType: 'pgvector',
  dimension: 1536,
});

const currentBase = ref<KnowledgeBase | null>(null);
const documents = ref<Document[]>([]);
const loadingDocs = ref(false);
const showDocsDialog = ref(false);
const showUploadDialog = ref(false);
const uploading = ref(false);
const uploadProgress = ref(0);
const uploadTab = ref('text');
const uploadRef = ref();
const selectedFile = ref<File | null>(null);
const uploadForm = ref({
  title: '',
  content: '',
  contentType: 'text/plain',
});

const showSearchDialog = ref(false);
const searching = ref(false);
const searchForm = ref({ query: '', topK: 5 });
const searchResults = ref<SearchResult[]>([]);

function isNoDataError(error: any): boolean {
  const status = error?.response?.status ?? error?.status;
  const message = String(error?.message ?? '');
  return status === 404 || message.includes('资源不存在') || message.includes('不存在');
}
async function loadBases() {
  loading.value = true;
  try {
    knowledgeBases.value = await knowledgeApi.listBases();
  } catch (error: any) {
    if (isNoDataError(error)) {
      knowledgeBases.value = [];
      return;
    }
    ElMessage.error(error.message || '加载知识库列表失败');
  } finally {
    loading.value = false;
  }
}

async function loadDocuments() {
  if (!currentBase.value) return;
  loadingDocs.value = true;
  try {
    documents.value = await knowledgeApi.listDocuments(currentBase.value.id);
  } catch (error: any) {
    if (isNoDataError(error)) {
      documents.value = [];
      return;
    }
    ElMessage.error(error.message || '加载文档列表失败');
  } finally {
    loadingDocs.value = false;
  }
}

async function handleCreate() {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入知识库名称');
    return;
  }
  creating.value = true;
  try {
    await knowledgeApi.createBase(createForm.value);
    ElMessage.success('创建成功');
    showCreateDialog.value = false;
    createForm.value = { name: '', description: '', vectorStoreType: 'pgvector', dimension: 1536 };
    await loadBases();
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败');
  } finally {
    creating.value = false;
  }
}

async function handleDelete(row: KnowledgeBase) {
  try {
    await ElMessageBox.confirm(`确定删除知识库「${row.name}」吗？`, '删除确认', { type: 'warning' });
    await knowledgeApi.deleteBase(row.id);
    ElMessage.success('删除成功');
    await loadBases();
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败');
  }
}

async function handleManageDocs(row: KnowledgeBase) {
  currentBase.value = row;
  showDocsDialog.value = true;
  documents.value = [];
  await loadDocuments();
}

function openSearch(row: KnowledgeBase) {
  currentBase.value = row;
  searchForm.value = { query: '', topK: 5 };
  searchResults.value = [];
  showSearchDialog.value = true;
}

async function handleUpload() {
  if (!uploadForm.value.title.trim()) {
    ElMessage.warning('请填写文档标题');
    return;
  }

  if (uploadTab.value === 'text') {
    if (!uploadForm.value.content.trim()) {
      ElMessage.warning('请填写文档内容');
      return;
    }
    await doUploadDocument();
  } else {
    if (!selectedFile.value) {
      ElMessage.warning('请选择要上传的文件');
      return;
    }
    await doUploadFile();
  }
}

async function doUploadDocument() {
  uploading.value = true;
  try {
    await knowledgeApi.uploadDocument({
      knowledgeBaseId: currentBase.value!.id,
      ...uploadForm.value,
    });
    ElMessage.success('上传并处理成功');
    showUploadDialog.value = false;
    resetUploadForm();
    await loadDocuments();
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败');
  } finally {
    uploading.value = false;
  }
}

async function doUploadFile() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的文件');
    return;
  }
  uploading.value = true;
  uploadProgress.value = 10;
  try {
    // 使用新的 Tika 自动解析接口，一步完成：文件上传 + 内容解析 + 文档创建
    await knowledgeApi.uploadDocumentFile(
      currentBase.value!.id,
      selectedFile.value,
      uploadForm.value.title || selectedFile.value.name,
      (p) => { uploadProgress.value = p; },
    );
    uploadProgress.value = 100;

    ElMessage.success('文件上传并解析成功，内容已入库');
    showUploadDialog.value = false;
    resetUploadForm();
    await loadDocuments();
  } catch (error: any) {
    ElMessage.error(error.message || '文件上传失败');
  } finally {
    uploading.value = false;
    uploadProgress.value = 0;
  }
}

function resetUploadForm() {
  uploadForm.value = { title: '', content: '', contentType: 'text/plain' };
  selectedFile.value = null;
  uploadTab.value = 'text';
}

function handleFileChange(file: any) {
  selectedFile.value = file.raw;
  if (!uploadForm.value.title && file.name) {
    uploadForm.value.title = file.name.replace(/\.[^.]+$/, '');
  }
}

function handleFileRemove() {
  selectedFile.value = null;
}

function handleDownload(row: Document) {
  window.open(knowledgeApi.getDownloadUrl(row.id), '_blank');
}

function formatFileSize(size?: number): string {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

async function handleDeleteDoc(row: Document) {
  try {
    await ElMessageBox.confirm(`确定删除文档「${row.title}」吗？`, '删除确认', { type: 'warning' });
    await knowledgeApi.deleteDocument(row.id);
    ElMessage.success('删除成功');
    await loadDocuments();
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败');
  }
}

async function handleSearch() {
  if (!currentBase.value || !searchForm.value.query.trim()) {
    ElMessage.warning('请输入检索问题');
    return;
  }
  searching.value = true;
  try {
    searchResults.value = await knowledgeApi.search({
      knowledgeBaseId: currentBase.value.id,
      query: searchForm.value.query,
      topK: searchForm.value.topK,
    });
  } catch (error: any) {
    ElMessage.error(error.message || '检索失败');
  } finally {
    searching.value = false;
  }
}

function documentStatusLabel(status?: string) {
  return ({ pending: '待处理', processing: '处理中', completed: '已完成', failed: '失败' } as Record<string, string>)[status ?? ''] ?? status ?? '-';
}

function documentStatusType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'completed') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'processing' || status === 'pending') return 'warning';
  return 'info';
}

onMounted(loadBases);
</script>

<style scoped>
.knowledge-container {
  padding: 24px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.header p {
  margin: 6px 0 0;
  color: var(--text-tertiary);
  font-size: 13px;
}

.docs-toolbar,
.search-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.search-toolbar {
  justify-content: flex-start;
  margin-bottom: 16px;
}

.search-toolbar .el-input {
  flex: 1;
}

.file-upload-area {
  margin-top: 20px;
}

.file-upload-area :deep(.el-upload) {
  width: 100%;
}

.file-upload-area :deep(.el-upload-dragger) {
  width: 100%;
  height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>

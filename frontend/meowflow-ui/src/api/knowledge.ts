import http from './http';
import { serviceUrl } from './endpoints';

export interface KnowledgeBase {
  id: number | string;
  name: string;
  description?: string;
  vectorStoreType?: string;
  dimension?: number;
  createTime?: string;
  updateTime?: string;
  deleted?: boolean;
}

export interface Document {
  id: number | string;
  knowledgeBaseId: number | string;
  title: string;
  content?: string;
  contentType?: string;
  filePath?: string;
  fileSize?: number;
  chunkCount?: number;
  status?: string;
  errorMessage?: string;
  createTime?: string;
  updateTime?: string;
  deleted?: boolean;
}

export interface DocumentChunk {
  id: number | string;
  documentId: number | string;
  content: string;
  chunkIndex?: number;
  embedding?: number[];
  createTime?: string;
}

export interface SearchResult {
  chunkId: number | string;
  content: string;
  score: number;
  documentId?: number | string;
  documentTitle?: string;
}

const base = '/api/infra/knowledge';
const url = (path = '') => serviceUrl('infra', `${base}${path}`);

export const knowledgeApi = {
  // 知识库管理
  listBases(): Promise<KnowledgeBase[]> {
    return http.get(url('/bases')).then((r: any) => r.data ?? []);
  },

  getBase(id: number | string): Promise<KnowledgeBase> {
    return http.get(url(`/bases/${id}`)).then((r: any) => r.data);
  },

  createBase(payload: {
    name: string;
    description?: string;
    vectorStoreType?: string;
    dimension?: number;
  }): Promise<KnowledgeBase> {
    return http.post(url('/bases'), payload).then((r: any) => r.data);
  },

  deleteBase(id: number | string): Promise<void> {
    return http.delete(url(`/bases/${id}`)).then(() => undefined);
  },

  // 文档管理
  listDocuments(knowledgeBaseId: number | string): Promise<Document[]> {
    return http
      .get(url('/documents'), { params: { knowledgeBaseId } })
      .then((r: any) => r.data ?? []);
  },

  uploadDocument(payload: {
    knowledgeBaseId: number | string;
    title: string;
    content: string;
    contentType?: string;
  }): Promise<Document> {
    return http.post(url('/documents'), payload).then((r: any) => r.data);
  },

  /**
   * 上传文件并自动解析内容（使用 Apache Tika）
   * 支持 PDF, DOCX, PPTX, XLSX, TXT, HTML, JSON 等格式
   */
  uploadDocumentFile(
    knowledgeBaseId: number | string,
    file: File,
    title?: string,
    onProgress?: (percent: number) => void,
  ): Promise<Document> {
    const formData = new FormData();
    formData.append('knowledgeBaseId', String(knowledgeBaseId));
    formData.append('file', file);
    if (title) formData.append('title', title);
    return http
      .post(url('/documents/upload'), formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (event) => {
          if (onProgress && event.total) {
            onProgress(Math.round((event.loaded * 100) / event.total));
          }
        },
      })
      .then((r: any) => r.data as Document);
  },

  getDocument(id: number | string): Promise<Document> {
    return http.get(url(`/documents/${id}`)).then((r: any) => r.data);
  },

  getDocumentChunks(id: number | string): Promise<DocumentChunk[]> {
    return http.get(url(`/documents/${id}/chunks`)).then((r: any) => r.data ?? []);
  },

  deleteDocument(id: number | string): Promise<void> {
    return http.delete(url(`/documents/${id}`)).then(() => undefined);
  },

  /**
   * 下载文档原文件（如果已归档）
   * 后端返回 302 重定向到实际下载地址，这里直接拼出完整 URL 交给浏览器打开
   */
  getDownloadUrl(id: number | string): string {
    return url(`/documents/${id}/download`);
  },

  // 检索
  search(payload: {
    query: string;
    knowledgeBaseId: number | string;
    topK?: number;
  }): Promise<SearchResult[]> {
    return http.post(url('/search'), payload).then((r: any) => r.data ?? []);
  },

  buildContext(payload: {
    query: string;
    knowledgeBaseId: number | string;
    topK?: number;
    maxLength?: number;
  }): Promise<string> {
    return http.post(url('/search/context'), payload).then((r: any) => r.data ?? '');
  },
};

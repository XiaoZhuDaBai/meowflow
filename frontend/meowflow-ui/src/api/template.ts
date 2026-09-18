/**
 * 模板市场 API
 *
 * 后端接口:
 *   - POST /api/template/search     按关键字/分类/标签/排序/分页搜索，返回 MyBatis IPage<TemplateDTO>
 *   - GET  /api/template/{id}       获取模板详情
 *   - POST /api/template/{id}/use   使用模板（仅 useCount+1，不返回工作流实例）
 *   - GET  /api/template/search/categories  获取全部分类
 *   - GET  /api/template/search/tags        获取全部标签
 *
 * Mock 兼容:
 *   - GET  /templates               返回扁平 { items, total }，字段为前端 Template
 *   - GET  /templates/:id           返回前端 Template（含 coverEmoji 等 mock 字段）
 *   - POST /templates/:id/use       直接返回一个由模板生成的 Workflow
 *
 * 通过 serviceUrl('template', backendPath, mockPath) 区分：
 *   真实后端通过网关 + servicePrefix 转发，Mock 走本地拦截表。
 */

import { isMockEnabled, serviceUrl } from './endpoints';
import type { Template } from './types';
import type { PaginatedResponse } from '@/types/workflow';
import { adaptPageResponse } from '@/types/api';

// =============================================================================
// 后端 DTO（与 backend/meowflow/meowflow-template 对齐）
// =============================================================================

/** 后端 TemplateDTO：与 TemplateSearchController / TemplateController 一致 */
export interface BackendTemplateDTO {
  /** 业务键：DB 自增 / 或 "builtin:..." 前缀（内置模板）。 */
  id: string | number;
  name: string;
  description?: string;
  /** 工作流定义 JSON（Full Definition：{nodes, edges, ...}），由共享契约 WorkflowFullDefinition 归一化 */
  definition?: string;
  /** 兼容字段，与 definition 等价 */
  workflowJson?: string;
  /** 模板对应工作流的图形（MarketWorkflowGraph：{nodes{cx,cy,w,h}, edges}） */
  workflowGraph?: string;
  categoryId?: string | number;
  categoryName?: string;
  coverImage?: string;
  coverIcon?: string;
  previewImages?: string;
  tagIds?: string[] | string;
  tagNames?: string[];
  useCount?: number;
  /** 0-5 评分；与 frontend `rating` 字段兼容 */
  score?: number;
  rating?: number;
  reviewCount?: number;
  status?: string;
  reviewStatus?: string;
  reviewComment?: string;
  reviewRemark?: string;
  author?: string;
  authorName?: string;
  createBy?: string;
  createByName?: string;
  createTime?: string;
  updateTime?: string;
  isPublic?: string | boolean;
  isFeatured?: string | boolean;
  /** 业务版本（v1/v2…） */
  version?: string;
  /** 修订计数 */
  templateVersion?: number;
  remark?: string;
}

/** 后端搜索请求体 */
export interface BackendTemplateSearchRequest {
  keyword?: string;
  categoryId?: string;
  tagIds?: string[];
  /** useCount | rating | createTime */
  sortBy?: 'useCount' | 'rating' | 'createTime' | string;
  /** asc | desc */
  sortOrder?: 'asc' | 'desc' | string;
  reviewStatus?: string;
  isPublic?: string;
  pageNum?: number;
  pageSize?: number;
}

/** 后端分类（与 TemplateCategory 实体对齐） */
export interface BackendTemplateCategory {
  id: string | number;
  name: string;
  parentId?: string | number;
  level?: number;
  sort?: number;
  code?: string;
  icon?: string;
  description?: string;
  status?: string;
  templateCount?: number;
  children?: BackendTemplateCategory[];
}

/** 后端标签 */
export interface BackendTemplateTag {
  id: string | number;
  name: string;
  color?: string;
  sort?: number;
  usageCount?: number;
}

/** 前端 SearchQuery（前端 UI 使用的搜索参数形态） */
export interface TemplateSearchQuery {
  keyword?: string;
  /** 前端传入的是分类名（mock 兼容），后端传入 categoryId */
  category?: string;
  categoryId?: string;
  tags?: string[];
  /** popular | recent | rating（前端 UI 语义） */
  sort?: 'popular' | 'recent' | 'rating';
  page?: number;
  pageSize?: number;
}

/** 解析后的工作流结构（从 workflowJson 字符串中提取） */
export interface ParsedWorkflowContent {
  nodes: ParsedWorkflowNode[];
  edges: ParsedWorkflowEdge[];
  raw?: any;
  error?: string;
}

export interface ParsedWorkflowNode {
  id: string;
  name?: string;
  type?: string;
  category?: string;
  description?: string;
  /** 节点左上角 x（来自 n.x 或 n.position.x） */
  x?: number;
  /** 节点左上角 y（来自 n.y 或 n.position.y） */
  y?: number;
  /** 节点中心点 x（优先使用 workflowGraph 提供，缺省时由 x + NODE_W/2 计算） */
  cx?: number;
  /** 节点中心点 y */
  cy?: number;
  /** 节点宽度（来自 workflowGraph，默认 240） */
  width?: number;
  /** 节点高度（默认 60） */
  height?: number;
  config?: Record<string, any>;
  raw?: any;
}

export interface ParsedWorkflowEdge {
  id?: string;
  source: string;
  target: string;
  label?: string;
  /** condition / loop / error / default / branch / unknown */
  edgeType?: string;
  /** 源节点出口索引（默认从 workflowGraph 中计算） */
  sourceIndex?: number;
  /** 目标节点入口索引（默认 0） */
  targetIndex?: number;
  /** 连线配置（条件表达式 / 循环条件 / 自定义参数等） */
  config?: Record<string, any>;
  raw?: any;
}

// =============================================================================
// 映射：DTD → 前端 Template
// =============================================================================

/**
 * 将后端 TemplateDTO 映射为前端 Template，
 * 同时兼容 mock（已是 Template 结构）的输入。
 */
export function mapTemplateDTO(raw: BackendTemplateDTO | Template | null | undefined): Template | null {
  if (!raw) return null;
  const obj = raw as any;

  // mock 数据已带 coverEmoji/isOfficial/rating/usageCount 等前端字段，直接返回
  const isAlreadyTemplate =
    typeof obj.coverEmoji === 'string' ||
    typeof obj.isOfficial === 'boolean' ||
    Array.isArray(obj.tagIds) === false && Array.isArray(obj.tags);

  // tagNames -> tags；保留 tagIds 用于后续按 id 过滤
  const tags: string[] = Array.isArray(obj.tagNames)
    ? obj.tagNames.filter((s: any) => typeof s === 'string')
    : Array.isArray(obj.tags)
    ? obj.tags
    : [];

  const categoryId: string | undefined = obj.categoryId ?? obj.category;
  const categoryName: string | undefined = obj.categoryName ?? obj.category;
  const author: string = obj.createByName || obj.createBy || obj.author || '官方';

  // 兼容 mock 的 createdAt 是 ISO 字符串；后端 createTime 也是 ISO
  const createdAt: string =
    obj.createdAt ?? obj.createTime ?? new Date(0).toISOString();

  const result: Template = {
    id: String(obj.id),
    name: obj.name ?? '',
    description: obj.description ?? '',
    category: categoryName || categoryId || '未分类',
    tags,
    coverEmoji: obj.coverEmoji ?? obj.coverIcon ?? '',
    author,
    usageCount: Number(obj.useCount ?? obj.usageCount ?? 0),
    rating: Number(obj.score ?? obj.rating ?? 0),
    isOfficial: obj.isOfficial ?? (obj.isFeatured === 'Y' || obj.reviewStatus === 'approved'),
    workflowId: obj.workflowId,
    createdAt,
    likes: Number(obj.likes ?? 0),
    isLiked: Boolean(obj.isLiked),
    isFavorited: Boolean(obj.isFavorited),
    // 扩展字段透传，前端 Template 类型未声明但运行时存在
    ...(obj.workflowJson ? { workflowJson: obj.workflowJson } : {}),
    ...(obj.definition ? { workflowJson: obj.definition } : {}),
    ...(obj.workflowGraph ? { workflowGraph: obj.workflowGraph } : {}),
    ...(categoryId ? { categoryId } : {}),
    ...(obj.coverImage ? { coverImage: obj.coverImage } : {}),
    ...(obj.previewImages ? { previewImages: obj.previewImages } : {}),
    ...(obj.reviewCount !== undefined ? { reviewCount: obj.reviewCount } : {}),
    ...(obj.reviewStatus ? { reviewStatus: obj.reviewStatus } : {}),
    ...(obj.version !== undefined ? { version: obj.version } : {}),
    ...(obj.remark ? { remark: obj.remark } : {}),
  } as Template;

  // 标记原始字段保留以供详情页 / 预览使用
  // （兼容后端：definition 字段与 workflowJson 等价）
  if (obj.workflowJson) (result as any).__workflowJson = obj.workflowJson;
  else if (obj.definition) (result as any).__workflowJson = obj.definition;
  if (obj.workflowGraph) (result as any).__workflowGraph = obj.workflowGraph;

  return result;
}

/**
 * 将前端 sort 语义映射为后端 sortBy/sortOrder
 *  - popular -> useCount, desc
 *  - recent  -> createTime, desc
 *  - rating  -> rating, desc
 */
function mapSortToBackend(sort?: TemplateSearchQuery['sort']): Pick<BackendTemplateSearchRequest, 'sortBy' | 'sortOrder'> {
  switch (sort) {
    case 'recent':
      return { sortBy: 'createTime', sortOrder: 'desc' };
    case 'rating':
      return { sortBy: 'rating', sortOrder: 'desc' };
    case 'popular':
    default:
      return { sortBy: 'useCount', sortOrder: 'desc' };
  }
}

/**
 * 解析 workflowJson 字符串，返回节点与连线。
 * 支持多种 schema：
 *   { nodes: [...], edges: [...] }                —— AntV G6 风格 / 内置模板
 *   { nodes: [{ id, type, name, position:{x,y}, data, ... }] } —— workflow API 风格
 *   [ { ...node } ]                               —— 纯数组（视为 nodes）
 *
 * 自动归一化：
 *   - 节点坐标：n.x / n.y 或 n.position.x / n.position.y
 *   - 边类型：n.type / n.data.type / n.edgeType
 *   - 边标签：n.label / n.data.label / n.condition
 */
export function parseWorkflowJson(workflowJson?: string | null): ParsedWorkflowContent {
  const empty: ParsedWorkflowContent = { nodes: [], edges: [], raw: null };
  if (!workflowJson || typeof workflowJson !== 'string') return empty;
  let parsed: any;
  try {
    parsed = JSON.parse(workflowJson);
  } catch (e: any) {
    return { nodes: [], edges: [], raw: workflowJson, error: e?.message ?? 'parse error' };
  }
  const nodesRaw = Array.isArray(parsed?.nodes)
    ? parsed.nodes
    : Array.isArray(parsed)
      ? parsed
      : [];
  const edgesRaw = Array.isArray(parsed?.edges) ? parsed.edges : [];

  const DEFAULT_W = 240;
  const DEFAULT_H = 60;

  const nodes: ParsedWorkflowNode[] = nodesRaw.map((n: any, idx: number) => {
    const pos = n?.position;
    const x = typeof n?.x === 'number' ? n.x : typeof pos?.x === 'number' ? pos.x : undefined;
    const y = typeof n?.y === 'number' ? n.y : typeof pos?.y === 'number' ? pos.y : undefined;
    const config = n?.data ?? n?.config ?? {};
    return {
      id: String(n?.id ?? `node-${idx}`),
      name: n?.name ?? n?.title ?? n?.label ?? n?.type ?? `节点 ${idx + 1}`,
      type: n?.type,
      category: n?.category,
      description: n?.description ?? n?.remark,
      x,
      y,
      cx: typeof n?.cx === 'number' ? n.cx : x != null ? x + DEFAULT_W / 2 : undefined,
      cy: typeof n?.cy === 'number' ? n.cy : y != null ? y + DEFAULT_H / 2 : undefined,
      width: typeof n?.w === 'number' ? n.w : typeof n?.width === 'number' ? n.width : DEFAULT_W,
      height: typeof n?.h === 'number' ? n.h : typeof n?.height === 'number' ? n.height : DEFAULT_H,
      config,
      raw: n,
    };
  });

  const edges: ParsedWorkflowEdge[] = edgesRaw.map((e: any, idx: number) => {
    const data = e?.data ?? {};
    return {
      id: e?.id ?? `edge-${idx}`,
      source: String(e?.source ?? e?.from ?? ''),
      target: String(e?.target ?? e?.to ?? ''),
      label: e?.label ?? data?.label ?? (data?.condition?.operator ? String(data.condition.operator) : undefined),
      edgeType: e?.type ?? data?.type ?? e?.edgeType ?? (data?.condition ? 'condition' : 'default'),
      sourceIndex: typeof e?.sourceIndex === 'number' ? e.sourceIndex : undefined,
      targetIndex: typeof e?.targetIndex === 'number' ? e.targetIndex : 0,
      config: (data?.config && typeof data.config === 'object') ? data.config : undefined,
      raw: e,
    };
  });
  return { nodes, edges, raw: parsed };
}

/**
 * 解析 workflowGraph（后端 / mock 输出的轻量归一化结构）。
 * 与 parseWorkflowJson 的区别：workflowGraph 节点必含 cx/cy/w/h，
 * 用于直接渲染 SVG，无需再进行 layout 计算。
 */
export function parseWorkflowGraph(workflowGraph?: string | null): ParsedWorkflowContent {
  const empty: ParsedWorkflowContent = { nodes: [], edges: [], raw: null };
  if (!workflowGraph || typeof workflowGraph !== 'string') return empty;
  let parsed: any;
  try {
    parsed = JSON.parse(workflowGraph);
  } catch (e: any) {
    return { nodes: [], edges: [], raw: workflowGraph, error: e?.message ?? 'parse error' };
  }
  const nodesRaw = Array.isArray(parsed?.nodes) ? parsed.nodes : [];
  const edgesRaw = Array.isArray(parsed?.edges) ? parsed.edges : [];
  const nodes: ParsedWorkflowNode[] = nodesRaw.map((n: any, idx: number) => ({
    id: String(n?.id ?? `gnode-${idx}`),
    name: n?.name,
    type: n?.type,
    description: n?.description ?? n?.remark,
    category: n?.category,
    cx: typeof n?.cx === 'number' ? n.cx : undefined,
    cy: typeof n?.cy === 'number' ? n.cy : undefined,
    width: typeof n?.w === 'number' ? n.w : 240,
    height: typeof n?.h === 'number' ? n.h : 60,
    config: n?.data ?? n?.config ?? {},
    raw: n,
  }));
  const edges: ParsedWorkflowEdge[] = edgesRaw.map((e: any, idx: number) => ({
    id: e?.id ?? `gedge-${idx}`,
    source: String(e?.source ?? ''),
    target: String(e?.target ?? ''),
    label: e?.label,
    raw: e,
  }));
  return { nodes, edges, raw: parsed };
}

// =============================================================================
// HTTP helpers
// =============================================================================

import http from './http';

async function httpPost<T = any>(backendPath: string, mockPath: string, body?: any): Promise<T> {
  const url = serviceUrl('template', backendPath, mockPath);
  const resp = await http.post(url, body);
  return (resp as any).data as T;
}

async function httpGet<T = any>(backendPath: string, mockPath: string, params?: Record<string, any>): Promise<T> {
  const url = serviceUrl('template', backendPath, mockPath);
  const resp = await http.get(url, { params });
  return (resp as any).data as T;
}

async function httpDelete<T = any>(backendPath: string, mockPath: string): Promise<T> {
  const url = serviceUrl('template', backendPath, mockPath);
  const resp = await http.delete(url);
  return (resp as any).data as T;
}

// =============================================================================
// API
// =============================================================================

/**
 * 搜索/分页拉取模板列表。
 * 后端：POST /api/template/search（body: TemplateSearchRequest）
 * Mock： GET /templates?keyword=&category=（前端过滤）
 */
export const templateApi = {
  /**
   * 搜索模板（兼容真实后端 POST 与 mock GET）
   * - 后端：body 携带 keyword/categoryId/tagIds/sortBy/sortOrder/pageNum/pageSize，返回 MyBatis IPage
   * - Mock：GET /templates?keyword=&category=，返回 { items, total }
   */
  async search(query: TemplateSearchQuery = {}): Promise<PaginatedResponse<Template>> {
    const { sortBy, sortOrder } = mapSortToBackend(query.sort);
    const backendBody: BackendTemplateSearchRequest = {
      keyword: query.keyword?.trim() || undefined,
      categoryId: query.categoryId || (query.category && query.category !== 'all' ? query.category : undefined),
      tagIds: query.tags && query.tags.length ? query.tags : undefined,
      sortBy,
      sortOrder,
      pageNum: query.page ?? 1,
      pageSize: query.pageSize ?? 20,
    };

    // 真实后端 POST /api/template/search
    if (!isMockEnabled) {
      const raw = await httpPost<BackendTemplateDTO[] | { records?: BackendTemplateDTO[]; total?: number }>(
        '/api/template/search',
        '/templates',
        backendBody,
      );
      // 后端 MyBatis IPage: { records, total, size, current, pages } —— adaptPageResponse 兼容
      const page = adaptPageResponse<BackendTemplateDTO>(raw as any);
      return {
        items: (page.items || []).map((r) => mapTemplateDTO(r)).filter(Boolean) as Template[],
        total: page.total,
        page: page.page,
        pageSize: page.pageSize,
        totalPages: page.totalPages,
      };
    }

    // Mock：GET /templates?keyword=&category=  （兼容 mock 既有的 category 字段名）
    const params: Record<string, any> = {};
    if (backendBody.keyword) params.keyword = backendBody.keyword;
    if (query.category && query.category !== 'all') params.category = query.category;
    const raw = await httpGet<{ items: Template[]; total: number }>(
      '/api/template/search', // 真实后端路径——mock 模式不会被使用，但传入便于 searchUrl 复用
      '/templates',
      params,
    );
    const items = (raw?.items ?? []).map((r) => mapTemplateDTO(r as any)).filter(Boolean) as Template[];

    // 客户端按 sort 模拟服务端排序（mock 不支持）
    const sorted = sortItemsClient(items, query.sort);

    // 客户端分页
    const pageNum = query.page ?? 1;
    const pageSize = query.pageSize ?? 20;
    const start = (pageNum - 1) * pageSize;
    const slice = sorted.slice(start, start + pageSize);
    return {
      items: slice,
      total: sorted.length,
      page: pageNum,
      pageSize,
      totalPages: Math.max(1, Math.ceil(sorted.length / pageSize)),
    };
  },

  /** 获取模板详情：GET /api/template/{id} */
  async getById(id: string): Promise<Template | null> {
    const raw = await httpGet<BackendTemplateDTO | Template>(
      `/api/template/${encodeURIComponent(id)}`,
      `/templates/${encodeURIComponent(id)}`,
    );
    return mapTemplateDTO(raw as any);
  },

  /**
   * 使用模板：POST /api/template/{id}/use
   * 后端返回 Result<Void>（使用次数+1），不返回工作流实例。
   * 前端策略：返回模板详情（用于编辑器后续按模板 ID 加载 workflowJson）。
   */
  async use(id: string, _name?: string): Promise<{ template: Template }> {
    await httpPost<void>(
      `/api/template/${encodeURIComponent(id)}/use`,
      `/templates/${encodeURIComponent(id)}/use`,
      {},
    );
    // 后端 use 接口仅返回 void，需再拉详情以拿到 workflowJson
    const template = await this.getById(id);
    return { template: template as Template };
  },

  /** 加载分类树：GET /api/template/search/categories */
  async getCategories(): Promise<BackendTemplateCategory[]> {
    return httpGet<BackendTemplateCategory[]>(
      '/api/template/search/categories',
      '/templates/categories',
    );
  },

  /** 加载全部标签：GET /api/template/search/tags */
  async getTags(): Promise<BackendTemplateTag[]> {
    return httpGet<BackendTemplateTag[]>(
      '/api/template/search/tags',
      '/templates/tags',
    );
  },

  /** 点赞模板：POST /api/template/{id}/like */
  async like(id: string): Promise<void> {
    await httpPost<void>(
      `/api/template/${encodeURIComponent(id)}/like`,
      `/templates/${encodeURIComponent(id)}/like`,
      {},
    );
  },

  /** 取消点赞：DELETE /api/template/{id}/like */
  async unlike(id: string): Promise<void> {
    await httpDelete<void>(
      `/api/template/${encodeURIComponent(id)}/like`,
      `/templates/${encodeURIComponent(id)}/like`,
    );
  },

  /** 收藏模板：POST /api/template/{id}/favorite */
  async favorite(id: string): Promise<void> {
    await httpPost<void>(
      `/api/template/${encodeURIComponent(id)}/favorite`,
      `/templates/${encodeURIComponent(id)}/favorite`,
      {},
    );
  },

  /** 取消收藏：DELETE /api/template/{id}/favorite */
  async unfavorite(id: string): Promise<void> {
    await httpDelete<void>(
      `/api/template/${encodeURIComponent(id)}/favorite`,
      `/templates/${encodeURIComponent(id)}/favorite`,
    );
  },

  /** 获取我收藏的模板：GET /api/template/favorites */
  async getFavorites(): Promise<PaginatedResponse<Template>> {
    if (!isMockEnabled) {
      // 后端返回 Result.success(List<TemplateDTO>)，httpGet 解包后直接是数组
      const raw = await httpGet<BackendTemplateDTO[]>(
        '/api/template/favorites',
        '/templates/favorites',
      );
      const items = (Array.isArray(raw) ? raw : []).map(mapTemplateDTO).filter(Boolean) as Template[];
      return {
        items,
        total: items.length,
        page: 1,
        pageSize: items.length,
        totalPages: 1,
      };
    }
    const raw = await httpGet<{ items: Template[]; total: number }>(
      '/api/template/favorites',
      '/templates/favorites',
    );
    return {
      items: (raw?.items ?? []).map((r) => mapTemplateDTO(r as any)).filter(Boolean) as Template[],
      total: raw?.total ?? 0,
      page: 1,
      pageSize: raw?.items?.length ?? 0,
      totalPages: 1,
    };
  },

  /**
   * 创建模板（分享到模板市场）
   * POST /api/template
   * 后端返回 TemplateDTO（含 id、reviewStatus=pending）
   */
  async createTemplate(params: {
    name: string;
    description: string;
    definition: string;
    workflowGraph?: string;
    categoryId?: number;
    tagIds?: number[];
    coverIcon?: string;
    coverImage?: string;
    isPublic?: string;
    industry?: string;
    scene?: string;
    price?: number;
    remark?: string;
  }): Promise<BackendTemplateDTO> {
    if (!isMockEnabled) {
      return httpPost<BackendTemplateDTO>('/api/template', '/api/template', params);
    }
    // Mock 模式：模拟创建成功
    return Promise.resolve({
      id: Date.now(),
      name: params.name,
      description: params.description,
      definition: params.definition,
      reviewStatus: 'pending',
    } as BackendTemplateDTO);
  },
};

// 客户端排序兜底（mock 模式）
function sortItemsClient(items: Template[], sort?: TemplateSearchQuery['sort']): Template[] {
  const arr = items.slice();
  switch (sort) {
    case 'recent':
      arr.sort((a, b) => +new Date(b.createdAt || 0) - +new Date(a.createdAt || 0));
      break;
    case 'rating':
      arr.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
      break;
    case 'popular':
    default:
      arr.sort((a, b) => (b.usageCount ?? 0) - (a.usageCount ?? 0));
  }
  return arr;
}

export default templateApi;

// =============================================================================
// 模板审核 API（对接后端 /api/template/review/*）
// =============================================================================

/** 审核记录 */
export interface TemplateReviewRecord {
  id: number;
  templateId: number;
  templateName?: string;
  submitterId: number;
  submitterName?: string;
  reviewerId?: number;
  reviewerName?: string;
  status: 'pending' | 'approved' | 'rejected';
  action?: 'submit' | 'approve' | 'reject';
  comment?: string;
  result?: string;
  submittedAt?: string;
  reviewedAt?: string;
  createTime?: string;
  updateTime?: string;
}

/** 提交审核请求 */
export interface ReviewSubmitRequest {
  templateId: number;
}

/** 审核操作请求 */
export interface ReviewActionRequest {
  templateId: number;
  comment?: string;
}

/** 评分请求 */
export interface RatingRequest {
  score: number;
  comment?: string;
}

/** 评分统计 */
export interface RatingStatistics {
  averageScore: number;
  totalRatings: number;
  scoreDistribution: Record<number, number>;
}

/** 模板评分 */
export interface TemplateRating {
  id: number;
  templateId: number;
  userId: number;
  userName?: string;
  score: number;
  comment?: string;
  createTime?: string;
}

/**
 * 模板审核 API
 * - POST /api/template/review/submit/{templateId}  提交审核
 * - POST /api/template/review/approve               审核通过
 * - POST /api/template/review/reject               审核拒绝
 * - GET  /api/template/review/pending               待审核列表
 * - GET  /api/template/review/history               我的审核历史
 * - POST /api/template/review/rating                评分
 * - GET  /api/template/review/{templateId}/ratings 获取评分
 * - GET  /api/template/review/statistics/{templateId} 获取评分统计
 */
export const templateReviewApi = {
  /** 提交模板审核 */
  async submitForReview(templateId: number): Promise<TemplateReviewRecord> {
    return httpPost<TemplateReviewRecord>(
      `/api/template/review/submit/${templateId}`,
      `/template/review/submit/${templateId}`,
    );
  },

  /** 审核通过 */
  async approve(request: ReviewActionRequest): Promise<TemplateReviewRecord> {
    return httpPost<TemplateReviewRecord>(
      '/api/template/review/approve',
      '/template/review/approve',
      request,
    );
  },

  /** 审核拒绝 */
  async reject(request: ReviewActionRequest): Promise<TemplateReviewRecord> {
    return httpPost<TemplateReviewRecord>(
      '/api/template/review/reject',
      '/template/review/reject',
      request,
    );
  },

  /** 获取待审核列表 */
  async getPendingReviews(pageNum = 1, pageSize = 10): Promise<PaginatedResponse<TemplateReviewRecord>> {
    const raw = await httpGet<{ records?: TemplateReviewRecord[]; total?: number; size?: number; current?: number; pages?: number }>(
      '/api/template/review/pending',
      '/template/review/pending',
      { pageNum, pageSize },
    );
    return {
      items: raw?.records ?? [],
      total: raw?.total ?? 0,
      page: raw?.current ?? pageNum,
      pageSize: raw?.size ?? pageSize,
      totalPages: raw?.pages ?? 1,
    };
  },

  /** 获取我的审核历史 */
  async getMyReviewHistory(pageNum = 1, pageSize = 10): Promise<PaginatedResponse<TemplateReviewRecord>> {
    const raw = await httpGet<{ records?: TemplateReviewRecord[]; total?: number; size?: number; current?: number; pages?: number }>(
      '/api/template/review/history',
      '/template/review/history',
      { pageNum, pageSize },
    );
    return {
      items: raw?.records ?? [],
      total: raw?.total ?? 0,
      page: raw?.current ?? pageNum,
      pageSize: raw?.size ?? pageSize,
      totalPages: raw?.pages ?? 1,
    };
  },

  /** 评分模板 */
  async rate(templateId: number, request: RatingRequest): Promise<TemplateRating> {
    return httpPost<TemplateRating>(
      `/api/template/review/rating?templateId=${templateId}`,
      `/template/review/rating?templateId=${templateId}`,
      request,
    );
  },

  /** 获取模板评分列表 */
  async getRatings(templateId: number, sortType = 'RECENT'): Promise<PaginatedResponse<TemplateRating>> {
    const raw = await httpGet<{ records?: TemplateRating[]; total?: number; size?: number; current?: number; pages?: number }>(
      `/api/template/review/${templateId}/ratings`,
      `/template/review/${templateId}/ratings`,
      { sortType },
    );
    return {
      items: raw?.records ?? [],
      total: raw?.total ?? 0,
      page: raw?.current ?? 1,
      pageSize: raw?.size ?? 10,
      totalPages: raw?.pages ?? 1,
    };
  },

  /** 获取模板评分统计 */
  async getStatistics(templateId: number): Promise<RatingStatistics> {
    return httpGet<RatingStatistics>(
      `/api/template/review/statistics/${templateId}`,
      `/template/review/statistics/${templateId}`,
    );
  },
};

// 模板审核 API 已通过 export const templateReviewApi 导出
// 类型通过各自的 export interface 导出



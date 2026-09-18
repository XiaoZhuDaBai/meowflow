/**
 * 统一 API 类型与分页响应适配
 *
 * 后端 Spring Data Page 结构:
 *   { content: [...], totalElements, totalPages, size, number }
 *
 * 前端统一结构 (src/types/workflow.ts 中已定义):
 *   { items: [...], total, page, pageSize, totalPages }
 *
 * 通过 adaptPageResponse 在 API 层做一次转换。
 */

import type { PaginatedResponse } from './workflow';

/** 后端 Spring Data Page 形态 */
export interface BackendPage<T> {
  content?: T[];
  records?: T[];
  items?: T[];
  totalElements?: number;
  total?: number;
  totalPages?: number;
  pages?: number;
  size?: number;
  pageSize?: number;
  number?: number;
  current?: number;
  page?: number;
}

/** 统一 ApiResponse */
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
  traceId?: string;
}

/** 适配函数：后端 Page -> 前端 PaginatedResponse */
export function adaptPageResponse<T>(
  raw: BackendPage<T> | PaginatedResponse<T> | undefined | null,
  fallbackPage = 1,
  fallbackPageSize = 20,
): PaginatedResponse<T> {
  if (!raw) {
    return { items: [], total: 0, page: fallbackPage, pageSize: fallbackPageSize, totalPages: 0 };
  }

  // 已是前端形态
  if (Array.isArray((raw as PaginatedResponse<T>).items)) {
    return raw as PaginatedResponse<T>;
  }

  // 后端 Spring Page / MyBatis PageResponse 形态
  const page = raw as BackendPage<T>;
  const items = page.content ?? page.records ?? page.items ?? [];
  const total = Number(page.totalElements ?? page.total ?? items.length ?? 0);
  const pageSize = Number(page.size ?? page.pageSize ?? fallbackPageSize);
  const pageNumber = Number(page.current ?? page.page ?? ((page.number ?? 0) + 1));
  const totalPages = Number(page.totalPages ?? page.pages ?? (total === 0 ? 0 : Math.ceil(total / pageSize)));

  return {
    items,
    total,
    page: pageNumber,
    pageSize,
    totalPages,
  };
}

/** 列表查询参数（前端 + 后端通用） */
export interface PaginationParams {
  page?: number;
  pageSize?: number;
  current?: number;
  size?: number;
  sort?: string;
  order?: 'asc' | 'desc';
}

/** 列表通用参数（pagination + 业务过滤） */
export interface ListParams extends PaginationParams {
  keyword?: string;
  category?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}

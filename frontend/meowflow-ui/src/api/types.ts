import type { ApiResponse, PaginatedResponse } from '@/types/workflow';

export interface ListParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  category?: string;
  status?: string;
  sort?: string;
  order?: 'asc' | 'desc';
}

// 与 types/api.ts 保持兼容
export type { ListParams as LegacyListParams } from '@/types/api';

export interface DashboardToday {
  executions: number;
  successRate: number;
  avgDuration: number;
  cost: number;
}

export interface DashboardTrendPoint {
  date: string;
  executions: number;
  successRate: number;
  cost: number;
}

export interface DashboardStats {
  today: DashboardToday;
  trend: DashboardTrendPoint[];
  topWorkflows: { id: string; name: string; executions: number }[];
}

export interface CostStats {
  total: number;
  byWorkflow: { workflowId: string; workflowName: string; cost: number; percentage: number }[];
  byModel: { model: string; calls: number; cost: number }[];
  trend: { date: string; cost: number }[];
}

export interface LLMModel {
  id: string;
  name: string;
  provider: string;
  apiKeyMasked: string;
  endpoint?: string;
  enabled: boolean;
  isDefault?: boolean;
}

export interface IntegrationItem {
  id: string;
  name: string;
  type: 'dingtalk' | 'wxwork' | 'feishu' | 'email' | 'sms';
  enabled: boolean;
  config: Record<string, string>;
  description?: string;
}

export interface TeamMember {
  id: string;
  username: string;
  displayName: string;
  name?: string;
  role: string;
  department?: string;
  email: string;
  status: 'active' | 'disabled';
  joinedAt: string;
  permissions?: string[];
}

export interface Template {
  id: string;
  name: string;
  description: string;
  category: string;
  categoryId?: string;
  tags: string[];
  coverEmoji: string;
  /**
   * 备用封面图标：若 coverEmoji 为空，使用 coverIcon 作为 emoji 或 FontAwesome 类名。
   */
  coverIcon?: string;
  author: string;
  usageCount: number;
  rating: number;
  isOfficial: boolean;
  workflowId?: string;
  createdAt: string;
  // 扩展字段（后端返回但前端基础类型未声明）
  workflowJson?: string;
  workflowGraph?: string;
  coverImage?: string;
  previewImages?: string;
  reviewStatus?: string;
  reviewCount?: number;
  reviewComment?: string;
  remark?: string;
  version?: number;
  /** 点赞数（仅模板市场展示） */
  likes?: number;
  /** 当前用户是否已点赞 */
  isLiked?: boolean;
  /** 当前用户是否已收藏 */
  isFavorited?: boolean;
}

export type Req<T = any> = T;
export type Res<T> = Promise<T>;
export type ApiPage<T> = PaginatedResponse<T>;
export type R<T = any> = ApiResponse<T>;

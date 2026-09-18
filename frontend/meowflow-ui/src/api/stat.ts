import http from './http';
import { isMockEnabled, serviceUrl } from './endpoints';
import type { DashboardStats, CostStats, DashboardTrendPoint } from './types';
import { buildOverview, buildCostStats } from '@/mock/stats';

export interface ExecutionRecord {
  id: string;
  workflowId: string;
  workflowName: string;
  status: 'success' | 'failed' | 'running' | 'pending' | 'cancelled';
  trigger: string;
  startTime: string;
  endTime?: string;
  duration?: number;
  cost?: number;
}

export interface ExecutionPageResult {
  records: ExecutionRecord[];
  total: number;
  current: number;
  size: number;
}

export interface DashboardToday {
  executions: number;
  successRate: number;
  avgDuration: number;
  cost: number;
}

export const statApi = {
  /**
   * GET /workflow/api/stats/overview
   * 后端已在 StatsController 实现。
   * 失败时回退到 mock 让看板不至于空白。
   */
  overview(): Promise<DashboardStats> {
    return http
      .get(serviceUrl('workflow', '/api/stats/overview', '/stats/overview'))
      .then((r: any) => r.data)
      .catch((error) => {
        if (isMockEnabled) return buildOverview() as DashboardStats;
        throw error;
      });
  },

  /**
   * GET /workflow/api/stats/trend
   * 后端已在 StatsController 实现。
   */
  trend(): Promise<DashboardTrendPoint[]> {
    return http
      .get(serviceUrl('workflow', '/api/stats/trend', '/stats/trend'))
      .then((r: any) => r.data)
      .catch((error) => {
        if (isMockEnabled) return buildOverview().trend as DashboardTrendPoint[];
        throw error;
      });
  },

  /**
   * GET /workflow/api/stats/cost
   * 后端已在 StatsController 实现。
   */
  cost(): Promise<CostStats> {
    return http
      .get(serviceUrl('workflow', '/api/stats/cost', '/stats/cost'))
      .then((r: any) => r.data)
      .catch((error) => {
        if (isMockEnabled) return buildCostStats() as CostStats;
        throw error;
      });
  },

  /**
   * GET /monitor/api/monitor/metrics/latest — 从监控服务拉取最近 N 条指标。
   * 后端已有此端点，无需回退。
   */
  latestMetrics(limit = 50): Promise<any[]> {
    return http
      .get(serviceUrl('monitor', '/api/monitor/metrics/latest', '/metrics/latest'), {
        params: { limit },
      })
      .then((r: any) => r.data);
  },

  /**
   * GET /workflow/api/execution/page — 从执行记录分页数据适配看板统计
   */
  executionPage(params?: {
    workflowId?: string;
    status?: string;
    current?: number;
    size?: number;
    startDate?: string;
    endDate?: string;
  }): Promise<ExecutionPageResult> {
    return http
      .get(serviceUrl('workflow', '/api/execution/page', '/execution/page'), { params })
      .then((r: any) => r.data);
  },
};

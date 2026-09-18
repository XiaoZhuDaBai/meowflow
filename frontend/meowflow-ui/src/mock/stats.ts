import type { DashboardStats, CostStats } from '@/api/types';
import { dayjs } from '@/utils/format';

export function buildOverview(): DashboardStats {
  const trend = Array.from({ length: 7 }, (_, i) => {
    const d = dayjs().subtract(6 - i, 'day');
    return {
      date: d.format('YYYY-MM-DD'),
      executions: 80 + Math.floor(Math.random() * 120),
      successRate: 90 + Math.random() * 9,
      cost: 1.2 + Math.random() * 3.5,
    };
  });
  return {
    today: {
      executions: 142,
      successRate: 96.7,
      avgDuration: 1840,
      cost: 4.62,
    },
    trend,
    topWorkflows: [
      { id: 'wf-cs-bot', name: '智能客服工单分流', executions: 1248 },
      { id: 'wf-meeting-summary', name: '会议纪要生成', executions: 312 },
      { id: 'wf-hr-resume', name: 'HR 简历自动筛选', executions: 88 },
      { id: 'wf-product-copy', name: '电商商品文案', executions: 56 },
    ],
  };
}

export function buildCostStats(): CostStats {
  const trend = Array.from({ length: 7 }, (_, i) => {
    const d = dayjs().subtract(6 - i, 'day');
    return { date: d.format('YYYY-MM-DD'), cost: 1.5 + Math.random() * 4 };
  });
  return {
    total: 28.40,
    byWorkflow: [
      { workflowId: 'wf-cs-bot', workflowName: '智能客服工单分流', cost: 12.80, percentage: 45 },
      { workflowId: 'wf-meeting-summary', workflowName: '会议纪要生成', cost: 8.40, percentage: 30 },
      { workflowId: 'wf-product-copy', workflowName: '电商商品文案', cost: 4.20, percentage: 15 },
      { workflowId: 'wf-hr-resume', workflowName: 'HR 简历自动筛选', cost: 3.00, percentage: 10 },
    ],
    byModel: [
      { model: 'gpt-4o-mini', calls: 4128, cost: 14.20 },
      { model: 'gpt-4o', calls: 312, cost: 11.40 },
      { model: 'qwen-turbo', calls: 180, cost: 2.80 },
    ],
    trend,
  };
}
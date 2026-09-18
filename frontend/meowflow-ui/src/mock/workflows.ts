import type { Workflow } from '@/types/workflow';
import { STORAGE_KEYS } from '@/utils/constants';
import { getStoredJson, setStoredJson } from '@/utils/auth';

function uid(prefix: string) {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
}

const SEED: Workflow[] = [
  {
    id: 'wf-cs-bot',
    name: '智能客服工单分流',
    description: '接收客户咨询,自动分类并通知对应客服',
    category: '客服',
    status: 'running',
    version: 3,
    nodes: [
      { id: 'n1', type: 'trigger.webhook', category: 'trigger', name: 'Webhook 触发器', x: 80, y: 80, config: { method: 'POST', path: '/incoming/message' } },
      { id: 'n2', type: 'ai.classify', category: 'ai', name: 'AI 分类', x: 320, y: 80, config: { categories: '投诉,建议,咨询', model: 'gpt-4o-mini' } },
      { id: 'n3', type: 'flow.condition', category: 'flow', name: '条件分支', x: 560, y: 80, config: { expression: '{{classify.label}} == "投诉"' } },
      { id: 'n4', type: 'notify.dingtalk', category: 'notify', name: '钉钉通知 - 紧急', x: 780, y: 20, config: { webhook: 'https://oapi.dingtalk.com/robot/send?access_token=***' } },
      { id: 'n5', type: 'tool.http', category: 'tool', name: '提交工单', x: 780, y: 160, config: { method: 'POST', url: 'https://api.example.com/tickets' } },
    ],
    edges: [
      { id: 'e1', source: 'n1', target: 'n2' },
      { id: 'e2', source: 'n2', target: 'n3' },
      { id: 'e3', source: 'n3', target: 'n4', label: '是' },
      { id: 'e4', source: 'n3', target: 'n5', label: '否' },
    ],
    createdBy: '小猪大白',
    createdAt: '2026-06-01T09:30:00Z',
    updatedAt: '2026-07-10T11:20:00Z',
    publishedAt: '2026-07-09T17:00:00Z',
    stats: {
      executions: 1248,
      successRate: 96.4,
      avgDuration: 1820,
      totalCost: 21.84,
      todayExecutions: 48,
      todayCost: 0.78,
    },
  },
  {
    id: 'wf-hr-resume',
    name: 'HR 简历自动筛选',
    description: '招聘网站投递的简历自动评分与分类',
    category: 'HR',
    status: 'draft',
    version: 1,
    nodes: [
      { id: 'n1', type: 'trigger.cron', category: 'trigger', name: '定时拉取简历', x: 80, y: 80, config: { cron: '0 */2 * * *' } },
      { id: 'n2', type: 'ai.extract', category: 'ai', name: '信息提取', x: 320, y: 80, config: { schema: '{"name":"","phone":"","school":""}', model: 'gpt-4o-mini' } },
      { id: 'n3', type: 'ai.summarize', category: 'ai', name: '简历摘要', x: 560, y: 80, config: { maxWords: 120 } },
    ],
    edges: [
      { id: 'e1', source: 'n1', target: 'n2' },
      { id: 'e2', source: 'n2', target: 'n3' },
    ],
    createdBy: '小猪大白',
    createdAt: '2026-07-02T14:10:00Z',
    updatedAt: '2026-07-10T17:48:00Z',
    stats: {
      executions: 0,
      successRate: 0,
      avgDuration: 0,
      totalCost: 0,
      todayExecutions: 0,
      todayCost: 0,
    },
  },
  {
    id: 'wf-meeting-summary',
    name: '会议纪要生成',
    description: '上传会议录音转写文本,自动生成结构化纪要',
    category: '运营',
    status: 'running',
    version: 5,
    nodes: [
      { id: 'n1', type: 'trigger.form', category: 'trigger', name: '表单触发', x: 80, y: 80, config: { formId: 'meeting-upload' } },
      { id: 'n2', type: 'ai.llm', category: 'ai', name: 'LLM 纪要', x: 320, y: 80, config: { model: 'gpt-4o', prompt: '请根据以下会议内容生成结构化纪要...' } },
      { id: 'n3', type: 'notify.email', category: 'notify', name: '邮件通知参会人', x: 560, y: 80, config: { to: '{{participants}}', subject: '会议纪要' } },
    ],
    edges: [
      { id: 'e1', source: 'n1', target: 'n2' },
      { id: 'e2', source: 'n2', target: 'n3' },
    ],
    createdBy: '小猪大白',
    createdAt: '2026-05-12T10:00:00Z',
    updatedAt: '2026-07-09T09:15:00Z',
    publishedAt: '2026-07-09T10:00:00Z',
    stats: {
      executions: 312,
      successRate: 99.1,
      avgDuration: 4420,
      totalCost: 32.50,
      todayExecutions: 6,
      todayCost: 0.61,
    },
  },
  {
    id: 'wf-product-copy',
    name: '电商商品文案',
    description: '基于商品参数自动生成营销文案',
    category: '运营',
    status: 'stopped',
    version: 2,
    nodes: [
      { id: 'n1', type: 'trigger.webhook', category: 'trigger', name: '商品上架触发', x: 80, y: 80, config: { method: 'POST', path: '/shop/item/listed' } },
      { id: 'n2', type: 'ai.llm', category: 'ai', name: '文案生成', x: 320, y: 80, config: { model: 'gpt-4o-mini' } },
    ],
    edges: [
      { id: 'e1', source: 'n1', target: 'n2' },
    ],
    createdBy: '小猪大白',
    createdAt: '2026-04-22T09:00:00Z',
    updatedAt: '2026-06-15T14:00:00Z',
    stats: {
      executions: 56,
      successRate: 89.3,
      avgDuration: 2100,
      totalCost: 4.20,
      todayExecutions: 0,
      todayCost: 0,
    },
  },
  {
    id: 'wf-data-sync',
    name: '数据同步任务',
    description: '将 MySQL 中的订单增量同步到数仓',
    category: '数据',
    status: 'archived',
    version: 7,
    nodes: [
      { id: 'n1', type: 'trigger.cron', category: 'trigger', name: '凌晨执行', x: 80, y: 80, config: { cron: '0 2 * * *' } },
      { id: 'n2', type: 'tool.db', category: 'tool', name: '查询订单', x: 320, y: 80, config: { dsn: 'mysql-orders', sql: 'SELECT * FROM orders WHERE updated_at > ?' } },
      { id: 'n3', type: 'tool.http', category: 'tool', name: '推送到数仓', x: 560, y: 80, config: { method: 'POST', url: 'https://dwh.example.com/import' } },
    ],
    edges: [
      { id: 'e1', source: 'n1', target: 'n2' },
      { id: 'e2', source: 'n2', target: 'n3' },
    ],
    createdBy: '小猪大白',
    createdAt: '2026-02-10T08:00:00Z',
    updatedAt: '2026-05-30T18:00:00Z',
    stats: {
      executions: 180,
      successRate: 100,
      avgDuration: 12400,
      totalCost: 0,
      todayExecutions: 0,
      todayCost: 0,
    },
  },
];

export function loadWorkflows(): Workflow[] {
  const stored = getStoredJson<Workflow[] | null>(STORAGE_KEYS.WORKFLOWS, null);
  if (stored && Array.isArray(stored) && stored.length) return stored;
  setStoredJson(STORAGE_KEYS.WORKFLOWS, SEED);
  return SEED;
}

export function saveWorkflows(list: Workflow[]) {
  setStoredJson(STORAGE_KEYS.WORKFLOWS, list);
}

export function createWorkflowId() {
  return uid('wf');
}

export function createNodeId() {
  return uid('n');
}

export function createEdgeId() {
  return uid('e');
}
import type { Execution } from '@/types/workflow';
import { dayjs } from '@/utils/format';

const NAMES = [
  '智能客服工单分流',
  'HR 简历自动筛选',
  '会议纪要生成',
  '电商商品文案',
  '数据同步任务',
];

const TRIGGERS: Execution['trigger'][] = ['manual', 'webhook', 'schedule', 'form'];
const STATUSES: Execution['status'][] = ['success', 'success', 'success', 'success', 'failed', 'running'];

function buildOne(i: number): Execution {
  const start = dayjs().subtract(Math.floor(Math.random() * 7 * 24 * 60), 'minute');
  const dur = Math.floor(800 + Math.random() * 6000);
  const status = STATUSES[i % STATUSES.length];
  return {
    id: `exec-${start.valueOf().toString(36)}-${i}`,
    workflowId: `wf-${i % 5}`,
    workflowName: NAMES[i % NAMES.length],
    status,
    trigger: TRIGGERS[i % TRIGGERS.length],
    startTime: start.toISOString(),
    endTime: status === 'running' ? undefined : start.add(dur, 'millisecond').toISOString(),
    duration: status === 'running' ? undefined : dur,
    cost: +(Math.random() * 2).toFixed(3),
    nodes: [],
    error: status === 'failed' ? '节点 "AI 分类" 调用超时 (mock)' : undefined,
  };
}

export function buildLogs(count = 30): Execution[] {
  return Array.from({ length: count }, (_, i) => buildOne(i));
}

export function buildExecutionDetail(id: string): Execution | null {
  // re-use one of the seeded
  const seed = buildOne(0);
  return { ...seed, id };
}
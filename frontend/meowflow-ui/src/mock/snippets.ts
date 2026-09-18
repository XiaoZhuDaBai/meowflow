import type { WorkflowNode, WorkflowEdge } from '@/types/workflow';

export interface Snippet {
  id: string;
  name: string;
  description: string;
  tags: string[];
  icon: string;
  nodes: Array<Omit<WorkflowNode, 'id'> & { tempId: string }>;
  edges: Array<{ sourceTempId: string; targetTempId: string }>;
  author?: string;
  isOfficial?: boolean;
}

/**
 * 内置常用节点片段 - 可直接拖拽到画布使用
 */
export const BUILTIN_SNIPPETS: Snippet[] = [
  {
    id: 'snippet.feishu-dingtalk-notify',
    name: '飞书消息推送',
    description: '接收 webhook 消息，通过 AI 总结后发送到飞书',
    tags: ['通知', 'AI'],
    icon: 'fa-paper-plane',
    isOfficial: true,
    nodes: [
      {
        tempId: 't1',
        type: 'trigger.webhook',
        category: 'trigger',
        name: 'Webhook 触发器',
        x: 0,
        y: 0,
        config: { method: 'POST', path: '/hooks/notify' },
      },
      {
        tempId: 't2',
        type: 'ai.summarize',
        category: 'ai',
        name: '文本总结',
        x: 280,
        y: 0,
        config: { model: 'gpt-4o-mini', maxWords: 100 },
      },
      {
        tempId: 't3',
        type: 'notify.feishu',
        category: 'notify',
        name: '飞书通知',
        x: 560,
        y: 0,
        config: { webhook: '' },
      },
    ],
    edges: [
      { sourceTempId: 't1', targetTempId: 't2' },
      { sourceTempId: 't2', targetTempId: 't3' },
    ],
  },
  {
    id: 'snippet.ai-classify',
    name: 'AI 智能分类',
    description: '基于 AI 对输入内容进行分类,并根据类别执行不同分支',
    tags: ['AI', '分支'],
    icon: 'fa-tags',
    isOfficial: true,
    nodes: [
      {
        tempId: 't1',
        type: 'trigger.form',
        category: 'trigger',
        name: '表单触发器',
        x: 0,
        y: 0,
        config: { formId: '' },
      },
      {
        tempId: 't2',
        type: 'ai.classify',
        category: 'ai',
        name: 'AI 分类',
        x: 280,
        y: 0,
        config: {
          model: 'gpt-4o-mini',
          categories: '紧急,普通,建议',
          input: '{{input.text}}',
        },
      },
      {
        tempId: 't3',
        type: 'notify.dingtalk',
        category: 'notify',
        name: '钉钉通知',
        x: 560,
        y: -120,
        config: { webhook: '', atMobiles: '' },
      },
      {
        tempId: 't4',
        type: 'notify.email',
        category: 'notify',
        name: '邮件发送',
        x: 560,
        y: 120,
        config: { to: '', subject: '', body: '' },
      },
    ],
    edges: [
      { sourceTempId: 't1', targetTempId: 't2' },
      { sourceTempId: 't2', targetTempId: 't3' },
      { sourceTempId: 't2', targetTempId: 't4' },
    ],
  },
  {
    id: 'snippet.rag-qa',
    name: 'RAG 知识库问答',
    description: '从知识库检索相关内容，再由 LLM 生成回答',
    tags: ['RAG', 'AI'],
    icon: 'fa-book',
    isOfficial: true,
    nodes: [
      {
        tempId: 't1',
        type: 'trigger.imessage',
        category: 'trigger',
        name: '消息触发器',
        x: 0,
        y: 0,
        config: { platform: 'feishu' },
      },
      {
        tempId: 't2',
        type: 'ai.rag',
        category: 'ai',
        name: '知识库检索',
        x: 280,
        y: 0,
        config: { knowledgeBaseId: '', topK: 5 },
      },
      {
        tempId: 't3',
        type: 'ai.llm',
        category: 'ai',
        name: 'LLM 对话',
        x: 560,
        y: 0,
        config: {
          model: 'gpt-4o-mini',
          prompt: '请根据检索到的内容回答用户问题',
          temperature: 0.3,
        },
      },
      {
        tempId: 't4',
        type: 'notify.feishu',
        category: 'notify',
        name: '飞书通知',
        x: 840,
        y: 0,
        config: { webhook: '' },
      },
    ],
    edges: [
      { sourceTempId: 't1', targetTempId: 't2' },
      { sourceTempId: 't2', targetTempId: 't3' },
      { sourceTempId: 't3', targetTempId: 't4' },
    ],
  },
  {
    id: 'snippet.cron-report',
    name: '定时数据报表',
    description: '定时从数据库拉取数据，生成报表后通过邮件发送',
    tags: ['定时', '数据'],
    icon: 'fa-clock',
    isOfficial: true,
    nodes: [
      {
        tempId: 't1',
        type: 'trigger.cron',
        category: 'trigger',
        name: '定时触发器',
        x: 0,
        y: 0,
        config: { cron: '0 9 * * *', timezone: 'Asia/Shanghai' },
      },
      {
        tempId: 't2',
        type: 'tool.db',
        category: 'tool',
        name: '数据库查询',
        x: 280,
        y: 0,
        config: { dsn: '', sql: 'SELECT * FROM metrics WHERE date = CURRENT_DATE' },
      },
      {
        tempId: 't3',
        type: 'tool.code',
        category: 'tool',
        name: '生成报表',
        x: 560,
        y: 0,
        config: { language: 'javascript', source: 'return { rows: input.rows };' },
      },
      {
        tempId: 't4',
        type: 'notify.email',
        category: 'notify',
        name: '邮件发送',
        x: 840,
        y: 0,
        config: { to: 'team@example.com', subject: '日报', body: '' },
      },
    ],
    edges: [
      { sourceTempId: 't1', targetTempId: 't2' },
      { sourceTempId: 't2', targetTempId: 't3' },
      { sourceTempId: 't3', targetTempId: 't4' },
    ],
  },
  {
    id: 'snippet.http-retry',
    name: 'HTTP 请求重试',
    description: '调用外部 API，失败时重试，最后成功则通知',
    tags: ['HTTP'],
    icon: 'fa-globe',
    isOfficial: true,
    nodes: [
      {
        tempId: 't1',
        type: 'trigger.cron',
        category: 'trigger',
        name: '定时触发器',
        x: 0,
        y: 0,
        config: { cron: '*/5 * * * *' },
      },
      {
        tempId: 't2',
        type: 'tool.http',
        category: 'tool',
        name: 'HTTP 请求',
        x: 280,
        y: 0,
        config: { method: 'GET', url: 'https://api.example.com/health' },
      },
      {
        tempId: 't3',
        type: 'flow.condition',
        category: 'flow',
        name: '条件分支',
        x: 560,
        y: 0,
        config: { expression: '{{input.status}} === 200' },
      },
      {
        tempId: 't4',
        type: 'notify.email',
        category: 'notify',
        name: '邮件发送',
        x: 840,
        y: -100,
        config: { to: 'ops@example.com', subject: '服务正常' },
      },
      {
        tempId: 't5',
        type: 'notify.dingtalk',
        category: 'notify',
        name: '钉钉告警',
        x: 840,
        y: 100,
        config: { webhook: '' },
      },
    ],
    edges: [
      { sourceTempId: 't1', targetTempId: 't2' },
      { sourceTempId: 't2', targetTempId: 't3' },
      { sourceTempId: 't3', targetTempId: 't4' },
      { sourceTempId: 't3', targetTempId: 't5' },
    ],
  },
];

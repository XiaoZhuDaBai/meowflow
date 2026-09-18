/**
 * 内置官方模板 helper（前端 mock 与后端 builtin 同步）
 *
 * 约定：
 *   - id 以 "builtin:" 开头，与后端 BuiltinTemplateCatalog 完全一致
 *   - workflowJson 结构包含 nodes/edges/version，节点带 x/y 坐标
 *   - 同时输出 workflowGraph（轻量归一化结构，详情页可直接渲染）
 *   - 默认节点 240×60，水平间距 80，分支上下偏移 140（与后端 BuiltinNode 同步）
 */

import { applyDefaultConfig } from './builtinTemplateDefaults';
import type { NodeConfig } from './builtinTemplateDefaults';

export interface BuiltinNode {
  id: string;
  type: string;
  name: string;
  x: number;
  y: number;
  category?: string;
  data?: NodeConfig;
  description?: string;
}

export interface BuiltinEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  type?: string;
}

export interface BuiltinGraphNode {
  id: string;
  name: string;
  type: string;
  cx: number;
  cy: number;
  w: number;
  h: number;
}

export interface BuiltinGraphEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
}

export interface BuiltinGraph {
  nodes: BuiltinGraphNode[];
  edges: BuiltinGraphEdge[];
}

export interface BuiltinWorkflowJson {
  version: string;
  nodes: BuiltinNode[];
  edges: BuiltinEdge[];
}

export interface BuiltinTemplate {
  id: string;
  name: string;
  description: string;
  category: string;
  categoryName: string;
  tags: string[];
  coverEmoji: string;
  author: string;
  usageCount: number;
  rating: number;
  isOfficial: boolean;
  workflowId?: string;
  createdAt: string;
  workflowJson?: string;
  workflowGraph?: string;
}

const NODE_W = 240;
const NODE_H = 60;
const DX = 80;
const DY = 140;

function n(
  id: string,
  type: string,
  name: string,
  x: number,
  y: number,
  category?: string,
  descriptionOrConfig?: string | NodeConfig,
  customConfig?: NodeConfig,
): BuiltinNode {
  // 形参兼容：
  //   n('id', 'type', 'name', x, y, 'cat', 'description')
  //   n('id', 'type', 'name', x, y, 'cat', { ...config })
  //   n('id', 'type', 'name', x, y, 'cat', 'description', { ...config })
  let description: string | undefined;
  let overrides: NodeConfig | undefined;
  if (typeof descriptionOrConfig === 'string') {
    description = descriptionOrConfig;
    overrides = customConfig;
  } else {
    overrides = descriptionOrConfig;
  }

  const base: NodeConfig = overrides ? { ...overrides } : {};
  const config = applyDefaultConfig(type, base);
  return { id, type, name, x, y, category, data: config, description };
}

function e(id: string, source: string, target: string, label?: string): BuiltinEdge {
  return { id, source, target, label, type: 'default' };
}

/** 条件分支边（编辑器渲染为橙色虚线 + label badge） */
function eCondition(id: string, source: string, target: string, label?: string): BuiltinEdge {
  return { id, source, target, label, type: 'condition' };
}

/** 循环边（紫色实线） */
function eLoop(id: string, source: string, target: string, label?: string): BuiltinEdge {
  return { id, source, target, label, type: 'loop' };
}

/** 异常边（红色实线） */
function eError(id: string, source: string, target: string, label?: string): BuiltinEdge {
  return { id, source, target, label, type: 'error' };
}

function buildJson(nodes: BuiltinNode[], edges: BuiltinEdge[]): string {
  const data: BuiltinWorkflowJson = { version: 'v1', nodes, edges };
  return JSON.stringify(data);
}

function buildGraph(nodes: BuiltinNode[], edges: BuiltinEdge[]): string {
  const gNodes: BuiltinGraphNode[] = nodes.map(n => ({
    id: n.id,
    name: n.name,
    type: n.type,
    cx: n.x + NODE_W / 2,
    cy: n.y + NODE_H / 2,
    w: NODE_W,
    h: NODE_H,
  }));
  const gEdges: BuiltinGraphEdge[] = edges.map(e => ({
    id: e.id,
    source: e.source,
    target: e.target,
    label: e.label,
  }));
  return JSON.stringify({ nodes: gNodes, edges: gEdges });
}

function entry(
  id: string,
  name: string,
  description: string,
  category: string,
  categoryName: string,
  coverEmoji: string,
  usageCount: number,
  rating: number,
  tags: string[],
  nodes: BuiltinNode[],
  edges: BuiltinEdge[],
): BuiltinTemplate {
  return {
    id,
    name,
    description,
    category,
    categoryName,
    tags,
    coverEmoji,
    author: '喵流官方',
    usageCount,
    rating,
    isOfficial: true,
    createdAt: '2026-06-01T00:00:00Z',
    workflowJson: buildJson(nodes, edges),
    workflowGraph: buildGraph(nodes, edges),
  };
}

// ---------------------------------------------------------------------------
// 客服场景
// ---------------------------------------------------------------------------

function csAutoReply(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.webhook', 'IM 入口', 0, 0, 'trigger', '接收 IM 平台 webhook 消息，作为流程入口', {
      method: 'POST',
      path: '/hooks/cs/inbound',
      authToken: 'Bearer ****',
      timeout: 8000,
    }),
    n('auth', 'code.transform', '签名校验', DX + NODE_W, 0, 'transform', '使用 IM 平台签名 + timestamp 防重放校验', {
      language: 'javascript',
      source: 'const ts = input.headers["x-timestamp"]; const sig = input.headers["x-signature"]; const valid = Math.abs(Date.now() - +ts) < 60_000 && sig; return { valid, body: input.body };',
    }),
    n('extract', 'code.transform', '抽取消息字段', DX + NODE_W, -DY, 'transform', '从 webhook payload 中抽取 text / userId / channel', {
      language: 'javascript',
      source: 'const b = input.body || {}; return { text: b.text || "", userId: b.userId, channel: b.channel || "im" };',
    }),
    n('blacklist', 'code.transform', '黑名单校验', DX + NODE_W, DY, 'transform', '检查用户是否在黑名单中', {
      language: 'javascript',
      source: 'return { isBlacklisted: (input.blacklist || []).includes(input.userId) };',
    }),
    n('classify', 'ai.llm', '意图 & 情绪分类', 2 * (DX + NODE_W), 0, 'ai', '区分 FAQ / 投诉 / 业务办理 / 表扬 / 闲聊，并给出情绪标签', {
      model: 'gpt-4o-mini',
      temperature: 0.2,
      categories: 'FAQ,投诉,业务办理,表扬,闲聊',
      input: '{{extract.text}}',
      outputKey: 'category,sentiment',
    }),
    n('sensitive', 'ai.llm', '敏感词过滤', 2 * (DX + NODE_W), DY, 'ai', '识别 PII / 违禁内容（手机号、身份证、政治敏感）', {
      model: 'gpt-4o-mini',
      temperature: 0.1,
      prompt: '识别文本中的 PII / 敏感词，给出掩码版本。',
      outputKey: 'maskedText',
    }),
    n('branch', 'condition.switch', '分支路由', 3 * (DX + NODE_W), 0, 'control', undefined, {
      field: '{{classify.category}}',
      cases: [
        { value: 'FAQ', next: 'faq' },
        { value: '业务办理', next: 'llmReply' },
        { value: '投诉', next: 'human' },
        { value: '表扬', next: 'llmReply' },
        { value: '闲聊', next: 'llmReply' },
      ],
      defaultNext: 'llmReply',
    }),
    n('faq', 'knowledge.search', 'FAQ 知识库', 4 * (DX + NODE_W), -DY, 'ai', '从企业私有 FAQ 库检索 Top-3', {
      knowledgeBaseId: 'kb_cs_faq',
      topK: 3,
      scoreThreshold: 0.65,
      outputKey: 'faqContext',
    }),
    n('llmReply', 'ai.llm', 'LLM 草拟回复', 4 * (DX + NODE_W), 0, 'ai', '基于上下文草拟礼貌准确的回复', {
      model: 'gpt-4o-mini',
      temperature: 0.4,
      maxTokens: 512,
      systemPrompt: '你是专业客服，用语礼貌简洁，分点回答，不要泄露客户隐私。',
      prompt: '客户问题：{{sensitive.maskedText}}\n参考知识：{{faq.faqContext}}',
    }),
    n('human', 'condition.if', '是否转人工', 4 * (DX + NODE_W), DY, 'control', undefined, {
      expression: "{{classify.sentiment}} === 'negative' || {{classify.category}} === '投诉'",
      trueNext: 'assign',
      falseNext: 'send',
    }),
    n('assign', 'http.request', '分配坐席', 5 * (DX + NODE_W), DY, 'action', '调用工单系统分配客服坐席', {
      method: 'POST',
      url: 'https://ticket.internal/api/ticket/assign',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json', 'X-Service': 'meowflow' },
      bodyType: 'json',
      body: '{ "userId":"{{extract.userId}}", "channel":"{{extract.channel}}", "priority":"high" }',
      timeoutMs: 5000,
      retries: 3,
      retryOnFail: true,
    }),
    n('retry', 'code.transform', '重试退避', 5 * (DX + NODE_W), 0, 'transform', '失败时指数退避后重试上游调用', {
      language: 'javascript',
      source: 'const backoff = (input.attempt || 1) * 1000; return { sleepMs: backoff, nextAttempt: (input.attempt || 1) + 1 };',
    }),
    n('notify', 'notify.feishu', '通知坐席', 6 * (DX + NODE_W), DY, 'action', '飞书通知值班坐席，含客户消息和情绪', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
      atAll: false,
    }),
    n('feedback', 'http.request', '满意度回执', 6 * (DX + NODE_W), 0, 'action', '会话结束后向客户发送满意度调研', {
      method: 'POST',
      url: 'https://cs.internal/api/feedback/dispatch',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "userId":"{{extract.userId}}", "ticketId":"{{assign.ticketId}}" }',
    }),
    n('send', 'notify.feishu', '发送回复', 6 * (DX + NODE_W), -DY, 'action', '通过 IM 机器人将回复发送给客户', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
    }),
    n('rejectAuth', 'notify.feishu', '签名/黑名单拒绝', 3 * (DX + NODE_W), DY * 1.6, 'action', '签名失败或黑名单用户：告警安全团队并拒收', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: false,
    }),
    n('aggregate', 'transform.aggregator', '合并结果', 7 * (DX + NODE_W), 0, 'transform', '合并自动回复 / 人工流转 / 满意度回执', {
      language: 'javascript',
      source: 'return { autoReply: input.send?.text, faqHits: input.faq?.length || 0, humanHandoff: !!input.assign, feedbackUrl: input.feedback?.url };',
    }),
    n('end', 'end.aggregator', '结束', 8 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'auth'),
    eCondition('e2', 'auth', 'extract', '通过'),
    eError('e3', 'auth', 'rejectAuth', '失败'),
    e('e4', 'extract', 'classify'),
    e('e5', 'extract', 'sensitive'),
    e('e6', 'extract', 'blacklist'),
    eCondition('e7', 'blacklist', 'classify', '放行'),
    eError('e8', 'blacklist', 'rejectAuth', '黑名单'),
    e('e9', 'sensitive', 'classify'),
    e('e10', 'classify', 'branch'),
    eCondition('e11', 'branch', 'faq', 'FAQ'),
    eCondition('e12', 'branch', 'llmReply', '通用'),
    eCondition('e13', 'branch', 'human', '投诉'),
    e('e14', 'faq', 'llmReply'),
    e('e15', 'llmReply', 'send'),
    eCondition('e16', 'human', 'send', '自动'),
    eCondition('e17', 'human', 'assign', '转人工'),
    eLoop('e18', 'assign', 'retry', '失败重试'),
    e('e19', 'retry', 'assign'),
    e('e20', 'assign', 'notify'),
    e('e21', 'llmReply', 'feedback'),
    e('e22', 'send', 'aggregate'),
    e('e23', 'feedback', 'aggregate'),
    e('e24', 'notify', 'aggregate'),
    e('e25', 'aggregate', 'end'),
    eError('e26', 'rejectAuth', 'end', '签名失败'),
  ];
  return entry(
    'builtin:cs-auto-reply',
    '智能客服自动回复',
    '接入 IM / 工单系统，多层校验（签名 / 黑名单 / 敏感词）后 AI 识别意图并回复，必要时升级人工',
    'cs',
    '客服场景',
    '💬',
    1284,
    4.8,
    ['客服', 'AI', '自动回复', 'RAG'],
    nodes,
    edges,
  );
}

function csTicketRoute(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.webhook', 'Webhook 入口', 0, 0, 'trigger', '工单系统推送工单到 webhook', {
      method: 'POST',
      path: '/hooks/cs/ticket',
      authToken: 'Bearer ****',
      timeout: 8000,
    }),
    n('validate', 'code.transform', '字段校验', DX + NODE_W, 0, 'transform', '校验必填字段：title / content / userId', {
      language: 'javascript',
      source: 'const b = input.body || {}; const ok = !!(b.title && b.content && b.userId); return { ok, payload: b };',
    }),
    n('extract', 'code.transform', '抽取关键字段', DX + NODE_W, -DY, 'transform', '从工单 payload 中抽取标题/描述/客户ID', {
      language: 'javascript',
      source: 'const b = input.payload || {}; return { title: b.title, content: b.content, userId: b.userId, ticketId: b.id, priority: b.priority || "normal" };',
    }),
    n('classify', 'ai.llm', '工单分类', 2 * (DX + NODE_W), 0, 'ai', '判断工单应归属哪个子组，并标注紧急度', {
      model: 'gpt-4o-mini',
      temperature: 0.2,
      categories: '账单,技术支持,投诉,其他',
      input: '{{extract.title}} {{extract.content}}',
      outputKey: 'category,priority,confidence',
    }),
    n('confidence', 'condition.if', '置信度足够?', 2 * (DX + NODE_W), DY, 'control', undefined, {
      expression: '{{classify.confidence}} >= 0.7',
      trueNext: 'route',
      falseNext: 'manualReview',
    }),
    n('route', 'condition.switch', '按分类路由', 3 * (DX + NODE_W), 0, 'control', undefined, {
      field: '{{classify.category}}',
      cases: [
        { value: '账单', next: 'createA' },
        { value: '技术支持', next: 'createB' },
        { value: '投诉', next: 'createC' },
      ],
      defaultNext: 'createB',
    }),
    n('manualReview', 'notify.email', '转人工分诊', 3 * (DX + NODE_W), DY, 'action', '置信度低时交给分诊团队', {
      host: '',
      port: 465,
      ssl: true,
      to: 'cs-triage@meowflow.com',
      subject: '【待分诊】{{extract.title}}',
      body: '<pre>{{extract.content}}</pre>',
    }),
    n('createA', 'http.request', '分配：账单组', 4 * (DX + NODE_W), -DY, 'action', '把工单分配给账单组', {
      method: 'POST',
      url: 'https://ticket.internal/api/ticket/forward',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "ticketId":"{{extract.ticketId}}", "group":"billing", "priority":"{{extract.priority}}" }',
      timeoutMs: 5000,
      retries: 3,
    }),
    n('createB', 'http.request', '分配：技术支持', 4 * (DX + NODE_W), 0, 'action', '把工单分配给技术支持', {
      method: 'POST',
      url: 'https://ticket.internal/api/ticket/forward',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "ticketId":"{{extract.ticketId}}", "group":"tech-support", "priority":"{{extract.priority}}" }',
      timeoutMs: 5000,
      retries: 3,
    }),
    n('createC', 'http.request', '分配：投诉组', 4 * (DX + NODE_W), DY, 'action', '把工单分配给投诉组', {
      method: 'POST',
      url: 'https://ticket.internal/api/ticket/forward',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "ticketId":"{{extract.ticketId}}", "group":"complaint", "priority":"high" }',
      timeoutMs: 5000,
      retries: 3,
    }),
    n('retry', 'code.transform', '退避重试', 5 * (DX + NODE_W), 0, 'transform', '失败时指数退避（最多 3 次）', {
      language: 'javascript',
      source: 'return { sleepMs: 500 * Math.pow(2, input.attempt || 1), attempt: (input.attempt || 1) + 1 };',
    }),
    n('notify', 'notify.feishu', '发送飞书通知', 5 * (DX + NODE_W), -DY, 'action', '通过飞书群机器人通知对应处理组', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
    }),
    n('deadLetter', 'http.request', '失败死信', 6 * (DX + NODE_W), DY, 'action', '多次失败后写入死信队列，待人工介入', {
      method: 'POST',
      url: 'https://queue.internal/api/dead-letter',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "ticketId":"{{extract.ticketId}}", "error":"{{retry.lastError}}" }',
    }),
    n('end', 'end.aggregator', '结束', 7 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'validate'),
    eCondition('e2', 'validate', 'extract', '字段通过'),
    eError('e3', 'validate', 'deadLetter', '字段缺失'),
    e('e4', 'extract', 'classify'),
    e('e5', 'classify', 'confidence'),
    eCondition('e6', 'confidence', 'route', '高'),
    eCondition('e7', 'confidence', 'manualReview', '低'),
    eCondition('e8', 'route', 'createA', '账单'),
    eCondition('e9', 'route', 'createB', '技术'),
    eCondition('e10', 'route', 'createC', '投诉'),
    eLoop('e11', 'createA', 'retry', '失败'),
    eLoop('e12', 'createB', 'retry', '失败'),
    eLoop('e13', 'createC', 'retry', '失败'),
    e('e14', 'retry', 'notify'),
    eError('e15', 'retry', 'deadLetter', '重试耗尽'),
    e('e16', 'createA', 'notify'),
    e('e17', 'createB', 'notify'),
    e('e18', 'createC', 'notify'),
    e('e19', 'manualReview', 'end'),
    e('e20', 'notify', 'end'),
    e('e21', 'deadLetter', 'end'),
  ];
  return entry(
    'builtin:cs-ticket-route',
    '工单分类与路由',
    '接收工单后字段校验、AI 分类（带置信度）、按组分发；失败重试 + 死信队列兜底',
    'cs',
    '客服场景',
    '🎫',
    612,
    4.6,
    ['客服', '工单', 'AI', '重试'],
    nodes,
    edges,
  );
}

function csFeedbackAnalysis(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.cron', '每日 09:00', 0, 0, 'trigger', '每日早 9 点聚合分析昨日用户反馈', {
      cron: '0 9 * * *',
      timezone: 'Asia/Shanghai',
      enabled: true,
    }),
    n('fetch', 'http.request', '拉取反馈', DX + NODE_W, 0, 'action', '从工单系统拉取近 24 小时的用户反馈', {
      method: 'GET',
      url: 'https://ticket.internal/api/feedbacks/recent?hours=24',
      auth: { type: 'none' },
      headers: { 'X-Service': 'meowflow' },
      bodyType: 'json',
      timeoutMs: 10000,
      retries: 3,
    }),
    n('parse', 'code.transform', '解析与去重', 2 * (DX + NODE_W), 0, 'transform', '解析反馈为统一结构并按 feedbackId 去重', {
      language: 'javascript',
      source: 'const seen = new Set(); return (input.items || []).filter(f => !seen.has(f.id) && seen.add(f.id)).map(f => ({ id: f.id, text: f.text, channel: f.channel, user: f.userId }));',
    }),
    n('sentiment', 'ai.llm', '情感 & 主题分析', 3 * (DX + NODE_W), 0, 'ai', '对反馈进行情感打标 (positive/neutral/negative) 与主题归类', {
      model: 'gpt-4o-mini',
      temperature: 0.1,
      maxTokens: 2048,
      prompt: '逐条分析反馈，给出 sentiment 与 theme 与 urgency，输出 JSON 数组。',
    }),
    n('branch', 'condition.if', '负面?', 4 * (DX + NODE_W), 0, 'control', undefined, {
      expression: "input.sentiment.filter(s => s.sentiment === 'negative').length > 0",
      trueNext: 'alert',
      falseNext: 'summary',
    }),
    n('alert', 'notify.feishu', '推送告警', 5 * (DX + NODE_W), -DY, 'action', '负面反馈及时推送经理群处理', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
      atAll: false,
    }),
    n('summary', 'ai.llm', '生成日报摘要', 5 * (DX + NODE_W), DY, 'ai', '汇总当日反馈核心结论', {
      model: 'gpt-4o-mini',
      temperature: 0.3,
      maxWords: 300,
      language: 'zh-CN',
      prompt: '基于反馈数据输出段落式日报摘要，包含亮点、风险、建议。',
    }),
    n('render', 'code.transform', '渲染 HTML 报表', 6 * (DX + NODE_W), DY, 'transform', '把摘要渲染成邮件友好的 HTML', {
      language: 'javascript',
      source: 'return { html: `<h3>今日反馈日报</h3><pre>${input.summary.text}</pre>`, date: new Date().toISOString().slice(0, 10) };',
    }),
    n('sendEmail', 'notify.email', '发送日报邮件', 6 * (DX + NODE_W), 0, 'action', '邮件发送日报摘要', {
      host: '',
      port: 465,
      ssl: true,
      username: '',
      password: '',
      from: '',
      to: 'feedback@meowflow.com',
      subject: '【每日反馈日报】{{render.date}}',
      body: '<h3>今日反馈概要</h3><pre>{{render.html}}</pre>',
    }),
    n('archive', 'http.request', '归档知识库', 7 * (DX + NODE_W), 0, 'action', '把日报归档到知识库供后续查询', {
      method: 'POST',
      url: 'https://kb.internal/api/docs',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "title":"反馈日报 {{render.date}}", "content":"{{render.html}}" }',
    }),
    n('deadLetter', 'notify.feishu', '数据缺失告警', 4 * (DX + NODE_W), DY, 'action', '反馈源数据为空时通知运营', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
    }),
    n('end', 'end.aggregator', '结束', 8 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'fetch'),
    e('e2', 'fetch', 'parse'),
    eCondition('e3', 'parse', 'sentiment', '有数据'),
    eError('e4', 'parse', 'deadLetter', '空数据'),
    e('e5', 'sentiment', 'branch'),
    eCondition('e6', 'branch', 'alert', '是'),
    eCondition('e7', 'branch', 'summary', '否'),
    e('e8', 'summary', 'render'),
    e('e9', 'render', 'sendEmail'),
    e('e10', 'sendEmail', 'archive'),
    e('e11', 'archive', 'end'),
    e('e12', 'alert', 'end'),
    e('e13', 'deadLetter', 'end'),
  ];
  return entry(
    'builtin:cs-feedback-analysis',
    '用户反馈分析',
    '每日聚合多渠道反馈，AI 情感 / 主题分析，负面即时告警，正面归档',
    'cs',
    '客服场景',
    '💡',
    287,
    4.5,
    ['客服', '反馈', 'AI', '日报'],
    nodes,
    edges,
  );
}

// ---------------------------------------------------------------------------
// HR 场景
// ---------------------------------------------------------------------------

function hrResumeScreen(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.webhook', '投递接收', 0, 0, 'trigger', '招聘网站投递触发', {
      method: 'POST',
      path: '/hooks/hr/resume',
      authToken: 'Bearer ****',
      timeout: 10000,
    }),
    n('parse', 'code.transform', '解析简历文本', DX + NODE_W, 0, 'transform', '提取简历核心字段（姓名/学历/经验/技能）', {
      language: 'javascript',
      source: 'const r = input.body || {}; return { name: r.name, education: r.education, experience: r.experience, skills: r.skills || [], years: r.years || 0, raw: r.raw };',
    }),
    n('jd', 'knowledge.search', '匹配岗位 JD', 2 * (DX + NODE_W), -DY, 'ai', '从招聘 JD 知识库中检索 Top-3 匹配 JD', {
      knowledgeBaseId: 'kb_hr_jd',
      topK: 3,
      scoreThreshold: 0.7,
      outputKey: 'topJD',
    }),
    n('score', 'ai.llm', '匹配评分', 2 * (DX + NODE_W), 0, 'ai', '基于 Top JD + 简历打分（0-100）并解释', {
      model: 'gpt-4o-mini',
      temperature: 0.15,
      maxTokens: 512,
      prompt: '请根据 JD 与简历给出 0-100 评分，并解释原因，按维度（技能/经验/学历）拆解。',
    }),
    n('threshold', 'condition.if', '≥ 80 分?', 3 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{score.total}} >= 80',
      trueNext: 'toHr',
      falseNext: 'checkMid',
    }),
    n('checkMid', 'condition.if', '≥ 60 分?', 3 * (DX + NODE_W), DY, 'control', undefined, {
      expression: '{{score.total}} >= 60',
      trueNext: 'talentPool',
      falseNext: 'reject',
    }),
    n('toHr', 'notify.email', '推送给 HR', 4 * (DX + NODE_W), -DY, 'action', '把高分简历推送给 HR（含评分明细）', {
      host: '',
      port: 465,
      ssl: true,
      to: 'hr@meowflow.com',
      subject: '【高分简历】{{parse.name}} - {{score.total}} 分',
      body: '<h3>候选人：{{parse.name}}</h3><pre>评分明细：{{score.breakdown}}</pre>',
    }),
    n('talentPool', 'http.request', '入人才库', 4 * (DX + NODE_W), 0, 'action', '中等分简历入人才库，未来岗位自动推荐', {
      method: 'POST',
      url: 'https://ats.internal/api/talent-pool',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "name":"{{parse.name}}", "score":{{score.total}}, "skills":{{parse.skills}} }',
    }),
    n('reject', 'notify.email', '礼貌婉拒邮件', 4 * (DX + NODE_W), DY, 'action', '婉拒邮件（个性化签名）', {
      host: '',
      port: 465,
      ssl: true,
      to: '{{parse.email}}',
      subject: '感谢您的投递',
      body: '感谢您投递我们公司，未来有合适岗位会再联系您。',
    }),
    n('log', 'http.request', '写入 ATS 系统', 5 * (DX + NODE_W), -DY, 'action', '把通过者记录写入 ATS 候选人库', {
      method: 'POST',
      url: 'https://ats.internal/api/candidate',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "name": "{{parse.name}}", "score": {{score.total}}, "stage":"hr-review" }',
    }),
    n('interview', 'http.request', '预约初试', 5 * (DX + NODE_W), 0, 'action', '自动发送面试邀请日历', {
      method: 'POST',
      url: 'https://ats.internal/api/interview/schedule',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "candidate":"{{parse.name}}", "round":"phone-screen" }',
    }),
    n('end', 'end.aggregator', '结束', 6 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'parse'),
    e('e2', 'parse', 'score'),
    e('e3', 'parse', 'jd'),
    e('e4', 'jd', 'score'),
    e('e5', 'score', 'threshold'),
    eCondition('e6', 'threshold', 'toHr', '高分'),
    eCondition('e7', 'threshold', 'checkMid', '一般'),
    eCondition('e8', 'checkMid', 'talentPool', '中等'),
    eCondition('e9', 'checkMid', 'reject', '低分'),
    e('e10', 'toHr', 'log'),
    e('e11', 'toHr', 'interview'),
    e('e12', 'talentPool', 'end'),
    e('e13', 'reject', 'end'),
    e('e14', 'log', 'end'),
    e('e15', 'interview', 'end'),
  ];
  return entry(
    'builtin:hr-resume-screen',
    '简历自动筛选',
    '招聘网站投递自动解析、匹配 JD 三档评分（高分/中等/低分），分别推送 HR / 入人才库 / 自动婉拒',
    'hr',
    '人力资源',
    '📄',
    921,
    4.7,
    ['HR', '招聘', 'AI', 'RAG'],
    nodes,
    edges,
  );
}

function hrLeaveApprove(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.form', '员工提交请假', 0, 0, 'trigger', '员工填写请假表单提交', {
      formId: 'form_leave_request',
      fields: [
        { key: 'applicant', label: '申请人', type: 'string', required: true },
        { key: 'days', label: '天数', type: 'number', required: true },
        { key: 'reason', label: '事由', type: 'text', required: true },
        { key: 'leaveType', label: '类型', type: 'select', options: ['年假', '病假', '事假', '调休'], required: true },
      ],
    }),
    n('validate', 'code.transform', '校验时间冲突', DX + NODE_W, 0, 'transform', '检查团队成员同期请假冲突与黑名单时段', {
      language: 'javascript',
      source: 'const overlap = (input.days > 2 && (input.teamOnLeave || 0) >= 3); const inBlackout = (input.blackoutDates || []).some(d => input.dates?.includes(d)); return { overlap, inBlackout, days: input.days };',
    }),
    n('branch', 'condition.if', '时长路由', 2 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{validate.days}} <= 1 && !{{validate.inBlackout}}',
      trueNext: 'approve',
      falseNext: 'mgrApprove',
    }),
    n('approve', 'http.request', '主管一键审批', 3 * (DX + NODE_W), -DY, 'action', '短假主管直接审批', {
      method: 'POST',
      url: 'https://oa.internal/api/leave/quick-approve',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "applicant":"{{trigger.applicant}}", "days":{{validate.days}} }',
      timeoutMs: 5000,
      retries: 2,
    }),
    n('mgrApprove', 'http.request', '多级审批', 3 * (DX + NODE_W), DY, 'action', '长假进入多级审批流（主管→HRBP→总监）', {
      method: 'POST',
      url: 'https://oa.internal/api/leave/multi-approve',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "applicant":"{{trigger.applicant}}", "days":{{validate.days}}, "leaveType":"{{trigger.leaveType}}" }',
      timeoutMs: 8000,
      retries: 2,
    }),
    n('checkLeave', 'condition.if', '余额够?', 4 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{record.leaveBalance}} >= {{validate.days}}',
      trueNext: 'reply',
      falseNext: 'insufficient',
    }),
    n('record', 'http.request', '写入 OA 系统', 4 * (DX + NODE_W), DY, 'action', '记录到 OA 并扣减假期余额', {
      method: 'POST',
      url: 'https://oa.internal/api/leave/record',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "applicant":"{{trigger.applicant}}", "days":{{validate.days}}, "reason":"{{trigger.reason}}", "type":"{{trigger.leaveType}}" }',
    }),
    n('insufficient', 'notify.feishu', '余额不足告警', 5 * (DX + NODE_W), DY, 'action', '假期余额不足通知员工并退回', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
    }),
    n('reply', 'notify.feishu', '回复员工', 5 * (DX + NODE_W), -DY, 'action', '通知员工审批结果（含审批人）', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
    }),
    n('syncCalendar', 'http.request', '同步日历', 5 * (DX + NODE_W), 0, 'action', '同步到企业日历', {
      method: 'POST',
      url: 'https://calendar.internal/api/event',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "title":"{{trigger.applicant}} 请假", "days":{{validate.days}} }',
    }),
    n('end', 'end.aggregator', '结束', 6 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'validate'),
    e('e2', 'validate', 'branch'),
    eCondition('e3', 'branch', 'approve', '短假'),
    eCondition('e4', 'branch', 'mgrApprove', '长假/黑名单'),
    e('e5', 'approve', 'record'),
    e('e6', 'mgrApprove', 'record'),
    e('e7', 'record', 'checkLeave'),
    eCondition('e8', 'checkLeave', 'reply', '够'),
    eCondition('e9', 'checkLeave', 'insufficient', '不足'),
    e('e10', 'reply', 'syncCalendar'),
    e('e11', 'syncCalendar', 'end'),
    e('e12', 'insufficient', 'end'),
  ];
  return entry(
    'builtin:hr-leave-approve',
    '请假审批',
    '员工提交请假 → 校验冲突 / 黑名单 / 余额 → 短假主管审批 / 长假多级审批 → 通知 + 同步日历',
    'hr',
    '人力资源',
    '🌴',
    412,
    4.6,
    ['HR', '审批', '表单'],
    nodes,
    edges,
  );
}

function hrOnboarding(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.form', 'HR 提交入职', 0, 0, 'trigger', 'HR 提交入职信息表单', {
      formId: 'form_onboarding',
      fields: [
        { key: 'name', label: '姓名', type: 'string', required: true },
        { key: 'email', label: '邮箱', type: 'string', required: true },
        { key: 'position', label: '岗位', type: 'string', required: true },
        { key: 'department', label: '部门', type: 'string', required: true },
        { key: 'startDate', label: '入职日期', type: 'date', required: true },
      ],
    }),
    n('validate', 'code.transform', '查重校验', DX + NODE_W, 0, 'transform', '校验邮箱是否已存在，避免重复入职', {
      language: 'javascript',
      source: 'const dup = (input.existingUsers || []).some(u => u.email === input.email); return { dup, name: input.name, email: input.email };',
    }),
    n('dup', 'condition.if', '邮箱未注册?', DX + NODE_W, DY, 'control', undefined, {
      expression: '!{{validate.dup}}',
      trueNext: 'createAccount',
      falseNext: 'dupAlert',
    }),
    n('dupAlert', 'notify.email', '邮箱重复告警', DX + NODE_W, -DY, 'action', '通知 HR 邮箱已注册', {
      host: '',
      port: 465,
      ssl: true,
      to: 'hr@meowflow.com',
      subject: '【入职冲突】{{trigger.email}} 已存在',
      body: '邮箱 {{trigger.email}} 已在系统中，请核实。',
    }),
    n('createAccount', 'http.request', '创建账号', 2 * (DX + NODE_W), 0, 'action', '调用账号系统创建企业账号', {
      method: 'POST',
      url: 'https://idp.internal/api/account/create',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "name":"{{trigger.name}}", "email":"{{trigger.email}}", "department":"{{trigger.department}}" }',
      timeoutMs: 8000,
      retries: 2,
    }),
    n('seat', 'http.request', '分配工位', 3 * (DX + NODE_W), -DY, 'action', '分配工位（按部门+入职日期）', {
      method: 'POST',
      url: 'https://seat.internal/api/seat/assign',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "email":"{{trigger.email}}", "department":"{{trigger.department}}" }',
    }),
    n('permission', 'http.request', '开通权限', 3 * (DX + NODE_W), DY, 'action', '按岗位模板开通 SaaS 权限', {
      method: 'POST',
      url: 'https://idp.internal/api/permission/grant',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "email":"{{trigger.email}}", "template":"{{trigger.position}}-template" }',
    }),
    n('hrBP', 'notify.email', '通知 HRBP', 2 * (DX + NODE_W), -DY, 'action', '通知对接的 HRBP（含入职 checklist）', {
      host: '',
      port: 465,
      ssl: true,
      to: 'hrbp@meowflow.com',
      subject: '新员工入职：{{trigger.name}}',
      body: '请与 {{trigger.name}} 同步入职事项。',
    }),
    n('welcome', 'ai.llm', '生成欢迎邮件', 2 * (DX + NODE_W), 0, 'ai', '个性化生成欢迎邮件', {
      model: 'gpt-4o-mini',
      temperature: 0.6,
      prompt: '写一封给 {{trigger.name}} 入职 {{trigger.position}} 部门 {{trigger.department}} 的欢迎邮件，语气热情。',
    }),
    n('schedule', 'ai.llm', '安排 onboarding 日程', 4 * (DX + NODE_W), 0, 'ai', '基于岗位模板生成第一周日程', {
      model: 'gpt-4o-mini',
      temperature: 0.3,
      prompt: '基于岗位 {{trigger.position}} 生成第一周入职日程，包含培训、导师介绍、团队会议。',
    }),
    n('sendWelcome', 'notify.email', '发送欢迎包', 4 * (DX + NODE_W), -DY, 'action', '发送欢迎邮件', {
      host: '',
      port: 465,
      ssl: true,
      to: '{{trigger.email}}',
      subject: '欢迎加入喵流！',
      body: '<pre>{{welcome.text}}\n\n第一周日程：{{schedule.text}}</pre>',
    }),
    n('end', 'end.aggregator', '结束', 5 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'validate'),
    e('e2', 'validate', 'dup'),
    eCondition('e3', 'dup', 'createAccount', '未注册'),
    eCondition('e4', 'dup', 'dupAlert', '已注册'),
    e('e5', 'dupAlert', 'end'),
    e('e6', 'createAccount', 'hrBP'),
    e('e7', 'createAccount', 'welcome'),
    e('e8', 'createAccount', 'seat'),
    e('e9', 'createAccount', 'permission'),
    e('e10', 'welcome', 'schedule'),
    e('e11', 'schedule', 'sendWelcome'),
    e('e12', 'sendWelcome', 'end'),
    e('e13', 'seat', 'end'),
    e('e14', 'permission', 'end'),
    e('e15', 'hrBP', 'end'),
  ];
  return entry(
    'builtin:hr-onboarding',
    '员工入职流程',
    '查重 → 自动开通账号 / 权限 / 工位 / 通知 HRBP / AI 生成欢迎邮件 + 第一周日程',
    'hr',
    '人力资源',
    '🤝',
    248,
    4.5,
    ['HR', '入职', 'AI'],
    nodes,
    edges,
  );
}

// ---------------------------------------------------------------------------
// 运营提效
// ---------------------------------------------------------------------------

function opsMeetingNotes(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.webhook', '音频上传触发', 0, 0, 'trigger', '上传会议录音触发', {
      method: 'POST',
      path: '/hooks/ops/meeting-upload',
      authToken: 'Bearer ****',
      timeout: 10000,
    }),
    n('validate', 'code.transform', '校验音频', DX + NODE_W, 0, 'transform', '校验音频格式、大小、时长', {
      language: 'javascript',
      source: 'const ok = input.size < 100*1024*1024 && /wav|mp3|m4a/.test(input.format); return { ok, audioUrl: input.audioUrl };',
    }),
    n('asr', 'http.request', 'ASR 语音转写', 2 * (DX + NODE_W), 0, 'action', '调用 ASR 服务转写为文本（带说话人）', {
      method: 'POST',
      url: 'https://asr.internal/api/recognize',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "audioUrl":"{{validate.audioUrl}}", "diarization":true, "format":"wav" }',
      timeoutMs: 120000,
      retries: 2,
    }),
    n('summarize', 'ai.llm', '摘要 / 决议 / TODO', 3 * (DX + NODE_W), 0, 'ai', '提取会议摘要、决议和待办', {
      model: 'gpt-4o-mini',
      temperature: 0.2,
      maxTokens: 2000,
      prompt: '从以下文本中提取：summary、decisions[]、todos[{owner,content,due}], 输出 JSON。',
    }),
    n('split', 'code.transform', '拆分决议条目', 4 * (DX + NODE_W), 0, 'transform', '将决议拆分为独立条目并校验完整性', {
      language: 'javascript',
      source: 'const decisions = (input.decisions || []).filter(d => d.content); const todos = (input.todos || []).filter(t => t.owner && t.content); return { decisions, todos, summary: input.summary || "" };',
    }),
    n('kbNote', 'http.request', '写入知识库', 5 * (DX + NODE_W), -DY, 'action', '把会议纪要写入知识库（含关键词索引）', {
      method: 'POST',
      url: 'https://kb.internal/api/docs',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "title":"会议纪要 {{split.summary.date}}", "content":"{{split.summary.text}}", "tags":["meeting"] }',
    }),
    n('email', 'notify.email', '发送邮件', 5 * (DX + NODE_W), DY, 'action', '邮件发送会议纪要给与会者', {
      host: '',
      port: 465,
      ssl: true,
      subject: '【会议纪要】{{split.summary.title}}',
      body: '<pre>{{split.summary.text}}</pre>',
    }),
    n('feishu', 'notify.feishu', '飞书群通知', 5 * (DX + NODE_W), 0, 'action', '飞书群推送摘要 + 决议列表', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
    }),
    n('todoSync', 'http.request', '同步待办', 6 * (DX + NODE_W), 0, 'action', '把决议中的 TODO 同步到 Jira / 飞书任务', {
      method: 'POST',
      url: 'https://jira.internal/api/issue/bulk-create',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "items":{{split.todos}} }',
    }),
    n('reject', 'notify.feishu', '音频异常告警', DX + NODE_W, -DY, 'action', '音频格式异常 / 超大文件告警', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: false,
    }),
    n('end', 'end.aggregator', '结束', 7 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'validate'),
    eCondition('e2', 'validate', 'asr', '通过'),
    eError('e3', 'validate', 'reject', '格式异常'),
    e('e4', 'asr', 'summarize'),
    e('e5', 'summarize', 'split'),
    e('e6', 'split', 'kbNote'),
    e('e7', 'split', 'email'),
    e('e8', 'split', 'feishu'),
    e('e9', 'split', 'todoSync'),
    e('e10', 'kbNote', 'end'),
    e('e11', 'email', 'end'),
    e('e12', 'feishu', 'end'),
    e('e13', 'todoSync', 'end'),
    e('e14', 'reject', 'end'),
  ];
  return entry(
    'builtin:ops-meeting-notes',
    '会议纪要生成',
    '上传音频 → 校验 → ASR 转写 → AI 提取摘要/决议/TODO → 知识库 + 邮件 + 飞书 + Jira',
    'ops',
    '运营提效',
    '🗒️',
    1421,
    4.9,
    ['运营', '会议', 'AI', 'RAG'],
    nodes,
    edges,
  );
}

function opsDailyReport(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.cron', '每日 18:00', 0, 0, 'trigger', '每日傍晚 6 点汇总', {
      cron: '0 18 * * *',
      timezone: 'Asia/Shanghai',
      enabled: true,
    }),
    n('fetch', 'http.request', '拉取日报源', DX + NODE_W, 0, 'action', '拉取各团队日报', {
      method: 'GET',
      url: 'https://reports.internal/api/daily-entries/today',
      auth: { type: 'none' },
      headers: { 'X-Service': 'meowflow' },
      bodyType: 'json',
      timeoutMs: 10000,
      retries: 3,
    }),
    n('validate', 'code.transform', '校验与去重', 2 * (DX + NODE_W), 0, 'transform', '校验数据完整性，按团队去重', {
      language: 'javascript',
      source: 'const seen = new Set(); const valid = (input.entries || []).filter(e => e.team && e.content && !seen.has(e.team + e.date) && seen.add(e.team + e.date)); return { entries: valid };',
    }),
    n('branch', 'condition.if', '有数据?', 3 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{validate.entries.length}} > 0',
      trueNext: 'merge',
      falseNext: 'emptyAlert',
    }),
    n('emptyAlert', 'notify.feishu', '日报缺失告警', 3 * (DX + NODE_W), DY, 'action', '今日日报缺失通知管理层', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: false,
    }),
    n('merge', 'code.transform', '合并多源数据', 4 * (DX + NODE_W), 0, 'transform', '合并并按团队归档', {
      language: 'javascript',
      source: 'const map = {}; (input.entries || []).forEach(e => { (map[e.team] ||= []).push(e); }); return map;',
    }),
    n('summary', 'ai.llm', '汇总摘要', 5 * (DX + NODE_W), 0, 'ai', '生成管理层可读的汇总摘要', {
      model: 'gpt-4o-mini',
      temperature: 0.3,
      maxTokens: 1500,
      prompt: '基于 {{merge}} 输出 200 字管理层摘要，重点强调风险与亮点。',
    }),
    n('risk', 'ai.llm', '风险识别', 5 * (DX + NODE_W), DY, 'ai', '从日报中识别潜在风险（延期/资源缺口/质量问题）', {
      model: 'gpt-4o-mini',
      temperature: 0.2,
      prompt: '从日报条目中识别风险，给出风险等级和建议。',
    }),
    n('send', 'notify.feishu', '发送到管理层群', 6 * (DX + NODE_W), 0, 'action', '通过飞书机器人推送到管理群（含摘要+风险）', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
      atAll: false,
    }),
    n('archive', 'http.request', '归档知识库', 6 * (DX + NODE_W), DY, 'action', '日报归档到知识库供检索', {
      method: 'POST',
      url: 'https://kb.internal/api/docs',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "title":"日报 {{merge.date}}", "content":"{{summary.text}}" }',
    }),
    n('end', 'end.aggregator', '结束', 7 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'fetch'),
    e('e2', 'fetch', 'validate'),
    e('e3', 'validate', 'branch'),
    eCondition('e4', 'branch', 'merge', '有'),
    eCondition('e5', 'branch', 'emptyAlert', '空'),
    e('e6', 'merge', 'summary'),
    e('e7', 'merge', 'risk'),
    e('e8', 'summary', 'send'),
    e('e9', 'risk', 'send'),
    e('e10', 'send', 'archive'),
    e('e11', 'archive', 'end'),
    e('e12', 'emptyAlert', 'end'),
  ];
  return entry(
    'builtin:ops-daily-report',
    '团队日报汇总',
    '定时拉取日报、校验去重、AI 汇总摘要 + 风险识别、推送管理层并归档知识库',
    'ops',
    '运营提效',
    '📊',
    532,
    4.5,
    ['运营', '日报', '定时', 'AI'],
    nodes,
    edges,
  );
}

function opsProductCopy(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.form', '提交商品参数', 0, 0, 'trigger', '提交商品参数表单', {
      formId: 'form_product',
      fields: [
        { key: 'productName', label: '商品名', type: 'string', required: true },
        { key: 'features', label: '卖点', type: 'text', required: true },
        { key: 'platform', label: '平台', type: 'select', options: ['京东', '淘宝', '小红书', '抖音'], required: true },
        { key: 'audience', label: '目标人群', type: 'string', required: true },
      ],
    }),
    n('extract', 'ai.llm', '卖点提炼', DX + NODE_W, -DY, 'ai', '对原始素材提炼 5 个核心卖点', {
      model: 'gpt-4o-mini',
      temperature: 0.3,
      prompt: '请从 {{trigger.features}} 中提炼 5 个高吸引力卖点。',
    }),
    n('profile', 'ai.llm', '人群画像', DX + NODE_W, 0, 'ai', '基于目标人群生成消费者画像', {
      model: 'gpt-4o-mini',
      temperature: 0.4,
      prompt: '描述 {{trigger.audience}} 的核心特征：年龄/性别/兴趣/购买动机。',
    }),
    n('style', 'ai.llm', '平台风格适配', 2 * (DX + NODE_W), -DY, 'ai', '基于目标平台调整文案风格', {
      model: 'gpt-4o-mini',
      temperature: 0.5,
      prompt: '基于平台 {{trigger.platform}} 的风格重写卖点，目标用户是 {{trigger.audience}}。',
    }),
    n('copy', 'ai.llm', '生成多版本文案', 3 * (DX + NODE_W), -DY, 'ai', '生成 3 个版本标题 + 文案 + hashtag', {
      model: 'gpt-4o-mini',
      temperature: 0.8,
      maxTokens: 2000,
      prompt: '生成 3 个版本标题 + 文案 + hashtag，输出 JSON 数组。',
    }),
    n('check', 'ai.llm', '合规审核', 3 * (DX + NODE_W), DY, 'ai', '审核文案是否合规（广告法、虚假宣传、违禁词）', {
      model: 'gpt-4o-mini',
      temperature: 0.1,
      prompt: '对文案做合规检查（广告法、虚假宣传、违禁词），输出 pass 与 issues。',
    }),
    n('branch', 'condition.if', '通过审核?', 4 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{check.pass}} === true',
      trueNext: 'score',
      falseNext: 'fixCopy',
    }),
    n('fixCopy', 'ai.llm', '修复文案', 4 * (DX + NODE_W), DY, 'ai', '基于 issues 修复文案', {
      model: 'gpt-4o-mini',
      temperature: 0.4,
      prompt: '基于 issues 修改文案，保留风格。',
    }),
    n('score', 'ai.llm', '质量评分', 5 * (DX + NODE_W), 0, 'ai', '对每版文案做综合评分（创意/合规/转化）', {
      model: 'gpt-4o-mini',
      temperature: 0.2,
      maxTokens: 800,
      prompt: '基于合规分数与创意分数综合打分，给出排序。',
    }),
    n('rank', 'code.transform', '排序选 Top', 6 * (DX + NODE_W), 0, 'transform', '对文案按综合评分排序选最佳', {
      language: 'javascript',
      source: 'const sorted = (input.copies || []).sort((a,b) => b.score - a.score); return { top: sorted[0], ranked: sorted };',
    }),
    n('store', 'http.request', '入库', 7 * (DX + NODE_W), 0, 'action', '把最佳版本 + 全量入库', {
      method: 'POST',
      url: 'https://cms.internal/api/copy/save',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "top":{{rank.top}}, "ranked":{{rank.ranked}}, "platform":"{{trigger.platform}}" }',
    }),
    n('preview', 'http.request', '渲染预览', 7 * (DX + NODE_W), -DY, 'action', '渲染文案预览图供运营选', {
      method: 'POST',
      url: 'https://render.internal/api/preview',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "copyId":"{{store.copyId}}", "platform":"{{trigger.platform}}" }',
    }),
    n('notify', 'notify.feishu', '通知运营', 7 * (DX + NODE_W), DY, 'action', '飞书通知运营文案已生成', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
    }),
    n('end', 'end.aggregator', '结束', 8 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'extract'),
    e('e2', 'trigger', 'profile'),
    e('e3', 'extract', 'style'),
    e('e4', 'profile', 'style'),
    e('e5', 'style', 'copy'),
    e('e6', 'copy', 'check'),
    eCondition('e7', 'check', 'branch', '审核'),
    eCondition('e8', 'branch', 'score', '通过'),
    eCondition('e9', 'branch', 'fixCopy', '不通过'),
    eLoop('e10', 'fixCopy', 'check', '重审'),
    e('e11', 'score', 'rank'),
    e('e12', 'rank', 'store'),
    e('e13', 'store', 'preview'),
    e('e14', 'store', 'notify'),
    e('e15', 'preview', 'end'),
    e('e16', 'notify', 'end'),
  ];
  return entry(
    'builtin:ops-product-copy',
    '营销文案生成',
    '卖点提炼 + 人群画像 → 平台风格适配 → 多版本文案 → 合规审核（不通过则修复重审）→ 评分入库 + 预览',
    'ops',
    '运营提效',
    '✍️',
    856,
    4.6,
    ['运营', '文案', 'AI', '合规'],
    nodes,
    edges,
  );
}

function opsDataWeekly(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.cron', '每周一 09:00', 0, 0, 'trigger', '每周一早上 9 点触发', {
      cron: '0 9 * * MON',
      timezone: 'Asia/Shanghai',
      enabled: true,
    }),
    n('query', 'http.request', '查询数据源', DX + NODE_W, 0, 'action', '查询上周业务核心指标', {
      method: 'GET',
      url: 'https://bi.internal/api/metrics/last-week',
      auth: { type: 'none' },
      bodyType: 'json',
      timeoutMs: 15000,
      retries: 3,
    }),
    n('clean', 'code.transform', '清洗与聚合', 2 * (DX + NODE_W), 0, 'transform', '清洗字段并计算环比/同比', {
      language: 'javascript',
      source: 'return (input.metrics || []).map(m => ({ key: m.key, value: m.value, lastWeek: m.lastWeek, delta: m.value - m.lastWeek, deltaPct: m.lastWeek ? ((m.value - m.lastWeek) / m.lastWeek * 100).toFixed(2) : 0 }));',
    }),
    n('rank', 'code.transform', 'Top / Bottom 排序', 3 * (DX + NODE_W), 0, 'transform', '取 Top 5 / Bottom 5 指标', {
      language: 'javascript',
      source: 'const sorted = (input || []).slice().sort((a,b) => b.deltaPct - a.deltaPct); return { top5: sorted.slice(0, 5), bottom5: sorted.slice(-5).reverse(), all: sorted };',
    }),
    n('insight', 'ai.llm', '趋势洞察', 4 * (DX + NODE_W), 0, 'ai', '识别趋势、异常、建议（结合 Top/Bottom）', {
      model: 'gpt-4o-mini',
      temperature: 0.2,
      maxTokens: 2000,
      prompt: '基于上周数据给出 3 个核心洞察 + 3 条建议。',
    }),
    n('render', 'ai.llm', '渲染图表说明', 5 * (DX + NODE_W), -DY, 'ai', '生成图表标题与一句话洞察', {
      model: 'gpt-4o-mini',
      temperature: 0.3,
      prompt: '为 Top5 指标生成图表标题与一句话洞察。',
    }),
    n('chart', 'http.request', '生成图表', 5 * (DX + NODE_W), DY, 'action', '调用图表服务生成 PNG 图表', {
      method: 'POST',
      url: 'https://chart.internal/api/render',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "type":"bar", "data":{{rank.top5}} }',
    }),
    n('email', 'notify.email', '发送周报', 5 * (DX + NODE_W), 0, 'action', '邮件发送周报（含图表）', {
      host: '',
      port: 465,
      ssl: true,
      subject: '【业务周报】{{insight.title}}',
      body: '<pre>{{insight.body}}</pre><img src="{{chart.url}}"/>',
    }),
    n('feishu', 'notify.feishu', '飞书推送', 6 * (DX + NODE_W), 0, 'action', '飞书群推送到管理层', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
    }),
    n('end', 'end.aggregator', '结束', 7 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'query'),
    e('e2', 'query', 'clean'),
    e('e3', 'clean', 'rank'),
    e('e4', 'rank', 'insight'),
    e('e5', 'rank', 'render'),
    e('e6', 'insight', 'email'),
    e('e7', 'render', 'chart'),
    e('e8', 'chart', 'email'),
    e('e9', 'email', 'feishu'),
    e('e10', 'feishu', 'end'),
  ];
  return entry(
    'builtin:ops-data-weekly',
    '数据周报生成',
    '周期性拉取指标 → 清洗 / 聚合 / 排序 → AI 洞察 + 图表渲染 → 邮件 + 飞书',
    'ops',
    '运营提效',
    '📈',
    384,
    4.6,
    ['运营', '数据', 'AI', '周报'],
    nodes,
    edges,
  );
}

// ---------------------------------------------------------------------------
// 财务行政
// ---------------------------------------------------------------------------

function financeInvoiceOcr(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.webhook', '上传发票触发', 0, 0, 'trigger', '上传发票图片触发', {
      method: 'POST',
      path: '/hooks/finance/invoice',
      authToken: 'Bearer ****',
      timeout: 15000,
    }),
    n('validate', 'code.transform', '校验图片', DX + NODE_W, 0, 'transform', '校验图片格式与大小', {
      language: 'javascript',
      source: 'const ok = /png|jpe?g|pdf/.test(input.format) && input.size < 10*1024*1024; return { ok, imageUrl: input.imageUrl, format: input.format };',
    }),
    n('ocr', 'http.request', 'OCR 识别', 2 * (DX + NODE_W), 0, 'action', '调用 OCR 识别发票字段', {
      method: 'POST',
      url: 'https://ocr.internal/api/invoice/recognize',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "imageUrl":"{{validate.imageUrl}}", "format":"{{validate.format}}" }',
      timeoutMs: 30000,
      retries: 2,
    }),
    n('verify', 'ai.llm', '字段校验 / 真伪', 3 * (DX + NODE_W), 0, 'ai', '校验发票号、金额、税号等字段并判断真伪', {
      model: 'gpt-4o-mini',
      temperature: 0.05,
      prompt: '校验以下发票字段并给出风险等级 + 原因（低/中/高）。',
    }),
    n('dupCheck', 'code.transform', '查重', 4 * (DX + NODE_W), 0, 'transform', '校验发票号是否重复报销', {
      language: 'javascript',
      source: 'const dup = (input.history || []).some(h => h.invoiceNo === input.invoiceNo); return { dup, riskLevel: input.riskLevel };',
    }),
    n('branch', 'condition.if', '通过?', 5 * (DX + NODE_W), 0, 'control', undefined, {
      expression: "{{verify.riskLevel}} === 'low' && !{{dupCheck.dup}}",
      trueNext: 'archive',
      falseNext: 'alert',
    }),
    n('archive', 'http.request', '入档案系统', 6 * (DX + NODE_W), -DY, 'action', '把发票推入档案系统', {
      method: 'POST',
      url: 'https://archive.internal/api/invoices',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "invoiceNo":"{{verify.invoiceNo}}", "amount":{{verify.amount}}, "taxNo":"{{verify.taxNo}}" }',
    }),
    n('approve', 'http.request', '提交财务审批', 6 * (DX + NODE_W), 0, 'action', '提交财务审批流', {
      method: 'POST',
      url: 'https://oa.internal/api/finance/invoice-approve',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "invoiceNo":"{{verify.invoiceNo}}", "amount":{{verify.amount}} }',
    }),
    n('alert', 'notify.email', '通知财务复核', 6 * (DX + NODE_W), DY, 'action', '通知财务复核（重复或风险等级高）', {
      host: '',
      port: 465,
      ssl: true,
      to: 'finance-review@meowflow.com',
      subject: '【待复核发票】{{verify.invoiceNo}}',
      body: '<pre>{{verify.notes}}</pre>',
    }),
    n('payment', 'http.request', '触发付款', 7 * (DX + NODE_W), 0, 'action', '审批通过后调支付系统', {
      method: 'POST',
      url: 'https://pay.internal/api/payment/disburse',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "invoiceNo":"{{verify.invoiceNo}}", "amount":{{verify.amount}} }',
    }),
    n('end', 'end.aggregator', '结束', 8 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'validate'),
    eCondition('e2', 'validate', 'ocr', '通过'),
    eError('e3', 'validate', 'alert', '图片异常'),
    e('e4', 'ocr', 'verify'),
    e('e5', 'verify', 'dupCheck'),
    e('e6', 'dupCheck', 'branch'),
    eCondition('e7', 'branch', 'archive', '通过'),
    eCondition('e8', 'branch', 'alert', '复核'),
    e('e9', 'archive', 'approve'),
    e('e10', 'approve', 'payment'),
    e('e11', 'payment', 'end'),
    e('e12', 'alert', 'end'),
  ];
  return entry(
    'builtin:finance-invoice-ocr',
    '发票识别与归档',
    '扫描 / 上传发票 → OCR → 字段校验 + 真伪识别 + 查重 → 通过归档并触发付款 / 不通过通知复核',
    'finance',
    '财务行政',
    '🧾',
    354,
    4.7,
    ['财务', '发票', 'AI', 'OCR'],
    nodes,
    edges,
  );
}

function financeReimburse(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.form', '员工提交报销', 0, 0, 'trigger', '员工报销表单', {
      formId: 'form_reimburse',
      fields: [
        { key: 'applicant', label: '申请人', type: 'string', required: true },
        { key: 'amount', label: '金额', type: 'number', required: true },
        { key: 'category', label: '类别', type: 'select', options: ['差旅', '招待', '办公'], required: true },
        { key: 'reason', label: '事由', type: 'text', required: true },
        { key: 'invoiceNo', label: '发票号', type: 'string', required: true },
      ],
    }),
    n('dedup', 'code.transform', '查重校验', DX + NODE_W, 0, 'transform', '查重并校验规则', {
      language: 'javascript',
      source: 'const exists = (input.dups || []).some(d => d.invoiceNo === input.invoiceNo); const budgetLeft = (input.budget || 0) - (input.used || 0); return { exists, budgetLeft, amount: input.amount };',
    }),
    n('budgetCheck', 'condition.if', '预算够?', 2 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{dedup.budgetLeft}} >= {{dedup.amount}}',
      trueNext: 'amount',
      falseNext: 'budgetAlert',
    }),
    n('budgetAlert', 'notify.feishu', '预算超支', 2 * (DX + NODE_W), DY, 'action', '部门预算不足告警财务', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: false,
    }),
    n('amount', 'condition.if', '金额路由', 3 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{dedup.amount}} >= 5000',
      trueNext: 'leader',
      falseNext: 'approve',
    }),
    n('leader', 'http.request', '主管审批', 4 * (DX + NODE_W), -DY, 'action', '主管审批', {
      method: 'POST',
      url: 'https://oa.internal/api/reimburse/lead-approve',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "applicant":"{{trigger.applicant}}", "amount":{{dedup.amount}} }',
      timeoutMs: 5000,
      retries: 2,
    }),
    n('finance', 'http.request', '财务终审', 4 * (DX + NODE_W), 0, 'action', '财务终审（大额）', {
      method: 'POST',
      url: 'https://oa.internal/api/reimburse/finance-approve',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "applicant":"{{trigger.applicant}}", "amount":{{dedup.amount}}, "category":"{{trigger.category}}" }',
      timeoutMs: 5000,
      retries: 2,
    }),
    n('approve', 'http.request', '自动审批', 4 * (DX + NODE_W), DY, 'action', '小额自动审批', {
      method: 'POST',
      url: 'https://oa.internal/api/reimburse/auto-approve',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "applicant":"{{trigger.applicant}}", "amount":{{dedup.amount}} }',
      timeoutMs: 5000,
    }),
    n('pay', 'http.request', '调支付系统', 5 * (DX + NODE_W), DY, 'action', '调用支付系统', {
      method: 'POST',
      url: 'https://pay.internal/api/payment/disburse',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "applicant":"{{trigger.applicant}}", "amount":{{dedup.amount}} }',
      timeoutMs: 10000,
      retries: 3,
    }),
    n('notify', 'notify.feishu', '通知员工', 6 * (DX + NODE_W), 0, 'action', '飞书通知员工', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
    }),
    n('archive', 'http.request', '报销归档', 6 * (DX + NODE_W), -DY, 'action', '报销记录归档', {
      method: 'POST',
      url: 'https://archive.internal/api/reimburse',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "applicant":"{{trigger.applicant}}", "amount":{{dedup.amount}}, "category":"{{trigger.category}}" }',
    }),
    n('end', 'end.aggregator', '结束', 7 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'dedup'),
    e('e2', 'dedup', 'budgetCheck'),
    eCondition('e3', 'budgetCheck', 'amount', '够'),
    eCondition('e4', 'budgetCheck', 'budgetAlert', '超支'),
    e('e5', 'amount', 'approve'),
    eCondition('e6', 'amount', 'leader', '中等'),
    eCondition('e7', 'amount', 'finance', '大额'),
    e('e8', 'approve', 'pay'),
    e('e9', 'leader', 'pay'),
    e('e10', 'finance', 'pay'),
    e('e11', 'pay', 'notify'),
    e('e12', 'notify', 'archive'),
    e('e13', 'archive', 'end'),
    e('e14', 'budgetAlert', 'end'),
  ];
  return entry(
    'builtin:finance-reimburse',
    '报销审批',
    '员工提交报销单 → 查重 + 预算校验 → 按金额路由（小额自动/中额主管/大额财务）→ 支付 + 通知 + 归档',
    'finance',
    '财务行政',
    '💰',
    268,
    4.5,
    ['财务', '审批', 'AI', '表单'],
    nodes,
    edges,
  );
}

function financeArReminder(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.cron', '每日 09:00', 0, 0, 'trigger', '每日早上 9 点扫描应收款', {
      cron: '0 9 * * *',
      timezone: 'Asia/Shanghai',
      enabled: true,
    }),
    n('query', 'http.request', '拉取应收款', DX + NODE_W, 0, 'action', '从 ERP 拉取应收款列表', {
      method: 'GET',
      url: 'https://erp.internal/api/ar/list',
      auth: { type: 'none' },
      bodyType: 'json',
      timeoutMs: 15000,
      retries: 3,
    }),
    n('filter', 'code.transform', '筛选逾期', 2 * (DX + NODE_W), 0, 'transform', '筛选逾期 / 即将到期 / 正常', {
      language: 'javascript',
      source: 'const items = input.items || []; return { overdue: items.filter(i => i.daysOverdue > 0), upcoming: items.filter(i => i.daysToDue > 0 && i.daysToDue <= 3), normal: items.filter(i => i.daysToDue > 3) };',
    }),
    n('branch', 'condition.if', '有逾期?', 3 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{filter.overdue.length}} > 0',
      trueNext: 'escalate',
      falseNext: 'remind',
    }),
    n('escalate', 'notify.email', '抄送财务主管', 4 * (DX + NODE_W), -DY, 'action', '把逾期客户抄送给财务主管', {
      host: '',
      port: 465,
      ssl: true,
      to: 'ar-lead@meowflow.com',
      subject: '【逾期应收款告警】共 {{filter.overdue.length}} 笔',
      body: '<pre>{{filter.overdue}}</pre>',
    }),
    n('remind', 'ai.llm', '生成催收文案', 4 * (DX + NODE_W), DY, 'ai', '生成个性化催收文案', {
      model: 'gpt-4o-mini',
      temperature: 0.4,
      prompt: '基于即将到期客户生成礼貌的催收话术。',
    }),
    n('send', 'notify.feishu', '发送客户', 5 * (DX + NODE_W), DY, 'action', '通过飞书外发机器人送达客户', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
    }),
    n('email', 'notify.email', '抄送邮件', 5 * (DX + NODE_W), 0, 'action', '抄送邮件给销售跟单', {
      host: '',
      port: 465,
      ssl: true,
      to: 'sales-followup@meowflow.com',
      subject: '【应收款提醒】即将到期 {{filter.upcoming.length}} 笔',
      body: '<pre>详情见飞书群通知</pre>',
    }),
    n('end', 'end.aggregator', '结束', 6 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'query'),
    e('e2', 'query', 'filter'),
    e('e3', 'filter', 'branch'),
    eCondition('e4', 'branch', 'escalate', '已逾期'),
    eCondition('e5', 'branch', 'remind', '即将到期'),
    e('e6', 'remind', 'send'),
    e('e7', 'remind', 'email'),
    e('e8', 'escalate', 'end'),
    e('e9', 'send', 'end'),
    e('e10', 'email', 'end'),
  ];
  return entry(
    'builtin:finance-ar-reminder',
    '应收款提醒',
    '每日扫描应收款 → 筛选三档（逾期/即将到期/正常）→ 逾期抄送主管 / 即将到期 AI 生成催收文案 + 飞书 + 邮件',
    'finance',
    '财务行政',
    '⏰',
    198,
    4.4,
    ['财务', '催收', 'AI'],
    nodes,
    edges,
  );
}

// ---------------------------------------------------------------------------
// 通用 / 数据
// ---------------------------------------------------------------------------

function generalDataSync(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.cron', '每小时 00 分', 0, 0, 'trigger', '每小时整点同步', {
      cron: '0 * * * *',
      timezone: 'Asia/Shanghai',
      enabled: true,
    }),
    n('mode', 'code.transform', '判断同步模式', DX + NODE_W, 0, 'transform', '基于上次同步时间决定全量/增量', {
      language: 'javascript',
      source: 'const hoursSinceLast = (Date.now() - (input.lastSync || 0)) / 3600_000; return { mode: hoursSinceLast > 24 ? "full" : "incremental", lastSync: input.lastSync };',
    }),
    n('extract', 'http.request', '拉取数据', 2 * (DX + NODE_W), 0, 'action', '支持全量或增量拉取', {
      method: 'GET',
      url: 'https://source-db.internal/api/orders',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json', 'X-Mode': '{{mode.mode}}' },
      bodyType: 'json',
      timeoutMs: 60000,
      retries: 3,
    }),
    n('clean', 'code.transform', '字段映射', 3 * (DX + NODE_W), 0, 'transform', '字段映射与清洗', {
      language: 'javascript',
      source: 'return (input.items || []).map(i => ({ id: i.id, total: i.amount, date: i.createdAt, customer: i.customerId, status: i.status }));',
    }),
    n('dedup', 'code.transform', '去重 / 主键合并', 4 * (DX + NODE_W), 0, 'transform', '按主键去重', {
      language: 'javascript',
      source: 'const seen = new Set(); return (input || []).filter(x => !seen.has(x.id) && seen.add(x.id));',
    }),
    n('branch', 'condition.if', '批量 vs 流式', 5 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{dedup.length}} >= 1000',
      trueNext: 'batch',
      falseNext: 'stream',
    }),
    n('batch', 'http.request', '批量写入数仓', 6 * (DX + NODE_W), -DY, 'action', '大批量走数仓', {
      method: 'POST',
      url: 'https://dwh.internal/api/load',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "rows": {{dedup}} }',
      timeoutMs: 120000,
    }),
    n('stream', 'http.request', '写入 Kafka', 6 * (DX + NODE_W), DY, 'action', '小批量走流式', {
      method: 'POST',
      url: 'https://kafka.internal/api/produce',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "topic":"orders-stream", "rows": {{dedup}} }',
      timeoutMs: 30000,
    }),
    n('retry', 'code.transform', '指数退避', 6 * (DX + NODE_W), 0, 'transform', '失败时指数退避', {
      language: 'javascript',
      source: 'return { sleepMs: Math.min(30000, 1000 * Math.pow(2, input.attempt || 1)), attempt: (input.attempt || 1) + 1 };',
    }),
    n('notify', 'notify.feishu', '完成通知', 7 * (DX + NODE_W), 0, 'action', '完成通知', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'rich',
    }),
    n('alert', 'notify.feishu', '失败告警', 7 * (DX + NODE_W), DY, 'action', '同步失败告警运维', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: true,
    }),
    n('end', 'end.aggregator', '结束', 8 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'mode'),
    e('e2', 'mode', 'extract'),
    e('e3', 'extract', 'clean'),
    e('e4', 'clean', 'dedup'),
    e('e5', 'dedup', 'branch'),
    eCondition('e6', 'branch', 'batch', '批量'),
    eCondition('e7', 'branch', 'stream', '流式'),
    eLoop('e8', 'batch', 'retry', '失败'),
    eLoop('e9', 'stream', 'retry', '失败'),
    eError('e10', 'retry', 'alert', '重试耗尽'),
    e('e11', 'retry', 'notify'),
    e('e12', 'batch', 'notify'),
    e('e13', 'stream', 'notify'),
    e('e14', 'notify', 'end'),
    e('e15', 'alert', 'end'),
  ];
  return entry(
    'builtin:data-sync',
    '数据同步任务',
    '定时把业务库数据同步到数仓 / Kafka，自动判断全量 / 增量，大批量走数仓 / 小批量走 Kafka，失败重试',
    'general',
    '通用',
    '🗄️',
    318,
    4.4,
    ['数据', '同步', '定时', 'Kafka'],
    nodes,
    edges,
  );
}

function generalWebhook(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.webhook', 'Webhook 入口', 0, 0, 'trigger', '通用 webhook 接收', {
      method: 'POST',
      path: '/hooks/generic',
      authToken: '',
      timeout: 10000,
    }),
    n('verify', 'code.transform', '签名校验', DX + NODE_W, 0, 'transform', '使用 HMAC 校验签名', {
      language: 'javascript',
      source: 'const valid = input.headers["x-signature"] === input.expected && Math.abs(Date.now() - +input.headers["x-timestamp"]) < 60_000; return { valid };',
    }),
    n('rateLimit', 'code.transform', '限流检查', DX + NODE_W, DY, 'transform', '检查调用方是否超过速率限制', {
      language: 'javascript',
      source: 'const count = (input.recentCount || 0); return { exceeded: count > 100, count };',
    }),
    n('branch', 'condition.if', '签名有效?', 2 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{verify.valid}} === true',
      trueNext: 'handle',
      falseNext: 'reject',
    }),
    n('rateBranch', 'condition.if', '限流?', 2 * (DX + NODE_W), DY, 'control', undefined, {
      expression: '{{rateLimit.exceeded}} === false',
      trueNext: 'handle',
      falseNext: 'rateAlert',
    }),
    n('handle', 'code.transform', '执行业务逻辑', 3 * (DX + NODE_W), -DY, 'transform', '处理上游业务', {
      language: 'javascript',
      source: 'return { ok: true, processedAt: Date.now(), payload: input.body };',
    }),
    n('reject', 'notify.feishu', '签名失败告警', 3 * (DX + NODE_W), 0, 'action', '签名失败告警', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: false,
    }),
    n('rateAlert', 'notify.feishu', '限流告警', 3 * (DX + NODE_W), DY, 'action', '调用方超限告警', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: false,
    }),
    n('respond', 'http.request', '回调外部', 4 * (DX + NODE_W), -DY, 'action', '回调外部系统', {
      method: 'POST',
      url: 'https://callback.example.com/webhook',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "id":"{{trigger.id}}", "status":"ok" }',
      timeoutMs: 5000,
    }),
    n('retry', 'code.transform', '重试退避', 4 * (DX + NODE_W), 0, 'transform', '回调失败时退避重试', {
      language: 'javascript',
      source: 'return { sleepMs: 500 * Math.pow(2, input.attempt || 1), attempt: (input.attempt || 1) + 1 };',
    }),
    n('log', 'http.request', '审计日志', 5 * (DX + NODE_W), -DY, 'action', '写入审计日志', {
      method: 'POST',
      url: 'https://audit.internal/api/log',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "trigger":"{{trigger.id}}", "status":"ok" }',
    }),
    n('end', 'end.aggregator', '结束', 6 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'verify'),
    e('e2', 'trigger', 'rateLimit'),
    e('e3', 'verify', 'branch'),
    eCondition('e4', 'branch', 'handle', '通过'),
    eCondition('e5', 'branch', 'reject', '失败'),
    e('e6', 'rateLimit', 'rateBranch'),
    eCondition('e7', 'rateBranch', 'handle', '未超限'),
    eCondition('e8', 'rateBranch', 'rateAlert', '超限'),
    e('e9', 'handle', 'respond'),
    eLoop('e10', 'respond', 'retry', '失败'),
    eError('e11', 'retry', 'reject', '回调失败'),
    e('e12', 'respond', 'log'),
    e('e13', 'log', 'end'),
    e('e14', 'reject', 'end'),
    e('e15', 'rateAlert', 'end'),
  ];
  return entry(
    'builtin:general-webhook',
    '通用 Webhook 接入',
    '通用 Webhook 接收 → HMAC 签名 + 时间戳校验 + 限流 → 执行业务 + 回调（重试） + 审计日志',
    'general',
    '通用',
    '🔗',
    425,
    4.5,
    ['通用', 'Webhook', '回调', '限流'],
    nodes,
    edges,
  );
}

function generalBackup(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.cron', '每日 02:00', 0, 0, 'trigger', '凌晨 2 点备份', {
      cron: '0 2 * * *',
      timezone: 'Asia/Shanghai',
      enabled: true,
    }),
    n('check', 'code.transform', '检查磁盘空间', DX + NODE_W, 0, 'transform', '备份前检查磁盘剩余空间', {
      language: 'javascript',
      source: 'return { ok: (input.diskFree || 0) > 10 * 1024 * 1024 * 1024, diskFree: input.diskFree };',
    }),
    n('diskBranch', 'condition.if', '磁盘够?', DX + NODE_W, DY, 'control', undefined, {
      expression: '{{check.ok}} === true',
      trueNext: 'dump',
      falseNext: 'diskAlert',
    }),
    n('diskAlert', 'notify.feishu', '磁盘告警', DX + NODE_W, -DY, 'action', '磁盘不足告警运维', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: true,
    }),
    n('dump', 'http.request', '导出数据库', 2 * (DX + NODE_W), 0, 'action', '导出数据库快照', {
      method: 'POST',
      url: 'https://db.internal/api/dump',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      timeoutMs: 1800000,
      retries: 1,
    }),
    n('compress', 'code.transform', '压缩 / 加密', 3 * (DX + NODE_W), 0, 'transform', '压缩 + AES 加密', {
      language: 'javascript',
      source: 'return { file: input.file, alg: "aes-256-gcm", size: input.file.length, checksum: input.checksum };',
    }),
    n('upload', 'http.request', '上传 OSS', 4 * (DX + NODE_W), 0, 'action', '上传对象存储', {
      method: 'PUT',
      url: 'https://oss.internal/buckets/db-backup/{{trigger.date}}/dump.gz.enc',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/octet-stream' },
      bodyType: 'json',
      timeoutMs: 300000,
    }),
    n('branch', 'condition.if', '上传成功?', 5 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{upload.status}} === 200',
      trueNext: 'log',
      falseNext: 'alert',
    }),
    n('retry', 'code.transform', '退避重试', 5 * (DX + NODE_W), DY, 'transform', '失败时指数退避', {
      language: 'javascript',
      source: 'return { sleepMs: 5000 * Math.pow(2, input.attempt || 1), attempt: (input.attempt || 1) + 1 };',
    }),
    n('log', 'code.transform', '写入备份日志', 6 * (DX + NODE_W), -DY, 'transform', '记录备份日志（含 checksum）', {
      language: 'javascript',
      source: 'return { ok: true, size: input.size, checksum: input.checksum, when: Date.now() };',
    }),
    n('audit', 'http.request', '记录审计', 6 * (DX + NODE_W), 0, 'action', '写审计系统', {
      method: 'POST',
      url: 'https://audit.internal/api/backup',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "date":"{{trigger.date}}", "size":{{log.size}}, "checksum":"{{log.checksum}}" }',
    }),
    n('alert', 'notify.feishu', '告警运维', 6 * (DX + NODE_W), DY, 'action', '备份失败告警', {
      webhook: '{{NOTIFY_WEBHOOK}}',
      msgType: 'text',
      atAll: true,
    }),
    n('end', 'end.aggregator', '结束', 7 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'check'),
    e('e2', 'check', 'diskBranch'),
    eCondition('e3', 'diskBranch', 'dump', '够'),
    eCondition('e4', 'diskBranch', 'diskAlert', '不足'),
    e('e5', 'dump', 'compress'),
    e('e6', 'compress', 'upload'),
    e('e7', 'upload', 'branch'),
    eCondition('e8', 'branch', 'log', '成功'),
    eCondition('e9', 'branch', 'alert', '失败'),
    eLoop('e10', 'upload', 'retry', '失败'),
    e('e11', 'retry', 'upload'),
    e('e12', 'log', 'audit'),
    e('e13', 'audit', 'end'),
    e('e14', 'alert', 'end'),
    e('e15', 'diskAlert', 'end'),
  ];
  return entry(
    'builtin:general-backup',
    '定时备份',
    '磁盘预检 → DB dump → 压缩加密 → OSS 上传（失败重试） → 备份日志 + 审计',
    'general',
    '通用',
    '💾',
    256,
    4.5,
    ['通用', '备份', '定时', 'OSS'],
    nodes,
    edges,
  );
}

function generalDataClean(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    n('trigger', 'trigger.form', '上传原始数据', 0, 0, 'trigger', '上传原始数据文件', {
      formId: 'form_data_clean',
      fields: [
        { key: 'fileUrl', label: '文件 URL', type: 'string', required: true },
        { key: 'format', label: '格式', type: 'select', options: ['csv', 'excel', 'json'], required: true },
        { key: 'ruleProfile', label: '规则集', type: 'select', options: ['默认', '严格', '宽松'], required: false },
      ],
    }),
    n('validate', 'code.transform', '文件校验', DX + NODE_W, 0, 'transform', '校验文件存在与大小', {
      language: 'javascript',
      source: 'const ok = !!input.fileUrl && (input.size || 0) < 100*1024*1024; return { ok, fileUrl: input.fileUrl, format: input.format, ruleProfile: input.ruleProfile || "默认" };',
    }),
    n('parse', 'code.transform', '解析 CSV / Excel', 2 * (DX + NODE_W), 0, 'transform', '解析为通用结构', {
      language: 'javascript',
      source: 'return { rows: input.rows || [], cols: input.cols || [], total: (input.rows || []).length };',
    }),
    n('clean', 'ai.llm', '字段归一化', 3 * (DX + NODE_W), 0, 'ai', '归一化字段并标注异常值', {
      model: 'gpt-4o-mini',
      temperature: 0.1,
      maxTokens: 2000,
      prompt: '对每行数据做字段归一化，并标注缺失/异常。',
    }),
    n('branch', 'condition.if', '数据可补?', 4 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{clean.missingCount}} <= 0',
      trueNext: 'export',
      falseNext: 'fill',
    }),
    n('fill', 'ai.llm', 'AI 补全缺失', 5 * (DX + NODE_W), -DY, 'ai', '智能补全缺失字段', {
      model: 'gpt-4o-mini',
      temperature: 0.3,
      prompt: '基于上下文补全缺失字段值。',
    }),
    n('fillLoop', 'condition.if', '还有缺失?', 5 * (DX + NODE_W), 0, 'control', undefined, {
      expression: '{{fill.remainingMissing}} > 0',
      trueNext: 'reject',
      falseNext: 'export',
    }),
    n('reject', 'code.transform', '进入异常池', 5 * (DX + NODE_W), DY, 'transform', '无法补全的进入异常池', {
      language: 'javascript',
      source: 'return { rejected: input.bad || [] };',
    }),
    n('dedup', 'code.transform', '去重', 6 * (DX + NODE_W), 0, 'transform', '基于主键去重', {
      language: 'javascript',
      source: 'const seen = new Set(); const rows = input.filled || input.rows; const out = []; for (const r of rows) { const k = r.id || r.email || JSON.stringify(r); if (!seen.has(k)) { seen.add(k); out.push(r); } } return out;',
    }),
    n('export', 'http.request', '导出结构化数据', 6 * (DX + NODE_W), -DY, 'action', '导出到目标存储', {
      method: 'POST',
      url: 'https://dwh.internal/api/clean-data',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "rows":{{dedup}}, "format":"{{trigger.format}}" }',
    }),
    n('auditLog', 'http.request', '审计日志', 7 * (DX + NODE_W), 0, 'action', '写入数据血缘审计', {
      method: 'POST',
      url: 'https://audit.internal/api/lineage',
      auth: { type: 'none' },
      headers: { 'Content-Type': 'application/json' },
      bodyType: 'json',
      body: '{ "sourceFile":"{{trigger.fileUrl}}", "rows":{{dedup.length}}, "ruleProfile":"{{trigger.ruleProfile}}" }',
    }),
    n('end', 'end.aggregator', '结束', 8 * (DX + NODE_W), 0, 'end', undefined, {
      outputMode: 'return_last',
    }),
  ];
  const edges: BuiltinEdge[] = [
    e('e1', 'trigger', 'validate'),
    eCondition('e2', 'validate', 'parse', '通过'),
    eError('e3', 'validate', 'end', '文件异常'),
    e('e4', 'parse', 'clean'),
    e('e5', 'clean', 'branch'),
    eCondition('e6', 'branch', 'fill', '可补'),
    eCondition('e7', 'branch', 'export', '完整'),
    e('e8', 'fill', 'fillLoop'),
    eCondition('e9', 'fillLoop', 'export', '全部补齐'),
    eCondition('e10', 'fillLoop', 'reject', '仍有缺失'),
    e('e11', 'fill', 'dedup'),
    e('e12', 'reject', 'dedup'),
    e('e13', 'dedup', 'export'),
    e('e14', 'export', 'auditLog'),
    e('e15', 'auditLog', 'end'),
  ];
  return entry(
    'builtin:data-clean',
    '数据清洗转换',
    '上传原始数据 → 解析 → AI 归一化 → 智能补全 + 无法补全入异常池 → 去重 → 导出 + 数据血缘审计',
    'general',
    '数据处理',
    '🧹',
    187,
    4.6,
    ['数据', '清洗', 'AI', '血缘'],
    nodes,
    edges,
  );
}

// ---------------------------------------------------------------------------
// 暴露
// ---------------------------------------------------------------------------

export const BUILTIN_TEMPLATES: BuiltinTemplate[] = [
  csAutoReply(),
  csTicketRoute(),
  csFeedbackAnalysis(),
  hrResumeScreen(),
  hrLeaveApprove(),
  hrOnboarding(),
  opsMeetingNotes(),
  opsDailyReport(),
  opsProductCopy(),
  opsDataWeekly(),
  financeInvoiceOcr(),
  financeReimburse(),
  financeArReminder(),
  generalDataSync(),
  generalWebhook(),
  generalBackup(),
  generalDataClean(),
];

export function findBuiltinTemplate(id: string): BuiltinTemplate | undefined {
  return BUILTIN_TEMPLATES.find(t => t.id === id);
}

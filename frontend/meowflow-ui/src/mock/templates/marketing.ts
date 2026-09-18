/**
 * 营销场景模板 (8 个)
 *
 * 涵盖内容生成、社媒运营、线索管理、活动报名等常见营销场景
 */

import type { BuiltinTemplate, BuiltinNode, BuiltinEdge } from '../builtinTemplates';
import { N, E, EC, EL, llmConfig, httpConfig, kbConfig, codeConfig, switchConfig, ifConfig } from '../templateBuilders';

// =================================================================
// 1. 营销文案生成器 - 输入产品简介，输出多平台营销文案
// =================================================================
function mkMarketingContent(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '产品信息表单', 0, 0, 'trigger', '用户填写产品名称、卖点、目标人群', {
      formId: 'form_marketing_content',
      fields: JSON.stringify({ productName: 'string', usp: 'string', audience: 'string', tone: 'select' }),
    }),
    N('llm', 'ai.llm', 'LLM 营销文案生成', 1, 0, 'ai', '基于产品信息生成多平台适配的营销文案', llmConfig({
      model: 'gpt-4o',
      temperature: 0.85,
      prompt: `根据以下产品信息生成营销文案：
产品名：{{trigger.productName}}
核心卖点：{{trigger.usp}}
目标人群：{{trigger.audience}}
语气：{{trigger.tone}}

请分别生成：
1. 小红书风格 (200字 + 3个标签)
2. 抖音脚本 (60秒脚本 + 钩子)
3. 微信公众号 (标题 + 摘要 + 正文)
4. 朋友圈文案 (30字 + emoji)
输出 JSON 格式。`,
      systemPrompt: '你是资深营销文案专家，熟悉各平台调性。',
      maxTokens: 2000,
    })),
    N('classify', 'ai.llm', '合规审查', 2, 0, 'ai', '检查文案是否含违禁词/虚假宣传/绝对化用语', llmConfig({
      temperature: 0.1,
      prompt: '审查以下营销文案，标注任何违规内容。\n\n{{llm.output}}\n\n输出 {passed: bool, issues: [...]}',
      outputKey: 'review',
    })),
    N('branch', 'condition.if', '是否通过', 3, 0, 'control', undefined, ifConfig(
      '{{classify.passed}} === true', 'send', 'revise'
    )),
    N('revise', 'ai.llm', '修订文案', 3, 1, 'ai', '根据审查意见自动修订', llmConfig({
      prompt: '根据以下审查意见修订文案：\n问题：{{classify.issues}}\n原文：{{llm.output}}',
    })),
    N('send', 'notify.feishu', '推送营销团队', 4, 0, 'action', '通过飞书通知营销团队审阅'),
    N('save', 'http.request', '归档到知识库', 4, 1, 'action', '把优质文案存入素材库', httpConfig({
      method: 'POST',
      url: 'https://crm.internal/api/marketing/content',
      body: '{ "content":"{{llm.output}}", "tags":["auto-generated"] }',
    })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'llm'),
    E('e2', 'llm', 'classify'),
    E('e3', 'classify', 'branch'),
    EC('e4', 'branch', 'send', '通过'),
    EC('e5', 'branch', 'revise', '不通过'),
    E('e6', 'revise', 'save'),
    E('e7', 'send', 'end'),
    E('e8', 'save', 'end'),
  ];
  return {
    id: 'builtin:marketing-content-generator',
    name: '营销文案生成器',
    description: '输入产品信息，一键生成多平台营销文案，含合规审查',
    category: 'marketing',
    categoryName: '营销场景',
    tags: ['文案', 'AI生成', '多平台', '合规'],
    coverEmoji: '✨',
    author: '喵流官方',
    usageCount: 1820,
    rating: 4.8,
    isOfficial: true,
    difficulty: 'easy',
    estimatedSetupMinutes: 10,
    requiredModels: ['gpt-4o', 'gpt-4o-mini'],
    requiredIntegrations: [],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// =================================================================
// 2. 社媒自动发布 - 多平台同步发布
// =================================================================
function mkSocialPublish(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '定时触发', 0, 0, 'trigger', '每天 9:00 触发', { cron: '0 9 * * *', timezone: 'Asia/Shanghai' }),
    N('fetch', 'http.request', '获取待发内容', 1, 0, 'action', '从内容池获取今日待发内容', httpConfig({
      method: 'GET',
      url: 'https://cms.internal/api/content/pending?platforms=wechat,xiaohongshu,douyin',
    })),
    N('split', 'condition.switch', '按平台拆分', 2, 0, 'control', '分发到不同发布节点', switchConfig(
      '{{fetch.platform}}',
      [
        { value: 'wechat', next: 'pub_wechat' },
        { value: 'xiaohongshu', next: 'pub_xhs' },
        { value: 'douyin', next: 'pub_douyin' },
      ],
      'log_skip'
    )),
    N('pub_wechat', 'http.request', '发布公众号', 3, -1, 'action', '微信公众号 API 发布', httpConfig({
      method: 'POST', url: 'https://api.weixin.qq.com/cgi-bin/message/mass/sendall',
      headers: { 'Content-Type': 'application/json' },
    })),
    N('pub_xhs', 'http.request', '发布小红书', 3, 0, 'action', '小红书 API 发布'),
    N('pub_douyin', 'http.request', '发布抖音', 3, 1, 'action', '抖音开放平台发布'),
    N('log_skip', 'code.transform', '跳过记录', 3, 2, 'transform', '记录不支持的平台'),
    N('stats', 'http.request', '汇总发布统计', 4, 0, 'action', '把发布结果写入统计', httpConfig({
      method: 'POST', url: 'https://bi.internal/api/social-publish/log',
    })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'split'),
    EC('e3', 'split', 'pub_wechat', '公众号'),
    EC('e4', 'split', 'pub_xhs', '小红书'),
    EC('e5', 'split', 'pub_douyin', '抖音'),
    EC('e6', 'split', 'log_skip', '其他'),
    E('e7', 'pub_wechat', 'stats'),
    E('e8', 'pub_xhs', 'stats'),
    E('e9', 'pub_douyin', 'stats'),
    E('e10', 'log_skip', 'stats'),
    E('e11', 'stats', 'end'),
  ];
  return {
    id: 'builtin:social-multi-publish',
    name: '多平台自动发布',
    description: '定时从内容池拉取内容，自动同步发布到公众号/小红书/抖音',
    category: 'marketing',
    categoryName: '营销场景',
    tags: ['自动化', '社媒', '定时', '多平台'],
    coverEmoji: '📱',
    author: '喵流官方',
    usageCount: 1450,
    rating: 4.7,
    isOfficial: true,
    difficulty: 'medium',
    estimatedSetupMinutes: 20,
    requiredModels: [],
    requiredIntegrations: ['微信公众号', '小红书', '抖音'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// =================================================================
// 3. 线索评分系统 - 自动评估销售线索质量
// =================================================================
function mkLeadScoring(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', 'CRM 线索推送', 0, 0, 'trigger', 'CRM 系统推送新线索', { method: 'POST', path: '/hooks/leads/new' }),
    N('enrich', 'http.request', '数据丰富', 1, 0, 'action', '调用第三方丰富线索数据', httpConfig({
      method: 'POST', url: 'https://api.enrichment.io/v1/person',
    })),
    N('score', 'ai.llm', 'AI 评分', 2, 0, 'ai', '综合行业、规模、行为打分', llmConfig({
      model: 'gpt-4o',
      temperature: 0.2,
      prompt: `你是销售线索评分专家。基于以下信息给 0-100 分:
线索：{{trigger.body}}
丰富数据：{{enrich.response}}
评估维度: 购买意向(40%), 预算(30%), 决策权(20%), 时机(10%)
输出 {score, tier: A/B/C/D, reason}`,
      outputKey: 'score,tier,reason',
    })),
    N('branch', 'condition.switch', '分层路由', 3, 0, 'control', 'A/B/C/D 分配不同跟进策略', switchConfig(
      '{{score.tier}}',
      [
        { value: 'A', next: 'assign_senior' },
        { value: 'B', next: 'assign_normal' },
        { value: 'C', next: 'nurture' },
        { value: 'D', next: 'discard' },
      ]
    )),
    N('assign_senior', 'http.request', '分配高级销售', 4, -2, 'action', '分配给资深销售', httpConfig({
      method: 'POST', url: 'https://crm.internal/api/leads/assign-senior',
    })),
    N('assign_normal', 'http.request', '分配普通销售', 4, -1, 'action', '分配给普通销售', httpConfig({
      method: 'POST', url: 'https://crm.internal/api/leads/assign',
    })),
    N('nurture', 'http.request', '进入培育池', 4, 0, 'action', '添加到邮件培育序列', httpConfig({
      method: 'POST', url: 'https://mail.internal/api/nurture/add',
    })),
    N('discard', 'code.transform', '归档低质量', 4, 1, 'transform', '记录原因后归档', codeConfig('return { reason: input.score.reason, archived: true };')),
    N('notify', 'notify.feishu', '销售通知', 5, -2, 'action', '飞书通知销售'),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'enrich'),
    E('e2', 'enrich', 'score'),
    E('e3', 'score', 'branch'),
    EC('e4', 'branch', 'assign_senior', 'A'),
    EC('e5', 'branch', 'assign_normal', 'B'),
    EC('e6', 'branch', 'nurture', 'C'),
    EC('e7', 'branch', 'discard', 'D'),
    E('e8', 'assign_senior', 'notify'),
    E('e9', 'assign_normal', 'notify'),
    E('e10', 'nurture', 'end'),
    E('e11', 'discard', 'end'),
    E('e12', 'notify', 'end'),
  ];
  return {
    id: 'builtin:lead-scoring',
    name: 'AI 线索评分',
    description: 'CRM 新线索自动丰富数据 + AI 评分 + 分层分配',
    category: 'marketing',
    categoryName: '营销场景',
    tags: ['销售', 'CRM', 'AI评分', '自动化'],
    coverEmoji: '🎯',
    author: '喵流官方',
    usageCount: 980,
    rating: 4.9,
    isOfficial: true,
    difficulty: 'hard',
    estimatedSetupMinutes: 30,
    requiredModels: ['gpt-4o'],
    requiredIntegrations: ['CRM', '飞书'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// =================================================================
// 4. 用户画像生成 - 整合多源数据生成 360° 用户画像
// =================================================================
function mkUserPersona(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', '用户标识', 0, 0, 'trigger', '接收 userId'),
    N('fetch_orders', 'http.request', '查询订单', 1, -1, 'action', '获取最近订单', httpConfig({ method: 'GET', url: 'https://shop.internal/api/users/{{trigger.userId}}/orders' })),
    N('fetch_events', 'http.request', '查询行为', 1, 0, 'action', '获取行为事件', httpConfig({ method: 'GET', url: 'https://analytics.internal/api/users/{{trigger.userId}}/events' })),
    N('fetch_chat', 'http.request', '查询会话', 1, 1, 'action', '获取客服会话', httpConfig({ method: 'GET', url: 'https://cs.internal/api/users/{{trigger.userId}}/conversations' })),
    N('aggregate', 'transform.aggregator', '数据合并', 2, 0, 'transform', '合并多源数据'),
    N('persona', 'ai.llm', '生成画像', 3, 0, 'ai', 'AI 生成用户画像', llmConfig({
      model: 'gpt-4o',
      prompt: '基于以下多源数据生成结构化用户画像:\n{{aggregate.data}}\n\n输出: {标签, 消费力等级, 偏好品类, 沟通风格, 推荐触达时机}',
    })),
    N('save', 'http.request', '持久化画像', 4, 0, 'action', '保存到画像库', httpConfig({ method: 'POST', url: 'https://crm.internal/api/personas' })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch_orders'),
    E('e2', 'trigger', 'fetch_events'),
    E('e3', 'trigger', 'fetch_chat'),
    E('e4', 'fetch_orders', 'aggregate'),
    E('e5', 'fetch_events', 'aggregate'),
    E('e6', 'fetch_chat', 'aggregate'),
    E('e7', 'aggregate', 'persona'),
    E('e8', 'persona', 'save'),
    E('e9', 'save', 'end'),
  ];
  return {
    id: 'builtin:user-persona',
    name: '用户画像生成',
    description: '整合订单/行为/会话数据，AI 生成 360° 用户画像',
    category: 'marketing',
    categoryName: '营销场景',
    tags: ['用户画像', 'AI', '数据整合'],
    coverEmoji: '👤',
    author: '喵流官方',
    usageCount: 720,
    rating: 4.7,
    isOfficial: true,
    difficulty: 'medium',
    estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'],
    requiredIntegrations: ['订单系统', '分析系统', '客服系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// =================================================================
// 5. 邮件营销活动 - 个性化邮件批量发送
// =================================================================
function mkEmailCampaign(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '定时发送', 0, 0, 'trigger', '每周一上午 10 点'),
    N('fetch_users', 'http.request', '获取目标用户', 1, 0, 'action', '从 CRM 拉取目标用户', httpConfig({ method: 'GET', url: 'https://crm.internal/api/campaign/audience' })),
    N('loop', 'iteration.loop', '逐个用户处理', 2, 0, 'control', '对每个用户生成个性化内容'),
    N('gen', 'ai.llm', '个性化文案', 3, 0, 'ai', '基于用户画像生成文案', llmConfig({
      prompt: '根据用户画像 {{$item.persona}} 生成个性化营销文案。',
      systemPrompt: '文案需简洁、有吸引力。',
    })),
    N('send', 'http.request', '发送邮件', 4, 0, 'action', '调用邮件 API', httpConfig({
      method: 'POST', url: 'https://mail.internal/api/send',
      body: '{ "to":"{{$item.email}}", "subject":"专属推荐", "body":"{{gen.text}}" }',
    })),
    N('log', 'http.request', '发送日志', 5, 0, 'action', '记录发送结果', httpConfig({ method: 'POST', url: 'https://bi.internal/api/email/log' })),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch_users'),
    E('e2', 'fetch_users', 'loop'),
    EL('e3', 'loop', 'gen'),
    E('e4', 'gen', 'send'),
    EL('e5', 'send', 'loop'),
    E('e6', 'send', 'log'),
    E('e7', 'log', 'end'),
  ];
  return {
    id: 'builtin:email-campaign',
    name: '个性化邮件营销',
    description: '定时拉取用户群体，AI 生成个性化文案批量发送',
    category: 'marketing',
    categoryName: '营销场景',
    tags: ['邮件营销', 'AI文案', 'CRM'],
    coverEmoji: '📧',
    author: '喵流官方',
    usageCount: 640,
    rating: 4.6,
    isOfficial: true,
    difficulty: 'medium',
    estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o-mini'],
    requiredIntegrations: ['CRM', '邮件服务'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// =================================================================
// 6. A/B 测试自动分流 - 自动分配实验组
// =================================================================
function mkABTest(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', '用户访问', 0, 0, 'trigger', '接收 userId + feature'),
    N('check', 'http.request', '查实验配置', 1, 0, 'action', '从实验平台查询实验状态', httpConfig({ method: 'GET', url: 'https://exp.internal/api/experiments/{{trigger.feature}}' })),
    N('hash', 'code.transform', '哈希分桶', 2, 0, 'transform', '对 userId 哈希取模分桶', codeConfig('const hash = require("crypto").createHash("md5").update(input.userId + input.feature).digest("hex"); const bucket = parseInt(hash.slice(0,8), 16) % 100; return { bucket, group: bucket < 50 ? "A" : "B" };')),
    N('groupA', 'code.transform', 'A 组配置', 3, -1, 'transform', 'A 组配置处理'),
    N('groupB', 'code.transform', 'B 组配置', 3, 1, 'transform', 'B 组配置处理'),
    N('log', 'http.request', '埋点上报', 4, 0, 'action', '上报曝光埋点', httpConfig({ method: 'POST', url: 'https://analytics.internal/api/ab/exposure' })),
    N('return', 'end.aggregator', '返回配置', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'check'),
    E('e2', 'check', 'hash'),
    EC('e3', 'hash', 'groupA', 'A'),
    EC('e4', 'hash', 'groupB', 'B'),
    E('e5', 'groupA', 'log'),
    E('e6', 'groupB', 'log'),
    E('e7', 'log', 'return'),
  ];
  return {
    id: 'builtin:ab-test-router',
    name: 'A/B 测试自动分流',
    description: '基于用户哈希自动分配实验组，并埋点上报',
    category: 'marketing',
    categoryName: '营销场景',
    tags: ['A/B测试', '灰度', '埋点'],
    coverEmoji: '🧪',
    author: '喵流官方',
    usageCount: 420,
    rating: 4.5,
    isOfficial: true,
    difficulty: 'medium',
    estimatedSetupMinutes: 20,
    requiredModels: [],
    requiredIntegrations: ['实验平台', '分析埋点'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// =================================================================
// 7. 活动报名审核 - 表单提交后自动审核
// =================================================================
function mkEventReview(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '活动报名', 0, 0, 'trigger', '用户提交活动报名表单'),
    N('dedup', 'code.transform', '查重', 1, 0, 'transform', '检查是否重复报名', codeConfig('return { isDuplicate: false /* call db */ };')),
    N('check', 'condition.if', '是否重复', 2, 0, 'control', undefined, ifConfig('{{dedup.isDuplicate}} === true', 'reject', 'verify')),
    N('reject', 'notify.email', '驳回通知', 2, 1, 'action', '通知已报名'),
    N('verify', 'ai.llm', 'AI 资质审核', 3, 0, 'ai', '基于提交信息审核资格', llmConfig({
      prompt: '审核活动报名信息：\n{{trigger.formData}}\n\n评估维度：身份真实性、报名合理性、风险等级。',
      outputKey: 'passed,riskLevel',
    })),
    N('risk', 'condition.if', '是否高风险', 4, 0, 'control', undefined, ifConfig('{{verify.riskLevel}} === "high"', 'manual', 'auto')),
    N('auto', 'http.request', '自动通过', 5, 0, 'action', '自动通过报名', httpConfig({ method: 'POST', url: 'https://event.internal/api/registrations/approve' })),
    N('manual', 'notify.feishu', '转人工', 5, 1, 'action', '推送给运营人员审核'),
    N('email', 'notify.email', '发送确认', 6, 0, 'action', '发送报名确认邮件'),
    N('end', 'end.aggregator', '结束', 7, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'dedup'),
    E('e2', 'dedup', 'check'),
    EC('e3', 'check', 'reject', '是'),
    EC('e4', 'check', 'verify', '否'),
    E('e5', 'verify', 'risk'),
    EC('e6', 'risk', 'auto', '低风险'),
    EC('e7', 'risk', 'manual', '高风险'),
    E('e8', 'auto', 'email'),
    E('e9', 'manual', 'end'),
    E('e10', 'reject', 'end'),
    E('e11', 'email', 'end'),
  ];
  return {
    id: 'builtin:event-registration-review',
    name: '活动报名审核',
    description: '表单提交后查重 + AI 资质审核 + 自动通过/转人工',
    category: 'marketing',
    categoryName: '营销场景',
    tags: ['活动报名', '审核', 'AI'],
    coverEmoji: '🎫',
    author: '喵流官方',
    usageCount: 380,
    rating: 4.4,
    isOfficial: true,
    difficulty: 'medium',
    estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o-mini'],
    requiredIntegrations: ['活动系统', '邮件'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// =================================================================
// 8. 营销日报 - 每天自动生成营销数据日报
// =================================================================
function mkMarketingDaily(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '日报触发', 0, 0, 'trigger', '每天 18:00 触发'),
    N('fetch', 'http.request', '拉取数据', 1, 0, 'action', '从 BI 拉取今日营销数据', httpConfig({ method: 'GET', url: 'https://bi.internal/api/marketing/daily' })),
    N('analyze', 'ai.llm', '智能分析', 2, 0, 'ai', 'AI 分析数据异常/亮点/建议', llmConfig({
      model: 'gpt-4o',
      prompt: '分析今日营销数据:\n{{fetch.response}}\n\n输出 JSON: {highlights, anomalies, recommendations}',
    })),
    N('gen', 'ai.llm', '生成日报', 3, 0, 'ai', '生成 Markdown 格式日报', llmConfig({
      prompt: '基于分析结果生成 Markdown 日报:\n数据: {{fetch.response}}\n分析: {{analyze.output}}',
      systemPrompt: '日报要清晰、有洞察。',
    })),
    N('send', 'notify.feishu', '推送日报', 4, 0, 'action', '推送营销团队群'),
    N('save', 'http.request', '归档报告', 4, 1, 'action', '保存到知识库', httpConfig({ method: 'POST', url: 'https://kb.internal/api/marketing-reports' })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'analyze'),
    E('e3', 'analyze', 'gen'),
    E('e4', 'gen', 'send'),
    E('e5', 'gen', 'save'),
    E('e6', 'send', 'end'),
    E('e7', 'save', 'end'),
  ];
  return {
    id: 'builtin:marketing-daily-report',
    name: '营销数据日报',
    description: '每天自动拉取营销数据，AI 分析生成日报推送',
    category: 'marketing',
    categoryName: '营销场景',
    tags: ['BI', '日报', 'AI分析'],
    coverEmoji: '📊',
    author: '喵流官方',
    usageCount: 890,
    rating: 4.8,
    isOfficial: true,
    difficulty: 'easy',
    estimatedSetupMinutes: 15,
    requiredModels: ['gpt-4o'],
    requiredIntegrations: ['BI平台', '飞书'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

export const MARKETING_TEMPLATES: BuiltinTemplate[] = [
  mkMarketingContent(),
  mkSocialPublish(),
  mkLeadScoring(),
  mkUserPersona(),
  mkEmailCampaign(),
  mkABTest(),
  mkEventReview(),
  mkMarketingDaily(),
];

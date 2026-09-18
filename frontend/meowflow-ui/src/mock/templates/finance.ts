/**
 * 金融场景模板 (6 个)
 */
import type { BuiltinTemplate, BuiltinNode, BuiltinEdge } from '../builtinTemplates';
import { N, E, EC, llmConfig, httpConfig, codeConfig, ifConfig } from '../templateBuilders';

// 1. 贷款申请审核
function mkLoanReview(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '贷款申请', 0, 0, 'trigger'),
    N('verify_id', 'http.request', '身份验证', 1, 0, 'action', '调用征信系统', httpConfig({ method: 'POST', url: 'https://credit.internal/api/verify' })),
    N('score', 'ai.llm', 'AI 风险评估', 2, 0, 'ai', '基于多维度评估', llmConfig({
      model: 'gpt-4o',
      prompt: '评估贷款风险:\n申请: {{trigger.formData}}\n征信: {{verify_id.response}}\n评估维度: 信用分、负债比、收入稳定性、行业风险。\n输出 {riskScore, decision: approve/review/reject, reasons}',
    })),
    N('branch', 'condition.switch', '决策路由', 3, 0, 'control'),
    N('auto_approve', 'http.request', '自动通过', 4, -1, 'action', undefined, httpConfig({ method: 'POST', url: 'https://loan.internal/api/approve' })),
    N('manual', 'notify.feishu', '转人工审批', 4, 0, 'action'),
    N('reject', 'notify.email', '驳回通知', 4, 1, 'action'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'verify_id'),
    E('e2', 'verify_id', 'score'),
    E('e3', 'score', 'branch'),
    EC('e4', 'branch', 'auto_approve', '低风险'),
    EC('e5', 'branch', 'manual', '中风险'),
    EC('e6', 'branch', 'reject', '高风险'),
    E('e7', 'auto_approve', 'end'),
    E('e8', 'manual', 'end'),
    E('e9', 'reject', 'end'),
  ];
  return {
    id: 'builtin:loan-review', name: '贷款申请审核',
    description: '贷款申请自动征信 + AI 风险评估 + 分级处理',
    category: 'finance', categoryName: '金融场景',
    tags: ['贷款', '风控', 'AI'], coverEmoji: '💳',
    author: '喵流官方', usageCount: 420, rating: 4.8, isOfficial: true,
    difficulty: 'hard', estimatedSetupMinutes: 30,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['征信', '贷款系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 2. 合同关键信息抽取
function mkContractExtraction(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '上传合同', 0, 0, 'trigger'),
    N('ocr', 'http.request', '合同 OCR', 1, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://ocr.internal/api/contract' })),
    N('extract', 'ai.llm', '信息抽取', 2, 0, 'ai', undefined, llmConfig({
      prompt: '抽取合同关键信息:\n{{ocr.text}}\n\n输出 JSON: {合同金额, 双方, 期限, 付款条款, 违约责任, 生效日期}',
    })),
    N('save', 'http.request', '结构化存储', 3, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://contract.internal/api/extract' })),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'ocr'),
    E('e2', 'ocr', 'extract'),
    E('e3', 'extract', 'save'),
    E('e4', 'save', 'end'),
  ];
  return {
    id: 'builtin:contract-extract', name: '合同信息抽取',
    description: '合同 OCR + AI 自动抽取结构化信息',
    category: 'finance', categoryName: '金融场景',
    tags: ['合同', 'OCR', '抽取'], coverEmoji: '📑',
    author: '喵流官方', usageCount: 380, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['OCR'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 3. 舆情监控
function mkSentimentMonitor(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每小时', 0, 0, 'trigger'),
    N('fetch', 'http.request', '拉取新闻', 1, 0, 'action', undefined, httpConfig({ method: 'GET', url: 'https://news.internal/api/feed' })),
    N('analyze', 'ai.llm', '情感分析', 2, 0, 'ai', undefined, llmConfig({
      prompt: '分析以下新闻情感:\n{{fetch.news}}\n\n输出: {positive, neutral, negative, keyTopics, overallScore}',
    })),
    N('alert', 'condition.if', '负面判断', 3, 0, 'control', undefined, ifConfig('{{analyze.negative}} > 阈值', 'warn', 'log')),
    N('warn', 'notify.feishu', '舆情告警', 4, 0, 'action'),
    N('log', 'http.request', '记录数据', 4, 1, 'action', undefined, httpConfig({ method: 'POST', url: 'https://bi.internal/api/sentiment' })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'analyze'),
    E('e3', 'analyze', 'alert'),
    EC('e4', 'alert', 'warn', '负面'),
    EC('e5', 'alert', 'log', '正常'),
    E('e6', 'warn', 'end'),
    E('e7', 'log', 'end'),
  ];
  return {
    id: 'builtin:sentiment-monitor', name: '舆情监控',
    description: '定时抓取新闻 + AI 情感分析 + 负面舆情告警',
    category: 'finance', categoryName: '金融场景',
    tags: ['舆情', 'AI', '情感'], coverEmoji: '📰',
    author: '喵流官方', usageCount: 240, rating: 4.6, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['新闻源'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 4. 行情分析报告
function mkMarketAnalysis(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每日收盘后', 0, 0, 'trigger'),
    N('fetch', 'http.request', '拉取行情', 1, 0, 'action'),
    N('news', 'http.request', '拉取新闻', 1, 1, 'action'),
    N('analyze', 'ai.llm', 'AI 综合分析', 2, 0, 'ai', undefined, llmConfig({
      model: 'gpt-4o',
      prompt: '综合分析:\n行情: {{fetch.response}}\n新闻: {{news.response}}\n输出: {趋势, 风险点, 关注标的}',
    })),
    N('gen', 'ai.llm', '生成报告', 3, 0, 'ai', undefined, llmConfig({ prompt: '基于分析 {{analyze.output}} 生成研报 (Markdown)。' })),
    N('notify', 'notify.feishu', '推送投研团队', 4, 0, 'action'),
    N('save', 'http.request', '归档', 4, 1, 'action'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'trigger', 'news'),
    E('e3', 'fetch', 'analyze'),
    E('e4', 'news', 'analyze'),
    E('e5', 'analyze', 'gen'),
    E('e6', 'gen', 'notify'),
    E('e7', 'gen', 'save'),
    E('e8', 'notify', 'end'),
    E('e9', 'save', 'end'),
  ];
  return {
    id: 'builtin:market-analysis', name: '行情分析报告',
    description: '每日自动生成市场行情分析报告',
    category: 'finance', categoryName: '金融场景',
    tags: ['行情', 'AI', '研报'], coverEmoji: '📈',
    author: '喵流官方', usageCount: 310, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['行情API'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 5. 反欺诈检测
function mkFraudDetection(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', '交易事件', 0, 0, 'trigger'),
    N('enrich', 'http.request', '数据丰富', 1, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://risk.internal/api/enrich' })),
    N('detect', 'ai.llm', 'AI 反欺诈', 2, 0, 'ai', undefined, llmConfig({
      prompt: '评估交易欺诈风险:\n交易: {{trigger.body}}\n丰富数据: {{enrich.response}}\n输出: {riskScore, signals, recommendation}',
    })),
    N('branch', 'condition.switch', '决策', 3, 0, 'control'),
    N('block', 'http.request', '拦截交易', 4, -1, 'action', undefined, httpConfig({ method: 'POST', url: 'https://payment.internal/api/block' })),
    N('verify', 'notify.sms', '二次验证', 4, 0, 'action', '发送验证码'),
    N('pass', 'http.request', '通过', 4, 1, 'action', undefined, httpConfig({ method: 'POST', url: 'https://payment.internal/api/approve' })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'enrich'),
    E('e2', 'enrich', 'detect'),
    E('e3', 'detect', 'branch'),
    EC('e4', 'branch', 'block', '高风险'),
    EC('e5', 'branch', 'verify', '中风险'),
    EC('e6', 'branch', 'pass', '低风险'),
    E('e7', 'block', 'end'),
    E('e8', 'verify', 'end'),
    E('e9', 'pass', 'end'),
  ];
  return {
    id: 'builtin:fraud-detection', name: '反欺诈检测',
    description: '实时交易反欺诈检测，自动拦截/验证/通过',
    category: 'finance', categoryName: '金融场景',
    tags: ['反欺诈', 'AI', '风控'], coverEmoji: '🚫',
    author: '喵流官方', usageCount: 580, rating: 4.9, isOfficial: true,
    difficulty: 'hard', estimatedSetupMinutes: 30,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['风控', '支付'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 6. 客户理财建议
function mkInvestmentAdvice(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '风险测评', 0, 0, 'trigger'),
    N('fetch_market', 'http.request', '市场行情', 1, 0, 'action'),
    N('fetch_profile', 'http.request', '客户画像', 1, 1, 'action'),
    N('gen', 'ai.llm', '生成建议', 2, 0, 'ai', undefined, llmConfig({
      model: 'gpt-4o',
      prompt: '基于客户画像 {{fetch_profile.response}} 和市场 {{fetch_market.response}} 生成个性化理财建议。\n输出: {资产配置, 推荐产品, 风险提示}',
    })),
    N('notify', 'notify.email', '发送建议', 3, 0, 'action'),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch_market'),
    E('e2', 'trigger', 'fetch_profile'),
    E('e3', 'fetch_market', 'gen'),
    E('e4', 'fetch_profile', 'gen'),
    E('e5', 'gen', 'notify'),
    E('e6', 'notify', 'end'),
  ];
  return {
    id: 'builtin:investment-advice', name: '理财建议生成',
    description: '基于客户画像和市场行情生成个性化建议',
    category: 'finance', categoryName: '金融场景',
    tags: ['理财', 'AI', '个性化'], coverEmoji: '💎',
    author: '喵流官方', usageCount: 180, rating: 4.5, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['行情API'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

export const FINANCE_TEMPLATES: BuiltinTemplate[] = [
  mkLoanReview(), mkContractExtraction(), mkSentimentMonitor(),
  mkMarketAnalysis(), mkFraudDetection(), mkInvestmentAdvice(),
];

/**
 * 数据分析场景模板 (8 个)
 *
 * 涵盖日志分析、SQL 生成、异常检测、可视化等数据场景
 */

import type { BuiltinTemplate, BuiltinNode, BuiltinEdge } from '../builtinTemplates';
import { N, E, EC, EL, llmConfig, httpConfig, codeConfig, switchConfig, ifConfig } from '../templateBuilders';

// 1. 智能日志分析 - 错误日志聚类 + 根因定位
function mkLogAnalysis(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每小时', 0, 0, 'trigger', '每小时触发一次'),
    N('fetch', 'http.request', '拉取错误日志', 1, 0, 'action', '查询上一小时错误日志', httpConfig({ method: 'GET', url: 'https://log.internal/api/errors?range=1h' })),
    N('cluster', 'ai.llm', '日志聚类', 2, 0, 'ai', 'AI 把相似错误聚类', llmConfig({
      model: 'gpt-4o',
      prompt: '把以下错误日志聚类成 5-10 类:\n{{fetch.logs}}\n\n输出: [{cluster, count, sample, severity}]',
    })),
    N('root_cause', 'ai.llm', '根因分析', 3, 0, 'ai', '针对 top 错误分析根因', llmConfig({
      prompt: '分析以下错误日志的根因:\n{{cluster.output}}\n\n对每个 top 集群给出根因假设、修复建议、影响范围。',
    })),
    N('branch', 'condition.if', '严重判断', 4, 0, 'control', undefined, ifConfig('存在严重错误', 'alert', 'save')),
    N('alert', 'notify.feishu', '告警值班', 5, 0, 'action', '飞书通知值班 SRE'),
    N('save', 'http.request', '归档报告', 5, 1, 'action', '归档分析报告', httpConfig({ method: 'POST', url: 'https://kb.internal/api/log-analysis' })),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'cluster'),
    E('e3', 'cluster', 'root_cause'),
    E('e4', 'root_cause', 'branch'),
    EC('e5', 'branch', 'alert', '严重'),
    EC('e6', 'branch', 'save', '正常'),
    E('e7', 'alert', 'end'),
    E('e8', 'save', 'end'),
  ];
  return {
    id: 'builtin:log-analysis',
    name: '智能日志分析',
    description: '自动聚类错误日志 + 根因定位 + 严重问题告警',
    category: 'data-analysis',
    categoryName: '数据分析',
    tags: ['日志', 'AI', 'SRE'],
    coverEmoji: '📋',
    author: '喵流官方', usageCount: 480, rating: 4.8, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['日志系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 2. 自然语言查询数据库 - Text-to-SQL
function mkTextToSql(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', '用户提问', 0, 0, 'trigger', '接收自然语言问题'),
    N('schema', 'http.request', '获取表结构', 1, 0, 'action', '查询数据库元数据', httpConfig({ method: 'GET', url: 'https://db.internal/api/schema' })),
    N('gen_sql', 'ai.llm', '生成 SQL', 2, 0, 'ai', '基于 schema 生成 SQL', llmConfig({
      model: 'gpt-4o',
      prompt: '基于表结构 {{schema.output}} 把问题 {{trigger.question}} 转为 SQL。\n只输出 SQL，不要解释。',
    })),
    N('validate', 'code.transform', 'SQL 校验', 3, 0, 'transform', '只允许 SELECT，禁用危险操作', codeConfig('const sql = input.sql?.trim() || ""; if (!/^\\s*SELECT/i.test(sql)) throw new Error("Only SELECT allowed"); return { safe: true, sql };')),
    N('exec', 'http.request', '执行查询', 4, 0, 'action', '执行 SQL', httpConfig({ method: 'POST', url: 'https://db.internal/api/sql/exec' })),
    N('gen_answer', 'ai.llm', '生成回答', 5, 0, 'ai', '基于数据生成自然语言回答', llmConfig({
      prompt: '用户问题: {{trigger.question}}\n查询结果: {{exec.data}}\n\n用自然语言回答用户。',
    })),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'schema'),
    E('e2', 'schema', 'gen_sql'),
    E('e3', 'gen_sql', 'validate'),
    E('e4', 'validate', 'exec'),
    E('e5', 'exec', 'gen_answer'),
    E('e6', 'gen_answer', 'end'),
  ];
  return {
    id: 'builtin:text-to-sql',
    name: 'Text-to-SQL 问答',
    description: '自然语言提问 + 自动生成 SQL + AI 解释结果',
    category: 'data-analysis',
    categoryName: '数据分析',
    tags: ['NL2SQL', 'AI', '数据库'],
    coverEmoji: '🗣️',
    author: '喵流官方', usageCount: 920, rating: 4.9, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['数据库'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 3. 异常检测告警 - 实时监控指标异常
function mkAnomalyDetection(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每 5 分钟', 0, 0, 'trigger', '每 5 分钟监控一次'),
    N('fetch', 'http.request', '拉取指标', 1, 0, 'action', '查询 Prometheus 指标', httpConfig({ method: 'GET', url: 'https://prom.internal/api/v1/query?query=...' })),
    N('detect', 'ai.llm', 'AI 异常检测', 2, 0, 'ai', '对比历史模式识别异常', llmConfig({
      prompt: '分析指标 {{fetch.metrics}} 是否异常。对比昨天同时段、历史均值、季节性。\n输出 {anomalies: [...], severity}',
    })),
    N('branch', 'condition.if', '异常判断', 3, 0, 'control', undefined, ifConfig('{{detect.anomalies.length}} > 0', 'alert', 'log')),
    N('alert', 'notify.feishu', '告警通知', 4, 0, 'action', '飞书告警值班'),
    N('create_incident', 'http.request', '创建事件', 4, 1, 'action', '在事件系统创建工单', httpConfig({ method: 'POST', url: 'https://incident.internal/api/create' })),
    N('log', 'code.transform', '正常记录', 4, 2, 'transform', '记录正常状态'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'detect'),
    E('e3', 'detect', 'branch'),
    EC('e4', 'branch', 'alert', '异常'),
    EC('e5', 'branch', 'log', '正常'),
    E('e6', 'alert', 'create_incident'),
    E('e7', 'create_incident', 'end'),
    E('e8', 'log', 'end'),
  ];
  return {
    id: 'builtin:anomaly-detection',
    name: 'AI 异常检测',
    description: '定时监控指标 + AI 异常检测 + 自动告警',
    category: 'data-analysis',
    categoryName: '数据分析',
    tags: ['监控', 'AI', '告警'],
    coverEmoji: '🚨',
    author: '喵流官方', usageCount: 360, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['Prometheus', '飞书'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 4. 数据可视化生成 - 数据 + 自然语言生成图表
function mkDataViz(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '用户提问', 0, 0, 'trigger', '用户输入想要的可视化'),
    N('fetch', 'http.request', '查询数据', 1, 0, 'action', '从数据仓库查询数据', httpConfig({ method: 'POST', url: 'https://bi.internal/api/query' })),
    N('analyze', 'ai.llm', '图表推荐', 2, 0, 'ai', '基于数据推荐图表类型', llmConfig({
      prompt: '基于数据 {{fetch.data}} 和用户需求 {{trigger.requirement}} 推荐图表类型 (柱/折线/饼/散点等)，说明理由。',
    })),
    N('gen', 'ai.llm', '生成配置', 3, 0, 'ai', '生成 ECharts 配置 JSON', llmConfig({
      prompt: '基于数据 {{fetch.data}} 生成 ECharts 配置。\n图表类型: {{analyze.output}}',
    })),
    N('save', 'http.request', '保存图表', 4, 0, 'action', '保存为可视化看板', httpConfig({ method: 'POST', url: 'https://bi.internal/api/dashboards' })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'analyze'),
    E('e3', 'analyze', 'gen'),
    E('e4', 'gen', 'save'),
    E('e5', 'save', 'end'),
  ];
  return {
    id: 'builtin:data-viz',
    name: 'AI 数据可视化',
    description: '自然语言描述 + AI 自动生成图表配置',
    category: 'data-analysis',
    categoryName: '数据分析',
    tags: ['可视化', 'AI', 'BI'],
    coverEmoji: '📊',
    author: '喵流官方', usageCount: 540, rating: 4.6, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 15,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['BI'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 5. 用户行为分析 - 漏斗分析
function mkFunnelAnalysis(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每日', 0, 0, 'trigger', '每日凌晨分析'),
    N('fetch', 'http.request', '拉取事件', 1, 0, 'action', '从埋点系统拉取昨日事件', httpConfig({ method: 'GET', url: 'https://track.internal/api/events/yesterday' })),
    N('funnel', 'code.transform', '构建漏斗', 2, 0, 'transform', '基于预设漏斗计算转化率', codeConfig('return { funnel: [{step:"访问", users:10000},{step:"注册", users:3000},{step:"下单", users:500},{step:"支付", users:300}] };')),
    N('analyze', 'ai.llm', 'AI 分析', 3, 0, 'ai', '分析漏斗异常步骤', llmConfig({
      prompt: '分析漏斗:\n{{funnel.output}}\n\n输出: {低转化步骤, 原因假设, 优化建议}',
    })),
    N('gen', 'ai.llm', '生成报告', 4, 0, 'ai', '生成分析报告'),
    N('save', 'http.request', '归档', 5, 0, 'action', '保存报告', httpConfig({ method: 'POST', url: 'https://bi.internal/api/funnel-reports' })),
    N('notify', 'notify.feishu', '推送增长团队', 5, 1, 'action', '通知增长团队'),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'funnel'),
    E('e3', 'funnel', 'analyze'),
    E('e4', 'analyze', 'gen'),
    E('e5', 'gen', 'save'),
    E('e6', 'gen', 'notify'),
    E('e7', 'save', 'end'),
    E('e8', 'notify', 'end'),
  ];
  return {
    id: 'builtin:funnel-analysis',
    name: '转化漏斗分析',
    description: '每日自动分析转化漏斗 + AI 给优化建议',
    category: 'data-analysis',
    categoryName: '数据分析',
    tags: ['增长', '漏斗', 'AI'],
    coverEmoji: '🌪️',
    author: '喵流官方', usageCount: 290, rating: 4.6, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['埋点', 'BI'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 6. 转化漏斗实时监控 - 实时异常下滑告警
function mkRealtimeFunnel(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每 30 分钟', 0, 0, 'trigger'),
    N('fetch', 'http.request', '查询实时数据', 1, 0, 'action'),
    N('compare', 'code.transform', '对比基线', 2, 0, 'transform', undefined, codeConfig('return { dropRate: 0.3, baseline: 0.2, isAbnormal: 0.3 > 0.2 * 1.5 };')),
    N('branch', 'condition.if', '异常判断', 3, 0, 'control', undefined, ifConfig('{{compare.isAbnormal}}', 'alert', 'end')),
    N('alert', 'notify.feishu', '下滑告警', 4, 0, 'action'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'compare'),
    E('e3', 'compare', 'branch'),
    EC('e4', 'branch', 'alert', '异常'),
    EC('e5', 'branch', 'end', '正常'),
  ];
  return {
    id: 'builtin:realtime-funnel-monitor',
    name: '实时漏斗监控',
    description: '每 30 分钟对比转化率，异常自动告警',
    category: 'data-analysis',
    categoryName: '数据分析',
    tags: ['实时', '监控', '增长'],
    coverEmoji: '⏱️',
    author: '喵流官方', usageCount: 210, rating: 4.5, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 15,
    requiredModels: [], requiredIntegrations: ['埋点'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 7. 数据质量监控 - 监控数据完整性
function mkDataQuality(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每日 6 点', 0, 0, 'trigger'),
    N('checks', 'http.request', '运行质量检查', 1, 0, 'action', '执行 DQ 检查 (空值/重复/异常值)', httpConfig({ method: 'POST', url: 'https://dq.internal/api/run-checks' })),
    N('report', 'ai.llm', 'AI 分析', 2, 0, 'ai', '分析质量问题严重性', llmConfig({
      prompt: '分析数据质量报告:\n{{checks.result}}\n\n输出: {criticalIssues, trends, recommendations}',
    })),
    N('branch', 'condition.if', '严重问题', 3, 0, 'control', undefined, ifConfig('存在关键问题', 'alert', 'archive')),
    N('alert', 'notify.feishu', '通知数据团队', 4, 0, 'action'),
    N('archive', 'http.request', '归档报告', 4, 1, 'action', undefined, httpConfig({ method: 'POST', url: 'https://kb.internal/api/dq-reports' })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'checks'),
    E('e2', 'checks', 'report'),
    E('e3', 'report', 'branch'),
    EC('e4', 'branch', 'alert', '有严重问题'),
    EC('e5', 'branch', 'archive', '正常'),
    E('e6', 'alert', 'end'),
    E('e7', 'archive', 'end'),
  ];
  return {
    id: 'builtin:data-quality-monitor',
    name: '数据质量监控',
    description: '每日自动运行 DQ 检查 + AI 分析 + 严重问题告警',
    category: 'data-analysis',
    categoryName: '数据分析',
    tags: ['数据质量', '监控'],
    coverEmoji: '🛡️',
    author: '喵流官方', usageCount: 180, rating: 4.5, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['DQ系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 8. 业务指标看板自动生成
function mkMetricsBoard(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每日 8 点', 0, 0, 'trigger'),
    N('fetch_kpi', 'http.request', '拉取核心 KPI', 1, -1, 'action'),
    N('fetch_growth', 'http.request', '拉取增长指标', 1, 0, 'action'),
    N('fetch_finance', 'http.request', '拉取财务指标', 1, 1, 'action'),
    N('aggregate', 'transform.aggregator', '数据汇总', 2, 0, 'transform'),
    N('gen', 'ai.llm', '生成洞察', 3, 0, 'ai', '生成业务洞察', llmConfig({
      prompt: '分析核心指标:\n{{aggregate.data}}\n\n输出关键洞察和异常指标。',
    })),
    N('save', 'http.request', '更新看板', 4, 0, 'action', '自动更新 BI 看板'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch_kpi'),
    E('e2', 'trigger', 'fetch_growth'),
    E('e3', 'trigger', 'fetch_finance'),
    E('e4', 'fetch_kpi', 'aggregate'),
    E('e5', 'fetch_growth', 'aggregate'),
    E('e6', 'fetch_finance', 'aggregate'),
    E('e7', 'aggregate', 'gen'),
    E('e8', 'gen', 'save'),
    E('e9', 'save', 'end'),
  ];
  return {
    id: 'builtin:metrics-board',
    name: '业务指标看板',
    description: '每日自动更新核心 KPI 看板 + AI 洞察',
    category: 'data-analysis',
    categoryName: '数据分析',
    tags: ['BI', '看板', 'KPI'],
    coverEmoji: '📊',
    author: '喵流官方', usageCount: 670, rating: 4.7, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 15,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['BI'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

export const DATA_ANALYSIS_TEMPLATES: BuiltinTemplate[] = [
  mkLogAnalysis(),
  mkTextToSql(),
  mkAnomalyDetection(),
  mkDataViz(),
  mkFunnelAnalysis(),
  mkRealtimeFunnel(),
  mkDataQuality(),
  mkMetricsBoard(),
];

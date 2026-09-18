/**
 * 办公自动化场景模板 (8 个)
 *
 * 涵盖周报、会议、审批、报销、合同等高频办公场景
 */

import type { BuiltinTemplate, BuiltinNode, BuiltinEdge } from '../builtinTemplates';
import { N, E, EC, EL, llmConfig, httpConfig, codeConfig, switchConfig, ifConfig } from '../templateBuilders';

// 1. 周报生成器 - 一键生成结构化周报
function mkWeeklyReport(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '周五下午', 0, 0, 'trigger', '每周五 17:00 提醒写周报'),
    N('form', 'trigger.form', '填写本周事项', 1, 0, 'trigger', '用户填写本周完成事项'),
    N('aggregate', 'http.request', '汇总数据', 2, 0, 'action', '从任务系统拉取本周完成的任务', httpConfig({ method: 'GET', url: 'https://task.internal/api/my-tasks?range=week' })),
    N('gen', 'ai.llm', '生成周报', 3, 0, 'ai', '基于事项 + 数据生成周报', llmConfig({
      model: 'gpt-4o',
      prompt: '基于用户填写事项 {{form.formData}} 和系统数据 {{aggregate.response}} 生成结构化周报。\n\n结构:\n## 本周完成\n## 数据亮点\n## 下周计划\n## 风险与求助',
    })),
    N('save', 'http.request', '保存周报', 4, 0, 'action', '保存到周报系统', httpConfig({ method: 'POST', url: 'https://oa.internal/api/weekly-reports' })),
    N('notify', 'notify.feishu', '推送主管', 4, 1, 'action', '推送飞书给直属主管'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'form'),
    E('e2', 'form', 'aggregate'),
    E('e3', 'aggregate', 'gen'),
    E('e4', 'gen', 'save'),
    E('e5', 'gen', 'notify'),
    E('e6', 'save', 'end'),
    E('e7', 'notify', 'end'),
  ];
  return {
    id: 'builtin:weekly-report-generator',
    name: '周报生成器',
    description: '周五提醒 + 一键生成结构化周报',
    category: 'office',
    categoryName: '办公自动化',
    tags: ['周报', 'AI', 'OA'],
    coverEmoji: '📝',
    author: '喵流官方', usageCount: 2100, rating: 4.9, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 10,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['OA系统', '飞书'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 2. 会议纪要生成 - 上传录音自动生成会议纪要
function mkMeetingMinutes(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', '上传录音', 0, 0, 'trigger', '上传会议录音触发'),
    N('asr', 'http.request', '语音转写', 1, 0, 'action', '调用 ASR 服务转写为文字', httpConfig({ method: 'POST', url: 'https://asr.internal/api/transcribe' })),
    N('analyze', 'ai.llm', 'LLM 分析', 2, 0, 'ai', '分析转写文本生成纪要', llmConfig({
      model: 'gpt-4o',
      prompt: '基于以下会议转写生成结构化纪要:\n{{asr.text}}\n\n输出:\n## 会议概要\n## 关键讨论\n## 决议事项\n## 待办任务(负责人+截止日期)\n## 下次会议',
    })),
    N('extract_tasks', 'ai.llm', '提取任务', 3, 0, 'ai', '从纪要中提取任务', llmConfig({
      prompt: '从纪要中提取所有待办任务，输出 JSON 数组 [{task, owner, dueDate}]:\n{{analyze.output}}',
    })),
    N('create_tasks', 'http.request', '创建任务', 4, 0, 'action', '在任务系统批量创建', httpConfig({ method: 'POST', url: 'https://task.internal/api/tasks/batch' })),
    N('save_minutes', 'http.request', '保存纪要', 4, 1, 'action', '保存到会议库', httpConfig({ method: 'POST', url: 'https://oa.internal/api/meetings' })),
    N('notify', 'notify.feishu', '推送参会人', 5, 0, 'action', '通知所有参会人'),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'asr'),
    E('e2', 'asr', 'analyze'),
    E('e3', 'analyze', 'extract_tasks'),
    E('e4', 'analyze', 'save_minutes'),
    E('e5', 'extract_tasks', 'create_tasks'),
    E('e6', 'create_tasks', 'notify'),
    E('e7', 'save_minutes', 'notify'),
    E('e8', 'notify', 'end'),
  ];
  return {
    id: 'builtin:meeting-minutes',
    name: '会议纪要生成',
    description: '上传录音自动转写 + AI 提取纪要和任务',
    category: 'office',
    categoryName: '办公自动化',
    tags: ['会议', 'ASR', 'AI', '任务'],
    coverEmoji: '🎙️',
    author: '喵流官方', usageCount: 1340, rating: 4.8, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['ASR', '任务系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 3. 请假审批流 - 多级审批 + 自动通知
function mkLeaveApproval(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '请假申请', 0, 0, 'trigger', '员工提交请假申请'),
    N('days_check', 'condition.if', '天数判断', 1, 0, 'control', undefined, ifConfig(
      '{{trigger.days}} <= 3', 'manager_approve', 'director_approve'
    )),
    N('manager_approve', 'notify.feishu', '主管审批', 2, -1, 'action', '推送主管审批'),
    N('director_approve', 'notify.feishu', '总监审批', 2, 1, 'action', '推送总监审批'),
    N('wait', 'http.request', '等待审批', 3, 0, 'action', 'Webhook 等待审批结果', httpConfig({ method: 'POST', url: 'https://oa.internal/api/approvals/wait' })),
    N('check', 'condition.if', '审批结果', 4, 0, 'control', undefined, ifConfig('{{wait.approved}} === true', 'notify_hr', 'notify_reject')),
    N('notify_hr', 'http.request', '通知 HR', 5, 0, 'action', '通知 HR 备案', httpConfig({ method: 'POST', url: 'https://hr.internal/api/leave/record' })),
    N('notify_reject', 'notify.feishu', '驳回通知', 5, 1, 'action', '通知申请人'),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'days_check'),
    EC('e2', 'days_check', 'manager_approve', '≤3天'),
    EC('e3', 'days_check', 'director_approve', '>3天'),
    E('e4', 'manager_approve', 'wait'),
    E('e5', 'director_approve', 'wait'),
    E('e6', 'wait', 'check'),
    EC('e7', 'check', 'notify_hr', '通过'),
    EC('e8', 'check', 'notify_reject', '驳回'),
    E('e9', 'notify_hr', 'end'),
    E('e10', 'notify_reject', 'end'),
  ];
  return {
    id: 'builtin:leave-approval',
    name: '请假审批流',
    description: '多级审批 (≤3天主管 / >3天总监) + HR 备案',
    category: 'office',
    categoryName: '办公自动化',
    tags: ['审批', 'OA', 'HR'],
    coverEmoji: '🏖️',
    author: '喵流官方', usageCount: 1120, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: [], requiredIntegrations: ['OA', 'HR'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 4. 报销审批 - 发票 OCR + 规则校验 + 审批
function mkExpenseApproval(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '提交报销', 0, 0, 'trigger', '员工提交报销单 + 发票照片'),
    N('ocr', 'http.request', '发票 OCR', 1, 0, 'action', '调用 OCR 服务识别发票', httpConfig({ method: 'POST', url: 'https://ocr.internal/api/invoice' })),
    N('validate', 'ai.llm', '合规校验', 2, 0, 'ai', '检查金额、抬头、用途合规', llmConfig({
      prompt: '校验报销合规性:\n发票: {{ocr.result}}\n规则: 餐饮≤100/餐, 打车≤200/次, 住宿≤500/晚。\n输出 {valid, issues, suggestions}',
    })),
    N('branch', 'condition.if', '是否合规', 3, 0, 'control', undefined, ifConfig('{{validate.valid}} === true', 'approve', 'reject')),
    N('approve', 'notify.feishu', '主管审批', 4, 0, 'action', '推送主管'),
    N('reject', 'notify.feishu', '驳回 + 说明', 4, 1, 'action', '通知申请人并说明问题'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'ocr'),
    E('e2', 'ocr', 'validate'),
    E('e3', 'validate', 'branch'),
    EC('e4', 'branch', 'approve', '合规'),
    EC('e5', 'branch', 'reject', '不合规'),
    E('e6', 'approve', 'end'),
    E('e7', 'reject', 'end'),
  ];
  return {
    id: 'builtin:expense-approval',
    name: '智能报销审批',
    description: '发票 OCR + AI 合规校验 + 自动审批',
    category: 'office',
    categoryName: '办公自动化',
    tags: ['报销', 'OCR', 'AI审核'],
    coverEmoji: '💰',
    author: '喵流官方', usageCount: 870, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['OCR', '财务系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 5. 招聘简历筛选 - 自动评估简历匹配度
function mkResumeScreening(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '上传简历', 0, 0, 'trigger', 'HR 上传候选人简历'),
    N('parse', 'http.request', '简历解析', 1, 0, 'action', '调用简历解析服务', httpConfig({ method: 'POST', url: 'https://hr.internal/api/resume/parse' })),
    N('jd', 'knowledge.search', '查询 JD', 2, 0, 'ai', '从知识库检索 JD 要求', {}),
    N('score', 'ai.llm', 'AI 匹配评分', 3, 0, 'ai', '基于 JD 评估候选人', llmConfig({
      model: 'gpt-4o',
      prompt: '评估候选人匹配度:\n简历: {{parse.result}}\nJD: {{jd.context}}\n\n输出 {matchScore: 0-100, strengths, gaps, recommendation: "强烈推荐/推荐/考虑/不推荐"}',
    })),
    N('branch', 'condition.if', '分数判断', 4, 0, 'control', undefined, ifConfig('{{score.matchScore}} >= 70', 'push_interview', 'archive')),
    N('push_interview', 'notify.feishu', '推送面试官', 5, 0, 'action', '推荐给面试官'),
    N('archive', 'http.request', '归档人才库', 5, 1, 'action', '进入人才库', httpConfig({ method: 'POST', url: 'https://hr.internal/api/talent-pool' })),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'parse'),
    E('e2', 'parse', 'jd'),
    E('e3', 'parse', 'score'),
    E('e4', 'jd', 'score'),
    E('e5', 'score', 'branch'),
    EC('e6', 'branch', 'push_interview', '高分'),
    EC('e7', 'branch', 'archive', '低分'),
    E('e8', 'push_interview', 'end'),
    E('e9', 'archive', 'end'),
  ];
  return {
    id: 'builtin:resume-screening',
    name: 'AI 简历筛选',
    description: '简历解析 + JD 匹配评分 + 自动推荐/归档',
    category: 'office',
    categoryName: '办公自动化',
    tags: ['HR', '招聘', 'AI评分'],
    coverEmoji: '👔',
    author: '喵流官方', usageCount: 560, rating: 4.6, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['HR系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 6. 日程提醒 - 每天生成今日日程
function mkDailySchedule(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每日 8 点', 0, 0, 'trigger', '每天 8:00 推送日程'),
    N('fetch', 'http.request', '获取今日日程', 1, 0, 'action', '从日历 API 拉取', httpConfig({ method: 'GET', url: 'https://calendar.internal/api/today' })),
    N('analyze', 'ai.llm', '智能分析', 2, 0, 'ai', '分析日程重要性/冲突/准备事项', llmConfig({
      prompt: '分析今日日程:\n{{fetch.response}}\n\n输出: {priority, conflicts, prepItems}',
    })),
    N('gen', 'ai.llm', '生成提醒', 3, 0, 'ai', '生成友好提醒文案', llmConfig({
      prompt: '基于分析 {{analyze.output}} 生成今日提醒 (Markdown):\n- 重要事项 (按时间)\n- 冲突预警\n- 准备清单',
    })),
    N('push', 'notify.feishu', '推送提醒', 4, 0, 'action', '飞书推送'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'analyze'),
    E('e3', 'analyze', 'gen'),
    E('e4', 'gen', 'push'),
    E('e5', 'push', 'end'),
  ];
  return {
    id: 'builtin:daily-schedule',
    name: '每日日程提醒',
    description: '每天 8 点自动分析日程 + 智能提醒',
    category: 'office',
    categoryName: '办公自动化',
    tags: ['日程', '提醒', 'AI'],
    coverEmoji: '📅',
    author: '喵流官方', usageCount: 1480, rating: 4.8, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 10,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['日历', '飞书'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 7. 合同审核 - 上传合同自动审查风险点
function mkContractReview(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '上传合同', 0, 0, 'trigger', '上传合同 PDF'),
    N('parse', 'http.request', '合同解析', 1, 0, 'action', 'OCR + 文本提取', httpConfig({ method: 'POST', url: 'https://ocr.internal/api/contract' })),
    N('split', 'code.transform', '条款拆分', 2, 0, 'transform', '按条款拆分'),
    N('review', 'ai.llm', 'AI 风险审查', 3, 0, 'ai', '逐条审查风险', llmConfig({
      model: 'gpt-4o',
      prompt: '审查合同条款风险:\n{{split.clauses}}\n\n关注: 付款条款、违约责任、知识产权、争议解决、保密条款。\n输出: [{clause, riskLevel: low/medium/high, issue, suggestion}]',
    })),
    N('summary', 'ai.llm', '生成摘要', 4, 0, 'ai', '生成审查报告', llmConfig({
      prompt: '基于审查结果 {{review.output}} 生成结构化报告 (Markdown)。',
    })),
    N('high_risk', 'condition.if', '高风险判断', 5, 0, 'control', undefined, ifConfig('风险等级 === high', 'alert', 'save')),
    N('alert', 'notify.feishu', '法务告警', 6, 0, 'action', '通知法务紧急审查'),
    N('save', 'http.request', '归档合同', 6, 1, 'action', '归档到合同库', httpConfig({ method: 'POST', url: 'https://contract.internal/api/save' })),
    N('end', 'end.aggregator', '结束', 7, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'parse'),
    E('e2', 'parse', 'split'),
    E('e3', 'split', 'review'),
    E('e4', 'review', 'summary'),
    E('e5', 'summary', 'high_risk'),
    EC('e6', 'high_risk', 'alert', '高风险'),
    EC('e7', 'high_risk', 'save', '低风险'),
    E('e8', 'alert', 'end'),
    E('e9', 'save', 'end'),
  ];
  return {
    id: 'builtin:contract-review',
    name: '合同风险审核',
    description: '上传合同 + AI 审查条款风险 + 高风险自动告警',
    category: 'office',
    categoryName: '办公自动化',
    tags: ['合同', '法务', 'AI', 'OCR'],
    coverEmoji: '📜',
    author: '喵流官方', usageCount: 340, rating: 4.9, isOfficial: true,
    difficulty: 'hard', estimatedSetupMinutes: 30,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['OCR', '合同系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 8. 智能数据报表 - 数据库查询 + 报表生成
function mkDataReport(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每周一', 0, 0, 'trigger', '每周一 9:00 生成周报'),
    N('sql_gen', 'ai.llm', 'SQL 生成', 1, 0, 'ai', '根据自然语言生成 SQL', llmConfig({
      prompt: '生成查询上周数据的 SQL:\n表: orders(id, user_id, amount, created_at, status)\n指标: 订单数、GMV、客单价、新增用户、转化率',
    })),
    N('query', 'http.request', '执行查询', 2, 0, 'action', '执行 SQL', httpConfig({ method: 'POST', url: 'https://bi.internal/api/sql/exec' })),
    N('analyze', 'ai.llm', 'AI 分析', 3, 0, 'ai', '分析数据趋势/异常', llmConfig({
      prompt: '分析数据:\n{{query.data}}\n\n输出: {trends, anomalies, insights}',
    })),
    N('viz', 'ai.llm', '生成图表配置', 4, 0, 'ai', '生成 ECharts 配置', llmConfig({
      prompt: '基于数据生成 ECharts 配置: {{query.data}}',
    })),
    N('save', 'http.request', '保存报表', 5, 0, 'action', '保存报表', httpConfig({ method: 'POST', url: 'https://bi.internal/api/reports' })),
    N('notify', 'notify.feishu', '推送订阅人', 5, 1, 'action', '通知订阅人'),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'sql_gen'),
    E('e2', 'sql_gen', 'query'),
    E('e3', 'query', 'analyze'),
    E('e4', 'query', 'viz'),
    E('e5', 'analyze', 'save'),
    E('e6', 'viz', 'save'),
    E('e7', 'save', 'notify'),
    E('e8', 'notify', 'end'),
  ];
  return {
    id: 'builtin:data-report',
    name: '智能数据报表',
    description: '自然语言生成 SQL + AI 分析 + 自动生成图表',
    category: 'office',
    categoryName: '办公自动化',
    tags: ['数据', '报表', 'SQL生成'],
    coverEmoji: '📈',
    author: '喵流官方', usageCount: 720, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['BI'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

export const OFFICE_TEMPLATES: BuiltinTemplate[] = [
  mkWeeklyReport(),
  mkMeetingMinutes(),
  mkLeaveApproval(),
  mkExpenseApproval(),
  mkResumeScreening(),
  mkDailySchedule(),
  mkContractReview(),
  mkDataReport(),
];

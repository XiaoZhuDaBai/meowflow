/**
 * 教育 + 医疗 + 开发者 + 客服剩余模板 (共 16 个)
 */

import type { BuiltinTemplate, BuiltinNode, BuiltinEdge } from '../builtinTemplates';
import { N, E, EC, EL, llmConfig, httpConfig, codeConfig, ifConfig } from '../templateBuilders';

// ====================== 教育场景 (6 个) ======================

// 1. 作业批改
function mkHomeworkGrading(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '学生提交', 0, 0, 'trigger', '提交作业照片 + 文本'),
    N('ocr', 'http.request', 'OCR 识别', 1, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://ocr.internal/api/handwriting' })),
    N('grade', 'ai.llm', 'AI 批改', 2, 0, 'ai', undefined, llmConfig({
      model: 'gpt-4o',
      prompt: '批改作业:\n题目: {{trigger.problem}}\n答案: {{ocr.text}}\n\n输出: {score, corrections, suggestions, encouragement}',
    })),
    N('save', 'http.request', '保存成绩', 3, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://edu.internal/api/homework' })),
    N('feedback', 'notify.email', '反馈学生', 3, 1, 'action', '发送邮件给学生'),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'ocr'),
    E('e2', 'ocr', 'grade'),
    E('e3', 'grade', 'save'),
    E('e4', 'grade', 'feedback'),
    E('e5', 'save', 'end'),
    E('e6', 'feedback', 'end'),
  ];
  return {
    id: 'builtin:homework-grading', name: 'AI 作业批改',
    description: 'OCR 识别 + AI 批改打分 + 个性化反馈',
    category: 'education', categoryName: '教育场景',
    tags: ['教育', 'OCR', 'AI'], coverEmoji: '✏️',
    author: '喵流官方', usageCount: 780, rating: 4.8, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['OCR', '教务系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 2. 试卷生成
function mkExamGenerator(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '出卷需求', 0, 0, 'trigger', '学科/年级/难度/题型'),
    N('gen', 'ai.llm', '生成试卷', 1, 0, 'ai', undefined, llmConfig({
      model: 'gpt-4o',
      prompt: '基于以下要求生成试卷:\n{{trigger.formData}}\n\n输出 Markdown 格式试卷 + 答案。',
    })),
    N('save', 'http.request', '保存试卷', 2, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://edu.internal/api/exams' })),
    N('end', 'end.aggregator', '结束', 3, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'gen'),
    E('e2', 'gen', 'save'),
    E('e3', 'save', 'end'),
  ];
  return {
    id: 'builtin:exam-generator', name: 'AI 试卷生成',
    description: '基于学科/难度要求自动生成试卷',
    category: 'education', categoryName: '教育场景',
    tags: ['试卷', 'AI', '出题'], coverEmoji: '📝',
    author: '喵流官方', usageCount: 640, rating: 4.7, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 10,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['教务系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 3. 智能问答 - 学科辅导
function mkTutoringBot(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.imessage', '学生提问', 0, 0, 'trigger'),
    N('classify', 'ai.llm', '学科分类', 1, 0, 'ai', undefined, llmConfig({
      prompt: '判断学生问题的学科和知识点:\n{{trigger.text}}\n输出: {subject, knowledgePoint, difficulty}',
    })),
    N('search', 'knowledge.search', '检索教辅', 2, 0, 'ai', undefined, {}),
    N('answer', 'ai.llm', 'AI 解答', 3, 0, 'ai', undefined, llmConfig({
      prompt: '作为 {{classify.subject}} 老师，详细解答:\n问题: {{trigger.text}}\n参考: {{search.context}}\n输出步骤化解答。',
    })),
    N('send', 'notify.feishu', '回复学生', 4, 0, 'action'),
    N('log', 'http.request', '记录学习轨迹', 4, 1, 'action', undefined, httpConfig({ method: 'POST', url: 'https://edu.internal/api/learning-log' })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'classify'),
    E('e2', 'classify', 'search'),
    E('e3', 'search', 'answer'),
    E('e4', 'answer', 'send'),
    E('e5', 'answer', 'log'),
    E('e6', 'send', 'end'),
    E('e7', 'log', 'end'),
  ];
  return {
    id: 'builtin:tutoring-bot', name: '学科辅导机器人',
    description: '学生提问 + 学科分类 + AI 解答',
    category: 'education', categoryName: '教育场景',
    tags: ['辅导', 'AI', '问答'], coverEmoji: '🤖',
    author: '喵流官方', usageCount: 1280, rating: 4.9, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['IM', '教务系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 4. 学习路径推荐
function mkLearningPath(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '学生画像', 0, 0, 'trigger'),
    N('fetch_history', 'http.request', '历史成绩', 1, 0, 'action'),
    N('gen', 'ai.llm', '生成路径', 2, 0, 'ai', undefined, llmConfig({
      prompt: '基于学生画像 {{trigger.formData}} 和历史成绩 {{fetch_history.response}} 生成 30 天学习路径。',
    })),
    N('create_plan', 'http.request', '创建计划', 3, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://edu.internal/api/learning-plan' })),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch_history'),
    E('e2', 'fetch_history', 'gen'),
    E('e3', 'gen', 'create_plan'),
    E('e4', 'create_plan', 'end'),
  ];
  return {
    id: 'builtin:learning-path', name: '学习路径推荐',
    description: '基于学生画像生成个性化学习路径',
    category: 'education', categoryName: '教育场景',
    tags: ['学习路径', 'AI'], coverEmoji: '🗺️',
    author: '喵流官方', usageCount: 320, rating: 4.6, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['教务系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 5. 教研分析
function mkTeachingAnalysis(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每月末', 0, 0, 'trigger'),
    N('fetch_data', 'http.request', '拉取教学数据', 1, 0, 'action'),
    N('analyze', 'ai.llm', 'AI 教研分析', 2, 0, 'ai', undefined, llmConfig({
      prompt: '分析本月教学数据:\n{{fetch_data.response}}\n输出: {教学亮点, 待改进, 教研建议}',
    })),
    N('gen', 'ai.llm', '生成报告', 3, 0, 'ai', undefined, llmConfig({ prompt: '基于分析 {{analyze.output}} 生成月度教研报告。' })),
    N('notify', 'notify.feishu', '通知教研组长', 4, 0, 'action'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch_data'),
    E('e2', 'fetch_data', 'analyze'),
    E('e3', 'analyze', 'gen'),
    E('e4', 'gen', 'notify'),
    E('e5', 'notify', 'end'),
  ];
  return {
    id: 'builtin:teaching-analysis', name: '教研分析报告',
    description: '每月自动生成教研分析报告',
    category: 'education', categoryName: '教育场景',
    tags: ['教研', 'AI'], coverEmoji: '📊',
    author: '喵流官方', usageCount: 180, rating: 4.5, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['教务系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 6. 知识点图谱构建
function mkKnowledgeGraph(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '学科教材', 0, 0, 'trigger', '上传教材'),
    N('parse', 'http.request', '解析教材', 1, 0, 'action'),
    N('extract', 'ai.llm', '抽取知识点', 2, 0, 'ai', undefined, llmConfig({
      prompt: '从教材 {{parse.text}} 中抽取所有知识点及其关系。\n输出 JSON: {nodes: [{name, importance}], edges: [{from, to, type}]}',
    })),
    N('save', 'http.request', '保存图谱', 3, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://kg.internal/api/save' })),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'parse'),
    E('e2', 'parse', 'extract'),
    E('e3', 'extract', 'save'),
    E('e4', 'save', 'end'),
  ];
  return {
    id: 'builtin:knowledge-graph', name: '知识点图谱',
    description: '从教材自动构建知识点图谱',
    category: 'education', categoryName: '教育场景',
    tags: ['知识图谱', 'AI'], coverEmoji: '🕸️',
    author: '喵流官方', usageCount: 140, rating: 4.7, isOfficial: true,
    difficulty: 'hard', estimatedSetupMinutes: 30,
    requiredModels: ['gpt-4o'], requiredIntegrations: [],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// ====================== 医疗场景 (4 个) ======================

// 1. 病例摘要
function mkMedicalSummary(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '上传病历', 0, 0, 'trigger'),
    N('extract', 'ai.llm', '病例摘要', 1, 0, 'ai', undefined, llmConfig({
      prompt: '生成结构化病例摘要:\n{{trigger.content}}\n输出: {主诉, 病史, 检查, 诊断, 治疗, 医嘱}',
    })),
    N('save', 'http.request', '归档病历', 2, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://his.internal/api/summary' })),
    N('end', 'end.aggregator', '结束', 3, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'extract'),
    E('e2', 'extract', 'save'),
    E('e3', 'save', 'end'),
  ];
  return {
    id: 'builtin:medical-summary', name: '病例摘要生成',
    description: '病历自动结构化摘要',
    category: 'medical', categoryName: '医疗场景',
    tags: ['医疗', 'AI', '病例'], coverEmoji: '🏥',
    author: '喵流官方', usageCount: 220, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['HIS'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 2. 药品咨询
function mkDrugInquiry(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.imessage', '患者咨询', 0, 0, 'trigger'),
    N('search', 'knowledge.search', '检索药品库', 1, 0, 'ai', undefined, {}),
    N('answer', 'ai.llm', 'AI 解答', 2, 0, 'ai', undefined, llmConfig({
      prompt: '作为药师回答:\n问题: {{trigger.text}}\n参考: {{search.context}}\n注意: 不能替代医生。',
    })),
    N('send', 'notify.feishu', '回复患者', 3, 0, 'action'),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'search'),
    E('e2', 'search', 'answer'),
    E('e3', 'answer', 'send'),
    E('e4', 'send', 'end'),
  ];
  return {
    id: 'builtin:drug-inquiry', name: '药品咨询机器人',
    description: '患者咨询 + 药品知识库 + AI 解答',
    category: 'medical', categoryName: '医疗场景',
    tags: ['医疗', 'AI', '药品'], coverEmoji: '💊',
    author: '喵流官方', usageCount: 340, rating: 4.6, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['IM'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 3. 影像初筛
function mkImagePreScreening(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.form', '上传影像', 0, 0, 'trigger'),
    N('ai', 'ai.llm', 'AI 初筛', 1, 0, 'ai', '多模态 AI 影像分析', llmConfig({
      prompt: '分析以下医学影像:\n{{trigger.image}}\n输出: {finding, suspicious, recommendation}',
    })),
    N('branch', 'condition.if', '可疑判断', 2, 0, 'control', undefined, ifConfig('{{ai.suspicious}}', 'review', 'archive')),
    N('review', 'notify.feishu', '推送医生', 3, 0, 'action'),
    N('archive', 'http.request', '归档', 3, 1, 'action'),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'ai'),
    E('e2', 'ai', 'branch'),
    EC('e3', 'branch', 'review', '可疑'),
    EC('e4', 'branch', 'archive', '正常'),
    E('e5', 'review', 'end'),
    E('e6', 'archive', 'end'),
  ];
  return {
    id: 'builtin:image-pre-screening', name: '医学影像初筛',
    description: 'AI 影像分析 + 可疑结果推医生',
    category: 'medical', categoryName: '医疗场景',
    tags: ['医疗', 'AI', '影像'], coverEmoji: '🩻',
    author: '喵流官方', usageCount: 110, rating: 4.5, isOfficial: true,
    difficulty: 'hard', estimatedSetupMinutes: 30,
    requiredModels: ['gpt-4o-vision'], requiredIntegrations: ['PACS'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 4. 随访提醒
function mkFollowUpReminder(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每日 9 点', 0, 0, 'trigger'),
    N('fetch', 'http.request', '获取待随访', 1, 0, 'action'),
    N('gen', 'ai.llm', '生成提醒话术', 2, 0, 'ai', undefined, llmConfig({
      prompt: '为患者 {{$item.name}} 生成个性化随访提醒:\n病种: {{$item.disease}}\n距离上次: {{$item.days}} 天\n输出温和、专业的提醒。',
    })),
    N('send', 'notify.sms', '发送短信', 3, 0, 'action'),
    N('log', 'http.request', '记录随访', 3, 1, 'action'),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    EL('e2', 'fetch', 'gen'),
    E('e3', 'gen', 'send'),
    E('e4', 'send', 'log'),
    EL('e5', 'log', 'fetch'),
    E('e6', 'log', 'end'),
  ];
  return {
    id: 'builtin:follow-up-reminder', name: '患者随访提醒',
    description: '每日自动提醒患者随访',
    category: 'medical', categoryName: '医疗场景',
    tags: ['随访', 'AI'], coverEmoji: '📞',
    author: '喵流官方', usageCount: 280, rating: 4.6, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 15,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['HIS', '短信'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// ====================== 开发者工具 (6 个) ======================

// 1. CI/CD 流水线
function mkCICDPipeline(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', 'Git Push', 0, 0, 'trigger', 'Git 推送触发'),
    N('checkout', 'http.request', '拉取代码', 1, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://ci.internal/api/checkout' })),
    N('lint', 'http.request', '代码检查', 2, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://ci.internal/api/lint' })),
    N('branch', 'condition.if', '检查通过', 3, 0, 'control', undefined, ifConfig('{{lint.passed}}', 'test', 'notify_fail')),
    N('test', 'http.request', '运行测试', 4, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://ci.internal/api/test' })),
    N('branch2', 'condition.if', '测试通过', 5, 0, 'control', undefined, ifConfig('{{test.passed}}', 'build', 'notify_fail')),
    N('build', 'http.request', '构建镜像', 6, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://ci.internal/api/build' })),
    N('deploy', 'http.request', '部署', 7, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://ci.internal/api/deploy' })),
    N('notify_fail', 'notify.feishu', '失败通知', 8, -1, 'action'),
    N('notify_ok', 'notify.feishu', '成功通知', 8, 0, 'action'),
    N('end', 'end.aggregator', '结束', 9, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'checkout'),
    E('e2', 'checkout', 'lint'),
    E('e3', 'lint', 'branch'),
    EC('e4', 'branch', 'test', '通过'),
    EC('e5', 'branch', 'notify_fail', '失败'),
    E('e6', 'test', 'branch2'),
    EC('e7', 'branch2', 'build', '通过'),
    EC('e8', 'branch2', 'notify_fail', '失败'),
    E('e9', 'build', 'deploy'),
    E('e10', 'deploy', 'notify_ok'),
    E('e11', 'notify_ok', 'end'),
    E('e12', 'notify_fail', 'end'),
  ];
  return {
    id: 'builtin:cicd-pipeline', name: 'CI/CD 流水线',
    description: '代码检查 + 测试 + 构建 + 部署自动化',
    category: 'developer', categoryName: '开发者工具',
    tags: ['CI/CD', 'DevOps'], coverEmoji: '🔧',
    author: '喵流官方', usageCount: 920, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: [], requiredIntegrations: ['Git', 'CI'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 2. 代码审查
function mkCodeReview(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', 'PR 创建', 0, 0, 'trigger'),
    N('fetch', 'http.request', '获取 diff', 1, 0, 'action'),
    N('review', 'ai.llm', 'AI 代码审查', 2, 0, 'ai', undefined, llmConfig({
      model: 'gpt-4o',
      prompt: '审查以下代码 diff:\n{{fetch.diff}}\n关注: bug、性能、安全、可读性、命名。\n输出: {comments: [{line, severity, message}]}',
    })),
    N('comment', 'http.request', '提交评论', 3, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://github.internal/api/comments' })),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'review'),
    E('e3', 'review', 'comment'),
    E('e4', 'comment', 'end'),
  ];
  return {
    id: 'builtin:code-review', name: 'AI 代码审查',
    description: 'PR 创建自动 AI 审查 + 提交评论',
    category: 'developer', categoryName: '开发者工具',
    tags: ['代码审查', 'AI'], coverEmoji: '🔍',
    author: '喵流官方', usageCount: 1450, rating: 4.8, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['GitHub'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 3. Bug 自动分类
function mkBugTriage(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', 'Bug 提交', 0, 0, 'trigger'),
    N('analyze', 'ai.llm', 'Bug 分析', 1, 0, 'ai', undefined, llmConfig({
      prompt: '分析 Bug 报告:\n{{trigger.body}}\n输出: {severity, category, component, suggestedAssignee}',
    })),
    N('assign', 'http.request', '分配', 2, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://jira.internal/api/assign' })),
    N('end', 'end.aggregator', '结束', 3, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'analyze'),
    E('e2', 'analyze', 'assign'),
    E('e3', 'assign', 'end'),
  ];
  return {
    id: 'builtin:bug-triage', name: 'Bug 自动分类',
    description: 'Bug 提交自动分类/分级/分配',
    category: 'developer', categoryName: '开发者工具',
    tags: ['Bug', 'AI'], coverEmoji: '🐞',
    author: '喵流官方', usageCount: 540, rating: 4.6, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 15,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['Jira'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 4. API 自动测试
function mkApiTest(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每日凌晨', 0, 0, 'trigger'),
    N('gen_cases', 'ai.llm', '生成测试用例', 1, 0, 'ai', undefined, llmConfig({
      prompt: '基于 OpenAPI 规范生成测试用例:\n{{trigger.openapi}}\n包含正常/异常/边界场景。',
    })),
    N('run', 'http.request', '执行测试', 2, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://test.internal/api/run' })),
    N('report', 'ai.llm', 'AI 分析失败', 3, 0, 'ai', undefined, llmConfig({
      prompt: '分析失败用例:\n{{run.failures}}\n输出可能根因。',
    })),
    N('branch', 'condition.if', '失败判断', 4, 0, 'control', undefined, ifConfig('有失败', 'alert', 'end')),
    N('alert', 'notify.feishu', '测试失败告警', 5, 0, 'action'),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'gen_cases'),
    E('e2', 'gen_cases', 'run'),
    E('e3', 'run', 'report'),
    E('e4', 'report', 'branch'),
    EC('e5', 'branch', 'alert', '有失败'),
    EC('e6', 'branch', 'end', '全部通过'),
    E('e7', 'alert', 'end'),
  ];
  return {
    id: 'builtin:api-auto-test', name: 'API 自动化测试',
    description: '每日自动生成测试用例 + 执行 + 失败分析',
    category: 'developer', categoryName: '开发者工具',
    tags: ['测试', 'AI'], coverEmoji: '🧪',
    author: '喵流官方', usageCount: 380, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['测试平台'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 5. 技术文档生成
function mkDocGenerator(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', '代码提交', 0, 0, 'trigger'),
    N('parse', 'http.request', '解析代码', 1, 0, 'action'),
    N('gen', 'ai.llm', '生成文档', 2, 0, 'ai', undefined, llmConfig({
      prompt: '基于代码 {{parse.code}} 生成 Markdown 技术文档 (用途、参数、示例、注意事项)。',
    })),
    N('commit', 'http.request', '提交文档', 3, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://github.internal/api/docs' })),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'parse'),
    E('e2', 'parse', 'gen'),
    E('e3', 'gen', 'commit'),
    E('e4', 'commit', 'end'),
  ];
  return {
    id: 'builtin:doc-generator', name: '技术文档生成',
    description: '代码提交自动生成技术文档',
    category: 'developer', categoryName: '开发者工具',
    tags: ['文档', 'AI'], coverEmoji: '📚',
    author: '喵流官方', usageCount: 420, rating: 4.7, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['Git'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 6. 依赖安全扫描
function mkDependencyScan(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每周一', 0, 0, 'trigger'),
    N('scan', 'http.request', '扫描依赖', 1, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://sca.internal/api/scan' })),
    N('branch', 'condition.if', '漏洞判断', 2, 0, 'control', undefined, ifConfig('{{scan.vulns.length}} > 0', 'alert', 'log')),
    N('alert', 'notify.feishu', '漏洞告警', 3, 0, 'action'),
    N('gen_fix', 'ai.llm', '生成修复建议', 3, 1, 'ai', undefined, llmConfig({
      prompt: '基于漏洞 {{scan.vulns}} 生成升级建议和补丁方案。',
    })),
    N('log', 'code.transform', '记录结果', 3, 2, 'transform'),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'scan'),
    E('e2', 'scan', 'branch'),
    EC('e3', 'branch', 'alert', '有漏洞'),
    EC('e4', 'branch', 'log', '无漏洞'),
    E('e5', 'alert', 'gen_fix'),
    E('e6', 'gen_fix', 'end'),
    E('e7', 'log', 'end'),
  ];
  return {
    id: 'builtin:dependency-scan', name: '依赖安全扫描',
    description: '每周自动扫描依赖漏洞 + 修复建议',
    category: 'developer', categoryName: '开发者工具',
    tags: ['安全', '依赖'], coverEmoji: '🔒',
    author: '喵流官方', usageCount: 290, rating: 4.5, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['SCA'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// ====================== 客服场景补充 (7 个) ======================

// 1. 智能工单分配
function mkTicketAssign(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', '工单创建', 0, 0, 'trigger'),
    N('classify', 'ai.llm', '工单分类', 1, 0, 'ai', undefined, llmConfig({
      prompt: '分类工单:\n{{trigger.body}}\n输出: {category, priority, skills}',
    })),
    N('route', 'http.request', '分配坐席', 2, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://cs.internal/api/assign' })),
    N('notify', 'notify.feishu', '通知坐席', 3, 0, 'action'),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'classify'),
    E('e2', 'classify', 'route'),
    E('e3', 'route', 'notify'),
    E('e4', 'notify', 'end'),
  ];
  return {
    id: 'builtin:ticket-assign', name: '智能工单分配',
    description: '工单自动分类 + 智能分配',
    category: 'customer-service', categoryName: '客服场景',
    tags: ['工单', 'AI'], coverEmoji: '🎫',
    author: '喵流官方', usageCount: 680, rating: 4.6, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 15,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['工单系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 2. 多轮对话客服
function mkMultiTurnChat(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.imessage', '客户消息', 0, 0, 'trigger'),
    N('fetch_history', 'http.request', '查询历史', 1, 0, 'action', '查询该客户历史会话'),
    N('search', 'knowledge.search', '检索知识库', 2, 0, 'ai', undefined, {}),
    N('answer', 'ai.llm', '生成回复', 3, 0, 'ai', undefined, llmConfig({
      prompt: '你是专业客服，结合历史 {{fetch_history.response}} 和知识 {{search.context}} 回答:\n客户: {{trigger.text}}',
    })),
    N('check', 'condition.if', '是否能答', 4, 0, 'control', undefined, ifConfig('能回答', 'send', 'human')),
    N('send', 'notify.feishu', '回复客户', 5, 0, 'action'),
    N('human', 'http.request', '转人工', 5, 1, 'action', undefined, httpConfig({ method: 'POST', url: 'https://cs.internal/api/human-handoff' })),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch_history'),
    E('e2', 'fetch_history', 'search'),
    E('e3', 'search', 'answer'),
    E('e4', 'answer', 'check'),
    EC('e5', 'check', 'send', '能答'),
    EC('e6', 'check', 'human', '不能答'),
    E('e7', 'send', 'end'),
    E('e8', 'human', 'end'),
  ];
  return {
    id: 'builtin:multi-turn-chat', name: '多轮对话客服',
    description: '多轮上下文 + 知识库 + 智能回复或转人工',
    category: 'customer-service', categoryName: '客服场景',
    tags: ['客服', '多轮', 'AI'], coverEmoji: '💬',
    author: '喵流官方', usageCount: 1120, rating: 4.8, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['IM'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 3. 满意度调研
function mkCSATSurvey(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.webhook', '会话结束', 0, 0, 'trigger'),
    N('wait', 'http.request', '等待 10 分钟', 1, 0, 'action', '等待冷却'),
    N('send', 'http.request', '发送调研', 2, 0, 'action', undefined, httpConfig({ method: 'POST', url: 'https://cs.internal/api/survey/send' })),
    N('wait_reply', 'http.request', '等待回复', 3, 0, 'action'),
    N('analyze', 'ai.llm', 'AI 分析', 4, 0, 'ai', undefined, llmConfig({
      prompt: '分析调研反馈:\n{{wait_reply.response}}\n输出: {score, issues, suggestions}',
    })),
    N('save', 'http.request', '归档结果', 5, 0, 'action'),
    N('low_score', 'condition.if', '低分判断', 5, 1, 'control', undefined, ifConfig('{{analyze.score}} < 3', 'alert', 'end')),
    N('alert', 'notify.feishu', '低分告警', 6, 0, 'action'),
    N('end', 'end.aggregator', '结束', 7, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'wait'),
    E('e2', 'wait', 'send'),
    E('e3', 'send', 'wait_reply'),
    E('e4', 'wait_reply', 'analyze'),
    E('e5', 'analyze', 'save'),
    E('e6', 'analyze', 'low_score'),
    EC('e7', 'low_score', 'alert', '低分'),
    EC('e8', 'low_score', 'end', '正常'),
    E('e9', 'alert', 'end'),
    E('e10', 'save', 'end'),
  ];
  return {
    id: 'builtin:csat-survey', name: '满意度调研',
    description: '会话结束后自动调研 + 低分告警',
    category: 'customer-service', categoryName: '客服场景',
    tags: ['客服', '调研'], coverEmoji: '⭐',
    author: '喵流官方', usageCount: 480, rating: 4.5, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 20,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['客服系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 4. 坐席质检
function mkAgentQA(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每日', 0, 0, 'trigger'),
    N('fetch', 'http.request', '拉取录音', 1, 0, 'action'),
    N('asr', 'http.request', '语音转写', 2, 0, 'action'),
    N('qa', 'ai.llm', 'AI 质检', 3, 0, 'ai', undefined, llmConfig({
      prompt: '质检会话:\n{{asr.text}}\n评估: 服务态度、合规性、专业度。\n输出: {score, issues}',
    })),
    N('branch', 'condition.if', '问题判断', 4, 0, 'control', undefined, ifConfig('{{qa.issues.length}} > 0', 'alert', 'archive')),
    N('alert', 'notify.feishu', '告警主管', 5, 0, 'action'),
    N('archive', 'http.request', '归档', 5, 1, 'action'),
    N('end', 'end.aggregator', '结束', 6, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'asr'),
    E('e3', 'asr', 'qa'),
    E('e4', 'qa', 'branch'),
    EC('e5', 'branch', 'alert', '有问题'),
    EC('e6', 'branch', 'archive', '正常'),
    E('e7', 'alert', 'end'),
    E('e8', 'archive', 'end'),
  ];
  return {
    id: 'builtin:agent-qa', name: '坐席质检',
    description: '每日自动质检 + AI 评估 + 问题告警',
    category: 'customer-service', categoryName: '客服场景',
    tags: ['客服', '质检', 'AI'], coverEmoji: '🎧',
    author: '喵流官方', usageCount: 340, rating: 4.7, isOfficial: true,
    difficulty: 'hard', estimatedSetupMinutes: 30,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['ASR', '客服系统'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 5. 知识库自动更新
function mkKBUpdater(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '每周', 0, 0, 'trigger'),
    N('fetch', 'http.request', '拉取高频问题', 1, 0, 'action'),
    N('cluster', 'ai.llm', '问题聚类', 2, 0, 'ai', undefined, llmConfig({
      prompt: '聚类高频问题:\n{{fetch.questions}}\n输出 [{cluster, sample, count}]',
    })),
    N('gen', 'ai.llm', '生成答案', 3, 0, 'ai', undefined, llmConfig({
      prompt: '为每个聚类生成标准答案。',
    })),
    N('review', 'notify.feishu', '人工审核', 4, 0, 'action'),
    N('save', 'http.request', '入库', 4, 1, 'action', undefined, httpConfig({ method: 'POST', url: 'https://kb.internal/api/docs' })),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    E('e2', 'fetch', 'cluster'),
    E('e3', 'cluster', 'gen'),
    E('e4', 'gen', 'review'),
    E('e5', 'gen', 'save'),
    E('e6', 'review', 'end'),
    E('e7', 'save', 'end'),
  ];
  return {
    id: 'builtin:kb-auto-updater', name: '知识库自动更新',
    description: '从高频问题聚类自动生成知识库条目',
    category: 'customer-service', categoryName: '客服场景',
    tags: ['知识库', 'AI'], coverEmoji: '📚',
    author: '喵流官方', usageCount: 290, rating: 4.6, isOfficial: true,
    difficulty: 'medium', estimatedSetupMinutes: 25,
    requiredModels: ['gpt-4o'], requiredIntegrations: ['客服系统', 'KB'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 6. 客户回访
function mkCustomerRecall(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.cron', '购买后 7 天', 0, 0, 'trigger'),
    N('fetch', 'http.request', '获取订单', 1, 0, 'action'),
    N('gen', 'ai.llm', '生成话术', 2, 0, 'ai', undefined, llmConfig({
      prompt: '为客户 {{$item.userId}} 生成回访话术:\n订单: {{$item.order}}\n要求: 友好、询问使用感受、推荐相关产品。',
    })),
    N('send', 'notify.sms', '发送回访', 3, 0, 'action'),
    N('log', 'http.request', '记录回访', 3, 1, 'action'),
    N('end', 'end.aggregator', '结束', 4, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'fetch'),
    EL('e2', 'fetch', 'gen'),
    E('e3', 'gen', 'send'),
    E('e4', 'send', 'log'),
    EL('e5', 'log', 'fetch'),
    E('e6', 'log', 'end'),
  ];
  return {
    id: 'builtin:customer-recall', name: '智能客户回访',
    description: '购买后自动回访 + 个性化话术',
    category: 'customer-service', categoryName: '客服场景',
    tags: ['回访', 'AI'], coverEmoji: '📞',
    author: '喵流官方', usageCount: 420, rating: 4.5, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 15,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['订单系统', '短信'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

// 7. FAQ 智能检索
function mkSmartFAQ(): BuiltinTemplate {
  const nodes: BuiltinNode[] = [
    N('trigger', 'trigger.imessage', '客户问题', 0, 0, 'trigger'),
    N('search', 'knowledge.search', 'FAQ 检索', 1, 0, 'ai', undefined, {}),
    N('branch', 'condition.if', '命中判断', 2, 0, 'control', undefined, ifConfig('{{search.score}} > 阈值', 'answer', 'human')),
    N('answer', 'ai.llm', '基于 FAQ 回答', 3, 0, 'ai', undefined, llmConfig({
      prompt: '基于 FAQ {{search.context}} 回答:\n问题: {{trigger.text}}',
    })),
    N('human', 'http.request', '转人工', 3, 1, 'action'),
    N('send', 'notify.feishu', '回复客户', 4, 0, 'action'),
    N('end', 'end.aggregator', '结束', 5, 0, 'end'),
  ];
  const edges: BuiltinEdge[] = [
    E('e1', 'trigger', 'search'),
    E('e2', 'search', 'branch'),
    EC('e3', 'branch', 'answer', '命中'),
    EC('e4', 'branch', 'human', '未命中'),
    E('e5', 'answer', 'send'),
    E('e6', 'human', 'send'),
    E('e7', 'send', 'end'),
  ];
  return {
    id: 'builtin:smart-faq', name: '智能 FAQ',
    description: '客户问题智能检索 FAQ，未命中转人工',
    category: 'customer-service', categoryName: '客服场景',
    tags: ['FAQ', 'AI'], coverEmoji: '❓',
    author: '喵流官方', usageCount: 1620, rating: 4.8, isOfficial: true,
    difficulty: 'easy', estimatedSetupMinutes: 15,
    requiredModels: ['gpt-4o-mini'], requiredIntegrations: ['IM', 'KB'],
    createdAt: '2026-09-01T00:00:00Z',
  } as BuiltinTemplate;
}

export const EDUCATION_TEMPLATES: BuiltinTemplate[] = [
  mkHomeworkGrading(), mkExamGenerator(), mkTutoringBot(),
  mkLearningPath(), mkTeachingAnalysis(), mkKnowledgeGraph(),
];

export const MEDICAL_TEMPLATES: BuiltinTemplate[] = [
  mkMedicalSummary(), mkDrugInquiry(), mkImagePreScreening(), mkFollowUpReminder(),
];

export const DEVELOPER_TEMPLATES: BuiltinTemplate[] = [
  mkCICDPipeline(), mkCodeReview(), mkBugTriage(),
  mkApiTest(), mkDocGenerator(), mkDependencyScan(),
];

export const CUSTOMER_SERVICE_EXTRA_TEMPLATES: BuiltinTemplate[] = [
  mkTicketAssign(), mkMultiTurnChat(), mkCSATSurvey(),
  mkAgentQA(), mkKBUpdater(), mkCustomerRecall(), mkSmartFAQ(),
];

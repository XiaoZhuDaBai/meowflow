/**
 * 模板分类元数据 (Template Categories)
 *
 * 8 大行业分类，共 50+ 模板
 */

export interface TemplateCategoryMeta {
  code: string;
  name: string;
  emoji: string;
  description: string;
  color: string;
}

export const TEMPLATE_CATEGORIES: TemplateCategoryMeta[] = [
  {
    code: 'customer-service',
    name: '客服场景',
    emoji: '🤖',
    description: '智能客服、工单分配、FAQ 检索等',
    color: '#6366f1',
  },
  {
    code: 'marketing',
    name: '营销场景',
    emoji: '📈',
    description: '内容生成、社媒发布、线索评分等',
    color: '#ec4899',
  },
  {
    code: 'office',
    name: '办公自动化',
    emoji: '💼',
    description: '周报、会议纪要、审批流程等',
    color: '#10b981',
  },
  {
    code: 'data-analysis',
    name: '数据分析',
    emoji: '🔍',
    description: '日志分析、SQL 生成、可视化等',
    color: '#0ea5e9',
  },
  {
    code: 'finance',
    name: '金融场景',
    emoji: '💰',
    description: '风控审核、合同抽取、行情分析',
    color: '#f59e0b',
  },
  {
    code: 'education',
    name: '教育场景',
    emoji: '🎓',
    description: '作业批改、试卷生成、学习路径',
    color: '#8b5cf6',
  },
  {
    code: 'medical',
    name: '医疗场景',
    emoji: '🏥',
    description: '病例摘要、药品咨询、随访提醒',
    color: '#ef4444',
  },
  {
    code: 'developer',
    name: '开发者工具',
    emoji: '⚙️',
    description: 'CI/CD、代码审查、Bug 分类',
    color: '#64748b',
  },
];

/** 难度等级元数据 */
export const DIFFICULTY_META = {
  easy: { label: '简单', color: '#10b981', minutes: 5 },
  medium: { label: '中等', color: '#f59e0b', minutes: 15 },
  hard: { label: '高级', color: '#ef4444', minutes: 30 },
} as const;

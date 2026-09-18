/**
 * 模板统一导出 (Template Index)
 *
 * 8 大分类共 50+ 模板:
 *   - 客服场景: 8 个 (含原有 csAutoReply)
 *   - 营销场景: 8 个
 *   - 办公自动化: 8 个
 *   - 数据分析: 8 个
 *   - 金融场景: 6 个
 *   - 教育场景: 6 个
 *   - 医疗场景: 4 个
 *   - 开发者工具: 6 个
 */

import type { BuiltinTemplate } from '../builtinTemplates';
import { MARKETING_TEMPLATES } from './marketing';
import { OFFICE_TEMPLATES } from './office';
import { DATA_ANALYSIS_TEMPLATES } from './data-analysis';
import { FINANCE_TEMPLATES } from './finance';
import { EDUCATION_TEMPLATES, MEDICAL_TEMPLATES, DEVELOPER_TEMPLATES, CUSTOMER_SERVICE_EXTRA_TEMPLATES } from './rest';

/**
 * 所有官方模板
 */
export const ALL_BUILTIN_TEMPLATES: BuiltinTemplate[] = [
  ...MARKETING_TEMPLATES,
  ...OFFICE_TEMPLATES,
  ...DATA_ANALYSIS_TEMPLATES,
  ...FINANCE_TEMPLATES,
  ...EDUCATION_TEMPLATES,
  ...MEDICAL_TEMPLATES,
  ...DEVELOPER_TEMPLATES,
  ...CUSTOMER_SERVICE_EXTRA_TEMPLATES,
];

/**
 * 按分类聚合
 */
export const TEMPLATES_BY_CATEGORY: Record<string, BuiltinTemplate[]> = {
  marketing: MARKETING_TEMPLATES,
  office: OFFICE_TEMPLATES,
  'data-analysis': DATA_ANALYSIS_TEMPLATES,
  finance: FINANCE_TEMPLATES,
  education: EDUCATION_TEMPLATES,
  medical: MEDICAL_TEMPLATES,
  developer: DEVELOPER_TEMPLATES,
  'customer-service': CUSTOMER_SERVICE_EXTRA_TEMPLATES,
};

/**
 * 模板总数
 */
export const TOTAL_TEMPLATE_COUNT = ALL_BUILTIN_TEMPLATES.length;

export {
  MARKETING_TEMPLATES,
  OFFICE_TEMPLATES,
  DATA_ANALYSIS_TEMPLATES,
  FINANCE_TEMPLATES,
  EDUCATION_TEMPLATES,
  MEDICAL_TEMPLATES,
  DEVELOPER_TEMPLATES,
  CUSTOMER_SERVICE_EXTRA_TEMPLATES,
};

/**
 * 节点表现中心 —— 给前端"流程图预览 / 节点列表 / 配置预览"共用。
 *
 * 主要职责：
 *  1. 根据节点 type 给出对应 FontAwesome 图标 + 颜色 + 中文标签 + 入参/出参摘要
 *  2. 兼容不同命名（`http.request` vs `tool.http`、`condition.if` vs `flow.if-else` 等）
 *  3. 在视图层选定"展示哪些参数"（以 keyConfig 形式输出）
 */

import { findNodeDefinition } from '@/mock/nodes';
import type { NodeDefinition } from '@/types/node';

export interface NodeVisualMeta {
  /** 节点类型 */
  type: string;
  /** 中文显示名（如"Webhook 触发器"） */
  label: string;
  /** 分类：trigger / ai / flow / tool / notify / transform / end */
  category: 'trigger' | 'ai' | 'flow' | 'tool' | 'notify' | 'transform' | 'end' | 'data' | 'default';
  /** 分类中文名 */
  categoryLabel: string;
  /** 分类主色 */
  categoryColor: string;
  /** 分类背景色（柔和） */
  categorySoft: string;
  /** 顶部色条 icon：FontAwesome class */
  icon: string;
  /** 节点主色（用于 SVG 边框/图标背景） */
  color: string;
  /** 节点简介 */
  description?: string;
  /** 入参 (paramPorts) 摘要 */
  inputs: Array<{ key: string; label: string; type: string }>;
  /** 出参 (paramPorts) 摘要 */
  outputs: Array<{ key: string; label: string; type: string }>;
}

/**
 * 类型别名映射（内置模板使用的精简名 → 节点目录中的标准 type）
 * 内置模板为了避免与引擎类型冲突，约定了一些短名，这里集中映射回 NODE_CATALOG。
 */
const TYPE_ALIAS: Record<string, string> = {
  'http.request': 'tool.http',
  'code.transform': 'tool.code',
  'condition.if': 'flow.condition',
  'condition.switch': 'flow.if-else',
  'transform.aggregator': 'flow.aggregation',
  'end.aggregator': 'flow.aggregation',
  'knowledge.search': 'ai.rag',
  'notify.feishu': 'notify.feishu',
  'notify.dingtalk': 'notify.dingtalk',
  'notify.wxwork': 'notify.wxwork',
  'notify.email': 'notify.email',
  'notify.sms': 'notify.sms',
  'trigger.webhook': 'trigger.webhook',
  'trigger.cron': 'trigger.cron',
  'trigger.form': 'trigger.form',
  'trigger.imessage': 'trigger.imessage',
  'ai.llm': 'ai.llm',
  'ai.classify': 'ai.classify',
  'ai.extract': 'ai.extract',
  'ai.summarize': 'ai.summarize',
  'ai.rag': 'ai.rag',
};

/** 分类 -> 中文标签 / 主色 / 柔色 */
const CATEGORY_THEME: Record<string, { label: string; color: string; soft: string; icon: string }> = {
  trigger:    { label: '触发器', color: '#8b5cf6', soft: '#f5f3ff', icon: 'fa-bolt' },
  ai:         { label: 'AI',     color: '#3b82f6', soft: '#eff6ff', icon: 'fa-brain' },
  flow:       { label: '流程',   color: '#10b981', soft: '#ecfdf5', icon: 'fa-diagram-project' },
  tool:       { label: '工具',   color: '#f59e0b', soft: '#fffbeb', icon: 'fa-wrench' },
  notify:     { label: '通知',   color: '#ec4899', soft: '#fdf2f8', icon: 'fa-paper-plane' },
  transform:  { label: '转换',   color: '#6366f1', soft: '#eef2ff', icon: 'fa-shuffle' },
  end:        { label: '结束',   color: '#64748b', soft: '#f1f5f9', icon: 'fa-flag-checkered' },
  data:       { label: '数据',   color: '#06b6d4', soft: '#ecfeff', icon: 'fa-database' },
  default:    { label: '通用',   color: '#94a3b8', soft: '#f8fafc', icon: 'fa-cube' },
};

/** 内置模板的精简 type → 分类映射（只有 NODE_CATALOG 找不到时才用） */
const TYPE_CATEGORY_FALLBACK: Record<string, NodeVisualMeta['category']> = {
  'trigger.webhook': 'trigger',
  'trigger.cron': 'trigger',
  'trigger.form': 'trigger',
  'trigger.imessage': 'trigger',
  'ai.llm': 'ai',
  'ai.classify': 'ai',
  'ai.extract': 'ai',
  'ai.summarize': 'ai',
  'ai.rag': 'ai',
  'knowledge.search': 'ai',
  'http.request': 'tool',
  'tool.http': 'tool',
  'tool.db': 'tool',
  'tool.code': 'tool',
  'code.transform': 'transform',
  'transform.aggregator': 'transform',
  'condition.if': 'flow',
  'condition.switch': 'flow',
  'flow.condition': 'flow',
  'flow.if-else': 'flow',
  'flow.loop': 'flow',
  'flow.iteration': 'flow',
  'flow.parallel': 'flow',
  'flow.wait': 'flow',
  'flow.aggregation': 'transform',
  'notify.feishu': 'notify',
  'notify.dingtalk': 'notify',
  'notify.wxwork': 'notify',
  'notify.email': 'notify',
  'notify.sms': 'notify',
  'end.aggregator': 'end',
  'end.return': 'end',
};

function resolveCategoryFromType(type?: string): NodeVisualMeta['category'] {
  if (!type) return 'default';
  if (TYPE_CATEGORY_FALLBACK[type]) return TYPE_CATEGORY_FALLBACK[type];
  if (type.startsWith('trigger')) return 'trigger';
  if (type.startsWith('ai.')) return 'ai';
  if (type.startsWith('flow.')) return 'flow';
  if (type.startsWith('tool.')) return 'tool';
  if (type.startsWith('notify.')) return 'notify';
  if (type.startsWith('condition')) return 'flow';
  if (type.startsWith('code') || type.startsWith('transform')) return 'transform';
  if (type.startsWith('end')) return 'end';
  return 'default';
}

/**
 * 给定一个 type，返回完整的节点视觉元信息。
 * - 优先从 NODE_CATALOG 查找；查不到则根据别名/类型推断
 */
export function getNodeVisualMeta(type?: string, fallbackName?: string): NodeVisualMeta {
  const t = type || 'unknown';
  const aliased = TYPE_ALIAS[t] ?? t;
  const def: NodeDefinition | undefined = findNodeDefinition(aliased);

  const category = resolveCategoryFromType(t);
  const theme = CATEGORY_THEME[category] ?? CATEGORY_THEME.default;

  if (def) {
    return {
      type: t,
      label: def.name || fallbackName || t,
      category,
      categoryLabel: theme.label,
      categoryColor: theme.color,
      categorySoft: theme.soft,
      icon: def.icon || theme.icon,
      color: def.color || theme.color,
      description: def.description,
      inputs: (def.inputs || []).map((p) => ({ key: p.key, label: p.label, type: p.type })),
      outputs: (def.outputs || []).map((p) => ({ key: p.key, label: p.label, type: p.type })),
    };
  }

  return {
    type: t,
    label: fallbackName || t,
    category,
    categoryLabel: theme.label,
    categoryColor: theme.color,
    categorySoft: theme.soft,
    icon: theme.icon,
    color: theme.color,
    description: undefined,
    inputs: [],
    outputs: [],
  };
}

// =============================================================================
// config 提取 / 展示策略
// =============================================================================

/** 参数 key → 中文标签 */
const PARAM_LABEL_BY_KEY: Record<string, string> = {
  method: '请求方法',
  path: '路径',
  url: 'URL',
  body: 'Body',
  headers: 'Headers',
  cron: 'Cron',
  timezone: '时区',
  formId: '表单 ID',
  model: '模型',
  modelId: '模型',
  temperature: '温度',
  maxTokens: '最大 Tokens',
  maxWords: '最大字数',
  prompt: 'Prompt',
  systemPrompt: '系统提示词',
  expression: '表达式',
  field: '判断字段',
  source: '代码',
  language: '语言',
  webhook: 'Webhook',
  to: '收件人',
  from: '发件人',
  subject: '主题',
  knowledgeBaseId: '知识库',
  topK: '召回数',
  scoreThreshold: '阈值',
  retryTimes: '重试次数',
  timeout: '超时 (ms)',
};

/**
 * "概要展示" — 仅挑选能让用户一眼看懂节点的 2-3 个关键参数。
 */
export const KEY_PARAM_KEYS = [
  'method', 'path', 'url',
  'cron', 'timezone', 'formId',
  'model', 'modelId', 'prompt', 'temperature',
  'expression', 'field',
  'webhook', 'to', 'subject',
  'knowledgeBaseId', 'topK',
];

/** 从 config 中提取"展示项" [{ key, label, value }] */
export interface ConfigDisplayItem {
  key: string;
  label: string;
  value: unknown;
  display: string;  // 已 mask / 截断的展示串
  sensitive?: boolean;
}

const SENSITIVE_KEYS = new Set([
  'password', 'authToken', 'secret', 'apiKey', 'accessKeyId', 'accessKeySecret',
  'x-signature', 'expected', 'signKey',
]);

function maskValue(v: unknown): { display: string; sensitive: boolean } {
  if (v === null || v === undefined) return { display: '—', sensitive: false };
  if (typeof v === 'string') {
    const s = v.trim();
    if (s.length === 0) return { display: '（未配置）', sensitive: false };
    return { display: s.length > 80 ? `${s.slice(0, 80)}…` : s, sensitive: false };
  }
  if (typeof v === 'number' || typeof v === 'boolean') {
    return { display: String(v), sensitive: false };
  }
  // 数组 / 对象 → JSON 串 + 截断
  let json: string;
  try {
    json = JSON.stringify(v);
  } catch {
    json = String(v);
  }
  return { display: json.length > 120 ? `${json.slice(0, 120)}…` : json, sensitive: false };
}

/** 全部参数列表（用于"配置预览"Tab） */
export function buildConfigTable(config: Record<string, unknown> | undefined | null): ConfigDisplayItem[] {
  if (!config || typeof config !== 'object') return [];
  return Object.keys(config)
    .sort()
    .map((key) => {
      const v = (config as Record<string, unknown>)[key];
      const sensitive = SENSITIVE_KEYS.has(key);
      const { display: rawDisplay } = maskValue(v);
      const display = sensitive && rawDisplay !== '—' && rawDisplay !== '（未配置）'
        ? '••••••••'
        : rawDisplay;
      const item: ConfigDisplayItem = {
        key,
        label: PARAM_LABEL_BY_KEY[key] ?? key,
        value: v,
        display,
      };
      if (sensitive) item.sensitive = true;
      return item;
    });
}

/** 仅取关键参数（用于 SVG / 节点列表顶部 chip） */
export function buildKeyParamChips(config: Record<string, unknown> | undefined | null, max = 3): ConfigDisplayItem[] {
  if (!config) return [];
  return KEY_PARAM_KEYS
    .map((key) => {
      if (!(key in config)) return null;
      const v = (config as Record<string, unknown>)[key];
      const { display } = maskValue(v);
      const sensitive = SENSITIVE_KEYS.has(key);
      const item: ConfigDisplayItem = {
        key,
        label: PARAM_LABEL_BY_KEY[key] ?? key,
        value: v,
        display: sensitive && display !== '—' && display !== '（未配置）' ? '••••' : display,
      };
      if (sensitive) item.sensitive = true;
      return item;
    })
    .filter((x): x is ConfigDisplayItem => x !== null && x.display !== '（未配置）')
    .slice(0, max);
}

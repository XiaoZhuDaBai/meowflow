/**
 * 模板构造函数 helper
 *
 * 提供简洁的 API 来快速构造工作流模板节点/边，
 * 减少模板文件 70% 的样板代码。
 */
import type { NodeConfig } from './builtinTemplateDefaults';

export type EdgeType = 'default' | 'condition' | 'loop' | 'error';

export interface TemplateNode {
  id: string;
  type: string;
  name: string;
  x: number;
  y: number;
  category?: string;
  description?: string;
  data?: NodeConfig;
}

export interface TemplateEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  type?: EdgeType;
}

const NODE_W = 240;
const DX = 80;
const DY = 140;

/**
 * 创建节点（增强版，支持位置策略）
 * @param col 列号（水平位置，0 开始）
 * @param row 行号（垂直偏移，0 = 主干，正数向下，负数向上）
 */
export function N(
  id: string,
  type: string,
  name: string,
  col: number,
  row = 0,
  category?: string,
  description?: string,
  data?: NodeConfig,
): TemplateNode {
  return {
    id,
    type,
    name,
    x: col * (NODE_W + DX),
    y: row * DY,
    category,
    description,
    data,
  };
}

/** 创建普通边 */
export function E(id: string, source: string, target: string, label?: string): TemplateEdge {
  return { id, source, target, label, type: 'default' };
}

/** 创建条件分支边（橙色虚线） */
export function EC(id: string, source: string, target: string, label?: string): TemplateEdge {
  return { id, source, target, label, type: 'condition' };
}

/** 创建循环边（紫色实线） */
export function EL(id: string, source: string, target: string, label?: string): TemplateEdge {
  return { id, source, target, label, type: 'loop' };
}

/** 创建错误边（红色实线） */
export function EE(id: string, source: string, target: string, label?: string): TemplateEdge {
  return { id, source, target, label, type: 'error' };
}

/**
 * 通用 LLM 节点配置
 */
export function llmConfig(opts: {
  model?: string;
  temperature?: number;
  prompt: string;
  systemPrompt?: string;
  maxTokens?: number;
  outputKey?: string;
}): NodeConfig {
  return {
    model: opts.model || 'gpt-4o-mini',
    temperature: opts.temperature ?? 0.7,
    prompt: opts.prompt,
    systemPrompt: opts.systemPrompt,
    maxTokens: opts.maxTokens,
    outputKey: opts.outputKey,
  };
}

/** 通用 HTTP 节点配置 */
export function httpConfig(opts: {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  url: string;
  body?: string;
  headers?: Record<string, string>;
}): NodeConfig {
  return {
    method: opts.method,
    url: opts.url,
    headers: opts.headers || { 'Content-Type': 'application/json' },
    body: opts.body,
    timeoutMs: 10000,
  };
}

/** 知识库检索配置 */
export function kbConfig(opts: { knowledgeBaseId: string; topK?: number; query?: string }): NodeConfig {
  return {
    knowledgeBaseId: opts.knowledgeBaseId,
    topK: opts.topK || 5,
    query: opts.query,
  };
}

/** 代码转换节点 */
export function codeConfig(source: string, language = 'javascript'): NodeConfig {
  return { language, source };
}

/** IF 节点 */
export function ifConfig(expression: string, trueNext?: string, falseNext?: string): NodeConfig {
  return { expression, trueNext, falseNext };
}

/** Switch 节点 */
export function switchConfig(field: string, cases: Array<{ value: string; next: string }>, defaultNext?: string): NodeConfig {
  return { field, cases, defaultNext };
}

/** 通知节点 */
export function notifyConfig(channel: 'feishu' | 'email' | 'sms' | 'dingtalk', message: string, webhook?: string): NodeConfig {
  return {
    webhook: webhook || '{{NOTIFY_WEBHOOK}}',
    msgType: 'text',
    message,
  };
}

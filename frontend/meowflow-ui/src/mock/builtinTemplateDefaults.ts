/**
 * 内置官方模板的"开箱即用"默认参数表。
 *
 * 每个节点类型都给出一个默认 config，这样插入模板时不需要再"裸奔"进入"高级配置"。
 *
 * 与前端的 `builtinTemplates.ts` 保持严格一致 — 它们是同步生成的镜像。
 */
import type { BuiltinNode } from './builtinTemplates';

export type NodeConfig = Record<string, any>;

/** 触发器 / Trigger 默认参数 */
export const TRIGGER_WEBHOOK: NodeConfig = {
  method: 'POST',
  path: '/hooks/incoming',
  authToken: '',
  timeout: 10000,
};

export const TRIGGER_CRON: NodeConfig = {
  cron: '0 9 * * *',
  timezone: 'Asia/Shanghai',
  enabled: true,
};

export const TRIGGER_FORM: NodeConfig = {
  formId: 'meowflow_default_form',
  fields: [
    { key: 'submitter', label: '提交人', type: 'string', required: true },
    { key: 'content', label: '内容', type: 'text', required: true },
  ],
};

export const TRIGGER_MESSAGE: NodeConfig = {
  platform: 'feishu',
  keyword: '',
  matchMode: 'exact',
};

/** AI 默认参数 */
export const AI_LLM: NodeConfig = {
  model: 'gpt-4o-mini',
  temperature: 0.3,
  maxTokens: 1024,
  systemPrompt: '你是喵流工作流的助手，请根据用户输入友好、专业地回答。',
  prompt: '请处理以下输入：\n{{input.text}}',
};

export const AI_CLASSIFY: NodeConfig = {
  model: 'gpt-4o-mini',
  temperature: 0.2,
  categories: '紧急,普通,建议',
  input: '{{input.text}}',
  outputKey: 'category',
};

export const AI_EXTRACT: NodeConfig = {
  model: 'gpt-4o-mini',
  schema: '[]',
  outputKey: 'extracted',
};

export const AI_SUMMARIZE: NodeConfig = {
  model: 'gpt-4o-mini',
  maxWords: 200,
  tone: 'concise',
  language: 'zh-CN',
};

export const AI_RAG: NodeConfig = {
  knowledgeBaseId: '',
  topK: 5,
  threshold: 0.7,
};

/** 工具 / Tool 默认参数 */
export const TOOL_HTTP: NodeConfig = {
  method: 'POST',
  url: '',
  headers: { 'Content-Type': 'application/json' },
  body: '',
  timeout: 10000,
  retryTimes: 1,
  retryOnFail: true,
};

export const TOOL_DB: NodeConfig = {
  dsn: '',
  sql: '',
  params: {},
  fetchSize: 1000,
};

export const TOOL_CODE: NodeConfig = {
  language: 'javascript',
  source: '// 同步转换：处理上游数据并返回 result\nreturn { ok: true, input };',
};

export const TOOL_KNOWLEDGE: NodeConfig = {
  knowledgeBaseId: 'kb_default',
  topK: 3,
  scoreThreshold: 0.6,
  outputKey: 'context',
};

/** 控制流 / Control 默认参数 */
export const CONTROL_IF: NodeConfig = {
  expression: '{{input.score}} >= 80',
  trueNext: 'true',
  falseNext: 'false',
};

export const CONTROL_SWITCH: NodeConfig = {
  field: '{{input.category}}',
  cases: [
    { value: '紧急', next: 'urgent' },
    { value: '普通', next: 'normal' },
    { value: '建议', next: 'suggestion' },
  ],
  defaultNext: 'fallback',
};

export const CONTROL_TRANSFORM: NodeConfig = {
  language: 'jsonata',
  source: '$',
  description: '使用 JSONata / JS 表达式做字段映射',
};

export const CONTROL_AGGREGATOR: NodeConfig = {
  mode: 'wait_all',
  timeoutMs: 30000,
};

/** 通知默认参数 */
export const NOTIFY_FEISHU: NodeConfig = {
  webhook: '',
  secret: '',
  atMobiles: [],
  atAll: false,
  msgType: 'text',
};

export const NOTIFY_DINGTALK: NodeConfig = {
  webhook: '',
  secret: '',
  atMobiles: [],
  atAll: false,
  msgType: 'markdown',
};

export const NOTIFY_WXWORK: NodeConfig = {
  webhook: '',
  mentionedList: [],
  msgType: 'markdown',
};

export const NOTIFY_EMAIL: NodeConfig = {
  host: '',
  port: 465,
  ssl: true,
  username: '',
  password: '',
  from: '',
  to: '',
  subject: '',
  body: '',
  cc: [],
};

export const NOTIFY_SMS: NodeConfig = {
  accessKeyId: '',
  accessKeySecret: '',
  signName: '喵流',
  templateCode: '',
  phoneNumbers: [],
  templateParam: '{}',
};

export const END_AGGREGATOR: NodeConfig = {
  outputMode: 'return_last',
};

export const TRIGGER_DEFAULT_CONFIG: NodeConfig = {
  note: '默认触发器',
};

// =============================================================================
// 每个节点的 config 应用：返回新对象，避免外部修改污染
// =============================================================================
export function applyDefaultConfig(type: string, base: NodeConfig = {}): NodeConfig {
  let defaults: NodeConfig = {};
  switch (type) {
    case 'trigger.webhook': defaults = TRIGGER_WEBHOOK; break;
    case 'trigger.cron': defaults = TRIGGER_CRON; break;
    case 'trigger.form': defaults = TRIGGER_FORM; break;
    case 'trigger.imessage': defaults = TRIGGER_MESSAGE; break;
    case 'ai.llm': defaults = AI_LLM; break;
    case 'ai.classify': defaults = AI_CLASSIFY; break;
    case 'ai.extract': defaults = AI_EXTRACT; break;
    case 'ai.summarize': defaults = AI_SUMMARIZE; break;
    case 'ai.rag': defaults = AI_RAG; break;
    case 'tool.http': case 'http.request': defaults = TOOL_HTTP; break;
    case 'tool.db': defaults = TOOL_DB; break;
    case 'tool.code': defaults = TOOL_CODE; break;
    case 'knowledge.search': defaults = TOOL_KNOWLEDGE; break;
    case 'condition.if': defaults = CONTROL_IF; break;
    case 'condition.switch': defaults = CONTROL_SWITCH; break;
    case 'code.transform': case 'transform.aggregator': defaults = CONTROL_TRANSFORM; break;
    case 'transform.aggregator': defaults = CONTROL_AGGREGATOR; break;
    case 'end.aggregator': defaults = END_AGGREGATOR; break;
    case 'notify.feishu': defaults = NOTIFY_FEISHU; break;
    case 'notify.dingtalk': defaults = NOTIFY_DINGTALK; break;
    case 'notify.wxwork': defaults = NOTIFY_WXWORK; break;
    case 'notify.email': defaults = NOTIFY_EMAIL; break;
    case 'notify.sms': defaults = NOTIFY_SMS; break;
    default: defaults = TRIGGER_DEFAULT_CONFIG; break;
  }
  return { ...defaults, ...base };
}

/**
 * 给一批节点应用默认 data；只对原 data 为空/未设置时生效。
 * 这样已经手工指定覆盖值的节点不会被默认值覆盖。
 */
export function fillConfigs(nodes: BuiltinNode[]): BuiltinNode[] {
  return nodes.map((node) => {
    const needsFill = !node.data || Object.keys(node.data).length === 0;
    if (!needsFill) return node;
    return { ...node, data: applyDefaultConfig(node.type) };
  });
}

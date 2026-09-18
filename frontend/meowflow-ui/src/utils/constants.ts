export const APP_NAME = '喵流';

export const STORAGE_KEYS = {
  TOKEN: 'meowflow.token',
  ACCESS_TOKEN: 'meowflow.access_token',
  REFRESH_TOKEN: 'meowflow.refresh_token',
  USER: 'meowflow.user',
  WORKFLOWS: 'meowflow.workflows',
  EXECUTIONS: 'meowflow.executions',
  TEMPLATES: 'meowflow.templates',
  SETTINGS_LLM: 'meowflow.settings.llm',
  SETTINGS_INTEGRATIONS: 'meowflow.settings.integrations',
  TEAM: 'meowflow.team',
};

export const STATUS_LABEL: Record<string, string> = {
  draft: '草稿',
  running: '运行中',
  stopped: '已停止',
  archived: '已归档',
  pending: '等待中',
  success: '成功',
  failed: '失败',
  cancelled: '已取消',
  waiting: '等待中',
  idle: '空闲',
};

export const STATUS_COLOR: Record<string, string> = {
  draft: '#94a3b8',
  running: '#10b981',
  stopped: '#64748b',
  archived: '#94a3b8',
  pending: '#f59e0b',
  success: '#10b981',
  failed: '#ef4444',
  cancelled: '#64748b',
  waiting: '#f59e0b',
  idle: '#cbd5e1',
};

export const NODE_CATEGORY_META: Record<string, { label: string; icon: string; color: string }> = {
  trigger: { label: '触发器', icon: 'fa-bolt', color: '#8b5cf6' },
  ai:      { label: 'AI',     icon: 'fa-brain', color: '#3b82f6' },
  flow:    { label: '流程',   icon: 'fa-diagram-project', color: '#10b981' },
  tool:    { label: '工具',   icon: 'fa-wrench', color: '#f59e0b' },
  notify:  { label: '通知',   icon: 'fa-paper-plane', color: '#ec4899' },
};

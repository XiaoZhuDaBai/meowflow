import type { LLMModel, IntegrationItem } from '@/api/types';

export const DEFAULT_LLM_MODELS: LLMModel[] = [
  { id: 'llm-1', name: 'GPT-4o',         provider: 'OpenAI',  enabled: true,  isDefault: true,  apiKeyMasked: 'sk-xxxx****fA82', endpoint: 'https://api.openai.com/v1' },
  { id: 'llm-2', name: 'GPT-4o mini',    provider: 'OpenAI',  enabled: true,  isDefault: false, apiKeyMasked: 'sk-xxxx****bE91', endpoint: 'https://api.openai.com/v1' },
  { id: 'llm-3', name: 'DeepSeek-V3',    provider: 'DeepSeek', enabled: true, isDefault: false, apiKeyMasked: 'sk-xxxx****ac21', endpoint: 'https://api.deepseek.com/v1' },
  { id: 'llm-4', name: '通义千问 Turbo', provider: '阿里云',   enabled: false, isDefault: false, apiKeyMasked: 'sk-xxxx****0d12', endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1' },
  { id: 'llm-5', name: '文心一言 4',     provider: '百度',     enabled: false, isDefault: false, apiKeyMasked: 'sk-xxxx****77aa', endpoint: 'https://aip.baidubce.com' },
];

export const DEFAULT_INTEGRATIONS: IntegrationItem[] = [
  { id: 'i-1', type: 'dingtalk', name: '钉钉机器人', enabled: true,  description: '工作流执行结果自动推送到钉钉群',
    config: { webhook: 'https://oapi.dingtalk.com/robot/send?access_token=****', secret: 'SEC****' } },
  { id: 'i-2', type: 'wxwork',   name: '企业微信机器人', enabled: false, description: '工作流执行结果推送到企微群',
    config: { webhook: '' } },
  { id: 'i-3', type: 'feishu',   name: '飞书机器人', enabled: false, description: '工作流执行结果推送到飞书群',
    config: { webhook: '' } },
  { id: 'i-4', type: 'email',    name: 'SMTP 邮件', enabled: true, description: '执行结果邮件通知',
    config: { host: 'smtp.example.com', port: '465', username: 'notify@meowflow.com' } },
  { id: 'i-5', type: 'sms',      name: '阿里云短信', enabled: false, description: '短信通知 (mock)',
    config: { accessKey: '****', signName: '喵流' } },
];
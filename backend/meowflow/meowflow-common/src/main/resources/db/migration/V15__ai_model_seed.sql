-- =============================================================
-- V15__ai_model_seed.sql
-- 以 AIModelEntity 的实际映射为准：model_type 保存模型标识(modelKey)。
-- 默认全部禁用，配置真实 API Key 后再启用。
-- =============================================================

INSERT INTO mf_ai_model
    (name, provider, model_type, api_base, enabled, is_default, priority, max_tokens, capabilities)
VALUES
    ('GPT-4o',              'OpenAI',    'gpt-4o',              'https://api.openai.com/v1',                            FALSE, FALSE, 100, 16384, 'chat,vision,function_call'),
    ('GPT-4o Mini',         'OpenAI',    'gpt-4o-mini',         'https://api.openai.com/v1',                            FALSE, FALSE, 95,  16384, 'chat,vision,function_call'),
    ('Claude 3.5 Sonnet',   'Anthropic', 'claude-3-5-sonnet-latest', 'https://api.anthropic.com',                        FALSE, FALSE, 98,  8192,  'chat,vision,function_call'),
    ('Claude 3 Haiku',      'Anthropic', 'claude-3-haiku-20240307',  'https://api.anthropic.com',                        FALSE, FALSE, 80,  4096,  'chat,vision'),
    ('DeepSeek-V3',         'DeepSeek',  'deepseek-chat',       'https://api.deepseek.com/v1',                           FALSE, FALSE, 75,  8192,  'chat,function_call'),
    ('qwen-plus',           'Aliyun',    'qwen-plus',           'https://dashscope.aliyuncs.com/compatible-mode/v1',     FALSE, FALSE, 70,  8192,  'chat,function_call'),
    ('qwen-max',            'Aliyun',    'qwen-max',            'https://dashscope.aliyuncs.com/compatible-mode/v1',     FALSE, FALSE, 68,  8192,  'chat,function_call'),
    ('text-embedding-v2',   'Aliyun',    'text-embedding-v2',   'https://dashscope.aliyuncs.com/compatible-mode/v1',     FALSE, FALSE, 60,  0,     'embedding'),
    ('glm-4',               'Zhipu',     'glm-4',               'https://open.bigmodel.cn/api/paas/v4',                  FALSE, FALSE, 65,  4096,  'chat,function_call'),
    ('glm-4-flash',         'Zhipu',     'glm-4-flash',         'https://open.bigmodel.cn/api/paas/v4',                  FALSE, FALSE, 62,  4096,  'chat')
ON CONFLICT (provider, model_type) DO UPDATE SET
    name = EXCLUDED.name,
    api_base = EXCLUDED.api_base,
    priority = EXCLUDED.priority,
    max_tokens = EXCLUDED.max_tokens,
    capabilities = EXCLUDED.capabilities;

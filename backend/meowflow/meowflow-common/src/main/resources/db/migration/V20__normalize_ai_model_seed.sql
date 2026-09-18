-- ============================================================
-- 归一化 AI 模型种子：消除 provider 别名导致的重复，并禁用未配置密钥的模型
-- ============================================================
UPDATE mf_ai_model
SET capabilities = CASE model_type
    WHEN 'gpt-4o' THEN 'chat,vision,function_call'
    WHEN 'gpt-4o-mini' THEN 'chat,vision,function_call'
    WHEN 'claude-3-5-sonnet-latest' THEN 'chat,vision,function_call'
    WHEN 'claude-3-haiku-20240307' THEN 'chat,vision'
    WHEN 'deepseek-chat' THEN 'chat,function_call'
    WHEN 'qwen-plus' THEN 'chat,function_call'
    WHEN 'qwen-max' THEN 'chat,function_call'
    WHEN 'text-embedding-v2' THEN 'embedding'
    WHEN 'glm-4' THEN 'chat,function_call'
    WHEN 'glm-4-flash' THEN 'chat'
    ELSE capabilities
END
WHERE capabilities IS NULL OR capabilities = '';

WITH normalized AS (
    SELECT id,
           CASE
               WHEN lower(provider) IN ('aliyun', 'ali', 'dashscope', '阿里云') THEN 'aliyun'
               WHEN lower(provider) IN ('baidu', 'wenxin', '百度') THEN 'baidu'
               ELSE lower(provider)
           END AS provider_key,
           model_type,
           api_key,
           capabilities
    FROM mf_ai_model
), ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY provider_key, model_type
               ORDER BY (COALESCE(api_key, '') <> '') DESC,
                        (COALESCE(capabilities, '') <> '') DESC,
                        id
           ) AS rn
    FROM normalized
)
DELETE FROM mf_ai_model
WHERE id IN (SELECT id FROM ranked WHERE rn > 1);

UPDATE mf_ai_model SET provider = 'Aliyun'
WHERE lower(provider) IN ('aliyun', 'ali', 'dashscope', '阿里云');

UPDATE mf_ai_model SET provider = 'Baidu'
WHERE lower(provider) IN ('baidu', 'wenxin', '百度');

UPDATE mf_ai_model
SET enabled = FALSE, is_default = FALSE
WHERE COALESCE(api_key, '') = '';

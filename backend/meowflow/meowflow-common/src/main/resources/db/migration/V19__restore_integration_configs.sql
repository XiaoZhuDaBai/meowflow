-- ============================================================
-- 恢复默认集成配置（保持禁用，待配置真实凭据后启用）
-- ============================================================
INSERT INTO mf_integration_config (type, name, enabled, retry_times, timeout_seconds, deleted)
SELECT v.type, v.name, FALSE, 3, 30, 0
FROM (VALUES
    ('dingtalk', '钉钉机器人'),
    ('wxwork', '企业微信机器人'),
    ('feishu', '飞书机器人'),
    ('email', 'SMTP 邮件'),
    ('sms', '阿里云短信')
) AS v(type, name)
WHERE NOT EXISTS (
    SELECT 1 FROM mf_integration_config c WHERE c.type = v.type
);

UPDATE mf_integration_config
SET name = 'SMTP 邮件', enabled = FALSE, deleted = 0, custom_config = NULL
WHERE type = 'email' AND name LIKE 'smoke-%';

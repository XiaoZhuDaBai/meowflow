-- =============================================================
-- V4__seed_data.sql
-- 喵流 MeowFlow 初始数据
-- 创建日期: 2026-07-13
-- =============================================================

-- ============================================================
-- 1. 超级管理员账号
-- 用户名: admin
-- 密码: meow@2026 (BCrypt 加密后的 hash, 请首次登录后修改!)
-- BCrypt("meow@2026") 示例 hash
-- ============================================================
INSERT INTO mf_sys_user (username, password, nickname, real_name, status, email)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '超级管理员', '系统管理员', 'active', 'admin@meowflow.com')
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- 2. 默认组织
-- ============================================================
INSERT INTO mf_sys_org (parent_id, name, code, sort, leader_user_id, status)
VALUES
    (0, '喵流官方', 'meowflow', 0, 1, 'active'),
    (0, '默认组织', 'default', 1, 1, 'active')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 3. 默认角色
-- ============================================================
INSERT INTO mf_sys_role (code, name, data_scope, sort, status, remark)
VALUES
    ('super_admin', '超级管理员', 'all', 0, 'active', '拥有所有权限'),
    ('admin', '管理员', 'org', 1, 'active', '组织级管理员'),
    ('member', '成员', 'self', 2, 'active', '普通成员,可见自己的资源'),
    ('viewer', '访客', 'self', 3, 'active', '只读')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 4. 默认岗位
-- ============================================================
INSERT INTO mf_sys_post (code, name, sort)
VALUES
    ('ceo', 'CEO', 0),
    ('cto', 'CTO', 1),
    ('pm', '产品经理', 2),
    ('dev', '工程师', 3),
    ('ops', '运营', 4)
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 5. 内置权限 (菜单/按钮)
-- ============================================================
INSERT INTO mf_sys_permission (id, parent_id, type, name, code, path, icon, sort)
VALUES
    -- 顶级菜单
    (1, 0, 'menu', '工作台', 'dashboard', '/dashboard', 'fa-tachometer-alt', 0),
    (2, 0, 'menu', '工作流', 'workflow', '/workflows', 'fa-diagram-project', 1),
    (3, 2, 'menu', '工作流列表', 'workflow:list', '/workflows', '', 1),
    (4, 2, 'menu', '工作流编辑器', 'workflow:editor', '/editor', '', 2),
    (5, 0, 'menu', '模板市场', 'template', '/templates', 'fa-puzzle-piece', 2),
    (6, 0, 'menu', '执行日志', 'log', '/logs', 'fa-list-alt', 3),
    (7, 0, 'menu', '统计看板', 'statistics', '/statistics', 'fa-chart-line', 4),
    (8, 0, 'menu', '用户中心', 'user', '/profile', 'fa-user', 5),
    (9, 0, 'menu', '团队管理', 'team', '/team', 'fa-users', 6),
    (10, 0, 'menu', '系统设置', 'system', '/settings', 'fa-cog', 7),
    -- 按钮权限
    (100, 3, 'btn', '新建工作流', 'workflow:create', NULL, NULL, 1),
    (101, 3, 'btn', '编辑工作流', 'workflow:edit', NULL, NULL, 2),
    (102, 3, 'btn', '删除工作流', 'workflow:delete', NULL, NULL, 3),
    (103, 3, 'btn', '执行工作流', 'workflow:run', NULL, NULL, 4),
    (104, 3, 'btn', '发布工作流', 'workflow:publish', NULL, NULL, 5),
    -- 设置子菜单
    (110, 10, 'menu', 'AI 模型配置', 'system:model', '/settings/model', 'fa-robot', 1),
    (111, 10, 'menu', '集成配置', 'system:integration', '/settings/integration', 'fa-plug', 2),
    (112, 10, 'menu', '告警规则', 'system:alert', '/settings/alert', 'fa-bell', 3)
ON CONFLICT (id) DO NOTHING;

-- 修正 id 序列
SELECT setval(pg_get_serial_sequence('mf_sys_permission','id'),
    GREATEST((SELECT MAX(id) FROM mf_sys_permission), 1));

-- ============================================================
-- 6. 管理员与角色关联
-- ============================================================
INSERT INTO mf_sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM mf_sys_user u, mf_sys_role r
WHERE u.username = 'admin' AND r.code = 'super_admin'
ON CONFLICT DO NOTHING;

-- super_admin 拥有所有菜单
INSERT INTO mf_sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM mf_sys_role r, mf_sys_permission p
WHERE r.code = 'super_admin'
ON CONFLICT DO NOTHING;

-- ============================================================
-- 7. 工作流分类
-- ============================================================
INSERT INTO mf_wf_category (id, parent_id, code, name, sort)
VALUES
    (1, 0, 'cs', '客服场景', 1),
    (2, 0, 'hr', '人力资源', 2),
    (3, 0, 'ops', '运营提效', 3),
    (4, 0, 'finance', '财务行政', 4),
    (5, 0, 'general', '通用', 9)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('mf_wf_category','id'),
    GREATEST((SELECT MAX(id) FROM mf_wf_category), 1));

-- ============================================================
-- 8. 默认 AI 模型配置
-- ============================================================
INSERT INTO mf_ai_model (name, provider, model_type, api_base, enabled, is_default, priority, max_tokens, temperature)
VALUES
    ('GPT-4o', 'OpenAI', 'gpt-4o', 'https://api.openai.com/v1', TRUE, FALSE, 90, 8192, 0.7),
    ('GPT-4o Mini', 'OpenAI', 'gpt-4o-mini', 'https://api.openai.com/v1', TRUE, TRUE, 80, 8192, 0.7),
    ('DeepSeek-V3', 'DeepSeek', 'deepseek-chat', 'https://api.deepseek.com/v1', TRUE, FALSE, 70, 8192, 0.7),
    ('通义千问 Turbo', '阿里云', 'qwen-plus', 'https://dashscope.aliyuncs.com/compatible-mode/v1', FALSE, FALSE, 60, 8192, 0.7),
    ('通义千问 Max', '阿里云', 'qwen-max', 'https://dashscope.aliyuncs.com/compatible-mode/v1', FALSE, FALSE, 50, 8192, 0.7),
    ('文心一言 4', '百度', 'ernie-4.0-8k', 'https://aip.baidubce.com', FALSE, FALSE, 50, 8192, 0.7),
    ('Claude 3.5 Sonnet', 'Anthropic', 'claude-3-5-sonnet-latest', 'https://api.anthropic.com', FALSE, FALSE, 85, 8192, 0.7),
    ('text-embedding-3-small', 'OpenAI', 'text-embedding-3-small', 'https://api.openai.com/v1', TRUE, TRUE, 80, 1536, 0.0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 9. 集成默认配置 (未启用,需后续配置)
-- ============================================================
INSERT INTO mf_int_config (type, name, enabled, config, description)
VALUES
    ('dingtalk', '钉钉机器人', FALSE, '{"webhook":"","secret":""}'::jsonb, '工作流执行结果自动推送到钉钉群'),
    ('wxwork', '企业微信机器人', FALSE, '{"webhook":""}'::jsonb, '工作流执行结果推送到企微群'),
    ('feishu', '飞书机器人', FALSE, '{"webhook":""}'::jsonb, '工作流执行结果推送到飞书群'),
    ('email', 'SMTP 邮件', FALSE, '{"host":"","port":465,"username":"","password":""}'::jsonb, '执行结果邮件通知'),
    ('sms', '阿里云短信', FALSE, '{"accessKey":"","secret":"","signName":"喵流"}'::jsonb, '短信通知')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 10. 默认告警规则
-- ============================================================
INSERT INTO mf_mon_alert_rule (name, metric, condition, threshold, duration_s, channels, enabled, owner_id)
VALUES
    ('执行失败告警', 'execution_failed', 'gt', 5, 60, '["email"]'::jsonb, TRUE, 1),
    ('执行超时告警', 'execution_timeout', 'gt', 3, 60, '["dingtalk"]'::jsonb, TRUE, 1),
    ('成本超限告警', 'cost_exceeded', 'gt', 100, 3600, '["email"]'::jsonb, TRUE, 1)
ON CONFLICT DO NOTHING;


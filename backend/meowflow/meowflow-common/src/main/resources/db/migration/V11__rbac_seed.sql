-- =============================================================
-- V11__rbac_seed.sql
-- 喵流 MeowFlow RBAC 权限数据初始化
-- 创建日期: 2026-09-12
-- 说明: 补充 V4 中缺失的权限和角色关联数据
--       可独立执行，用于修复权限数据为空的问题
-- =============================================================

-- ============================================================
-- 1. 确保管理员账号存在
-- =============================================================
INSERT INTO mf_sys_user (id, username, password, nickname, real_name, status, email)
VALUES (1, 'admin', '$2a$10$227xlzemuj7i1ZGBEuFl7./qmvvUeaZK0UFXRVHcBUViPwK0PRheq', '超级管理员', '系统管理员', 'active', 'admin@meowflow.com')
ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    nickname = EXCLUDED.nickname,
    real_name = EXCLUDED.real_name,
    status = EXCLUDED.status;



-- ============================================================
-- 2. 默认角色（如果不存在）
-- =============================================================
INSERT INTO mf_sys_role (code, name, data_scope, sort, status, remark)
VALUES
    ('super_admin', '超级管理员', 'all', 0, 'active', '拥有所有权限'),
    ('admin', '管理员', 'org', 1, 'active', '组织级管理员'),
    ('member', '成员', 'self', 2, 'active', '普通成员,可见自己的资源'),
    ('viewer', '访客', 'self', 3, 'active', '只读')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 3. 超级管理员权限菜单（完整的菜单树）
-- =============================================================
INSERT INTO mf_sys_permission (id, parent_id, type, name, code, path, icon, sort, status)
VALUES
    -- 顶级菜单
    (1, 0, 'menu', '工作台', 'dashboard', '/dashboard', 'fa-tachometer-alt', 0, 'active'),
    (2, 0, 'menu', '工作流', 'workflow', '/workflows', 'fa-diagram-project', 1, 'active'),
    (3, 2, 'menu', '工作流列表', 'workflow:list', '/workflows', '', 1, 'active'),
    (4, 2, 'menu', '工作流编辑器', 'workflow:editor', '/editor', '', 2, 'active'),
    (5, 0, 'menu', '模板市场', 'template', '/templates', 'fa-puzzle-piece', 2, 'active'),
    (6, 5, 'menu', '浏览模板', 'template:explore', '/explore', '', 0, 'active'),
    (7, 5, 'menu', '我的收藏', 'template:favorites', '/templates/favorites', '', 1, 'active'),
    (8, 0, 'menu', '执行日志', 'log', '/logs', 'fa-clipboard-list', 3, 'active'),
    (9, 0, 'menu', '统计看板', 'statistics', '/statistics', 'fa-chart-line', 4, 'active'),
    (10, 0, 'menu', '告警中心', 'alert', '/alerts', 'fa-bell', 5, 'active'),
    (11, 0, 'menu', '用户中心', 'user', '/profile', 'fa-user', 6, 'active'),
    (12, 0, 'menu', '团队管理', 'team', '/team', 'fa-users', 7, 'active'),
    (13, 0, 'menu', '系统设置', 'system', '/settings', 'fa-gear', 8, 'active'),
    
    -- 工作流按钮权限
    (100, 3, 'btn', '新建工作流', 'workflow:create', NULL, NULL, 1, 'active'),
    (101, 3, 'btn', '编辑工作流', 'workflow:edit', NULL, NULL, 2, 'active'),
    (102, 3, 'btn', '删除工作流', 'workflow:delete', NULL, NULL, 3, 'active'),
    (103, 3, 'btn', '执行工作流', 'workflow:run', NULL, NULL, 4, 'active'),
    (104, 3, 'btn', '发布工作流', 'workflow:publish', NULL, NULL, 5, 'active'),
    (105, 3, 'btn', '导出工作流', 'workflow:export', NULL, NULL, 6, 'active'),
    (106, 3, 'btn', '复制工作流', 'workflow:copy', NULL, NULL, 7, 'active'),
    
    -- 编辑器按钮权限
    (110, 4, 'btn', '保存版本', 'editor:save', NULL, NULL, 1, 'active'),
    (111, 4, 'btn', '发布版本', 'editor:publish', NULL, NULL, 2, 'active'),
    (112, 4, 'btn', '执行调试', 'editor:debug', NULL, NULL, 3, 'active'),
    (113, 4, 'btn', '停止调试', 'editor:stop', NULL, NULL, 4, 'active'),
    (114, 4, 'btn', '添加节点', 'editor:addNode', NULL, NULL, 5, 'active'),
    (115, 4, 'btn', '删除节点', 'editor:deleteNode', NULL, NULL, 6, 'active'),
    
    -- 模板按钮权限
    (120, 5, 'btn', '创建模板', 'template:create', NULL, NULL, 1, 'active'),
    (121, 5, 'btn', '编辑模板', 'template:edit', NULL, NULL, 2, 'active'),
    (122, 5, 'btn', '删除模板', 'template:delete', NULL, NULL, 3, 'active'),
    (123, 5, 'btn', '使用模板', 'template:use', NULL, NULL, 4, 'active'),
    (124, 5, 'btn', '点赞模板', 'template:like', NULL, NULL, 5, 'active'),
    (125, 5, 'btn', '收藏模板', 'template:favorite', NULL, NULL, 6, 'active'),
    (126, 5, 'btn', '评分模板', 'template:rating', NULL, NULL, 7, 'active'),
    
    -- 执行日志按钮权限
    (130, 8, 'btn', '查看详情', 'log:view', NULL, NULL, 1, 'active'),
    (131, 8, 'btn', '重新执行', 'log:rerun', NULL, NULL, 2, 'active'),
    (132, 8, 'btn', '取消执行', 'log:cancel', NULL, NULL, 3, 'active'),
    (133, 8, 'btn', '导出日志', 'log:export', NULL, NULL, 4, 'active'),
    
    -- 告警中心按钮权限
    (140, 10, 'btn', '创建规则', 'alert:rule:create', NULL, NULL, 1, 'active'),
    (141, 10, 'btn', '编辑规则', 'alert:rule:edit', NULL, NULL, 2, 'active'),
    (142, 10, 'btn', '删除规则', 'alert:rule:delete', NULL, NULL, 3, 'active'),
    (143, 10, 'btn', '创建沉默', 'alert:silence:create', NULL, NULL, 4, 'active'),
    (144, 10, 'btn', '解决告警', 'alert:resolve', NULL, NULL, 5, 'active'),
    
    -- 系统设置子菜单
    (150, 13, 'menu', 'AI 模型配置', 'system:model', '/settings/model', 'fa-robot', 1, 'active'),
    (151, 13, 'menu', '集成配置', 'system:integration', '/settings/integration', 'fa-plug', 2, 'active'),
    (152, 13, 'menu', '告警规则', 'system:alert', '/settings/alert', 'fa-bell', 3, 'active'),
    (153, 13, 'menu', '执行器节点', 'system:executor', '/settings/executor', 'fa-server', 4, 'active'),
    (154, 13, 'menu', 'Webhook 管理', 'system:webhook', '/settings/webhook', 'fa-link', 5, 'active'),
    
    -- 系统设置按钮权限
    (160, 150, 'btn', '添加模型', 'system:model:create', NULL, NULL, 1, 'active'),
    (161, 150, 'btn', '编辑模型', 'system:model:edit', NULL, NULL, 2, 'active'),
    (162, 150, 'btn', '删除模型', 'system:model:delete', NULL, NULL, 3, 'active'),
    (163, 151, 'btn', '添加集成', 'system:integration:create', NULL, NULL, 1, 'active'),
    (164, 151, 'btn', '编辑集成', 'system:integration:edit', NULL, NULL, 2, 'active'),
    (165, 151, 'btn', '删除集成', 'system:integration:delete', NULL, NULL, 3, 'active'),
    
    -- 团队管理按钮权限
    (170, 12, 'btn', '邀请成员', 'team:invite', NULL, NULL, 1, 'active'),
    (171, 12, 'btn', '移除成员', 'team:remove', NULL, NULL, 2, 'active'),
    (172, 12, 'btn', '修改角色', 'team:role', NULL, NULL, 3, 'active'),
    (173, 12, 'btn', '创建团队', 'team:create', NULL, NULL, 4, 'active'),
    
    -- 用户中心按钮权限
    (180, 11, 'btn', '修改资料', 'user:profile:edit', NULL, NULL, 1, 'active'),
    (181, 11, 'btn', '修改密码', 'user:password:change', NULL, NULL, 2, 'active'),
    (182, 11, 'btn', '上传头像', 'user:avatar:upload', NULL, NULL, 3, 'active')
ON CONFLICT (id) DO NOTHING;

-- 修正 id 序列
SELECT setval(pg_get_serial_sequence('mf_sys_permission','id'),
    GREATEST((SELECT MAX(id) FROM mf_sys_permission), 1));

-- ============================================================
-- 4. 管理员与角色关联
-- =============================================================
-- 先删除可能存在的错误关联
DELETE FROM mf_sys_user_role WHERE user_id = 1;

-- 重新插入正确关联
INSERT INTO mf_sys_user_role (user_id, role_id, org_id)
SELECT 1, r.id, 0
FROM mf_sys_role r
WHERE r.code = 'super_admin'
ON CONFLICT (user_id, role_id, org_id) DO NOTHING;

-- ============================================================
-- 5. 超级管理员拥有所有权限
-- =============================================================
DELETE FROM mf_sys_role_permission WHERE role_id IN (
    SELECT id FROM mf_sys_role WHERE code = 'super_admin'
);

INSERT INTO mf_sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM mf_sys_role r, mf_sys_permission p
WHERE r.code = 'super_admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- 6. 管理员拥有基本权限（不含系统设置）
-- =============================================================
DELETE FROM mf_sys_role_permission WHERE role_id IN (
    SELECT id FROM mf_sys_role WHERE code = 'admin'
);

INSERT INTO mf_sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM mf_sys_role r, mf_sys_permission p
WHERE r.code = 'admin'
AND p.id NOT IN (150, 151, 152, 153, 154, 160, 161, 162, 163, 164, 165)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- 7. 成员拥有基本操作权限
-- =============================================================
DELETE FROM mf_sys_role_permission WHERE role_id IN (
    SELECT id FROM mf_sys_role WHERE code = 'member'
);

INSERT INTO mf_sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM mf_sys_role r, mf_sys_permission p
WHERE r.code = 'member'
AND p.id IN (
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
    100, 101, 103, 106,
    110, 111, 112, 114,
    120, 123, 124, 125, 126,
    130, 131,
    140, 143, 144,
    170, 172,
    180, 181
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- 8. 访客只读权限
-- =============================================================
DELETE FROM mf_sys_role_permission WHERE role_id IN (
    SELECT id FROM mf_sys_role WHERE code = 'viewer'
);

INSERT INTO mf_sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM mf_sys_role r, mf_sys_permission p
WHERE r.code = 'viewer'
AND p.type = 'menu'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- 9. 验证数据
-- =============================================================
DO $$
DECLARE
    user_count INTEGER;
    role_count INTEGER;
    perm_count INTEGER;
    rp_count INTEGER;
    admin_perm_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count FROM mf_sys_user WHERE username = 'admin';
    SELECT COUNT(*) INTO role_count FROM mf_sys_role;
    SELECT COUNT(*) INTO perm_count FROM mf_sys_permission;
    SELECT COUNT(*) INTO rp_count FROM mf_sys_role_permission;
    SELECT COUNT(*) INTO admin_perm_count 
    FROM mf_sys_role_permission rp
    JOIN mf_sys_role r ON rp.role_id = r.id
    WHERE r.code = 'super_admin';
    
    RAISE NOTICE '=== RBAC 数据初始化验证 ===';
    RAISE NOTICE '管理员账号: %', user_count;
    RAISE NOTICE '角色数量: %', role_count;
    RAISE NOTICE '权限数量: %', perm_count;
    RAISE NOTICE '角色权限关联: %', rp_count;
    RAISE NOTICE '超级管理员权限数: %', admin_perm_count;
    
    IF user_count = 0 THEN
        RAISE WARNING '警告: 管理员账号不存在!';
    END IF;
    
    IF admin_perm_count = 0 THEN
        RAISE WARNING '警告: 超级管理员没有任何权限!';
    END IF;
END $$;

-- ============================================================
-- 10. 输出结果
-- =============================================================
SELECT '=== 超级管理员菜单权限 ===' AS info;
SELECT p.id, p.name, p.code, p.type
FROM mf_sys_role_permission rp
JOIN mf_sys_role r ON rp.role_id = r.id
JOIN mf_sys_permission p ON rp.permission_id = p.id
WHERE r.code = 'super_admin'
AND p.type = 'menu'
ORDER BY p.id;

SELECT '=== 超级管理员按钮权限 ===' AS info;
SELECT p.id, p.name, p.code, p.type
FROM mf_sys_role_permission rp
JOIN mf_sys_role r ON rp.role_id = r.id
JOIN mf_sys_permission p ON rp.permission_id = p.id
WHERE r.code = 'super_admin'
AND p.type = 'btn'
ORDER BY p.id;


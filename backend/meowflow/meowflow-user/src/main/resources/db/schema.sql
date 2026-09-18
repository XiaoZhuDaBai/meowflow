-- MeowFlow User 模块数据库初始化脚本 (PostgreSQL)
-- 表前缀: mf_sys_
-- 本文件仅保留 seed 数据(DML), 表结构统一由 scripts/sql/init.sql 创建

-- ============================================================
-- 种子数据 (已迁移自 scripts/sql/init.sql)
-- 管理员 ID 由 Bigserial 自增产生,无需手动指定
-- ============================================================

-- 修正 id 序列
SELECT setval(pg_get_serial_sequence('mf_sys_permission','id'),
    GREATEST((SELECT MAX(id) FROM mf_sys_permission), 1));

-- 插入默认管理员角色
INSERT INTO mf_sys_role (code, name, data_scope, sort, status, remark)
VALUES ('super_admin', '超级管理员', 'all', 0, 'active', '拥有所有权限')
ON CONFLICT (code) DO NOTHING;

-- 插入管理员角色
INSERT INTO mf_sys_role (code, name, data_scope, sort, status, remark)
VALUES ('admin', '管理员', 'org', 1, 'active', '组织级管理员')
ON CONFLICT (code) DO NOTHING;

-- 插入普通用户角色
INSERT INTO mf_sys_role (code, name, data_scope, sort, status, remark)
VALUES ('member', '成员', 'self', 2, 'active', '普通成员,可见自己的资源')
ON CONFLICT (code) DO NOTHING;

-- 插入访客角色
INSERT INTO mf_sys_role (code, name, data_scope, sort, status, remark)
VALUES ('viewer', '访客', 'self', 3, 'active', '只读')
ON CONFLICT (code) DO NOTHING;

-- 插入默认管理员用户 (密码: meow@2026)
-- BCrypt hash: $2a$10$227xlzemuj7i1ZGBEuFl7./qmvvUeaZK0UFXRVHcBUViPwK0PRheq
INSERT INTO mf_sys_user (username, password, nickname, real_name, status, email)
VALUES ('admin', '$2a$10$227xlzemuj7i1ZGBEuFl7./qmvvUeaZK0UFXRVHcBUViPwK0PRheq', '超级管理员', '系统管理员', 'active', 'admin@meowflow.com')
ON CONFLICT (username) DO NOTHING;

-- 关联管理员角色 (使用子查询获取动态 ID)
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


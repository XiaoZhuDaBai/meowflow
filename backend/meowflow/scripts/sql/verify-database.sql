-- =============================================================
-- 数据库完整性验证脚本
-- 运行方式: psql -U meowflow -d meowflow -f verify-database.sql
-- 创建日期: 2026-09-12
-- =============================================================

\echo '=========================================='
\echo '喵流 MeowFlow 数据库完整性验证'
\echo '=========================================='
\echo ''

-- ============================================================
-- 1. 核心表存在性检查
-- ============================================================
\echo '=== 1. 核心表存在性检查 ==='
\echo ''

DO $$
DECLARE
    tables TEXT[] := ARRAY[
        'mf_sys_user', 'mf_sys_role', 'mf_sys_permission', 'mf_sys_role_permission',
        'mf_sys_user_role', 'mf_sys_org', 'mf_sys_post',
        'mf_wf_workflow', 'mf_wf_workflow_version', 'mf_wf_execution', 'mf_wf_node_execution',
        'mf_wf_category', 'mf_wf_group',
        'mf_tpl_template', 'mf_tpl_tag', 'mf_tpl_template_tag', 'mf_tpl_rating',
        'mf_mon_alert_rule', 'mf_mon_alert', 'mf_mon_alert_silence',
        'mf_ai_model', 'knowledge_base', 'document', 'document_chunk',
        'mf_integration_config',
        'mf_exe_executor_node', 'mf_exe_task',
        'mf_wf_trigger', 'mf_wf_schedule', 'mf_wf_execution_snapshot',
        'mf_tpl_review_record', 'mf_log_event'
    ];
    t TEXT;
    missing_count INTEGER := 0;
BEGIN
    FOREACH t IN ARRAY tables LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = 'public' AND table_name = t
        ) THEN
            RAISE NOTICE '❌ 表缺失: %', t;
            missing_count := missing_count + 1;
        ELSE
            RAISE NOTICE '✅ 表存在: %', t;
        END IF;
    END LOOP;
    
    IF missing_count > 0 THEN
        RAISE WARNING '⚠️ 共有 % 个表缺失', missing_count;
    ELSE
        RAISE NOTICE '✅ 所有核心表都已创建';
    END IF;
END $$;
\echo ''

-- ============================================================
-- 2. 必需数据检查
-- ============================================================
\echo '=== 2. 必需数据检查 ==='
\echo ''

\echo '--- 2.1 管理员账号 ---'
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '管理员账号: ' || COUNT(*) || ' 个' AS info
FROM mf_sys_user WHERE username = 'admin';

\echo ''
\echo '--- 2.2 角色数据 ---'
SELECT 
    CASE WHEN COUNT(*) >= 4 THEN '✅' ELSE '❌' END AS status,
    '角色数量: ' || COUNT(*) || ' 个' AS info
FROM mf_sys_role;

SELECT name, code FROM mf_sys_role ORDER BY sort;

\echo ''
\echo '--- 2.3 权限数据 ---'
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '权限数量: ' || COUNT(*) || ' 个' AS info
FROM mf_sys_permission;

SELECT 
    type, COUNT(*) AS count
FROM mf_sys_permission
GROUP BY type;

\echo ''
\echo '--- 2.4 角色权限关联 ---'
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '角色权限关联: ' || COUNT(*) || ' 条' AS info
FROM mf_sys_role_permission;

-- 检查超级管理员是否有权限
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '超级管理员权限: ' || COUNT(*) || ' 个' AS info
FROM mf_sys_role_permission rp
JOIN mf_sys_role r ON rp.role_id = r.id
WHERE r.code = 'super_admin';

\echo ''
\echo '--- 2.5 工作流分类 ---'
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '工作流分类: ' || COUNT(*) || ' 个' AS info
FROM mf_wf_category;

\echo ''
\echo '--- 2.6 AI 模型配置 ---'
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    'AI 模型: ' || COUNT(*) || ' 个' AS info
FROM mf_ai_model;

\echo ''
\echo '--- 2.7 模板数据 ---'
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '模板总数: ' || COUNT(*) || ' 个' AS info
FROM mf_tpl_template;

SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '已审核模板: ' || COUNT(*) || ' 个' AS info
FROM mf_tpl_template WHERE review_status = 'approved';

SELECT 
    CASE WHEN COUNT(*) = 0 THEN '✅' ELSE '❌' END AS status,
    '空模板定义: ' || COUNT(*) || ' 个' AS info
FROM mf_tpl_template
WHERE jsonb_array_length(COALESCE(definition->'nodes', '[]'::jsonb)) = 0;

\echo ''
\echo '--- 2.8 模板标签 ---'
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '模板标签: ' || COUNT(*) || ' 个' AS info
FROM mf_tpl_tag;

\echo ''
\echo '--- 2.9 集成配置 ---'
SELECT 
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '集成配置: ' || COUNT(*) || ' 个' AS info
FROM mf_integration_config;

\echo ''
\echo '--- 2.10 告警规则 ---'
SELECT
    CASE WHEN COUNT(*) > 0 THEN '✅' ELSE '❌' END AS status,
    '告警规则: ' || COUNT(*) || ' 条' AS info
FROM mf_mon_alert_rule;

\echo ''

-- ============================================================
-- 3. Flyway 版本记录检查
-- ============================================================
\echo '=== 3. Flyway 版本记录 ==='
\echo ''

DO $$
BEGIN
    IF to_regclass('public.flyway_schema_history') IS NULL THEN
        RAISE NOTICE 'ℹ️ 未启用 Flyway，数据库由 scripts/sql/init.sql 初始化';
    ELSE
        RAISE NOTICE 'ℹ️ 已检测到 Flyway 历史表，请单独查询 flyway_schema_history 核对版本';
    END IF;
END $$;

\echo ''

-- ============================================================
-- 4. 索引检查
-- ============================================================
\echo '=== 4. 关键索引检查 ==='
\echo ''

DO $$
DECLARE
    indexes TEXT[] := ARRAY[
        'idx_mf_sys_user_username', 'idx_mf_sys_user_email',
        'idx_mf_wf_workflow_owner', 'idx_mf_wf_workflow_status',
        'idx_mf_wf_execution_workflow', 'idx_mf_wf_execution_status',
        'idx_mf_tpl_template_category', 'idx_mf_tpl_template_review_status',
        'idx_mf_ai_model_enabled'
    ];
    idx TEXT;
    missing_count INTEGER := 0;
BEGIN
    FOREACH idx IN ARRAY indexes LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes 
            WHERE tablename IN (
                SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'
            ) AND indexname = idx
        ) THEN
            RAISE NOTICE '⚠️ 索引缺失: %', idx;
            missing_count := missing_count + 1;
        ELSE
            RAISE NOTICE '✅ 索引存在: %', idx;
        END IF;
    END LOOP;
    
    IF missing_count > 0 THEN
        RAISE WARNING '⚠️ 共有 % 个索引缺失', missing_count;
    ELSE
        RAISE NOTICE '✅ 所有关键索引都已创建';
    END IF;
END $$;
\echo ''

-- ============================================================
-- 5. 总结
-- ============================================================
\echo '=========================================='
\echo '验证完成'
\echo '=========================================='
\echo ''
\echo '如果上述所有检查都显示 ✅，说明数据库已正确初始化。'
\echo '如果有任何 ❌ 或 ⚠️，请执行相应的修复 SQL 文件。'
\echo ''
\echo '修复文件顺序:'
\echo '  1. V11__rbac_seed.sql    - RBAC 权限数据'
\echo '  2. V12__template_tag_fix.sql - 模板标签和分类'
\echo '  3. V15__ai_model_seed.sql    - AI 模型配置'
\echo '  4. V17__restore_alert_rules.sql - 默认告警规则'
\echo '  5. V18__restore_critical_indexes.sql - 关键性能索引'
\echo '  6. V19__restore_integration_configs.sql - 默认集成配置'
\echo '  7. V20__normalize_ai_model_seed.sql - AI 模型去重归一化'
\echo '  8. V21__alert_detail_contract.sql - 告警详情字段'
\echo '  9. V22__backfill_builtin_template_definitions.sql - 完整内置模板定义'
\echo ''

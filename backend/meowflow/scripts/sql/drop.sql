-- =============================================================
-- MeowFlow PostgreSQL 清库脚本
-- 用途: 删除数据库中所有 MeowFlow 相关的表、视图、函数、触发器
-- 依赖: PostgreSQL 14+
-- 注意: 使用 CASCADE 会自动删除依赖的对象，请谨慎使用！
-- =============================================================

BEGIN;

-- 1. 删除所有 mf_* 开头的表 (使用 CASCADE 自动删除依赖的外键、索引、触发器)
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOR tbl IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'mf_%'
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I CASCADE', tbl);
        RAISE NOTICE 'Dropped table: %', tbl;
    END LOOP;
END $$;

-- 2. 删除知识库相关表
DROP TABLE IF EXISTS document_chunk CASCADE;
DROP TABLE IF EXISTS document CASCADE;
DROP TABLE IF EXISTS knowledge_base CASCADE;
DROP TABLE IF EXISTS vector_store CASCADE;

-- 3. 删除公共函数 (如果存在)
DROP FUNCTION IF EXISTS mf_set_audit_columns();
DROP FUNCTION IF EXISTS mf_soft_delete_check();
DROP FUNCTION IF EXISTS set_update_time();
DROP FUNCTION IF EXISTS mf_tpl_template_search_refresh();

-- 4. 删除扩展 (可选，一般不建议删除已安装的扩展)
-- DROP EXTENSION IF EXISTS "uuid-ossp";
-- DROP EXTENSION IF EXISTS "pgcrypto";
-- DROP EXTENSION IF EXISTS btree_gist;
-- DROP EXTENSION IF EXISTS pg_trgm;

COMMIT;

-- 验证清理结果
SELECT
    'Tables remaining in public schema:' AS info;
SELECT tablename, tableowner
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;


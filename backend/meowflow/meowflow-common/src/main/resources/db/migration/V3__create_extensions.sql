-- =============================================================
-- V3__create_extensions.sql
-- 喵流 MeowFlow PostgreSQL 扩展
-- 创建日期: 2026-07-13
-- =============================================================

-- UUID 生成扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 加密扩展
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- GIST 索引增强
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 模糊匹配扩展 (用于全文检索,依赖 V2 索引)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- pgvector 向量扩展 (可选,知识库向量检索用)
-- 如果需要向量检索,取消下面注释:
-- CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- 公共函数
-- ============================================================

-- 审计字段自动维护函数: create_time / update_time
CREATE OR REPLACE FUNCTION mf_set_audit_columns()
RETURNS TRIGGER AS $$
BEGIN
    IF to_jsonb(NEW) ? 'update_time' THEN
        NEW := jsonb_populate_record(NEW, jsonb_build_object('update_time', CURRENT_TIMESTAMP));
    END IF;

    IF TG_OP = 'INSERT' AND to_jsonb(NEW) ? 'create_time' THEN
        IF (to_jsonb(NEW) ->> 'create_time') IS NULL THEN
            NEW := jsonb_populate_record(NEW, jsonb_build_object('create_time', CURRENT_TIMESTAMP));
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION mf_set_audit_columns() IS '公共审计字段自动维护:create_time/update_time';

-- 软删除字段默认值函数
CREATE OR REPLACE FUNCTION mf_soft_delete_check()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.deleted IS NULL THEN
        NEW.deleted := FALSE;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION mf_soft_delete_check() IS '软删除字段默认值';

-- ============================================================
-- 触发器 (在表创建后手动应用)
-- ============================================================

-- 为常用表添加审计触发器
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT table_name FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name LIKE 'mf_%'
    LOOP
        -- 动态创建审计触发器 (如果表有 create_time/update_time 字段)
        EXECUTE format(
            'CREATE OR REPLACE TRIGGER trg_%s_audit
             BEFORE INSERT OR UPDATE ON %s
             FOR EACH ROW EXECUTE FUNCTION mf_set_audit_columns()',
            r.table_name, r.table_name
        );
    END LOOP;
END $$;

-- 为有 deleted 字段的表添加软删除触发器
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT t.table_name
        FROM information_schema.tables t
        JOIN information_schema.columns c
          ON c.table_schema = t.table_schema
         AND c.table_name = t.table_name
        WHERE t.table_schema = 'public'
          AND t.table_name LIKE 'mf_%'
          AND c.column_name = 'deleted'
    LOOP
        EXECUTE format(
            'CREATE OR REPLACE TRIGGER trg_%s_soft
             BEFORE INSERT ON %s
             FOR EACH ROW EXECUTE FUNCTION mf_soft_delete_check()',
            r.table_name, r.table_name
        );
    END LOOP;
END $$;



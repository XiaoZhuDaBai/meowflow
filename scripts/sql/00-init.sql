-- =============================================================
-- 喵流 MeowFlow 数据库初始化脚本
-- 库名: meowflow
-- 编码: UTF8
-- 前缀约定: 所有表 mf_<module>_<entity>
-- 创建日期: 2026-07-12
-- =============================================================

\set ON_ERROR_STOP on

-- 1. 创建数据库(若不存在)
SELECT 'CREATE DATABASE meowflow WITH ENCODING ''UTF8'' LC_COLLATE ''zh_CN.UTF-8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE pg_database = 'meowflow') \gexec

-- 切换到 meowflow 库
\c meowflow

-- 2. 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- UUID 生成
CREATE EXTENSION IF NOT EXISTS "pgcrypto";       -- 加密
CREATE EXTENSION IF NOT EXISTS "btree_gist";     -- GIST 索引增强

-- 3. 统一 schema(可选,默认 public)
-- 这里我们用 public schema,通过表前缀 mf_ 隔离

-- 4. 公共字段触发器: 自动维护 create_time / update_time
CREATE OR REPLACE FUNCTION mf_set_audit_columns()
RETURNS TRIGGER AS $$
BEGIN
  NEW.update_time := CURRENT_TIMESTAMP;
  IF TG_OP = 'INSERT' THEN
    IF NEW.create_time IS NULL THEN
      NEW.create_time := CURRENT_TIMESTAMP;
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION mf_set_audit_columns() IS '公共审计字段自动维护:create_time/update_time';

-- 5. 软删除触发器(替代硬 DELETE,要求字段 deleted BOOLEAN)
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
-- 模块拆分(本文件仅做全局基础设施)
-- 业务表 DDL 见: 01-schema.sql
-- 索引 DDL 见: 02-index.sql
-- 初始数据见: 03-data.sql
-- ============================================================

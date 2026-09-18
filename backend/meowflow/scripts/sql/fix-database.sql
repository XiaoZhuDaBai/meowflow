# =============================================================
# MeowFlow 数据库一键修复脚本
# 用途: 修复 RBAC 权限、模板标签、AI 模型等数据缺失问题
# 使用: psql -U meowflow -d meowflow -f fix-database.sql
# 创建日期: 2026-09-12
# =============================================================

-- 执行顺序很重要，请勿调整顺序

\echo '=========================================='
\echo '开始执行数据库修复...'
\echo '=========================================='

-- 1. RBAC 权限数据初始化
\echo ''
\echo '[1/3] 执行 V11__rbac_seed.sql - RBAC 权限数据...'
\i V11__rbac_seed.sql

-- 2. 模板标签和分类修复
\echo ''
\echo '[2/3] 执行 V12__template_tag_fix.sql - 模板标签和分类...'
\i V12__template_tag_fix.sql

-- 3. AI 模型配置初始化
\echo ''
\echo '[3/3] 执行 V13__ai_model_seed.sql - AI 模型配置...'
\i V13__ai_model_seed.sql

\echo ''
\echo '=========================================='
\echo '数据库修复完成!'
\echo '=========================================='
\echo ''
\echo '验证数据完整性请执行:'
\echo '  psql -U meowflow -d meowflow -f verify-database.sql'
\echo ''

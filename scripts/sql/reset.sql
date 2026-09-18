-- =============================================================
-- ⚠️ 仅本地开发用 - 清空全部业务表(保留 schema)
-- =============================================================

\set ON_ERROR_STOP on

TRUNCATE TABLE
  -- mf_sys
  mf_sys_audit_log,
  mf_sys_login_log,
  mf_sys_role_permission,
  mf_sys_user_role,
  mf_sys_permission,
  mf_sys_post,
  mf_sys_role,
  mf_sys_user,
  mf_sys_org,
  -- mf_wf
  mf_wf_execution_log,
  mf_wf_node_execution,
  mf_wf_execution,
  mf_wf_workflow_version,
  mf_wf_workflow,
  mf_wf_group,
  mf_wf_category,
  -- mf_tpl
  mf_tpl_review,
  mf_tpl_template,
  -- mf_mon
  mf_mon_metric_daily,
  mf_mon_alert,
  mf_mon_alert_rule,
  -- mf_ai
  mf_ai_invoke_log,
  mf_ai_chunk,
  mf_ai_document,
  mf_ai_knowledge,
  mf_ai_model,
  -- mf_int
  mf_int_send_log,
  mf_int_config,
  mf_int_mcp_tool
RESTART IDENTITY CASCADE;

\echo '✅ 已清空全部业务表'

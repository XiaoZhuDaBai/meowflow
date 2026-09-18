-- =============================================================
-- 喵流 MeowFlow - 索引脚本
-- 依赖: 必须先执行 01-schema.sql
-- 创建日期: 2026-07-12
-- =============================================================

\set ON_ERROR_STOP on

-- ==== mf_sys_* ====
CREATE INDEX idx_mf_sys_org_parent   ON mf_sys_org(parent_id);
CREATE INDEX idx_mf_sys_org_status   ON mf_sys_org(status);
CREATE INDEX idx_mf_sys_user_org     ON mf_sys_user(org_id);
CREATE INDEX idx_mf_sys_user_status  ON mf_sys_user(status);
CREATE INDEX idx_mf_sys_user_phone   ON mf_sys_user(phone);
CREATE INDEX idx_mf_sys_role_code    ON mf_sys_role(code);
CREATE INDEX idx_mf_sys_perm_parent  ON mf_sys_permission(parent_id);
CREATE INDEX idx_mf_sys_perm_type    ON mf_sys_permission(type);
CREATE INDEX idx_mf_sys_login_log_at ON mf_sys_login_log(login_at);
CREATE INDEX idx_mf_sys_login_log_user ON mf_sys_login_log(user_id);
CREATE INDEX idx_mf_sys_audit_user   ON mf_sys_audit_log(user_id);
CREATE INDEX idx_mf_sys_audit_module ON mf_sys_audit_log(module);
CREATE INDEX idx_mf_sys_audit_at     ON mf_sys_audit_log(created_at);

-- ==== mf_wf_* ====
CREATE INDEX idx_mf_wf_workflow_owner        ON mf_wf_workflow(owner_id);
CREATE INDEX idx_mf_wf_workflow_status       ON mf_wf_workflow(status);
CREATE INDEX idx_mf_wf_workflow_category     ON mf_wf_workflow(category_id);
CREATE INDEX idx_mf_wf_workflow_update_time  ON mf_wf_workflow(update_time DESC);
-- 全文检索(GIN trigram, 需要扩展)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_mf_wf_workflow_name_trgm ON mf_wf_workflow USING gin (name gin_trgm_ops);

CREATE INDEX idx_mf_wf_version_workflow ON mf_wf_workflow_version(workflow_id);
CREATE INDEX idx_mf_wf_version_status   ON mf_wf_workflow_version(publish_status);

CREATE INDEX idx_mf_wf_exec_workflow   ON mf_wf_execution(workflow_id);
CREATE INDEX idx_mf_wf_exec_status     ON mf_wf_execution(status);
CREATE INDEX idx_mf_wf_exec_started    ON mf_wf_execution(started_at DESC);
CREATE INDEX idx_mf_wf_exec_user       ON mf_wf_execution(trigger_user_id);
-- 复合索引: 工作流ID + 开始时间(常用于工作流执行历史查询)
CREATE INDEX idx_mf_wf_exec_wf_started ON mf_wf_execution(workflow_id, started_at DESC);

CREATE INDEX idx_mf_wf_node_exec_exec ON mf_wf_node_execution(execution_id);
CREATE INDEX idx_mf_wf_node_exec_node ON mf_wf_node_execution(node_id);
CREATE INDEX idx_mf_wf_node_exec_status ON mf_wf_node_execution(status);

CREATE INDEX idx_mf_wf_log_exec    ON mf_wf_execution_log(execution_id);
CREATE INDEX idx_mf_wf_log_level   ON mf_wf_execution_log(level);
CREATE INDEX idx_mf_wf_log_created ON mf_wf_execution_log(created_at DESC);

-- ==== mf_tpl_* ====
CREATE INDEX idx_mf_tpl_template_cat    ON mf_tpl_template(category_id);
CREATE INDEX idx_mf_tpl_template_status ON mf_tpl_template(status);
CREATE INDEX idx_mf_tpl_template_review ON mf_tpl_template(review_status);
CREATE INDEX idx_mf_tpl_template_score  ON mf_tpl_template(score DESC);
CREATE INDEX idx_mf_tpl_template_use    ON mf_tpl_template(use_count DESC);
CREATE INDEX idx_mf_tpl_review_tpl      ON mf_tpl_review(template_id);
CREATE INDEX idx_mf_tpl_review_user     ON mf_tpl_review(user_id);

-- ==== mf_mon_* ====
CREATE INDEX idx_mf_mon_alert_rule_enabled ON mf_mon_alert_rule(enabled);
CREATE INDEX idx_mf_mon_alert_status        ON mf_mon_alert(status);
CREATE INDEX idx_mf_mon_alert_triggered     ON mf_mon_alert(triggered_at DESC);
CREATE INDEX idx_mf_mon_metric_date         ON mf_mon_metric_daily(metric_date DESC);

-- ==== mf_ai_* ====
CREATE INDEX idx_mf_ai_model_provider ON mf_ai_model(provider);
CREATE INDEX idx_mf_ai_model_enabled  ON mf_ai_model(enabled);
CREATE INDEX idx_mf_ai_model_default  ON mf_ai_model(is_default);
CREATE INDEX idx_mf_ai_doc_kb         ON mf_ai_document(kb_id);
CREATE INDEX idx_mf_ai_doc_status     ON mf_ai_document(status);
CREATE INDEX idx_mf_ai_chunk_doc      ON mf_ai_chunk(document_id);
CREATE INDEX idx_mf_ai_chunk_kb       ON mf_ai_chunk(kb_id);
-- 向量索引(需 pgvector 扩展)
-- CREATE INDEX idx_mf_ai_chunk_embedding ON mf_ai_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_mf_ai_invoke_user     ON mf_ai_invoke_log(user_id);
CREATE INDEX idx_mf_ai_invoke_exec     ON mf_ai_invoke_log(execution_id);
CREATE INDEX idx_mf_ai_invoke_model    ON mf_ai_invoke_log(model_id);
CREATE INDEX idx_mf_ai_invoke_status   ON mf_ai_invoke_log(status);
CREATE INDEX idx_mf_ai_invoke_created  ON mf_ai_invoke_log(created_at DESC);

-- ==== mf_int_* ====
CREATE INDEX idx_mf_int_config_type    ON mf_int_config(type);
CREATE INDEX idx_mf_int_config_enabled ON mf_int_config(enabled);
CREATE INDEX idx_mf_int_send_log_cfg   ON mf_int_send_log(config_id);
CREATE INDEX idx_mf_int_send_log_type  ON mf_int_send_log(type);
CREATE INDEX idx_mf_int_send_log_at    ON mf_int_send_log(created_at DESC);
CREATE INDEX idx_mf_int_mcp_enabled    ON mf_int_mcp_tool(enabled);

\echo '✅ 全部索引创建完成'

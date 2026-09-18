-- ============================================================
-- 补齐当前开发库缺失的关键性能索引，兼容早期未完整执行 V2 的库
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_mf_sys_user_username ON mf_sys_user(username);
CREATE INDEX IF NOT EXISTS idx_mf_sys_user_email ON mf_sys_user(email);

CREATE INDEX IF NOT EXISTS idx_mf_wf_workflow_owner ON mf_wf_workflow(owner_id);
CREATE INDEX IF NOT EXISTS idx_mf_wf_workflow_status ON mf_wf_workflow(status);

CREATE INDEX IF NOT EXISTS idx_mf_wf_execution_workflow ON mf_wf_execution(workflow_id);
CREATE INDEX IF NOT EXISTS idx_mf_wf_execution_status ON mf_wf_execution(status);

CREATE INDEX IF NOT EXISTS idx_mf_tpl_template_category ON mf_tpl_template(category_id);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_template_review_status ON mf_tpl_template(review_status);

CREATE INDEX IF NOT EXISTS idx_mf_ai_model_enabled ON mf_ai_model(enabled);

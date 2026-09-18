CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =============================================================
-- V2__create_indexes.sql
-- 喵流 MeowFlow 性能索引
-- 依赖: V1__init_schema.sql
-- 创建日期: 2026-07-13
-- =============================================================

-- ============================================================
-- 系统模块索引 (mf_sys_*)
-- ============================================================

-- 组织索引
CREATE INDEX idx_mf_sys_org_parent ON mf_sys_org(parent_id);
CREATE INDEX idx_mf_sys_org_status ON mf_sys_org(status);
CREATE INDEX idx_mf_sys_org_deleted ON mf_sys_org(deleted);

-- 用户索引
CREATE INDEX idx_mf_sys_user_org ON mf_sys_user(org_id);
CREATE INDEX idx_mf_sys_user_status ON mf_sys_user(status);
CREATE INDEX idx_mf_sys_user_phone ON mf_sys_user(phone);
CREATE INDEX idx_mf_sys_user_email ON mf_sys_user(email);
CREATE UNIQUE INDEX idx_mf_sys_user_username ON mf_sys_user(username);

-- 角色索引
CREATE INDEX idx_mf_sys_role_code ON mf_sys_role(code);
CREATE INDEX idx_mf_sys_role_status ON mf_sys_role(status);

-- 权限索引
CREATE INDEX idx_mf_sys_permission_parent ON mf_sys_permission(parent_id);
CREATE INDEX idx_mf_sys_permission_type ON mf_sys_permission(type);
CREATE INDEX idx_mf_sys_permission_status ON mf_sys_permission(status);
CREATE INDEX idx_mf_sys_permission_code ON mf_sys_permission(code);

-- 日志索引
CREATE INDEX idx_mf_sys_login_log_user ON mf_sys_login_log(user_id);
CREATE INDEX idx_mf_sys_login_log_at ON mf_sys_login_log(login_at);
CREATE INDEX idx_mf_sys_login_log_status ON mf_sys_login_log(status);

CREATE INDEX idx_mf_sys_audit_user ON mf_sys_audit_log(user_id);
CREATE INDEX idx_mf_sys_audit_module ON mf_sys_audit_log(module);
CREATE INDEX idx_mf_sys_audit_created ON mf_sys_audit_log(created_at);


-- ============================================================
-- 工作流模块索引 (mf_wf_*)
-- ============================================================

-- 工作流主表索引
CREATE INDEX idx_mf_wf_workflow_category ON mf_wf_workflow(category_id);
CREATE INDEX idx_mf_wf_workflow_owner ON mf_wf_workflow(owner_id);
CREATE INDEX idx_mf_wf_workflow_status ON mf_wf_workflow(status);
CREATE INDEX idx_mf_wf_workflow_deleted ON mf_wf_workflow(deleted);
CREATE INDEX idx_mf_wf_workflow_update_time ON mf_wf_workflow(update_time DESC);

-- 全文检索索引 (需要 pg_trgm 扩展)
CREATE INDEX idx_mf_wf_workflow_name_trgm ON mf_wf_workflow USING gin (name gin_trgm_ops);
CREATE INDEX idx_mf_wf_workflow_desc_trgm ON mf_wf_workflow USING gin (description gin_trgm_ops);

-- 工作流版本索引
CREATE INDEX idx_mf_wf_workflow_version_wf ON mf_wf_workflow_version(workflow_id);
CREATE INDEX idx_mf_wf_workflow_version_status ON mf_wf_workflow_version(publish_status);

-- 执行记录索引 - 关键复合索引
CREATE INDEX idx_mf_wf_execution_workflow ON mf_wf_execution(workflow_id);
CREATE INDEX idx_mf_wf_execution_status ON mf_wf_execution(status);
CREATE INDEX idx_mf_wf_execution_trigger_user ON mf_wf_execution(trigger_user_id);
CREATE INDEX idx_mf_wf_execution_started ON mf_wf_execution(started_at DESC);

-- 复合索引: 工作流ID + 创建时间 (工作流执行历史查询)
CREATE INDEX idx_mf_wf_execution_wf_created ON mf_wf_execution(workflow_id, create_time DESC);

-- 复合索引: 状态 + 创建时间 (状态筛选 + 时间排序)
CREATE INDEX idx_mf_wf_execution_status_created ON mf_wf_execution(status, create_time DESC);

-- 节点执行索引
CREATE INDEX idx_mf_wf_node_execution_exec ON mf_wf_node_execution(execution_id);
CREATE INDEX idx_mf_wf_node_execution_node ON mf_wf_node_execution(node_id);
CREATE INDEX idx_mf_wf_node_execution_status ON mf_wf_node_execution(status);
CREATE INDEX idx_mf_wf_node_execution_created ON mf_wf_node_execution(create_time DESC);

-- 复合索引: 执行ID + 创建时间 (节点执行历史)
CREATE INDEX idx_mf_wf_node_execution_exec_created ON mf_wf_node_execution(execution_id, create_time DESC);

-- 执行日志索引
CREATE INDEX idx_mf_wf_execution_log_exec ON mf_wf_execution_log(execution_id);
CREATE INDEX idx_mf_wf_execution_log_level ON mf_wf_execution_log(level);
CREATE INDEX idx_mf_wf_execution_log_created ON mf_wf_execution_log(created_at DESC);

-- 复合索引: 执行ID + 创建时间 (执行日志查询)
CREATE INDEX idx_mf_wf_execution_log_exec_created ON mf_wf_execution_log(execution_id, created_at DESC);

-- 分类和分组索引
CREATE INDEX idx_mf_wf_category_status ON mf_wf_category(status);
CREATE INDEX idx_mf_wf_group_user ON mf_wf_group(user_id);

-- 触发器和调度索引
CREATE INDEX idx_mf_wf_trigger_workflow ON mf_wf_trigger(workflow_id);
CREATE INDEX idx_mf_wf_trigger_type ON mf_wf_trigger(type);
CREATE INDEX idx_mf_wf_trigger_enabled ON mf_wf_trigger(enabled);

CREATE INDEX idx_mf_wf_schedule_workflow ON mf_wf_schedule(workflow_id);
CREATE INDEX idx_mf_wf_schedule_enabled ON mf_wf_schedule(enabled);
CREATE INDEX idx_mf_wf_schedule_next_run ON mf_wf_schedule(next_run_at);


-- ============================================================
-- 模板模块索引 (mf_tpl_*)
-- ============================================================

-- 模板索引
CREATE INDEX idx_mf_tpl_template_category ON mf_tpl_template(category_id);
CREATE INDEX idx_mf_tpl_template_status ON mf_tpl_template(status);
CREATE INDEX idx_mf_tpl_template_review_status ON mf_tpl_template(review_status);
CREATE INDEX idx_mf_tpl_template_author ON mf_tpl_template(author_id);

-- 复合索引: 分类 + 状态 (模板列表查询)
CREATE INDEX idx_mf_tpl_template_cat_status ON mf_tpl_template(category_id, status);

-- 复合索引: 评分 DESC + 使用次数 DESC (模板推荐/排序)
CREATE INDEX idx_mf_tpl_template_score_use ON mf_tpl_template(score DESC, use_count DESC);

-- 模板评价索引
CREATE INDEX idx_mf_tpl_review_template ON mf_tpl_review(template_id);
CREATE INDEX idx_mf_tpl_review_user ON mf_tpl_review(user_id);


-- ============================================================
-- 监控模块索引 (mf_mon_*)
-- ============================================================

-- 告警规则索引
CREATE INDEX idx_mf_mon_alert_rule_enabled ON mf_mon_alert_rule(enabled);
CREATE INDEX idx_mf_mon_alert_rule_metric ON mf_mon_alert_rule(metric);

-- 告警记录索引
CREATE INDEX idx_mf_mon_alert_rule_id ON mf_mon_alert(rule_id);
CREATE INDEX idx_mf_mon_alert_status ON mf_mon_alert(status);
CREATE INDEX idx_mf_mon_alert_triggered ON mf_mon_alert(triggered_at DESC);

-- 每日指标索引
CREATE INDEX idx_mf_mon_metric_daily_date ON mf_mon_metric_daily(metric_date DESC);


-- ============================================================
-- AI 模块索引 (mf_ai_*)
-- ============================================================

-- 模型配置索引
CREATE INDEX idx_mf_ai_model_provider ON mf_ai_model(provider);
CREATE INDEX idx_mf_ai_model_enabled ON mf_ai_model(enabled);
CREATE INDEX idx_mf_ai_model_default ON mf_ai_model(is_default);
CREATE INDEX idx_mf_ai_model_priority ON mf_ai_model(priority DESC);

-- 知识库索引
CREATE INDEX idx_mf_ai_knowledge_owner ON mf_ai_knowledge(owner_id);
CREATE INDEX idx_mf_ai_knowledge_status ON mf_ai_knowledge(status);

-- 文档索引
CREATE INDEX idx_mf_ai_document_kb ON mf_ai_document(kb_id);
CREATE INDEX idx_mf_ai_document_status ON mf_ai_document(status);

-- 向量分块索引 (向量相似度检索 - 需要 pgvector 扩展)
-- 如果使用 pgvector,取消下面注释:
-- CREATE INDEX idx_mf_ai_chunk_embedding ON mf_ai_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 备选索引: BYTEA embedding 降级方案
CREATE INDEX idx_mf_ai_chunk_doc ON mf_ai_chunk(document_id);
CREATE INDEX idx_mf_ai_chunk_kb ON mf_ai_chunk(kb_id);

-- AI 调用日志索引
CREATE INDEX idx_mf_ai_invoke_log_user ON mf_ai_invoke_log(user_id);
CREATE INDEX idx_mf_ai_invoke_log_exec ON mf_ai_invoke_log(execution_id);
CREATE INDEX idx_mf_ai_invoke_log_model ON mf_ai_invoke_log(model_id);
CREATE INDEX idx_mf_ai_invoke_log_status ON mf_ai_invoke_log(status);
CREATE INDEX idx_mf_ai_invoke_log_created ON mf_ai_invoke_log(created_at DESC);


-- ============================================================
-- 集成模块索引 (mf_int_*)
-- ============================================================

-- 集成配置索引
CREATE INDEX idx_mf_int_config_type ON mf_int_config(type);
CREATE INDEX idx_mf_int_config_enabled ON mf_int_config(enabled);

-- 发送日志索引
CREATE INDEX idx_mf_int_send_log_config ON mf_int_send_log(config_id);
CREATE INDEX idx_mf_int_send_log_type ON mf_int_send_log(type);
CREATE INDEX idx_mf_int_send_log_status ON mf_int_send_log(status);
CREATE INDEX idx_mf_int_send_log_created ON mf_int_send_log(created_at DESC);

-- MCP 工具索引
CREATE INDEX idx_mf_int_mcp_tool_enabled ON mf_int_mcp_tool(enabled);
CREATE INDEX idx_mf_int_mcp_tool_name ON mf_int_mcp_tool(name);


-- ============================================================
-- 执行器模块索引 (mf_exe_*)
-- ============================================================

-- 执行器节点索引
CREATE INDEX idx_mf_exe_executor_node_status ON mf_exe_executor_node(status);
CREATE INDEX idx_mf_exe_executor_node_heartbeat ON mf_exe_executor_node(last_heartbeat);

-- 任务索引
CREATE INDEX idx_mf_exe_task_exec ON mf_exe_task(execution_id);
CREATE INDEX idx_mf_exe_task_node ON mf_exe_task(node_id);
CREATE INDEX idx_mf_exe_task_status ON mf_exe_task(status);
CREATE INDEX idx_mf_exe_task_priority ON mf_exe_task(priority DESC);
CREATE INDEX idx_mf_exe_task_created ON mf_exe_task(create_time DESC);


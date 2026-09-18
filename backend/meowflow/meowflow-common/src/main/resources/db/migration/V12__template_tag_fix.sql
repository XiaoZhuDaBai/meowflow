-- =============================================================
-- V12__template_tag_fix.sql
-- 模板标签、分类字段与非整型 template_version 修复
-- 注意：模板主体种子由 V7 负责，本迁移不再重复插入模板。
-- =============================================================

CREATE TABLE IF NOT EXISTS mf_tpl_tag (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(64) NOT NULL UNIQUE,
    color        VARCHAR(16),
    sort         INT DEFAULT 0,
    usage_count  BIGINT DEFAULT 0,
    create_by    BIGINT,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark       VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_tag_usage ON mf_tpl_tag(usage_count DESC);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_tag_sort ON mf_tpl_tag(sort);

CREATE TABLE IF NOT EXISTS mf_tpl_template_tag (
    template_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (template_id, tag_id)
);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_template_tag_tag ON mf_tpl_template_tag(tag_id);

-- 分类继续复用 mf_wf_category，补齐 TemplateCategory 实体字段。
ALTER TABLE mf_wf_category
    ADD COLUMN IF NOT EXISTS level       INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS remark      VARCHAR(255);

INSERT INTO mf_wf_category (parent_id, code, name, icon, sort, level, description)
VALUES
    (0, 'cs',      '客服场景', 'fa-headset',    1, 1, '智能客服、工单处理、用户反馈分析等'),
    (0, 'hr',      '人力资源', 'fa-users',      2, 1, '简历筛选、审批流程、员工管理等'),
    (0, 'ops',     '运营提效', 'fa-chart-line', 3, 1, '数据报告生成、文案创作、定时任务等'),
    (0, 'finance', '财务行政', 'fa-calculator', 4, 1, '发票处理、报销审批、应收款管理等'),
    (0, 'data',    '数据处理', 'fa-database',   5, 1, 'ETL同步、数据清洗、备份任务等'),
    (0, 'general', '通用场景', 'fa-cube',       9, 1, 'Webhook接入、定时任务、通用脚本等')
ON CONFLICT (code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    icon = EXCLUDED.icon,
    sort = EXCLUDED.sort,
    level = EXCLUDED.level,
    description = EXCLUDED.description;

-- 按 template.scene 对齐实际分类 ID，避免依赖固定自增 ID。
UPDATE mf_tpl_template t
SET category_id = c.id
FROM mf_wf_category c
WHERE t.scene = c.code
  AND (t.category_id IS DISTINCT FROM c.id);

INSERT INTO mf_tpl_tag (name, color, sort)
VALUES
    ('客服', '#f472b6', 1), ('AI', '#f97316', 2), ('自动回复', '#6366f1', 3),
    ('工单', '#fbbf24', 4), ('反馈', '#22d3ee', 5), ('HR', '#34d399', 6),
    ('招聘', '#10b981', 7), ('审批', '#8b5cf6', 8), ('表单', '#06b6d4', 9),
    ('入职', '#a855f7', 10), ('运营', '#fbbf24', 11), ('会议', '#22d3ee', 12),
    ('日报', '#60a5fa', 13), ('定时', '#a78bfa', 14), ('文案', '#ec4899', 15),
    ('数据', '#22d3ee', 16), ('财务', '#10b981', 17), ('发票', '#06b6d4', 18),
    ('催收', '#ef4444', 19), ('同步', '#0ea5e9', 20), ('通用', '#9ca3af', 21),
    ('Webhook', '#8b5cf6', 22), ('回调', '#ec4899', 23), ('备份', '#22c55e', 24),
    ('清洗', '#0ea5e9', 25), ('电商', '#f59e0b', 26), ('团队', '#3b82f6', 27),
    ('知识库', '#06b6d4', 28), ('RAG', '#8b5cf6', 29), ('自动化', '#22c55e', 30)
ON CONFLICT (name) DO UPDATE SET color = EXCLUDED.color, sort = EXCLUDED.sort;

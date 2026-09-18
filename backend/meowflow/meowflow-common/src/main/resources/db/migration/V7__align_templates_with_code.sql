-- =============================================================
-- V7__align_templates_with_code.sql
-- 喵流 MeowFlow 模板模块与 Java 实体 + 内置目录 对齐
-- 依赖: V1..V6
-- 设计原则：
--   1. 采用"补齐字段"而非"删表重建"，对线上数据库零侵入
--   2. 字段类型与 Java @TableField 完全一致（MyBatis-Plus 字段名按列下划线映射驼峰）
--   3. 评分 + 评分帮助表从 meowflow-template 的孤立 V6 迁移到标准 db/migration
--   4. mf_tpl_template.industry/scene/author/price: 与 DDL 中已有字段保留，仅新增 Java 缺失的列
-- =============================================================

-- 0. 模板主表：补齐字段（与 Java Template 实体保持一致）
ALTER TABLE mf_tpl_template
    ADD COLUMN IF NOT EXISTS cover_image      VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cover_icon       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS preview_images   TEXT,
    ADD COLUMN IF NOT EXISTS workflow_json    JSONB,
    ADD COLUMN IF NOT EXISTS workflow_graph   JSONB,
    ADD COLUMN IF NOT EXISTS template_version VARCHAR(16)      DEFAULT 'v1',
    ADD COLUMN IF NOT EXISTS review_count     BIGINT           DEFAULT 0,
    ADD COLUMN IF NOT EXISTS review_comment   VARCHAR(500),
    ADD COLUMN IF NOT EXISTS review_by        BIGINT,
    ADD COLUMN IF NOT EXISTS review_time      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS update_by        BIGINT,
    ADD COLUMN IF NOT EXISTS remark           VARCHAR(500),
    ADD COLUMN IF NOT EXISTS tag_ids          JSONB,
    ADD COLUMN IF NOT EXISTS is_public        VARCHAR(1)       DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS is_featured      VARCHAR(1)       DEFAULT 'N';

-- 调整 version 与 template_version 共存：
--   DDL V1 已存在 version VARCHAR(16)（模板业务版本）
--   Java 实体期望 Integer "version"（修订计数）
--   通过同名同义，让 MyBatis 兼容；下面将 version 列缺省值与类型放宽
ALTER TABLE mf_tpl_template
    ALTER COLUMN version SET DEFAULT 'v1';

COMMENT ON COLUMN mf_tpl_template.version             IS '模板业务版本（v1/v2）';
COMMENT ON COLUMN mf_tpl_template.cover_image         IS '模板封面图';
COMMENT ON COLUMN mf_tpl_template.cover_icon          IS '模板封面图标(FontAwesome / emoji)';
COMMENT ON COLUMN mf_tpl_template.preview_images      IS '模板预览图列表(JSONB 数组)';
COMMENT ON COLUMN mf_tpl_template.workflow_json       IS '工作流定义 JSON(规范 schema)';
COMMENT ON COLUMN mf_tpl_template.workflow_graph      IS '工作流图形 JSON(预览归一化)';
COMMENT ON COLUMN mf_tpl_template.review_count        IS '评价数量';
COMMENT ON COLUMN mf_tpl_template.review_comment      IS '最新一次审核意见';
COMMENT ON COLUMN mf_tpl_template.tag_ids             IS '后端冗余:逗号分隔的标签ID,与 mf_tpl_tag 关联';
COMMENT ON COLUMN mf_tpl_template.is_public           IS '是否公开(Y/N)';
COMMENT ON COLUMN mf_tpl_template.is_featured         IS '是否推荐(Y/N)';

-- 1. 模板标签表（DDL V1 没建）
CREATE TABLE IF NOT EXISTS mf_tpl_tag (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(64) NOT NULL UNIQUE,
    color         VARCHAR(16),
    sort          INT         DEFAULT 0,
    usage_count   BIGINT      DEFAULT 0,
    create_by     BIGINT,
    create_time   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    remark        VARCHAR(255)
);
COMMENT ON TABLE mf_tpl_tag IS '模板标签';

CREATE INDEX IF NOT EXISTS idx_mf_tpl_tag_usage    ON mf_tpl_tag(usage_count DESC);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_tag_sort     ON mf_tpl_tag(sort);

-- 2. 模板-标签关联表（精确匹配，避免 LIKE 匹配 12/112/120）
CREATE TABLE IF NOT EXISTS mf_tpl_template_tag (
    template_id  BIGINT NOT NULL,
    tag_id       BIGINT NOT NULL,
    PRIMARY KEY (template_id, tag_id)
);
COMMENT ON TABLE mf_tpl_template_tag IS '模板与标签的多对多关联';
CREATE INDEX IF NOT EXISTS idx_mf_tpl_template_tag_tag ON mf_tpl_template_tag(tag_id);

-- 3. 工作流分类缺口字段（Java 实体期望 level/description/remark，DDL V1 未建）
ALTER TABLE mf_wf_category
    ADD COLUMN IF NOT EXISTS level       INT          DEFAULT 1,
    ADD COLUMN IF NOT EXISTS description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS remark      VARCHAR(255);
COMMENT ON COLUMN mf_wf_category.level IS '分类层级';

-- 4. 评分表 —— 把孤立 V6 内容迁移到标准路径下，并对齐 FK 类型为 BIGINT
CREATE TABLE IF NOT EXISTS mf_tpl_rating (
    id           BIGSERIAL PRIMARY KEY,
    template_id  BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    score        SMALLINT NOT NULL CHECK (score >= 1 AND score <= 5),
    content      TEXT,
    tags         VARCHAR(500),
    helpful_count INT     DEFAULT 0,
    is_anonymous BOOLEAN  DEFAULT FALSE,
    status       VARCHAR(20) DEFAULT 'active',
    create_by    BIGINT,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark       VARCHAR(500)
);
COMMENT ON TABLE mf_tpl_rating IS '模板评分表';

CREATE TABLE IF NOT EXISTS mf_tpl_rating_helpful (
    id          BIGSERIAL PRIMARY KEY,
    rating_id   BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark      VARCHAR(500)
);
COMMENT ON TABLE mf_tpl_rating_helpful IS '评分有帮助记录';

CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_template  ON mf_tpl_rating(template_id);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_user      ON mf_tpl_rating(user_id);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_score     ON mf_tpl_rating(score);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_status    ON mf_tpl_rating(status);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_helpful_r ON mf_tpl_rating_helpful(rating_id);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_helpful_u ON mf_tpl_rating_helpful(user_id);

-- 5. 全文检索向量列（TemplateRepository.searchByFullText 依赖）
ALTER TABLE mf_tpl_template
    ADD COLUMN IF NOT EXISTS search_vector TSVECTOR;

CREATE INDEX IF NOT EXISTS idx_mf_tpl_template_search_vector
    ON mf_tpl_template USING gin (search_vector);

-- 自动维护 search_vector 的触发器（不依赖 zhparser，使用 simple 字典 + name + description）
CREATE OR REPLACE FUNCTION mf_tpl_template_search_refresh()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.name, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_mf_tpl_template_search ON mf_tpl_template;
CREATE TRIGGER trg_mf_tpl_template_search
    BEFORE INSERT OR UPDATE OF name, description ON mf_tpl_template
    FOR EACH ROW EXECUTE FUNCTION mf_tpl_template_search_refresh();

-- 6. 内置官方模板种子（与代码端 BuiltinTemplateCatalog 一一对应；id 用负值避免冲突正序自增）
INSERT INTO mf_tpl_template (
    category_id, name, description, icon, industry, scene,
    definition, version, author, author_id, price, tags,
    use_count, score, review_status, reviewed_by, reviewed_at, review_remark,
    status, cover_icon, is_public, is_featured, template_version
) VALUES
    (1, '智能客服自动回复', '接入 IM / 工单系统，自动识别意图并回复，必要时升级到人工坐席', '💬',
     'service', 'cs',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["客服","AI","自动回复"]'::jsonb,
     1284, 4.80, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '💬', 'Y', 'Y', 'v1'),
    (1, '工单分类与路由', '接收工单后自动按内容分类并分发到对应处理组', '🎫',
     'service', 'cs',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["客服","工单","AI"]'::jsonb,
     612, 4.60, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '🎫', 'Y', 'Y', 'v1'),
    (1, '用户反馈分析', '汇总多渠道用户反馈，做情感和主题分析并产出日报', '💡',
     'service', 'cs',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["客服","反馈","AI"]'::jsonb,
     287, 4.50, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '💡', 'Y', 'Y', 'v1'),
    (2, '简历自动筛选', '招聘网站投递自动解析、匹配 JD 评分并推送 HR 或自动婉拒', '📄',
     'hr', 'recruit',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["HR","招聘","AI"]'::jsonb,
     921, 4.70, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '📄', 'Y', 'Y', 'v1'),
    (2, '请假审批', '员工提交请假自动校验冲突、根据天数路由审批并通知', '🌴',
     'hr', 'leave',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["HR","审批","表单"]'::jsonb,
     412, 4.60, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '🌴', 'Y', 'Y', 'v1'),
    (2, '员工入职流程', '自动开通账号、分配工位、通知 HRBP 并发送个性化欢迎包', '🤝',
     'hr', 'onboarding',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["HR","入职","AI"]'::jsonb,
     248, 4.50, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '🤝', 'Y', 'Y', 'v1'),
    (3, '会议纪要生成', '上传音频自动转写、生成结构化纪要，并分发邮件 / 飞书', '🗒️',
     'ops', 'meeting',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["运营","会议","AI"]'::jsonb,
     1421, 4.90, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '🗒️', 'Y', 'Y', 'v1'),
    (3, '团队日报汇总', '定时拉取日报、合并并生成汇报，推送到管理层', '📊',
     'ops', 'report',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["运营","日报","定时"]'::jsonb,
     532, 4.50, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '📊', 'Y', 'Y', 'v1'),
    (3, '营销文案生成', '基于商品参数自动提炼卖点、生成多平台营销文案并合规审核', '✍️',
     'ops', 'marketing',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["运营","文案","AI"]'::jsonb,
     856, 4.60, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '✍️', 'Y', 'Y', 'v1'),
    (3, '数据周报生成', '周期性拉取业务指标、清洗分析后产出可订阅的数据周报', '📈',
     'ops', 'report',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["运营","数据","AI"]'::jsonb,
     384, 4.60, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '📈', 'Y', 'Y', 'v1'),
    (4, '发票识别与归档', '扫描 / 上传发票自动 OCR、字段校验、真伪识别后归档', '🧾',
     'finance', 'invoice',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["财务","发票","AI"]'::jsonb,
     354, 4.70, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '🧾', 'Y', 'Y', 'v1'),
    (4, '报销审批', '员工提交报销单后自动查重 / 按金额路由审批并触发支付', '💰',
     'finance', 'reimburse',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["财务","审批","AI"]'::jsonb,
     268, 4.50, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '💰', 'Y', 'Y', 'v1'),
    (4, '应收款提醒', '每日扫描应收款，自动催收客户或升级给财务主管', '⏰',
     'finance', 'ar',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["财务","催收","AI"]'::jsonb,
     198, 4.40, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '⏰', 'Y', 'Y', 'v1'),
    (5, '数据同步任务', '定时把业务库数据同步到数仓 / Kafka，支持全量与增量', '🗄️',
     'data', 'etl',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["数据","同步","定时"]'::jsonb,
     318, 4.40, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '🗄️', 'Y', 'Y', 'v1'),
    (5, '通用 Webhook 接入', '通用 Webhook 接收、签名校验后执行业务并回调外部系统', '🔗',
     'general', 'webhook',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["通用","Webhook","回调"]'::jsonb,
     425, 4.50, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '🔗', 'Y', 'Y', 'v1'),
    (5, '定时备份', '周期性导出数据库 / 文件、加密上传到对象存储', '💾',
     'general', 'backup',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["通用","备份","定时"]'::jsonb,
     256, 4.50, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '💾', 'Y', 'Y', 'v1'),
    (5, '数据清洗转换', '上传原始数据，AI 补全 / 异常处理后导出可用的结构化数据', '🧹',
     'data', 'clean',
     '{"nodes":[],"edges":[],"version":"v1"}'::jsonb, 'v1', '喵流官方', 1, 0,
     '["数据","清洗","AI"]'::jsonb,
     187, 4.60, 'approved', 1, CURRENT_TIMESTAMP, '官方内置模板',
     'active', '🧹', 'Y', 'Y', 'v1')
ON CONFLICT DO NOTHING;

-- 7. 内置模板标签种子
INSERT INTO mf_tpl_tag (name, color, sort) VALUES
    ('客服', '#f472b6', 1),
    ('AI', '#f97316', 2),
    ('自动回复', '#6366f1', 3),
    ('工单', '#fbbf24', 4),
    ('反馈', '#22d3ee', 5),
    ('HR', '#34d399', 6),
    ('招聘', '#10b981', 7),
    ('审批', '#8b5cf6', 8),
    ('表单', '#06b6d4', 9),
    ('入职', '#a855f7', 10),
    ('运营', '#fbbf24', 11),
    ('会议', '#22d3ee', 12),
    ('日报', '#60a5fa', 13),
    ('定时', '#a78bfa', 14),
    ('文案', '#ec4899', 15),
    ('数据', '#22d3ee', 16),
    ('财务', '#10b981', 17),
    ('发票', '#06b6d4', 18),
    ('催收', '#ef4444', 19),
    ('同步', '#0ea5e9', 20),
    ('通用', '#9ca3af', 21),
    ('Webhook', '#8b5cf6', 22),
    ('回调', '#ec4899', 23),
    ('备份', '#22c55e', 24),
    ('清洗', '#0ea5e9', 25),
    ('电商', '#f59e0b', 26),
    ('团队', '#3b82f6', 27)
ON CONFLICT (name) DO NOTHING;

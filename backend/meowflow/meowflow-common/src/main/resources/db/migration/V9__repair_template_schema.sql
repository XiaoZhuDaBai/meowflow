-- 修复模板主表与 Template 实体缺失列/类型不一致
ALTER TABLE mf_tpl_template
    ADD COLUMN IF NOT EXISTS cover_image      VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cover_icon       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS preview_images   TEXT,
    ADD COLUMN IF NOT EXISTS workflow_json    JSONB,
    ADD COLUMN IF NOT EXISTS workflow_graph   JSONB,
    ADD COLUMN IF NOT EXISTS template_version INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS review_count     BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS review_comment   VARCHAR(500),
    ADD COLUMN IF NOT EXISTS review_by        BIGINT,
    ADD COLUMN IF NOT EXISTS review_time      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS create_by        BIGINT,
    ADD COLUMN IF NOT EXISTS update_by        BIGINT,
    ADD COLUMN IF NOT EXISTS remark           VARCHAR(500),
    ADD COLUMN IF NOT EXISTS tag_ids          JSONB,
    ADD COLUMN IF NOT EXISTS is_public        VARCHAR(1) DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS is_featured      VARCHAR(1) DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS search_vector    TSVECTOR;

ALTER TABLE mf_tpl_template
    ALTER COLUMN template_version DROP DEFAULT;

ALTER TABLE mf_tpl_template
    ALTER COLUMN template_version TYPE INTEGER USING
        CASE WHEN template_version::text ~ '^[0-9]+$' THEN template_version::text::integer ELSE 1 END;

ALTER TABLE mf_tpl_template
    ALTER COLUMN template_version SET DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_mf_tpl_template_search_vector
    ON mf_tpl_template USING gin (search_vector);

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


CREATE TABLE IF NOT EXISTS mf_tpl_rating (
    id           BIGSERIAL PRIMARY KEY,
    template_id  BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    score        SMALLINT NOT NULL CHECK (score >= 1 AND score <= 5),
    content      TEXT,
    tags         VARCHAR(500),
    helpful_count INT DEFAULT 0,
    is_anonymous BOOLEAN DEFAULT FALSE,
    status       VARCHAR(20) DEFAULT 'active',
    create_by    BIGINT,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark       VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_template ON mf_tpl_rating(template_id);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_user ON mf_tpl_rating(user_id);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_score ON mf_tpl_rating(score);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_status ON mf_tpl_rating(status);

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
CREATE INDEX IF NOT EXISTS idx_mf_tpl_rating_helpful_r ON mf_tpl_rating_helpful(rating_id);


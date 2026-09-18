package com.meowflow.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.template.entity.Template;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TemplateRepository extends BaseMapper<Template> {

    @Select("SELECT * FROM mf_tpl_template WHERE status = 'active' ORDER BY create_time DESC")
    IPage<Template> findActiveTemplates(Page<Template> page);

    @Select("SELECT * FROM mf_tpl_template WHERE review_status = #{reviewStatus} AND status = 'active'")
    IPage<Template> findByReviewStatus(Page<Template> page, @Param("reviewStatus") String reviewStatus);

    @Select("SELECT * FROM mf_tpl_template WHERE category_id = #{categoryId} AND status = 'active' AND review_status = 'approved'")
    IPage<Template> findByCategoryId(Page<Template> page, @Param("categoryId") Long categoryId);

    @Select("SELECT * FROM mf_tpl_template WHERE is_featured = 'Y' AND status = 'active' AND review_status = 'approved' ORDER BY use_count DESC, score DESC")
    List<Template> findFeaturedTemplates();

    @Select("SELECT * FROM mf_tpl_template WHERE status = 'active' AND review_status = 'approved' ORDER BY use_count DESC LIMIT #{limit}")
    List<Template> findPopularTemplates(@Param("limit") int limit);

    @Select("SELECT * FROM mf_tpl_template WHERE status = 'active' AND review_status = 'approved' ORDER BY create_time DESC LIMIT #{limit}")
    List<Template> findLatestTemplates(@Param("limit") int limit);

    @Select("SELECT * FROM mf_tpl_template WHERE (name ILIKE CONCAT('%', #{keyword}, '%') OR description ILIKE CONCAT('%', #{keyword}, '%')) AND status = 'active' AND review_status = 'approved'")
    IPage<Template> searchByKeyword(Page<Template> page, @Param("keyword") String keyword);

    @Select("SELECT * FROM mf_tpl_template WHERE create_by = #{userId} AND status <> 'deleted' ORDER BY create_time DESC")
    IPage<Template> findByUserId(Page<Template> page, @Param("userId") Long userId);

    @Update("UPDATE mf_tpl_template SET use_count = COALESCE(use_count, 0) + 1 WHERE id = #{templateId}")
    void incrementUseCount(@Param("templateId") Long templateId);

    /**
     * 批量加 use_count（来自 Redis 计数 flush 任务）。
     * 多次 INCR 合并成一条 UPDATE，避免行锁热点。
     * <p>
     * 注意：传入空集合时 MyBatis foreach 会生成空 IN ()，这里直接 short-circuit 抛错让上层处理。
     */
    @Update("""
            <script>
            <foreach collection="list" item="item" separator=";">
                UPDATE mf_tpl_template
                SET use_count = COALESCE(use_count, 0) + #{item.delta}
                WHERE id = #{item.id}
            </foreach>
            </script>
            """)
    void batchIncrementUseCount(@Param("list") java.util.List<com.meowflow.template.entity.TemplateUseCountDelta> deltas);

    @Update("UPDATE mf_tpl_template SET score = #{score}, review_count = COALESCE(review_count, 0) + 1 WHERE id = #{templateId}")
    void updateRating(@Param("templateId") Long templateId, @Param("score") Double score);

    @Select("SELECT COUNT(*) FROM mf_tpl_template WHERE category_id = #{categoryId} AND status = 'active'")
    int countByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM mf_tpl_template WHERE id = #{id}")
    Template findById(@Param("id") Long id);

    /**
     * 兼容路径：模板 ID 既可能是 BIGINT 也可能是 "builtin:..." 字符串。
     * 调用方在使用内置模板时不会进入数据库分支，因此这里只要 Long 即可。
     */
    default Template findByStringId(String id) {
        try {
            long v = Long.parseLong(id);
            return findById(v);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    // -------- PostgreSQL full-text search --------

    /**
     * PostgreSQL 全文检索：基于 V7 迁移创建的 search_vector 列 + simple 字典。
     * 不再依赖 zhparser，安装无需额外扩展。
     */
    @Select("""
            SELECT t.*, ts_rank(t.search_vector, plainto_tsquery('simple', #{keyword})) AS rank
            FROM mf_tpl_template t
            WHERE t.search_vector @@ plainto_tsquery('simple', #{keyword})
              AND t.status = 'active'
              AND t.review_status = 'approved'
            ORDER BY rank DESC, t.use_count DESC
            """)
    IPage<Template> searchByFullText(Page<Template> page, @Param("keyword") String keyword);

    /**
     * PostgreSQL 模糊匹配：基于 pg_trgm。
     */
    @Select("""
            SELECT t.*
            FROM mf_tpl_template t
            WHERE (t.name % #{keyword} OR t.description % #{keyword})
              AND t.status = 'active'
              AND t.review_status = 'approved'
            ORDER BY similarity(t.name, #{keyword}) DESC, t.use_count DESC
            """)
    IPage<Template> searchByTrigram(Page<Template> page, @Param("keyword") String keyword);

    /**
     * 综合检索：全文 + 模糊匹配 + 类别过滤 + 标签过滤。
     * <p>注意：标签过滤通过 mf_tpl_template_tag 关联表精确匹配，取代旧的 LIKE 字符串扫描。
     */
    @Select("""
            <script>
            SELECT DISTINCT t.*,
                   COALESCE(ts_rank(t.search_vector, plainto_tsquery('simple', #{keyword})), 0) AS rank
            FROM mf_tpl_template t
            LEFT JOIN mf_tpl_template_tag tt ON tt.template_id = t.id
            <where>
              t.status = 'active'
              AND t.review_status = 'approved'
              <if test="keyword != null and keyword != ''">
              AND (
                t.search_vector @@ plainto_tsquery('simple', #{keyword})
                OR t.name ILIKE CONCAT('%', #{keyword}, '%')
                OR t.description ILIKE CONCAT('%', #{keyword}, '%')
              )
              </if>
              <if test="categoryId != null">
              AND t.category_id = #{categoryId}
              </if>
              <if test="tagIds != null and tagIds.size() > 0">
              AND tt.tag_id IN
                <foreach collection="tagIds" item="tagId" open="(" separator="," close=")">
                  #{tagId}
                </foreach>
              </if>
            </where>
            ORDER BY rank DESC NULLS LAST, t.use_count DESC
            </script>
            """)
    IPage<Template> advancedSearch(Page<Template> page,
                                   @Param("keyword") String keyword,
                                   @Param("categoryId") Long categoryId,
                                   @Param("tagIds") List<Long> tagIds);
}


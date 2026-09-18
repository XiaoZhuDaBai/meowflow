package com.meowflow.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.template.entity.TemplateRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TemplateRatingRepository extends BaseMapper<TemplateRating> {

    @Select("SELECT * FROM mf_tpl_rating WHERE template_id = #{templateId} AND status = 'active' ORDER BY create_time DESC")
    List<TemplateRating> findByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM mf_tpl_rating WHERE template_id = #{templateId} AND user_id = #{userId} AND status = 'active' LIMIT 1")
    TemplateRating findByTemplateIdAndUserId(@Param("templateId") Long templateId, @Param("userId") Long userId);

    @Select("SELECT AVG(score) FROM mf_tpl_rating WHERE template_id = #{templateId} AND status = 'active'")
    Double selectAverageScore(@Param("templateId") Long templateId);

    @Select("SELECT score, COUNT(*) as cnt FROM mf_tpl_rating WHERE template_id = #{templateId} AND status = 'active' GROUP BY score")
    List<Map<String, Object>> selectScoreDistribution(@Param("templateId") Long templateId);

    @Select("SELECT unnest(string_to_array(tags, ',')) as tag, COUNT(*) as cnt " +
            "FROM mf_tpl_rating WHERE template_id = #{templateId} AND tags IS NOT NULL AND status = 'active' " +
            "GROUP BY tag ORDER BY cnt DESC LIMIT 10")
    List<Map<String, Object>> selectTopTags(@Param("templateId") Long templateId);

    @Select("SELECT COUNT(*) FROM mf_tpl_rating WHERE template_id = #{templateId} AND status = 'active'")
    Long countByTemplateId(@Param("templateId") Long templateId);

    @Select("""
            <script>
            SELECT * FROM mf_tpl_rating WHERE template_id = #{templateId} AND status = 'active'
            ORDER BY ${orderBy}
            </script>
            """)
    IPage<TemplateRating> selectPage(Page<TemplateRating> page,
                                     @Param("templateId") Long templateId,
                                     @Param("orderBy") String orderBy);
}


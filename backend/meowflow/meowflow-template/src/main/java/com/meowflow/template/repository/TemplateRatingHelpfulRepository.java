package com.meowflow.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.template.entity.TemplateRatingHelpful;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TemplateRatingHelpfulRepository extends BaseMapper<TemplateRatingHelpful> {

    @Select("SELECT COUNT(*) > 0 FROM mf_tpl_rating_helpful WHERE rating_id = #{ratingId} AND user_id = #{userId}")
    boolean existsByRatingIdAndUserId(@Param("ratingId") Long ratingId, @Param("userId") Long userId);
}


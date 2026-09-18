package com.meowflow.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.template.entity.TemplateReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TemplateReviewRepository extends BaseMapper<TemplateReview> {

    @Select("SELECT * FROM mf_tpl_review WHERE template_id = #{templateId} ORDER BY create_time DESC")
    List<TemplateReview> findByTemplateId(@Param("templateId") Long templateId);

    @Select("SELECT * FROM mf_tpl_review WHERE reviewer_id = #{reviewerId} ORDER BY create_time DESC")
    IPage<TemplateReview> findByReviewerId(Page<TemplateReview> page, @Param("reviewerId") Long reviewerId);

    @Select("SELECT * FROM mf_tpl_review WHERE user_id = #{userId} ORDER BY create_time DESC")
    IPage<TemplateReview> findByUserId(Page<TemplateReview> page, @Param("userId") Long userId);

    @Select("SELECT * FROM mf_tpl_review WHERE action = #{action} ORDER BY create_time DESC")
    List<TemplateReview> findByAction(@Param("action") String action);

    @Select("SELECT * FROM mf_tpl_review WHERE result = #{result} ORDER BY create_time DESC")
    IPage<TemplateReview> findByResult(Page<TemplateReview> page, @Param("result") String result);

    @Select("SELECT * FROM mf_tpl_review WHERE template_id = #{templateId} AND user_id = #{userId} LIMIT 1")
    TemplateReview findByTemplateIdAndUserId(@Param("templateId") Long templateId, @Param("userId") Long userId);
}


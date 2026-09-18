package com.meowflow.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.template.entity.TemplateTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TemplateTagRepository extends BaseMapper<TemplateTag> {

    @Select("SELECT * FROM mf_tpl_tag ORDER BY usage_count DESC, sort ASC")
    List<TemplateTag> findAllOrderByUsageCount();

    @Select("SELECT * FROM mf_tpl_tag ORDER BY sort ASC")
    IPage<TemplateTag> findAllOrderBySort(Page<TemplateTag> page);

    @Select("SELECT * FROM mf_tpl_tag WHERE id IN (#{tagIds})")
    List<TemplateTag> findByIds(@Param("tagIds") String tagIds);

    @Select("SELECT * FROM mf_tpl_tag WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    List<TemplateTag> searchByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM mf_tpl_tag WHERE usage_count > #{minUsage} ORDER BY usage_count DESC")
    List<TemplateTag> findHotTags(@Param("minUsage") int minUsage);

    @Update("UPDATE mf_tpl_tag SET usage_count = usage_count + 1 WHERE id = #{tagId}")
    void incrementUsageCount(@Param("tagId") Long tagId);

    @Select("SELECT * FROM mf_tpl_tag WHERE name = #{name} LIMIT 1")
    TemplateTag findByName(@Param("name") String name);
}

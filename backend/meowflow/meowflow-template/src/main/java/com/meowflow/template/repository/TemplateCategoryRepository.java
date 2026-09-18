package com.meowflow.template.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.template.entity.TemplateCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TemplateCategoryRepository extends BaseMapper<TemplateCategory> {

    /**
     * 查询根分类。
     * <p>与公共 DDL 兼容：mf_wf_category.parent_id 是 BIGINT DEFAULT 0，
     * 这里的根定义为 parent_id = 0（与公共种子 V4 一致）。
     */
    @Select("SELECT * FROM mf_wf_category WHERE parent_id = 0 OR parent_id IS NULL ORDER BY sort ASC, id ASC")
    List<TemplateCategory> findRootCategories();

    @Select("SELECT * FROM mf_wf_category WHERE parent_id = #{parentId} ORDER BY sort ASC, id ASC")
    List<TemplateCategory> findByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM mf_wf_category WHERE level = #{level} ORDER BY sort ASC")
    List<TemplateCategory> findByLevel(@Param("level") Integer level);

    @Select("SELECT * FROM mf_wf_category WHERE status = 'active' ORDER BY sort ASC")
    IPage<TemplateCategory> findActiveCategories(Page<TemplateCategory> page);

    @Select("SELECT * FROM mf_wf_category WHERE code = #{code} LIMIT 1")
    TemplateCategory findByCode(@Param("code") String code);

    @Select("SELECT * FROM mf_wf_category WHERE name LIKE CONCAT('%', #{keyword}, '%') AND status = 'active'")
    List<TemplateCategory> searchByKeyword(@Param("keyword") String keyword);
}

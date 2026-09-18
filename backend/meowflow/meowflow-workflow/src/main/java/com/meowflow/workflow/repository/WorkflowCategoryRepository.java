package com.meowflow.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.workflow.entity.WorkflowCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface WorkflowCategoryRepository extends BaseMapper<WorkflowCategory> {

    @Select("SELECT * FROM mf_wf_category ORDER BY sort ASC, id ASC")
    List<WorkflowCategory> findAllOrderBySort();

    @Select("SELECT * FROM mf_wf_category WHERE parent_id = #{parentId} ORDER BY sort ASC")
    List<WorkflowCategory> findByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM mf_wf_category WHERE parent_id = 0 ORDER BY sort ASC")
    List<WorkflowCategory> findRootCategories();

    @Select("SELECT * FROM mf_wf_category WHERE code = #{code}")
    Optional<WorkflowCategory> findByCode(@Param("code") String code);

    @Select("SELECT * FROM mf_wf_category WHERE status = 'active' ORDER BY sort ASC")
    List<WorkflowCategory> findActiveCategories();
}

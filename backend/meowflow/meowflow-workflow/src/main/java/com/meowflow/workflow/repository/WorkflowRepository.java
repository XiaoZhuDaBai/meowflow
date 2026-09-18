package com.meowflow.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.workflow.entity.Workflow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkflowRepository extends BaseMapper<Workflow> {

    @Select("SELECT * FROM mf_wf_workflow WHERE deleted = false AND code = #{code} LIMIT 1")
    Workflow selectByCode(@Param("code") String code);

    @Select("SELECT * FROM mf_wf_workflow WHERE deleted = false AND status = 'running'")
    List<Workflow> findAllRunning();

    @Select("SELECT * FROM mf_wf_workflow WHERE deleted = false AND owner_id = #{ownerId}")
    List<Workflow> findByOwnerId(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM mf_wf_workflow WHERE deleted = false AND category_id = #{categoryId}")
    List<Workflow> findByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM mf_wf_workflow WHERE deleted = false AND group_id = #{groupId}")
    List<Workflow> findByGroupId(@Param("groupId") Long groupId);

    @Select("SELECT * FROM mf_wf_workflow WHERE deleted = false AND is_public = true")
    List<Workflow> findAllPublic();
}

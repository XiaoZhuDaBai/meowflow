package com.meowflow.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.workflow.entity.WorkflowVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface WorkflowVersionRepository extends BaseMapper<WorkflowVersion> {

    @Select("SELECT * FROM mf_wf_workflow_version WHERE workflow_id = #{workflowId} ORDER BY create_time DESC")
    List<WorkflowVersion> findByWorkflowId(@Param("workflowId") Long workflowId);

    @Select("SELECT * FROM mf_wf_workflow_version WHERE workflow_id = #{workflowId} AND version = #{version}")
    Optional<WorkflowVersion> findByWorkflowIdAndVersion(@Param("workflowId") Long workflowId, @Param("version") String version);

    @Select("SELECT * FROM mf_wf_workflow_version WHERE workflow_id = #{workflowId} AND publish_status = 'published' ORDER BY create_time DESC LIMIT 1")
    Optional<WorkflowVersion> findPublishedVersion(@Param("workflowId") Long workflowId);

    @Select("SELECT * FROM mf_wf_workflow_version WHERE workflow_id = #{workflowId} AND publish_status = 'draft' ORDER BY create_time DESC LIMIT 1")
    Optional<WorkflowVersion> findDraftVersion(@Param("workflowId") Long workflowId);

    @Select("SELECT COUNT(*) FROM mf_wf_workflow_version WHERE workflow_id = #{workflowId}")
    int countByWorkflowId(@Param("workflowId") Long workflowId);
}

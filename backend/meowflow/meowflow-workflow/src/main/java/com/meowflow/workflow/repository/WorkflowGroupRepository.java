package com.meowflow.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.workflow.entity.WorkflowGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkflowGroupRepository extends BaseMapper<WorkflowGroup> {

    @Select("SELECT * FROM mf_wf_group WHERE user_id = #{userId} ORDER BY sort ASC, id ASC")
    List<WorkflowGroup> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM mf_wf_group ORDER BY sort ASC, id ASC")
    List<WorkflowGroup> findAllOrderBySort();
}

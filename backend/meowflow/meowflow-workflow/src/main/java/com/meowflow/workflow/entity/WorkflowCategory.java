package com.meowflow.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_wf_category")
public class WorkflowCategory extends Model<WorkflowCategory> implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String code;

    private String name;

    private String icon;

    private Integer sort;

    private String status;

    private Integer level;

    private String description;

    private String remark;

    private Long createBy;

    private java.time.LocalDateTime createTime;

    private Long updateBy;

    private java.time.LocalDateTime updateTime;
}

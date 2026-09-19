package com.meowflow.executor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@TableName("mf_exe_executor_node")
public class ExecutorNodeEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    private String nodeId;

    private String name;

    private String host;

    private Integer port;

    private String status;

    private LocalDateTime lastHeartbeat;

    private Integer cpuCount;

    private Long memoryTotal;

    private Long memoryUsed;

    private Integer activeTasks;

    private Long completedTasks;

    private Long failedTasks;

    /**
     * 对应 mf_exe_executor_node.tags（jsonb）。必须显式声明 TypeHandler，
     * 否则 jsonb 列无法绑定 Java Map，写入时报类型不匹配。
     */
    @TableField(value = "tags", typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> tags;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}



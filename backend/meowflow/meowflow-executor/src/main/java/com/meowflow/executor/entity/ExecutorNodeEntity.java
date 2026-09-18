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

    private Map<String, Object> tags;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}



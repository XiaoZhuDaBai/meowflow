package com.meowflow.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * MQ 消息通用结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 操作类型: execute, finish, delay, retry
     */
    private String action;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点配置
     */
    private Map<String, Object> nodeConfig;

    /**
     * 上下文数据
     */
    private Map<String, Object> contextData;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 关联追踪ID
     */
    private String traceId;
}

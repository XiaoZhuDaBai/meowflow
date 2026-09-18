package com.meowflow.infra.agent;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 会话实体
 * 
 * <p>存储 Agent 的会话上下文，支持多轮对话记忆。
 * 每个 sessionId 对应一个独立会话，可跨多次执行保持上下文。</p>
 */
@Data
@TableName("mf_agent_session")
public class AgentSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话 ID（业务主键，UUID 格式）
     */
    private String sessionId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * Agent 节点 ID（可选，用于识别特定 Agent）
     */
    private String agentNodeId;

    /**
     * 会话状态：active / expired / archived
     */
    private String status;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 会话过期时间（默认 24 小时无活动后过期）
     */
    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_ARCHIVED = "archived";
}

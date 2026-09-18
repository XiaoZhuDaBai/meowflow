package com.meowflow.infra.agent;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 会话消息实体
 * 
 * <p>存储单条对话消息（user/assistant/tool），支持 10 轮滚动窗口。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mf_agent_message")
public class AgentMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 消息角色：user / assistant / system / tool
     */
    private String role;

    /**
     * 消息内容
     */
    @TableField(value = "content", typeHandler = org.apache.ibatis.type.StringTypeHandler.class)
    private String content;

    /**
     * 工具调用名称（仅 role=tool 时有值）
     */
    private String toolName;

    /**
     * 工具调用结果（仅 role=tool 时有值）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object toolResult;

    /**
     * 消息序号（同一 sessionId 内递增，用于排序和淘汰）
     */
    private Integer sequenceNumber;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_TOOL = "tool";
}

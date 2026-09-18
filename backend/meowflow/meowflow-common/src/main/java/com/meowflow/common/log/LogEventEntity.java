package com.meowflow.common.log;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 日志事件实体 (数据库表)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_log_event")
public class LogEventEntity {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String traceId;
    private String module;
    private String level;
    private String type;
    private String message;
    private String payload;
    private Long userId;
    private String userName;
    private String ip;
    private Long duration;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createTime;

    /**
     * 从 LogEvent 转换
     */
    public static LogEventEntity from(LogEvent event) {
        if (event == null) {
            return null;
        }
        LogEventEntity entity = new LogEventEntity();
        entity.setId(event.getId());
        entity.setTraceId(event.getTraceId());
        entity.setModule(event.getModule());
        entity.setLevel(event.getLevel());
        entity.setType(event.getType());
        entity.setMessage(event.getMessage());
        entity.setPayload(event.getPayload());
        entity.setUserId(event.getUserId());
        entity.setUserName(event.getUserName());
        entity.setIp(event.getIp());
        entity.setDuration(event.getDuration());
        entity.setStatus(event.getStatus());
        entity.setErrorMessage(event.getErrorMessage());
        entity.setCreateTime(event.getCreateTime());
        return entity;
    }

    /**
     * 转换为 LogEvent
     */
    public LogEvent to() {
        LogEvent event = new LogEvent();
        event.setId(this.id);
        event.setTraceId(this.traceId);
        event.setModule(this.module);
        event.setLevel(this.level);
        event.setType(this.type);
        event.setMessage(this.message);
        event.setPayload(this.payload);
        event.setUserId(this.userId);
        event.setUserName(this.userName);
        event.setIp(this.ip);
        event.setDuration(this.duration);
        event.setStatus(this.status);
        event.setErrorMessage(this.errorMessage);
        event.setCreateTime(this.createTime);
        return event;
    }
}

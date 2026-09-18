package com.meowflow.common.log;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用日志事件
 */
@Data
public class LogEvent {

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
}

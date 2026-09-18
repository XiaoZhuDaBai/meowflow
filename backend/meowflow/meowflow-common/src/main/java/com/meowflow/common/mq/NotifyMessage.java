package com.meowflow.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通知消息结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型: system, workflow, task, callback
     */
    private String type;

    /**
     * 关联ID（如工作流ID、任务ID等）
     */
    private Long relatedId;

    /**
     * 跳转链接
     */
    private String url;
}

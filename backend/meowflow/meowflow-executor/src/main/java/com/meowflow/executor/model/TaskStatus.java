package com.meowflow.executor.model;

/**
 * 任务状态枚举
 */
public enum TaskStatus {

    /**
     * 等待中
     */
    PENDING,

    /**
     * 已分配
     */
    ASSIGNED,

    /**
     * 执行中
     */
    RUNNING,

    /**
     * 成功完成
     */
    SUCCESS,

    /**
     * 执行失败
     */
    FAILED,

    /**
     * 已取消
     */
    CANCELLED,

    /**
     * 已超时
     */
    TIMEOUT,

    /**
     * 重试中
     */
    RETRYING
}

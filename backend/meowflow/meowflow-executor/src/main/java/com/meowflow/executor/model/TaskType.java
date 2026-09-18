package com.meowflow.executor.model;

/**
 * 任务类型枚举
 */
public enum TaskType {

    /**
     * 工作流节点执行
     */
    WORKFLOW_NODE,

    /**
     * 条件判断
     */
    CONDITION,

    /**
     * 脚本执行
     */
    SCRIPT,

    /**
     * HTTP 请求
     */
    HTTP_REQUEST,

    /**
     * AI 对话
     */
    AI_CHAT,

    /**
     * 知识库检索
     */
    KNOWLEDGE_SEARCH,

    /**
     * 消息通知
     */
    NOTIFICATION,

    /**
     * 定时任务
     */
    SCHEDULED,

    /**
     * 批处理
     */
    BATCH,

    /**
     * 自定义
     */
    CUSTOM
}

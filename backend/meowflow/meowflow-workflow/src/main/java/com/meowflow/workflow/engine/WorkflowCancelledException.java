package com.meowflow.workflow.engine;

import lombok.Getter;

/**
 * 执行被外部请求取消时抛出的运行时异常。
 *
 * 与 {@link WorkflowException} 的区别：
 * <ul>
 *   <li>{@link WorkflowException}：工作流内部错误（配置错误、环、死锁等）。</li>
 *   <li>{@link WorkflowCancelledException}：外部主动取消，不视为业务失败。</li>
 * </ul>
 */
@Getter
public class WorkflowCancelledException extends RuntimeException {

    private final Long executionId;
    private final String reason;

    public WorkflowCancelledException(Long executionId, String reason) {
        super("Workflow execution " + executionId + " was cancelled" + (reason != null ? ": " + reason : ""));
        this.executionId = executionId;
        this.reason = reason;
    }
}

package com.meowflow.common.exception;

import com.meowflow.common.result.ResultCode;
import lombok.Getter;

@Getter
public class WorkflowException extends BizException {

    private static final long serialVersionUID = 1L;

    private final String workflowId;
    private final String executionId;

    public WorkflowException(String workflowId, String message) {
        super(ResultCode.WORKFLOW_EXECUTE_ERROR, message);
        this.workflowId = workflowId;
        this.executionId = null;
        addDetail("workflowId", workflowId);
    }

    public WorkflowException(String workflowId, String executionId, String message) {
        super(ResultCode.WORKFLOW_EXECUTE_ERROR, message);
        this.workflowId = workflowId;
        this.executionId = executionId;
        addDetail("workflowId", workflowId);
        if (executionId != null) {
            addDetail("executionId", executionId);
        }
    }

    public WorkflowException(String workflowId, String message, Throwable cause) {
        super(ResultCode.WORKFLOW_EXECUTE_ERROR, message);
        this.workflowId = workflowId;
        this.executionId = null;
        initCause(cause);
    }
}
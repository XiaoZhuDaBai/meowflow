package com.meowflow.common.exception;

import com.meowflow.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowExceptionTest {

    @Test
    void constructor_withWorkflowIdAndMessage_shouldSetCorrectValues() {
        WorkflowException ex = new WorkflowException("wf-001", "执行失败");
        assertEquals(ResultCode.WORKFLOW_EXECUTE_ERROR.getCode(), ex.getCode());
        assertEquals("wf-001", ex.getWorkflowId());
        assertNull(ex.getExecutionId());
        assertEquals("执行失败", ex.getMessage());
        assertEquals("wf-001", ex.getDetails().get("workflowId"));
    }

    @Test
    void constructor_withWorkflowIdAndExecutionIdAndMessage_shouldSetAllValues() {
        WorkflowException ex = new WorkflowException("wf-001", "exec-123", "执行失败");
        assertEquals(ResultCode.WORKFLOW_EXECUTE_ERROR.getCode(), ex.getCode());
        assertEquals("wf-001", ex.getWorkflowId());
        assertEquals("exec-123", ex.getExecutionId());
        assertEquals("执行失败", ex.getMessage());
        assertEquals("wf-001", ex.getDetails().get("workflowId"));
        assertEquals("exec-123", ex.getDetails().get("executionId"));
    }

    @Test
    void constructor_withWorkflowIdAndMessageAndCause_shouldSetCause() {
        RuntimeException cause = new RuntimeException("原错误");
        WorkflowException ex = new WorkflowException("wf-001", "执行失败", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void constructor_withNullExecutionId_shouldNotAddExecutionIdToDetails() {
        WorkflowException ex = new WorkflowException("wf-001", (String) null, "执行失败");
        assertFalse(ex.getDetails().containsKey("executionId"));
    }

    @Test
    void exception_shouldBeRuntimeException() {
        WorkflowException ex = new WorkflowException("wf-001", "执行失败");
        assertTrue(ex instanceof RuntimeException);
        assertTrue(ex instanceof BizException);
    }
}

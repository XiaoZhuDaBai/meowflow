package com.meowflow.workflow.engine;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionResult {

    private Long executionId;
    private Long workflowId;
    private String version;
    private String status;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private String errorMessage;
    private Long costMs;
    private Integer costToken;
    private Double costAmount;

    private Map<String, NodeResult> nodeResults;

    public static ExecutionResult success(Long executionId, Long workflowId, String version,
            Map<String, Object> input, Map<String, Object> output, Long costMs) {
        return ExecutionResult.builder()
                .executionId(executionId)
                .workflowId(workflowId)
                .version(version)
                .status("success")
                .input(input)
                .output(output)
                .costMs(costMs)
                .build();
    }

    public static ExecutionResult failed(Long executionId, Long workflowId, String version,
            Map<String, Object> input, String errorMessage, Long costMs) {
        return ExecutionResult.builder()
                .executionId(executionId)
                .workflowId(workflowId)
                .version(version)
                .status("failed")
                .input(input)
                .errorMessage(errorMessage)
                .costMs(costMs)
                .build();
    }

    public static ExecutionResult cancelled(Long executionId, Long workflowId, String version,
            Map<String, Object> input, Long costMs) {
        return ExecutionResult.builder()
                .executionId(executionId)
                .workflowId(workflowId)
                .version(version)
                .status("cancelled")
                .input(input)
                .costMs(costMs)
                .build();
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status);
    }

    public boolean isCancelled() {
        return "cancelled".equals(status);
    }
}

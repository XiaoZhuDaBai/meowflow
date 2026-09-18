package com.meowflow.workflow.engine;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutionContext 单元测试
 */
class ExecutionContextTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        Map<String, Object> input = new HashMap<>();
        input.put("key", "value");

        // When
        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .workflowId(100L)
                .version("v1.0")
                .input(input)
                .build();

        // Then
        assertThat(context.getExecutionId()).isEqualTo(1L);
        assertThat(context.getWorkflowId()).isEqualTo(100L);
        assertThat(context.getVersion()).isEqualTo("v1.0");
        assertThat(context.getInput()).containsEntry("key", "value");
    }

    @Test
    void noArgsConstructor_shouldInitializeEmptyCollections() {
        // When
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.getNodeResults()).isNotNull().isEmpty();
        assertThat(context.getGlobalVariables()).isNotNull().isEmpty();
        assertThat(context.getEnvironmentVariables()).isNotNull().isEmpty();
        assertThat(context.getLoopCursors()).isNotNull().isEmpty();
    }

    @Test
    void setVariable_shouldStoreInBothMapsAndGetFromEither() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        context.setVariable("x", 42);

        // Then
        assertThat(context.getVariable("x")).isEqualTo(42);
    }

    @Test
    void getVariable_whenNotSet_returnsNull() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.getVariable("nonexistent")).isNull();
    }

    @Test
    void setNodeResult_shouldStoreResult() {
        // Given
        ExecutionContext context = new ExecutionContext();
        NodeResult result = NodeResult.success("node-1", com.meowflow.workflow.definition.NodeType.LLM, "Test",
                Map.of("output", "value"));

        // When
        context.setNodeResult("node-1", result);

        // Then
        NodeResult retrieved = context.getNodeResult("node-1");
        assertThat(retrieved).isEqualTo(result);
    }

    @Test
    void setNodeResult_withOutput_shouldStoreOutputAsVariable() {
        // Given
        ExecutionContext context = new ExecutionContext();
        NodeResult result = NodeResult.success("node-1", com.meowflow.workflow.definition.NodeType.LLM, "Test",
                Map.of("result", "success"));

        // When
        context.setNodeResult("node-1", result);

        // Then - output fields should be available as variables
        assertThat(context.getVariable("node-1.result")).isEqualTo("success");
    }

    @Test
    void getNodeResult_whenNotSet_returnsNull() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.getNodeResult("nonexistent")).isNull();
    }

    @Test
    void resolveExpression_withSimpleValue_returnsValue() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        Object result = context.resolveExpression("plain string");

        // Then
        assertThat(result).isEqualTo("plain string");
    }

    @Test
    void resolveExpression_withNullExpression_returnsNull() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.resolveExpression(null)).isNull();
    }

    @Test
    void resolveExpression_withEmptyExpression_returnsEmpty() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.resolveExpression("")).isEqualTo("");
    }

    @Test
    void resolveExpression_withTemplate_returnsInterpolatedString() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.setVariable("name", "World");

        // When
        Object result = context.resolveExpression("Hello, {{name}}!");

        // Then
        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    void resolveExpression_withUnknownPath_returnsOriginal() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        Object result = context.resolveExpression("{{unknown.path}}");

        // Then - unknown paths are not replaced
        assertThat(result).isEqualTo("{{unknown.path}}");
    }

    @Test
    void resolvePath_withInputPath_returnsInputValue() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.setInput(Map.of("name", "Alice"));

        // When
        Object result = context.resolvePath("input.name");

        // Then
        assertThat(result).isEqualTo("Alice");
    }

    @Test
    void resolvePath_withVarPath_returnsVariable() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.setVariable("counter", 100);

        // When
        Object result = context.resolvePath("var.counter");

        // Then
        assertThat(result).isEqualTo(100);
    }

    @Test
    void resolvePath_withDirectKey_returnsVariableValue() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.setVariable("myKey", "myValue");

        // When
        Object result = context.resolvePath("myKey");

        // Then
        assertThat(result).isEqualTo("myValue");
    }

    @Test
    void resolvePath_withNull_returnsNull() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.resolvePath(null)).isNull();
    }

    @Test
    void resolvePath_withEmpty_returnsNull() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.resolvePath("")).isNull();
    }

    @Test
    void resolvePath_withNodeOutput_returnsNestedValue() {
        // Given
        ExecutionContext context = new ExecutionContext();
        NodeResult result = NodeResult.success("node-1", com.meowflow.workflow.definition.NodeType.LLM, "Test",
                Map.of("data", Map.of("value", 42)));
        context.setNodeResult("node-1", result);

        // When
        Object value = context.resolvePath("node-1.data.value");

        // Then
        assertThat(value).isEqualTo(42);
    }

    @Test
    void resolvePath_whenTriggerNode_returnsInputFallback() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.setInput(Map.of("query", "hello"));
        NodeResult result = NodeResult.success("trigger-1",
                com.meowflow.workflow.definition.NodeType.TRIGGER_MANUAL, "Manual", null);
        context.setNodeResult("trigger-1", result);

        // When
        Object value = context.resolvePath("trigger-1.query");

        // Then - trigger output is null, fallback to input
        assertThat(value).isEqualTo("hello");
    }

    @Test
    void markRunning_shouldSetStatusAndStartTime() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        context.markRunning();

        // Then
        assertThat(context.getStatus()).isEqualTo("running");
        assertThat(context.getStartTime()).isNotNull();
    }

    @Test
    void markSuccess_shouldSetStatusToSuccess() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.markRunning();

        // When
        context.markSuccess();

        // Then
        assertThat(context.getStatus()).isEqualTo("success");
    }

    @Test
    void markFailed_shouldSetStatusAndErrorMessage() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        context.markFailed("Something went wrong");

        // Then
        assertThat(context.getStatus()).isEqualTo("failed");
        assertThat(context.getOutput()).containsEntry("_error", "Something went wrong");
    }

    @Test
    void getElapsedMs_afterMarkRunning_returnsDifference() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.markRunning();

        // When
        long elapsed = context.getElapsedMs();

        // Then
        assertThat(elapsed).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getElapsedMs_whenNotStarted_returnsZero() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.getElapsedMs()).isEqualTo(0);
    }

    @Test
    void getExecutedNodeIds_returnsAllRecordedNodes() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.setNodeResult("node-1",
                NodeResult.success("node-1", com.meowflow.workflow.definition.NodeType.LLM, "T1", null));
        context.setNodeResult("node-2",
                NodeResult.success("node-2", com.meowflow.workflow.definition.NodeType.LLM, "T2", null));

        // When
        var ids = context.getExecutedNodeIds();

        // Then
        assertThat(ids).containsExactlyInAnyOrder("node-1", "node-2");
    }

    @Test
    void isCancelled_whenNoToken_returnsFalse() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.isCancelled()).isFalse();
    }

    @Test
    void mergeOutput_shouldAddValuesToOutput() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        context.mergeOutput(Map.of("key1", "value1", "key2", 42));

        // Then
        assertThat(context.getOutput()).containsEntry("key1", "value1");
        assertThat(context.getOutput()).containsEntry("key2", 42);
    }

    @Test
    void mergeOutput_whenCalledMultipleTimes_mergesAllValues() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        context.mergeOutput(Map.of("a", 1));
        context.mergeOutput(Map.of("b", 2));

        // Then
        assertThat(context.getOutput()).containsEntry("a", 1);
        assertThat(context.getOutput()).containsEntry("b", 2);
    }

    @Test
    void mergeOutput_withNullInput_doesNothing() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        context.mergeOutput(null);

        // Then - should not throw
    }

    @Test
    void setEnvironmentVariable_shouldStore() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When
        context.setEnvironmentVariable("API_URL", "https://api.example.com");

        // Then
        assertThat(context.getEnvironmentVariable("API_URL")).isEqualTo("https://api.example.com");
    }

    @Test
    void resolveMap_shouldRecursivelyInterpolateStrings() {
        // Given
        ExecutionContext context = new ExecutionContext();
        context.setVariable("name", "World");
        Map<String, Object> input = new HashMap<>();
        input.put("greeting", "Hello, {{name}}!");
        input.put("count", 42);

        // When
        Map<String, Object> result = context.resolveMap(input);

        // Then
        assertThat(result.get("greeting")).isEqualTo("Hello, World!");
        assertThat(result.get("count")).isEqualTo(42);
    }

    @Test
    void resolveMap_withNullInput_returnsNull() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // Then
        assertThat(context.resolveMap(null)).isNull();
    }

    @Test
    void getNodeTimeout_withNullData_returnsDefault() {
        // Given
        ExecutionContext context = new ExecutionContext();
        com.meowflow.workflow.definition.NodeDefinition node = com.meowflow.workflow.definition.NodeDefinition.builder()
                .data(null)
                .build();

        // When
        java.time.Duration timeout = context.getNodeTimeout(node);

        // Then
        assertThat(timeout).isEqualTo(java.time.Duration.ofMinutes(5));
    }

    @Test
    void getNodeTimeout_withTimeoutConfigured_returnsConfiguredValue() {
        // Given
        ExecutionContext context = new ExecutionContext();
        Map<String, Object> data = Map.of("timeout", 60);
        com.meowflow.workflow.definition.NodeDefinition node = com.meowflow.workflow.definition.NodeDefinition.builder()
                .data(data)
                .build();

        // When
        java.time.Duration timeout = context.getNodeTimeout(node);

        // Then
        assertThat(timeout.getSeconds()).isEqualTo(60);
    }

    @Test
    void getNodeTimeout_withStringTimeout_parsesCorrectly() {
        // Given
        ExecutionContext context = new ExecutionContext();
        Map<String, Object> data = Map.of("timeout", "PT30S");
        com.meowflow.workflow.definition.NodeDefinition node = com.meowflow.workflow.definition.NodeDefinition.builder()
                .data(data)
                .build();

        // When
        java.time.Duration timeout = context.getNodeTimeout(node);

        // Then
        assertThat(timeout.getSeconds()).isEqualTo(30);
    }

    @Test
    void checkCancellation_whenNotCancelled_doesNotThrow() {
        // Given
        ExecutionContext context = new ExecutionContext();

        // When/Then - should not throw
        context.checkCancellation();
    }

    @Test
    void equalsAndHashCode() {
        // Given
        ExecutionContext ctx1 = ExecutionContext.builder().executionId(1L).build();
        ExecutionContext ctx2 = ExecutionContext.builder().executionId(1L).build();

        // Then
        assertThat(ctx1).isEqualTo(ctx2); // ExecutionContext 是值对象
    }
}


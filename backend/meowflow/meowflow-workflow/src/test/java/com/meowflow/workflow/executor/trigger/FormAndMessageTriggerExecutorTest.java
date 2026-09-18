package com.meowflow.workflow.executor.trigger;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FormAndMessageTriggerExecutorTest {

    @Test
    void formTriggerExposesFormData() {
        FormTriggerExecutor executor = new FormTriggerExecutor();
        NodeDefinition node = NodeDefinition.builder()
                .id("form")
                .type(NodeType.TRIGGER_FORM)
                .name("Form")
                .data(Map.of("formId", "f1"))
                .build();
        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .input(Map.of("name", "Ada"))
                .variables(new HashMap<>())
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("triggerType", "form");
        assertThat((Map<String, Object>) result.getOutput().get("formData"))
                .containsEntry("name", "Ada");
    }

    @Test
    void messageTriggerExposesPlatformAndText() {
        MessageTriggerExecutor executor = new MessageTriggerExecutor();
        NodeDefinition node = NodeDefinition.builder()
                .id("msg")
                .type(NodeType.TRIGGER_MESSAGE)
                .name("Message")
                .data(Map.of("platform", "dingtalk"))
                .build();
        ExecutionContext context = ExecutionContext.builder()
                .executionId(2L)
                .input(Map.of("text", "hello"))
                .variables(new HashMap<>())
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("platform", "dingtalk");
        assertThat(result.getOutput()).containsEntry("text", "hello");
    }
}

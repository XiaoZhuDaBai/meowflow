package com.meowflow.workflow.definition;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NodeType 枚举单元测试
 */
class NodeTypeTest {

    @Test
    void fromCode_withNull_shouldThrowException() {
        assertThatThrownBy(() -> NodeType.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown node type");
    }

    @Test
    void fromCode_withBlank_shouldThrowException() {
        assertThatThrownBy(() -> NodeType.fromCode(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromCode_withManual_shouldReturnTriggerManual() {
        NodeType type = NodeType.fromCode("manual");
        assertThat(type).isEqualTo(NodeType.TRIGGER_MANUAL);
        assertThat(type.isTrigger()).isTrue();
        assertThat(type.isEnd()).isFalse();
    }

    @Test
    void fromCode_withEmptyString_shouldThrow() {
        assertThatThrownBy(() -> NodeType.fromCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromCode_withInvalidCode_shouldThrow() {
        assertThatThrownBy(() -> NodeType.fromCode("nonexistent_type"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent_type");
    }

    @Test
    void fromCode_returnsAllAliases() {
        // 前端 DSL 别名
        assertThat(NodeType.fromCode("trigger.manual")).isEqualTo(NodeType.TRIGGER_MANUAL);
        assertThat(NodeType.fromCode("trigger.webhook")).isEqualTo(NodeType.TRIGGER_WEBHOOK);
        assertThat(NodeType.fromCode("trigger.cron")).isEqualTo(NodeType.TRIGGER_CRON);
        assertThat(NodeType.fromCode("trigger.form")).isEqualTo(NodeType.TRIGGER_FORM);
        assertThat(NodeType.fromCode("trigger.message")).isEqualTo(NodeType.TRIGGER_MESSAGE);
        assertThat(NodeType.fromCode("trigger.plugin")).isEqualTo(NodeType.TRIGGER_PLUGIN);

        // AI 别名
        assertThat(NodeType.fromCode("ai.llm")).isEqualTo(NodeType.LLM);
        assertThat(NodeType.fromCode("ai.classify")).isEqualTo(NodeType.CLASSIFY);
        assertThat(NodeType.fromCode("ai.extract")).isEqualTo(NodeType.EXTRACT);
        assertThat(NodeType.fromCode("ai.summarize")).isEqualTo(NodeType.SUMMARIZE);
        assertThat(NodeType.fromCode("ai.agent")).isEqualTo(NodeType.AGENT);
        assertThat(NodeType.fromCode("ai.parameter-extractor")).isEqualTo(NodeType.PARAMETER_EXTRACTOR);
        assertThat(NodeType.fromCode("ai.question-classifier")).isEqualTo(NodeType.QUESTION_CLASSIFIER);
        assertThat(NodeType.fromCode("ai.rag")).isEqualTo(NodeType.KNOWLEDGE_SEARCH);
        assertThat(NodeType.fromCode("ai.tool")).isEqualTo(NodeType.TOOL);

        // 工具别名
        assertThat(NodeType.fromCode("tool.db")).isEqualTo(NodeType.DB);
        assertThat(NodeType.fromCode("tool.workflow")).isEqualTo(NodeType.SUB_WORKFLOW);
        assertThat(NodeType.fromCode("tool.code")).isEqualTo(NodeType.CODE);
        assertThat(NodeType.fromCode("tool.http")).isEqualTo(NodeType.HTTP);
        assertThat(NodeType.fromCode("tool.mcp")).isEqualTo(NodeType.TOOL);
        assertThat(NodeType.fromCode("tool.list-operator")).isEqualTo(NodeType.LIST_OPERATOR);
        assertThat(NodeType.fromCode("tool.assign")).isEqualTo(NodeType.SET_VARIABLE);

        // 流程别名
        assertThat(NodeType.fromCode("flow.condition")).isEqualTo(NodeType.CONDITION);
        // 前端节点目录里 flow.if-else 的输出端口就是 true / false，必须落到 IF；
        // 落到 SWITCH 会让它输出分支名而非 true/false，两条分支边都命中不了。
        assertThat(NodeType.fromCode("flow.if-else")).isEqualTo(NodeType.IF);
        assertThat(NodeType.fromCode("condition.switch")).isEqualTo(NodeType.SWITCH);
        assertThat(NodeType.fromCode("flow.switch")).isEqualTo(NodeType.SWITCH);
        assertThat(NodeType.fromCode("flow.parallel")).isEqualTo(NodeType.FORK);
        assertThat(NodeType.fromCode("flow.fork")).isEqualTo(NodeType.FORK);
        assertThat(NodeType.fromCode("flow.join")).isEqualTo(NodeType.JOIN);
        assertThat(NodeType.fromCode("flow.loop")).isEqualTo(NodeType.LOOP);
        assertThat(NodeType.fromCode("flow.iteration")).isEqualTo(NodeType.LOOP);
        assertThat(NodeType.fromCode("flow.wait")).isEqualTo(NodeType.WAIT);
    }

    @Test
    void fromCode_isCaseInsensitive() {
        assertThat(NodeType.fromCode("MANUAL")).isEqualTo(NodeType.TRIGGER_MANUAL);
        assertThat(NodeType.fromCode("Manual")).isEqualTo(NodeType.TRIGGER_MANUAL);
        assertThat(NodeType.fromCode("mAnUaL")).isEqualTo(NodeType.TRIGGER_MANUAL);
    }

    @Test
    void fromJson_withUnknownCode_returnsNull() {
        // 反序列化时遇到未知类型返回 null，由发布校验给出可读错误
        assertThat(NodeType.fromJson("nonexistent_type")).isNull();
        assertThat(NodeType.fromJson(null)).isNull();
        assertThat(NodeType.fromJson("")).isNull();
    }

    @Test
    void fromJson_withValidCode_returnsEnumValue() {
        assertThat(NodeType.fromJson("manual")).isEqualTo(NodeType.TRIGGER_MANUAL);
        assertThat(NodeType.fromJson("ai.llm")).isEqualTo(NodeType.LLM);
    }

    @Test
    void isTrigger_returnsCorrectValue() {
        assertThat(NodeType.TRIGGER_MANUAL.isTrigger()).isTrue();
        assertThat(NodeType.TRIGGER_WEBHOOK.isTrigger()).isTrue();
        assertThat(NodeType.TRIGGER_CRON.isTrigger()).isTrue();
        assertThat(NodeType.TRIGGER_FORM.isTrigger()).isTrue();
        assertThat(NodeType.TRIGGER_MESSAGE.isTrigger()).isTrue();
        assertThat(NodeType.TRIGGER_PLUGIN.isTrigger()).isTrue();
        assertThat(NodeType.LLM.isTrigger()).isFalse();
        assertThat(NodeType.END.isTrigger()).isFalse();
        assertThat(NodeType.NOTE.isTrigger()).isFalse();
    }

    @Test
    void isEnd_returnsCorrectValue() {
        assertThat(NodeType.END.isEnd()).isTrue();
        assertThat(NodeType.AGGREGATOR.isEnd()).isTrue();
        assertThat(NodeType.SET_VARIABLE.isEnd()).isTrue();
        assertThat(NodeType.JOIN.isEnd()).isTrue();
        assertThat(NodeType.LLM.isEnd()).isFalse();
        assertThat(NodeType.TRIGGER_MANUAL.isEnd()).isFalse();
    }

    @Test
    void getCode_returnsCodeValue() {
        assertThat(NodeType.TRIGGER_MANUAL.getCode()).isEqualTo("manual");
        assertThat(NodeType.LLM.getCode()).isEqualTo("llm");
        assertThat(NodeType.ANSWER.getCode()).isEqualTo("answer");
    }

    @Test
    void getDesc_returnsDescription() {
        assertThat(NodeType.TRIGGER_MANUAL.getDesc()).contains("手动触发");
        assertThat(NodeType.LLM.getDesc()).contains("LLM");
        assertThat(NodeType.END.getDesc()).contains("结束");
    }

    @Test
    void values_includesAllNodeTypes() {
        assertThat(NodeType.values()).hasSizeGreaterThan(20);
    }
}

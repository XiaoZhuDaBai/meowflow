package com.meowflow.workflow.definition;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum NodeType {

    // Trigger types
    TRIGGER_MANUAL("manual", true, false, "手动触发"),
    TRIGGER_WEBHOOK("webhook", true, false, "Webhook 触发"),
    TRIGGER_CRON("cron", true, false, "定时触发"),
    TRIGGER_FORM("form", true, false, "表单触发"),
    TRIGGER_MESSAGE("message", true, false, "消息触发"),
    TRIGGER_PLUGIN("plugin", true, false, "插件触发"),

    // Action types
    LLM("llm", false, false, "LLM 节点"),
    CLASSIFY("classify", false, false, "AI 分类"),
    EXTRACT("extract", false, false, "信息提取"),
    SUMMARIZE("summarize", false, false, "文本总结"),
    AGENT("agent", false, false, "Agent 智能体"),
    PARAMETER_EXTRACTOR("parameter_extractor", false, false, "参数提取"),
    QUESTION_CLASSIFIER("question_classifier", false, false, "问题分类"),
    LIST_OPERATOR("list_operator", false, false, "列表操作"),
    WAIT("wait", false, false, "等待节点"),
    DB("db", false, false, "数据库操作"),
    DOCUMENT_EXTRACTOR("document_extractor", false, false, "文档提取"),
    HUMAN_INPUT("human_input", false, false, "人工输入"),
    SUB_WORKFLOW("sub_workflow", false, false, "子工作流"),
    HTTP("http", false, false, "HTTP 请求"),
    KNOWLEDGE_SEARCH("knowledge_search", false, false, "知识库搜索"),
    TOOL("tool", false, false, "MCP 工具"),
    NOTIFY("notify", false, false, "通知发送"),
    CODE("code", false, false, "代码执行"),
    TRANSFORM("transform", false, false, "数据转换"),

    // Control types
    CONDITION("condition", false, false, "条件分支"),
    BRANCH("branch", false, false, "多分支"),
    LOOP("loop", false, false, "循环"),

    // Advanced control flow nodes
    IF("if", false, false, "IF 条件"),
    SWITCH("switch", false, false, "SWITCH 多路选择"),
    FORK("fork", false, false, "FORK 并行分支"),
    JOIN("join", false, true, "JOIN 合并同步"),

    // End types
    END("end", false, true, "结束"),
    ANSWER("answer", false, true, "Answer 节点"),
    AGGREGATOR("aggregator", false, true, "聚合节点"),
    SET_VARIABLE("set_variable", false, true, "设置变量"),

    // UI-only type: a sticky note on the editor canvas. The engine must
    // skip it — it is not a real workflow node, just decoration. Drawn
    // for the human user, persisted with the workflow so the layout is
    // preserved on reload, but not executed.
    NOTE("note", false, false, "注释");

    private final String code;
    private final boolean trigger;
    private final boolean end;
    private final String desc;

    private static final Set<NodeType> TRIGGERS = EnumSet.of(
            TRIGGER_MANUAL, TRIGGER_WEBHOOK, TRIGGER_CRON, TRIGGER_FORM, TRIGGER_MESSAGE);

    private static final Set<NodeType> ENDS = EnumSet.of(
            END, AGGREGATOR, SET_VARIABLE);

    private static final Map<String, NodeType> ALIASES = buildAliases();

    NodeType(String code, boolean trigger, boolean end, String desc) {
        this.code = code;
        this.trigger = trigger;
        this.end = end;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public boolean isTrigger() {
        return trigger;
    }

    public boolean isEnd() {
        return end;
    }

    public String getDesc() {
        return desc;
    }

    public static NodeType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Unknown node type: " + code);
        }
        NodeType resolved = ALIASES.get(code.trim().toLowerCase());
        if (resolved == null) {
            throw new IllegalArgumentException("Unknown node type: " + code);
        }
        return resolved;
    }

    /**
     * JSON 反序列化入口：兼容前端 DSL 类型名（ai.llm / trigger.webhook 等）。
     * 未知类型返回 null，由发布校验给出可读错误，避免反序列化直接 500。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NodeType fromJson(String code) {
        try {
            return fromCode(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<String, NodeType> buildAliases() {
        Map<String, NodeType> aliases = new HashMap<>();
        for (NodeType type : values()) {
            aliases.put(type.code, type);
            aliases.put(type.name().toLowerCase(), type);
        }
        // 前端节点目录 / 模板使用的类型名。
        aliases.put("trigger.manual", TRIGGER_MANUAL);
        aliases.put("trigger.webhook", TRIGGER_WEBHOOK);
        aliases.put("trigger.cron", TRIGGER_CRON);
        aliases.put("trigger.form", TRIGGER_FORM);
        aliases.put("trigger.message", TRIGGER_MESSAGE);
        aliases.put("trigger.imessage", TRIGGER_MESSAGE);
        aliases.put("trigger.plugin", TRIGGER_PLUGIN);
        aliases.put("plugin-trigger", TRIGGER_PLUGIN);

        aliases.put("ai.llm", LLM);
        aliases.put("ai.classify", CLASSIFY);
        aliases.put("ai.extract", EXTRACT);
        aliases.put("ai.summarize", SUMMARIZE);
        aliases.put("ai.agent", AGENT);
        aliases.put("ai.parameter-extractor", PARAMETER_EXTRACTOR);
        aliases.put("ai.question-classifier", QUESTION_CLASSIFIER);
        aliases.put("tool.list-operator", LIST_OPERATOR);
        aliases.put("flow.wait", WAIT);
        aliases.put("tool.db", DB);
        aliases.put("tool.document-extractor", DOCUMENT_EXTRACTOR);
        aliases.put("human-input", HUMAN_INPUT);
        aliases.put("human_input", HUMAN_INPUT);
        aliases.put("flow.human-input", HUMAN_INPUT);
        aliases.put("sub-workflow", SUB_WORKFLOW);
        aliases.put("sub_workflow", SUB_WORKFLOW);
        aliases.put("tool.workflow", SUB_WORKFLOW);
        aliases.put("tool.sub-workflow", SUB_WORKFLOW);
        aliases.put("tool.subworkflow", SUB_WORKFLOW);
        aliases.put("tool.code", CODE);
        aliases.put("tool.abstract", CODE);
        aliases.put("code.transform", CODE);
        aliases.put("http.request", HTTP);
        aliases.put("tool.http", HTTP);
        aliases.put("ai.rag", KNOWLEDGE_SEARCH);
        aliases.put("knowledge.search", KNOWLEDGE_SEARCH);
        aliases.put("tool.mcp", TOOL);
        aliases.put("ai.tool", TOOL);

        aliases.put("notify.dingtalk", NOTIFY);
        aliases.put("notify.wxwork", NOTIFY);
        aliases.put("notify.feishu", NOTIFY);
        aliases.put("notify.email", NOTIFY);
        aliases.put("notify.sms", NOTIFY);

        aliases.put("flow.condition", CONDITION);
        aliases.put("condition.if", CONDITION);
        // flow.if-else 在前端节点目录里就是「true/false 二分支」（ports: true / false），
        // 必须映射到 IF；过去映射到 SWITCH 会让 SWITCH 输出分支名而不是 true/false，
        // 于是 condition-true / condition-false 边永远命中不了，两条分支都被静默跳过。
        aliases.put("flow.if-else", IF);
        aliases.put("condition.if-else", IF);
        // 多路选择（cases / defaultBranch）
        aliases.put("condition.switch", SWITCH);
        aliases.put("flow.switch", SWITCH);
        aliases.put("flow.branch", BRANCH);
        aliases.put("flow.loop", LOOP);
        aliases.put("flow.iteration", LOOP);
        aliases.put("flow.parallel", FORK);
        aliases.put("flow.fork", FORK);
        aliases.put("flow.join", JOIN);

        aliases.put("flow.aggregation", AGGREGATOR);
        aliases.put("end.aggregator", AGGREGATOR);
        aliases.put("transform.aggregator", AGGREGATOR);
        aliases.put("flow.template-transform", TRANSFORM);
        aliases.put("end.return", END);
        aliases.put("answer", ANSWER);
        aliases.put("tool.assign", SET_VARIABLE);
        aliases.put("tool.variable-assigner", SET_VARIABLE);
        return aliases;
    }
}

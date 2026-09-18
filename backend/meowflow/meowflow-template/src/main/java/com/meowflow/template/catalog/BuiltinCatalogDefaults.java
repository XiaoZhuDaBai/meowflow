package com.meowflow.template.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置模板的"开箱即用"默认参数表。
 *
 * <p>每个节点类型都给出一个默认 config，这样插入模板时不需要再"裸奔"进入"高级配置"。
 *
 * <p>与前端的 {@code src/mock/builtinTemplateDefaults.ts} 保持严格一致 — 它们是同步生成的镜像。
 */
public final class BuiltinCatalogDefaults {

    private BuiltinCatalogDefaults() {}

    public static Map<String, Object> triggerWebhook() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("method", "POST");
        m.put("path", "/hooks/incoming");
        m.put("authToken", "");
        m.put("timeout", 10000);
        return m;
    }

    public static Map<String, Object> triggerCron() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cron", "0 9 * * *");
        m.put("timezone", "Asia/Shanghai");
        m.put("enabled", true);
        return m;
    }

    public static Map<String, Object> triggerForm() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("formId", "meowflow_default_form");
        m.put("fields", java.util.List.of(
                java.util.Map.of("key", "submitter", "label", "提交人", "type", "string", "required", true),
                java.util.Map.of("key", "content", "label", "内容", "type", "text", "required", true)
        ));
        return m;
    }

    public static Map<String, Object> triggerMessage() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("platform", "feishu");
        m.put("keyword", "");
        m.put("matchMode", "exact");
        return m;
    }

    public static Map<String, Object> aiLlm() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("model", "gpt-4o-mini");
        m.put("temperature", 0.3);
        m.put("maxTokens", 1024);
        m.put("systemPrompt", "你是喵流工作流的助手，请根据用户输入友好、专业地回答。");
        m.put("prompt", "请处理以下输入：\n{{input.text}}");
        return m;
    }

    public static Map<String, Object> aiClassify() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("model", "gpt-4o-mini");
        m.put("temperature", 0.2);
        m.put("categories", "紧急,普通,建议");
        m.put("input", "{{input.text}}");
        m.put("outputKey", "category");
        return m;
    }

    public static Map<String, Object> aiExtract() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("model", "gpt-4o-mini");
        m.put("schema", "[]");
        m.put("outputKey", "extracted");
        return m;
    }

    public static Map<String, Object> aiSummarize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("model", "gpt-4o-mini");
        m.put("maxWords", 200);
        m.put("tone", "concise");
        m.put("language", "zh-CN");
        return m;
    }

    public static Map<String, Object> aiRag() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("knowledgeBaseId", "");
        m.put("topK", 5);
        m.put("threshold", 0.7);
        return m;
    }

    public static Map<String, Object> toolHttp() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("method", "POST");
        m.put("url", "");
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        m.put("headers", headers);
        m.put("body", "");
        m.put("timeout", 10000);
        m.put("retryTimes", 1);
        m.put("retryOnFail", true);
        return m;
    }

    public static Map<String, Object> toolDb() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dsn", "");
        m.put("sql", "");
        m.put("params", new LinkedHashMap<>());
        m.put("fetchSize", 1000);
        return m;
    }

    public static Map<String, Object> toolCode() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("language", "javascript");
        m.put("source", "// 同步转换：处理上游数据并返回 result\nreturn { ok: true, input };");
        return m;
    }

    public static Map<String, Object> toolKnowledge() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("knowledgeBaseId", "kb_default");
        m.put("topK", 3);
        m.put("scoreThreshold", 0.6);
        m.put("outputKey", "context");
        return m;
    }

    public static Map<String, Object> controlIf() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("expression", "{{input.score}} >= 80");
        m.put("trueNext", "true");
        m.put("falseNext", "false");
        return m;
    }

    public static Map<String, Object> controlSwitch() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", "{{input.category}}");
        m.put("cases", java.util.List.of(
                java.util.Map.of("value", "紧急", "next", "urgent"),
                java.util.Map.of("value", "普通", "next", "normal"),
                java.util.Map.of("value", "建议", "next", "suggestion")
        ));
        m.put("defaultNext", "fallback");
        return m;
    }

    public static Map<String, Object> controlTransform() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("language", "jsonata");
        m.put("source", "$");
        m.put("description", "使用 JSONata / JS 表达式做字段映射");
        return m;
    }

    public static Map<String, Object> controlAggregator() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", "wait_all");
        m.put("timeoutMs", 30000);
        return m;
    }

    public static Map<String, Object> notifyFeishu() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("webhook", "");
        m.put("secret", "");
        m.put("atMobiles", java.util.List.of());
        m.put("atAll", false);
        m.put("msgType", "text");
        return m;
    }

    public static Map<String, Object> notifyDingtalk() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("webhook", "");
        m.put("secret", "");
        m.put("atMobiles", java.util.List.of());
        m.put("atAll", false);
        m.put("msgType", "markdown");
        return m;
    }

    public static Map<String, Object> notifyWxwork() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("webhook", "");
        m.put("mentionedList", java.util.List.of());
        m.put("msgType", "markdown");
        return m;
    }

    public static Map<String, Object> notifyEmail() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("host", "");
        m.put("port", 465);
        m.put("ssl", true);
        m.put("username", "");
        m.put("password", "");
        m.put("from", "");
        m.put("to", "");
        m.put("subject", "");
        m.put("body", "");
        m.put("cc", java.util.List.of());
        return m;
    }

    public static Map<String, Object> notifySms() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accessKeyId", "");
        m.put("accessKeySecret", "");
        m.put("signName", "喵流");
        m.put("templateCode", "");
        m.put("phoneNumbers", java.util.List.of());
        m.put("templateParam", "{}");
        return m;
    }

    public static Map<String, Object> endAggregator() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("outputMode", "return_last");
        return m;
    }

    public static Map<String, Object> defaultEmpty() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("note", "默认配置");
        return m;
    }

    /**
     * 根据节点 type 给出默认 config
     */
    public static Map<String, Object> defaultsFor(String type) {
        if (type == null) return defaultEmpty();
        switch (type) {
            case "trigger.webhook": return triggerWebhook();
            case "trigger.cron": return triggerCron();
            case "trigger.form": return triggerForm();
            case "trigger.imessage": return triggerMessage();
            case "ai.llm": return aiLlm();
            case "ai.classify": return aiClassify();
            case "ai.extract": return aiExtract();
            case "ai.summarize": return aiSummarize();
            case "ai.rag": return aiRag();
            case "tool.http":
            case "http.request": return toolHttp();
            case "tool.db": return toolDb();
            case "tool.code": return toolCode();
            case "knowledge.search": return toolKnowledge();
            case "condition.if": return controlIf();
            case "condition.switch": return controlSwitch();
            case "code.transform":
            case "transform.aggregator": return controlTransform();
            case "end.aggregator": return endAggregator();
            case "notify.feishu": return notifyFeishu();
            case "notify.dingtalk": return notifyDingtalk();
            case "notify.wxwork": return notifyWxwork();
            case "notify.email": return notifyEmail();
            case "notify.sms": return notifySms();
            default: return defaultEmpty();
        }
    }

    /**
     * 应用覆盖：defaults 先，overrides 后（同名 key 会覆盖）。
     * 返回新的 LinkedHashMap，避免修改调用方入参。
     */
    public static Map<String, Object> merge(Map<String, Object> defaults, Map<String, Object> overrides) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (defaults != null) merged.putAll(defaults);
        if (overrides != null) merged.putAll(overrides);
        return merged;
    }
}
package com.meowflow.template.catalog;

import com.meowflow.common.workflow.MarketWorkflowGraph;
import com.meowflow.common.workflow.WorkflowFullDefinition;
import com.meowflow.common.workflow.WorkflowJsonConverter;
import com.meowflow.template.dto.TemplateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置（官方）模板目录生成器。
 */
@Slf4j
@Component
public class BuiltinTemplateCatalog {

    private static final double NODE_W = 240d;
    private static final double NODE_H = 60d;
    private static final double DX = 80d;
    private static final double DY = 140d;

    private final List<BuiltinTemplateEntry> entries = new ArrayList<>();

    public BuiltinTemplateCatalog() {
        registerCustomerServiceScenarios();
        registerHRScenarios();
        registerOpsScenarios();
        registerFinanceScenarios();
        registerGeneralAndDataScenarios();
        log.info("Registered {} built-in templates", entries.size());
    }

    public List<BuiltinTemplateEntry> listAll() {
        return new ArrayList<>(entries);
    }

    public BuiltinTemplateEntry findById(String id) {
        if (id == null) {
            return null;
        }
        return entries.stream()
                .filter(e -> e.getId().equals(id) || String.valueOf(e.getNumericId()).equals(id))
                .findFirst()
                .orElse(null);
    }

    public BuiltinTemplateEntry findByName(String name) {
        if (name == null) {
            return null;
        }
        return entries.stream().filter(e -> name.equals(e.getName())).findFirst().orElse(null);
    }

    /**
     * 数据库历史种子中存在仅包含空 nodes/edges 的模板。查询时用同名官方模板补齐完整定义，
     * 这样列表、详情和“从模板创建工作流”都能拿到可编辑的真实节点与连线。
     */
    public void enrichDefinitionIfMissing(TemplateDTO dto) {
        if (dto == null || !isWorkflowEmpty(dto.getDefinition())) {
            return;
        }
        BuiltinTemplateEntry builtin = findByName(dto.getName());
        if (builtin == null) {
            return;
        }
        dto.setDefinition(builtin.getDefinition());
        dto.setWorkflowJson(builtin.getWorkflowJson());
        dto.setWorkflowGraph(builtin.getWorkflowGraph());
    }

    private boolean isWorkflowEmpty(String definition) {
        if (definition == null || definition.isBlank()) {
            return true;
        }
        try {
            WorkflowFullDefinition def = WorkflowJsonConverter.normalize(definition);
            return def == null || def.getNodes() == null || def.getNodes().isEmpty();
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static Map<String, Object> nodeData(String type, Map<String, Object> overrides) {
        return BuiltinCatalogDefaults.merge(BuiltinCatalogDefaults.defaultsFor(type), overrides);
    }

    private static BuiltinNode.Node node(String id, String type, String name, double x, double y, String category,
                                         String description, Map<String, Object> configOverrides) {
        BuiltinNode.Node n = new BuiltinNode.Node();
        n.setId(id);
        n.setType(type);
        n.setName(name);
        n.setX(x);
        n.setY(y);
        n.setCategory(category);
        if (description != null) n.setDescription(description);
        n.setData(nodeData(type, configOverrides));
        return n;
    }

    private static BuiltinNode.Edge edge(String id, String source, String target) {
        return new BuiltinNode.Edge(id, source, target, null, "default", null);
    }

    private static BuiltinNode.Edge edge(String id, String source, String target, String label) {
        return new BuiltinNode.Edge(id, source, target, label, "default", null);
    }

    private static BuiltinNode.Edge edgeCondition(String id, String source, String target, String label) {
        return new BuiltinNode.Edge(id, source, target, label, "condition", null);
    }

    private static BuiltinNode.Edge edgeLoop(String id, String source, String target, String label) {
        return new BuiltinNode.Edge(id, source, target, label, "loop", null);
    }

    private static BuiltinNode.Edge edgeError(String id, String source, String target, String label) {
        return new BuiltinNode.Edge(id, source, target, label, "error", null);
    }

    private BuiltinTemplateEntry add(String id, String name, String description,
                                      long categoryId, String categoryName, String emoji,
                                      long useCount, double score, String scene, List<String> tags,
                                      List<BuiltinNode.Node> nodes, List<BuiltinNode.Edge> edges) {
        String industry = mapIndustry(scene);
        WorkflowFullDefinition def = toFullDefinition(nodes, edges);
        String json = WorkflowJsonConverter.toJsonString(def);
        MarketWorkflowGraph graph = WorkflowJsonConverter.toMarketGraph(def, NODE_W, NODE_H);
        String graphJson = WorkflowJsonConverter.toJsonString(graph);
        BuiltinTemplateEntry entry = BuiltinTemplateEntry.builder()
                .id(id).numericId(-1000L - entries.size()).name(name).description(description)
                .definition(json).workflowJson(json).workflowGraph(graphJson)
                .categoryId(categoryId).categoryName(categoryName).icon(emoji).coverIcon(emoji)
                .industry(industry).scene(scene).tagNames(java.util.Set.copyOf(tags))
                .useCount(useCount).score(score).reviewCount(0).reviewStatus("approved")
                .author("喵流官方").authorName("喵流官方").authorId(1L).price(0.0)
                .createTime(LocalDateTime.of(2026, 6, 1, 0, 0).toString())
                .isPublic("Y").isFeatured("Y").version("v1").remark("内置官方模板")
                .build();
        entries.add(entry);
        return entry;
    }

    private static String mapIndustry(String scene) {
        return switch (scene) {
            case "cs" -> "service";
            case "hr" -> "hr";
            case "ops" -> "ops";
            case "finance" -> "finance";
            case "data" -> "data";
            default -> "general";
        };
    }

    private WorkflowFullDefinition toFullDefinition(List<BuiltinNode.Node> nodes, List<BuiltinNode.Edge> edges) {
        WorkflowFullDefinition def = new WorkflowFullDefinition();
        def.setVersion("v1");
        List<WorkflowFullDefinition.Node> ns = new ArrayList<>();
        if (nodes != null) {
            for (BuiltinNode.Node n : nodes) {
                ns.add(WorkflowFullDefinition.Node.builder()
                        .id(n.getId()).type(n.getType()).name(n.getName())
                        .description(n.getDescription()).x(n.getX()).y(n.getY())
                        .category(n.getCategory()).data(n.getData()).build());
            }
        }
        def.setNodes(ns);
        List<WorkflowFullDefinition.Edge> es = new ArrayList<>();
        if (edges != null) {
            for (BuiltinNode.Edge e : edges) {
                es.add(WorkflowFullDefinition.Edge.builder()
                        .id(e.getId()).source(e.getSource()).target(e.getTarget())
                        .label(e.getLabel()).type(e.getType())
                        .sourceHandle(null).targetHandle(null)
                        .data(e.getData()).build());
            }
        }
        def.setEdges(es);
        return def;
    }

    // =========================================================================
    // 客服场景
    // =========================================================================
    private void registerCustomerServiceScenarios() {
        add("builtin:cs-auto-reply", "智能客服自动回复",
                "接入 IM / 工单系统，多层校验（签名 / 黑名单 / 敏感词）后 AI 识别意图并回复，必要时升级人工",
                1L, "客服场景", "💬",
                1284L, 4.8, "cs",
                List.of("客服", "AI", "自动回复", "RAG"),
                List.of(
                        node("trigger", "trigger.webhook", "IM 入口", 0, 0, "trigger", "接收 IM 平台 webhook 消息",
                                Map.of("method", "POST", "path", "/hooks/cs/inbound", "authToken", "Bearer ****", "timeout", 8000)),
                        node("auth", "code.transform", "签名校验", DX+NODE_W, 0, "transform", "使用 IM 平台签名校验",
                                Map.of("language", "javascript", "source", "const ts = input.headers[\"x-timestamp\"]; const sig = input.headers[\"x-signature\"]; const valid = Math.abs(Date.now() - +ts) < 60000 && sig; return { valid, body: input.body };")),
                        node("extract", "code.transform", "抽取消息字段", DX+NODE_W, -DY, "transform", "从 webhook payload 中抽取字段",
                                Map.of("language", "javascript", "source", "const b = input.body || {}; return { text: b.text || \"\", userId: b.userId, channel: b.channel || \"im\" };")),
                        node("classify", "ai.llm", "意图分类", 2*(DX+NODE_W), 0, "ai", "区分 FAQ / 投诉 / 业务办理",
                                Map.of("model", "gpt-4o-mini", "temperature", 0.2, "categories", "FAQ,投诉,业务办理,表扬,闲聊", "input", "{{extract.text}}", "outputKey", "category")),
                        node("branch", "condition.switch", "分支路由", 3*(DX+NODE_W), 0, "control", null, createSwitchConfig("{{classify.category}}",
                                List.of(Map.of("value", "FAQ", "next", "llmReply"),
                                        Map.of("value", "业务办理", "next", "llmReply"),
                                        Map.of("value", "投诉", "next", "llmReply"),
                                        Map.of("value", "表扬", "next", "llmReply"),
                                        Map.of("value", "闲聊", "next", "llmReply")),
                                "llmReply")),
                        node("llmReply", "ai.llm", "LLM 回复", 4*(DX+NODE_W), 0, "ai", "基于上下文草拟回复",
                                Map.of("model", "gpt-4o-mini", "temperature", 0.4, "maxTokens", 512, "prompt", "请回复用户问题")),
                        node("send", "notify.feishu", "发送回复", 5*(DX+NODE_W), 0, "action", "通过 IM 机器人将回复发送给客户",
                                Map.of("webhook", "{{NOTIFY_WEBHOOK}}", "msgType", "text")),
                        node("end", "end.aggregator", "结束", 6*(DX+NODE_W), 0, "end", null,
                                Map.of("outputMode", "return_last"))
                ),
                List.of(
                        edge("e1", "trigger", "auth"),
                        edgeCondition("e2", "auth", "extract", "通过"),
                        edge("e3", "extract", "classify"),
                        edge("e4", "classify", "branch"),
                        edgeCondition("e5", "branch", "llmReply", "通用"),
                        edge("e6", "llmReply", "send"),
                        edge("e7", "send", "end")
                )
        );

        add("builtin:cs-ticket-route", "工单分类与路由",
                "接收工单后字段校验、AI 分类、按组分发；失败重试 + 死信队列兜底",
                1L, "客服场景", "🎫", 612L, 4.6, "cs",
                List.of("客服", "工单", "AI", "重试"),
                List.of(
                        node("trigger", "trigger.webhook", "Webhook 入口", 0, 0, "trigger", "工单系统推送工单",
                                Map.of("method", "POST", "path", "/hooks/cs/ticket", "authToken", "Bearer ****", "timeout", 8000)),
                        node("classify", "ai.llm", "工单分类", DX+NODE_W, 0, "ai", "判断工单应归属哪个子组",
                                Map.of("model", "gpt-4o-mini", "temperature", 0.2, "categories", "账单,技术支持,投诉,其他", "input", "{{input.title}} {{input.content}}", "outputKey", "category")),
                        node("route", "condition.switch", "按分类路由", 2*(DX+NODE_W), 0, "control", null, createSwitchConfig("{{classify.category}}",
                                List.of(Map.of("value", "账单", "next", "createA"),
                                        Map.of("value", "技术支持", "next", "createB"),
                                        Map.of("value", "投诉", "next", "createC")),
                                "createB")),
                        node("createA", "http.request", "分配：账单组", 3*(DX+NODE_W), -DY, "action", "把工单分配给账单组",
                                Map.of("method", "POST", "url", "https://ticket.internal/api/ticket/forward", "headers", Map.of("Content-Type", "application/json"), "body", "{ \"ticketId\":\"{{input.ticketId}}\", \"group\":\"billing\" }", "timeoutMs", 5000)),
                        node("createB", "http.request", "分配：技术支持", 3*(DX+NODE_W), 0, "action", "把工单分配给技术支持",
                                Map.of("method", "POST", "url", "https://ticket.internal/api/ticket/forward", "headers", Map.of("Content-Type", "application/json"), "body", "{ \"ticketId\":\"{{input.ticketId}}\", \"group\":\"tech-support\" }", "timeoutMs", 5000)),
                        node("createC", "http.request", "分配：投诉组", 3*(DX+NODE_W), DY, "action", "把工单分配给投诉组",
                                Map.of("method", "POST", "url", "https://ticket.internal/api/ticket/forward", "headers", Map.of("Content-Type", "application/json"), "body", "{ \"ticketId\":\"{{input.ticketId}}\", \"group\":\"complaint\" }", "timeoutMs", 5000)),
                        node("notify", "notify.feishu", "发送通知", 4*(DX+NODE_W), 0, "action", "通过飞书群机器人通知",
                                Map.of("webhook", "{{NOTIFY_WEBHOOK}}", "msgType", "rich")),
                        node("end", "end.aggregator", "结束", 5*(DX+NODE_W), 0, "end", null,
                                Map.of("outputMode", "return_last"))
                ),
                List.of(
                        edge("e1", "trigger", "classify"),
                        edge("e2", "classify", "route"),
                        edgeCondition("e3", "route", "createA", "账单"),
                        edgeCondition("e4", "route", "createB", "技术"),
                        edgeCondition("e5", "route", "createC", "投诉"),
                        edge("e6", "createA", "notify"),
                        edge("e7", "createB", "notify"),
                        edge("e8", "createC", "notify"),
                        edge("e9", "notify", "end")
                )
        );
    }

    // =========================================================================
    // HR 场景
    // =========================================================================
    private void registerHRScenarios() {
        add("builtin:hr-resume-screen", "简历自动筛选",
                "招聘网站投递自动解析、匹配 JD 三档评分，分别推送 HR / 入人才库 / 自动婉拒",
                2L, "人力资源", "📄", 921L, 4.7, "hr",
                List.of("HR", "招聘", "AI"),
                List.of(
                        node("trigger", "trigger.webhook", "投递接收", 0, 0, "trigger", "招聘网站投递触发",
                                Map.of("method", "POST", "path", "/hooks/hr/resume", "authToken", "Bearer ****", "timeout", 10000)),
                        node("score", "ai.llm", "简历评分", DX+NODE_W, 0, "ai", "对简历进行评分",
                                Map.of("model", "gpt-4o-mini", "temperature", 0.15, "maxTokens", 512, "prompt", "请根据简历内容给出 0-100 评分")),
                        node("branch", "condition.if", "评分路由", 2*(DX+NODE_W), 0, "control", null,
                                Map.of("expression", "{{score.total}} >= 80", "trueNext", "toHr", "falseNext", "reject")),
                        node("toHr", "notify.email", "推送给 HR", 3*(DX+NODE_W), -DY, "action", "把高分简历推送给 HR",
                                Map.of("host", "", "port", 465, "ssl", true, "to", "hr@meowflow.com", "subject", "【高分简历】", "body", "有新的高分简历待处理")),
                        node("reject", "notify.email", "婉拒邮件", 3*(DX+NODE_W), DY, "action", "婉拒邮件",
                                Map.of("host", "", "port", 465, "ssl", true, "to", "{{input.email}}", "subject", "感谢您的投递", "body", "感谢您投递我们公司")),
                        node("end", "end.aggregator", "结束", 4*(DX+NODE_W), 0, "end", null,
                                Map.of("outputMode", "return_last"))
                ),
                List.of(
                        edge("e1", "trigger", "score"),
                        edge("e2", "score", "branch"),
                        edgeCondition("e3", "branch", "toHr", "高分"),
                        edgeCondition("e4", "branch", "reject", "低分"),
                        edge("e5", "toHr", "end"),
                        edge("e6", "reject", "end")
                )
        );

        add("builtin:hr-leave-approve", "请假审批",
                "员工提交请假 → 校验 → 审批 → 通知",
                2L, "人力资源", "🌴", 412L, 4.6, "hr",
                List.of("HR", "审批", "表单"),
                List.of(
                        node("trigger", "trigger.form", "员工提交请假", 0, 0, "trigger", "员工填写请假表单提交",
                                createFormTrigger()),
                        node("approve", "http.request", "主管审批", DX+NODE_W, 0, "action", "主管审批请假",
                                Map.of("method", "POST", "url", "https://oa.internal/api/leave/approve", "headers", Map.of("Content-Type", "application/json"), "body", "{ \"applicant\":\"{{trigger.applicant}}\", \"days\":{{trigger.days}} }", "timeoutMs", 5000)),
                        node("notify", "notify.feishu", "回复员工", 2*(DX+NODE_W), 0, "action", "通知员工审批结果",
                                Map.of("webhook", "{{NOTIFY_WEBHOOK}}", "msgType", "rich")),
                        node("end", "end.aggregator", "结束", 3*(DX+NODE_W), 0, "end", null,
                                Map.of("outputMode", "return_last"))
                ),
                List.of(
                        edge("e1", "trigger", "approve"),
                        edge("e2", "approve", "notify"),
                        edge("e3", "notify", "end")
                )
        );
    }

    // =========================================================================
    // 运营场景
    // =========================================================================
    private void registerOpsScenarios() {
        add("builtin:ops-meeting-notes", "会议纪要生成",
                "上传音频 → ASR 转写 → AI 提取摘要 → 通知",
                3L, "运营提效", "🗒️", 1421L, 4.9, "ops",
                List.of("运营", "会议", "AI"),
                List.of(
                        node("trigger", "trigger.webhook", "音频上传触发", 0, 0, "trigger", "上传会议录音触发",
                                Map.of("method", "POST", "path", "/hooks/ops/meeting-upload", "authToken", "Bearer ****", "timeout", 10000)),
                        node("summarize", "ai.llm", "生成摘要", DX+NODE_W, 0, "ai", "提取会议摘要、决议和待办",
                                Map.of("model", "gpt-4o-mini", "temperature", 0.2, "maxTokens", 2000, "prompt", "从以下文本中提取：summary、decisions[]、todos[]")),
                        node("notify", "notify.feishu", "飞书群通知", 2*(DX+NODE_W), 0, "action", "飞书群推送摘要",
                                Map.of("webhook", "{{NOTIFY_WEBHOOK}}", "msgType", "rich")),
                        node("end", "end.aggregator", "结束", 3*(DX+NODE_W), 0, "end", null,
                                Map.of("outputMode", "return_last"))
                ),
                List.of(
                        edge("e1", "trigger", "summarize"),
                        edge("e2", "summarize", "notify"),
                        edge("e3", "notify", "end")
                )
        );
    }

    // =========================================================================
    // 财务场景
    // =========================================================================
    private void registerFinanceScenarios() {
        add("builtin:finance-invoice-ocr", "发票识别与归档",
                "上传发票 → OCR → 字段校验 → 归档",
                4L, "财务行政", "🧾", 354L, 4.7, "finance",
                List.of("财务", "发票", "AI", "OCR"),
                List.of(
                        node("trigger", "trigger.webhook", "上传发票触发", 0, 0, "trigger", "上传发票图片触发",
                                Map.of("method", "POST", "path", "/hooks/finance/invoice", "authToken", "Bearer ****", "timeout", 15000)),
                        node("verify", "ai.llm", "字段校验", DX+NODE_W, 0, "ai", "校验发票号、金额等字段",
                                Map.of("model", "gpt-4o-mini", "temperature", 0.05, "prompt", "校验发票字段并给出风险等级")),
                        node("archive", "http.request", "入档案系统", 2*(DX+NODE_W), 0, "action", "把发票推入档案系统",
                                Map.of("method", "POST", "url", "https://archive.internal/api/invoices", "headers", Map.of("Content-Type", "application/json"), "body", "{ \"invoiceNo\":\"{{verify.invoiceNo}}\" }")),
                        node("end", "end.aggregator", "结束", 3*(DX+NODE_W), 0, "end", null,
                                Map.of("outputMode", "return_last"))
                ),
                List.of(
                        edge("e1", "trigger", "verify"),
                        edge("e2", "verify", "archive"),
                        edge("e3", "archive", "end")
                )
        );
    }

    // =========================================================================
    // 通用场景
    // =========================================================================
    private void registerGeneralAndDataScenarios() {
        add("builtin:general-webhook", "通用 Webhook 接入",
                "通用 Webhook 接收 → 校验 → 执行业务",
                5L, "通用", "🔗", 425L, 4.5, "general",
                List.of("通用", "Webhook", "回调"),
                List.of(
                        node("trigger", "trigger.webhook", "Webhook 入口", 0, 0, "trigger", "通用 webhook 接收",
                                Map.of("method", "POST", "path", "/hooks/generic", "authToken", "", "timeout", 10000)),
                        node("handle", "code.transform", "执行业务逻辑", DX+NODE_W, 0, "transform", "处理上游业务",
                                Map.of("language", "javascript", "source", "return { ok: true, processedAt: Date.now(), payload: input.body };")),
                        node("end", "end.aggregator", "结束", 2*(DX+NODE_W), 0, "end", null,
                                Map.of("outputMode", "return_last"))
                ),
                List.of(
                        edge("e1", "trigger", "handle"),
                        edge("e2", "handle", "end")
                )
        );
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    private static Map<String, Object> createSwitchConfig(String field, List<Map<String, String>> cases, String defaultNext) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("field", field);
        result.put("cases", cases);
        result.put("defaultNext", defaultNext);
        return result;
    }

    private static Map<String, Object> createFormTrigger() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("formId", "form_leave_request");
        m.put("fields", List.of(
                Map.of("key", "applicant", "label", "申请人", "type", "string", "required", true),
                Map.of("key", "days", "label", "天数", "type", "number", "required", true),
                Map.of("key", "reason", "label", "事由", "type", "text", "required", true),
                Map.of("key", "leaveType", "label", "类型", "type", "select", "options", List.of("年假", "病假", "事假", "调休"), "required", true)
        ));
        return m;
    }
}

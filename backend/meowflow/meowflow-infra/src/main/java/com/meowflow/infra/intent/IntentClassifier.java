package com.meowflow.infra.intent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.infra.cache.RedisCacheManager;
import com.meowflow.infra.client.ChatClient;
import com.meowflow.infra.client.ChatClient.ChatOptions;
import com.meowflow.infra.client.ChatClient.ChatResponse;
import com.meowflow.infra.client.ChatClient.Message;
import com.meowflow.infra.service.ChatClientFactory;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 意图识别服务 - 使用 LLM 进行意图分类
 * 借鉴 ragent 的 IntentClassifier 设计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifier {

    private final ChatClientFactory chatClientFactory;
    private final RedisCacheManager cacheManager;
    private final ObjectMapper objectMapper;
    
    private static final String INTENT_TREE_CACHE_KEY = "intent:tree";
    private static final Duration INTENT_TREE_CACHE_TTL = Duration.ofDays(7);
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final double CONFIDENCE_THRESHOLD = 0.3;

    @PostConstruct
    public void init() {
        log.info("IntentClassifier initialized");
    }

    /**
     * 分类用户意图
     *
     * @param question 用户问题
     * @return 意图得分列表（按得分降序）
     */
    public List<NodeScore> classify(String question) {
        return classify(question, null);
    }

    /**
     * 分类用户意图（带意图树缓存）
     *
     * @param question 用户问题
     * @param customIntentTree 自定义意图树（可选）
     * @return 意图得分列表（按得分降序）
     */
    public List<NodeScore> classify(String question, List<IntentNode> customIntentTree) {
        List<IntentNode> intentTree = customIntentTree != null ? customIntentTree : loadIntentTree();
        
        if (intentTree == null || intentTree.isEmpty()) {
            log.debug("No intent tree configured, returning empty classification");
            return List.of();
        }
        
        String systemPrompt = buildClassificationPrompt(intentTree);
        String userPrompt = "用户问题: " + question;
        
        try {
            ChatResponse response = chatClientFactory.chat(
                DEFAULT_MODEL,
                List.of(Message.system(systemPrompt), Message.user(userPrompt)),
                ChatOptions.of(0.1, 512) // 低温度确保稳定性
            );
            
            List<NodeScore> scores = parseClassificationResult(response.content(), intentTree);
            
            // 按分数降序排序
            scores.sort(Comparator.comparingDouble(NodeScore::getScore).reversed());
            
            log.info("Intent classification result for '{}': {} intents matched", 
                question, scores.size());
            
            return scores;
        } catch (Exception e) {
            log.error("Intent classification failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取最高置信度的意图
     */
    public NodeScore getTopIntent(String question) {
        List<NodeScore> scores = classify(question);
        return scores.stream()
            .filter(s -> s.getScore() >= CONFIDENCE_THRESHOLD)
            .findFirst()
            .orElse(null);
    }

    /**
     * 判断是否为闲聊/系统对话（不需要工具调用）
     */
    public boolean isChitchat(String question) {
        List<NodeScore> scores = classify(question);
        return scores.isEmpty() || scores.stream().allMatch(s -> s.getScore() < CONFIDENCE_THRESHOLD);
    }

    /**
     * 加载意图树（带 Redis 缓存）
     */
    private List<IntentNode> loadIntentTree() {
        try {
            return cacheManager.getOrLoad(
                INTENT_TREE_CACHE_KEY,
                this::buildDefaultIntentTree,
                INTENT_TREE_CACHE_TTL
            );
        } catch (Exception e) {
            log.error("Failed to load intent tree: {}", e.getMessage());
            return buildDefaultIntentTree();
        }
    }

    /**
     * 构建默认意图树
     */
    private List<IntentNode> buildDefaultIntentTree() {
        List<IntentNode> tree = new ArrayList<>();
        
        // 天气查询
        tree.add(new IntentNode("weather", "weather", "weather_search",
            "天气查询相关问题，如：今天天气、明天会下雨吗、某城市温度等",
            Map.of("type", "mcp_tool", "toolName", "weather")));
        
        // 知识问答
        tree.add(new IntentNode("qa", "knowledge_qa", "knowledge_search",
            "知识问答、信息查询，如：什么是XXX、XXX的原理、某个概念的解释",
            Map.of("type", "knowledge_base")));
        
        // 任务执行
        tree.add(new IntentNode("task", "task_execution", "task_executor",
            "需要执行特定任务，如：帮我发邮件、创建一个工作流、执行某个操作",
            Map.of("type", "mcp_tool")));
        
        // 闲聊
        tree.add(new IntentNode("chitchat", "chitchat", "chitchat",
            "闲聊、寒暄、问候，如：你好、今天怎么样、谢谢你",
            Map.of("type", "direct_response")));
        
        return tree;
    }

    /**
     * 构建分类提示词
     */
    private String buildClassificationPrompt(List<IntentNode> nodes) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个意图分类器。请根据用户的问题，判断用户最可能想要做什么。\n\n");
        prompt.append("请从以下意图中选择最匹配的一个（或多个）：\n\n");
        
        for (IntentNode node : nodes) {
            prompt.append(String.format("- %s（%s）: %s\n",
                node.getName(), node.getCode(), node.getDescription()));
        }
        
        prompt.append("\n");
        prompt.append("请以 JSON 格式输出分类结果：\n");
        prompt.append("{\n");
        prompt.append("  \"intents\": [\n");
        prompt.append("    {\"code\": \"意图代码\", \"score\": 0.0-1.0之间的置信度},\n");
        prompt.append("    ...\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("\n只输出 JSON，不要有其他文字。");
        
        return prompt.toString();
    }

    /**
     * 解析分类结果
     */
    @SuppressWarnings("unchecked")
    private List<NodeScore> parseClassificationResult(String content, List<IntentNode> nodes) {
        List<NodeScore> scores = new ArrayList<>();
        
        try {
            String json = cleanJsonResponse(content);
            JsonNode root = objectMapper.readTree(json);
            JsonNode intents = root.get("intents");
            
            if (intents != null && intents.isArray()) {
                for (JsonNode intentNode : intents) {
                    String code = intentNode.get("code").asText();
                    double score = intentNode.get("score").asDouble();
                    
                    // 找到对应的意图节点
                    nodes.stream()
                        .filter(n -> n.getCode().equals(code))
                        .findFirst()
                        .ifPresent(node -> scores.add(new NodeScore(node, score)));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse classification result: {}", e.getMessage());
        }
        
        return scores;
    }

    /**
     * 清理 JSON 响应
     */
    private String cleanJsonResponse(String content) {
        if (content == null) return "{}";
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    /**
     * 更新意图树缓存
     */
    public void updateIntentTreeCache(List<IntentNode> nodes) {
        cacheManager.put(INTENT_TREE_CACHE_KEY, nodes, INTENT_TREE_CACHE_TTL);
        log.info("Intent tree cache updated with {} nodes", nodes.size());
    }

    /**
     * 清除意图树缓存
     */
    public void clearIntentTreeCache() {
        cacheManager.delete(INTENT_TREE_CACHE_KEY);
        log.info("Intent tree cache cleared");
    }

    /**
     * 意图节点
     */
    @Data
    public static class IntentNode {
        private String code;
        private String name;
        private String handler;
        private String description;
        private Map<String, Object> config;

        public IntentNode() {}

        public IntentNode(String code, String name, String handler, String description, Map<String, Object> config) {
            this.code = code;
            this.name = name;
            this.handler = handler;
            this.description = description;
            this.config = config;
        }
    }

    /**
     * 意图得分
     */
    @Data
    public static class NodeScore {
        private final IntentNode node;
        private final double score;

        public String getCode() { return node.getCode(); }
        public String getName() { return node.getName(); }
        public String getHandler() { return node.getHandler(); }
        public Map<String, Object> getConfig() { return node.getConfig(); }
    }
}

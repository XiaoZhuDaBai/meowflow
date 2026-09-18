package com.meowflow.infra.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 成本计算器
 * <p>
 * 根据模型和 token 数量计算实际调用费用
 */
@Component
public class AICostCalculator {

    /**
     * 模型单价表 (每 1M tokens 的价格, USD)
     */
    private static final Map<String, ModelPrice> MODEL_PRICES;
    static {
        MODEL_PRICES = new HashMap<>();
        // OpenAI
        MODEL_PRICES.put("gpt-4o", new ModelPrice(5.0, 15.0));
        MODEL_PRICES.put("gpt-4o-mini", new ModelPrice(0.15, 0.6));
        MODEL_PRICES.put("gpt-4-turbo", new ModelPrice(10.0, 30.0));
        MODEL_PRICES.put("gpt-3.5-turbo", new ModelPrice(0.5, 1.5));
        // Anthropic
        MODEL_PRICES.put("claude-3-5-sonnet", new ModelPrice(3.0, 15.0));
        MODEL_PRICES.put("claude-3-5-haiku", new ModelPrice(0.8, 4.0));
        MODEL_PRICES.put("claude-3-opus", new ModelPrice(15.0, 75.0));
        MODEL_PRICES.put("claude-3-sonnet", new ModelPrice(3.0, 15.0));
        MODEL_PRICES.put("claude-3-haiku", new ModelPrice(0.25, 1.25));
        // Alibaba (通义千问)
        MODEL_PRICES.put("qwen-turbo", new ModelPrice(0.5, 2.0));
        MODEL_PRICES.put("qwen-plus", new ModelPrice(2.0, 8.0));
        MODEL_PRICES.put("qwen-max", new ModelPrice(20.0, 60.0));
        // Baidu (文心一言)
        MODEL_PRICES.put("ernie-4.0", new ModelPrice(20.0, 80.0));
        MODEL_PRICES.put("ernie-3.5", new ModelPrice(2.0, 8.0));
        // DeepSeek
        MODEL_PRICES.put("deepseek-chat", new ModelPrice(0.1, 0.27));
        MODEL_PRICES.put("deepseek-coder", new ModelPrice(0.14, 0.28));
        // MiniMax
        MODEL_PRICES.put("abab6-chat", new ModelPrice(0.5, 1.5));
        // Zhipu (智谱)
        MODEL_PRICES.put("glm-4", new ModelPrice(1.0, 4.0));
        MODEL_PRICES.put("glm-4-flash", new ModelPrice(0.1, 0.1));
        // ByteDance (豆包)
        MODEL_PRICES.put("doubao-pro-32k", new ModelPrice(1.0, 4.0));
        // Tencent (混元)
        MODEL_PRICES.put("hunyuan", new ModelPrice(1.0, 5.0));
    }

    /**
     * 计算单次调用的费用
     */
    public double calculate(AIInvokeLog log) {
        if (log == null || log.getModel() == null) {
            return 0.0;
        }

        ModelPrice price = MODEL_PRICES.get(log.getModel().toLowerCase());
        if (price == null) {
            return 0.0;
        }

        int promptTokens = log.getPromptTokens() != null ? log.getPromptTokens() : 0;
        int completionTokens = log.getCompletionTokens() != null ? log.getCompletionTokens() : 0;

        // 费用 = (promptTokens / 1M) * promptPrice + (completionTokens / 1M) * completionPrice
        return (promptTokens / 1_000_000.0) * price.getPromptPrice()
             + (completionTokens / 1_000_000.0) * price.getCompletionPrice();
    }

    /**
     * 计算费用并填充到 log 对象
     */
    public void calculateAndFill(AIInvokeLog log) {
        if (log == null) {
            return;
        }
        log.setCostAmount(calculate(log));
    }

    /**
     * 获取模型单价
     */
    public ModelPrice getModelPrice(String model) {
        return MODEL_PRICES.get(model.toLowerCase());
    }

    /**
     * 检查模型是否支持
     */
    public boolean isSupported(String model) {
        return model != null && MODEL_PRICES.containsKey(model.toLowerCase());
    }

    /**
     * 模型单价
     */
    @Data
    @AllArgsConstructor
    public static class ModelPrice {
        /**
         * 输入价格 (每 1M tokens, USD)
         */
        private double promptPrice;

        /**
         * 输出价格 (每 1M tokens, USD)
         */
        private double completionPrice;
    }
}

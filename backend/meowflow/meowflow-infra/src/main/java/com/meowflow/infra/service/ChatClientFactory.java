package com.meowflow.infra.service;

import com.meowflow.infra.client.ChatClient;
import com.meowflow.infra.client.ChatClient.*;
import com.meowflow.infra.client.*;
import com.meowflow.infra.router.HealthChecker;
import com.meowflow.infra.router.ModelCandidate;
import com.meowflow.infra.router.ModelRouter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * ChatClient 工厂服务
 */
@Slf4j
@Service
public class ChatClientFactory {

    /**
     * 供应商枚举（对外暴露的只读视图）
     */
    public enum AIProvider {
        OPENAI, CLAUDE, GEMINI, AZURE, QWEN, DOUBAO, DEEPSEEK, ZHIPU, MOONSHOT, OLLAMA, CUSTOM
    }

    private final ModelRouter modelRouter;
    private final HealthChecker healthChecker;

    @Value("${meowflow.infra.chat.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    @Value("${meowflow.infra.chat.openai.api-key:}")
    private String openaiApiKey;

    @Value("${meowflow.infra.chat.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${meowflow.infra.chat.anthropic.base-url:https://api.anthropic.com}")
    private String anthropicBaseUrl;

    @Value("${meowflow.infra.chat.anthropic.api-key:}")
    private String anthropicApiKey;

    @Value("${meowflow.infra.chat.anthropic.model:claude-3-5-sonnet-20240620}")
    private String anthropicModel;

    @Value("${meowflow.infra.chat.ali.base-url:}")
    private String aliBaseUrl;

    @Value("${meowflow.infra.chat.ali.api-key:}")
    private String aliApiKey;

    @Value("${meowflow.infra.chat.ali.model:qwen-turbo}")
    private String aliModel;

    @Value("${meowflow.infra.chat.baidu.api-key:}")
    private String baiduApiKey;

    @Value("${meowflow.infra.chat.baidu.secret-key:}")
    private String baiduSecretKey;

    @Value("${meowflow.infra.chat.baidu.model:ernie-4.0-8k-latest}")
    private String baiduModel;

    public ChatClientFactory(ModelRouter modelRouter, HealthChecker healthChecker) {
        this.modelRouter = modelRouter;
        this.healthChecker = healthChecker;
    }

    @PostConstruct
    public void init() {
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            ChatClient openaiClient = new ChatClientOpenAI(openaiBaseUrl, openaiApiKey, openaiModel);
            modelRouter.registerClient("openai:" + openaiModel, openaiClient);
            log.info("Registered OpenAI client: {}", openaiModel);
        }

        if (anthropicApiKey != null && !anthropicApiKey.isEmpty()) {
            ChatClient anthropicClient = new ChatClientAnthropic(anthropicBaseUrl, anthropicApiKey, anthropicModel);
            modelRouter.registerClient("anthropic:" + anthropicModel, anthropicClient);
            log.info("Registered Anthropic client: {}", anthropicModel);
        }

        if (aliApiKey != null && !aliApiKey.isEmpty()) {
            ChatClient aliClient = new ChatClientAli(aliBaseUrl, aliApiKey, aliModel);
            modelRouter.registerClient("ali:" + aliModel, aliClient);
            log.info("Registered Ali client: {}", aliModel);
        }

        if (baiduApiKey != null && !baiduApiKey.isEmpty()) {
            ChatClient baiduClient = new ChatClientBaidu(baiduApiKey, baiduSecretKey, baiduModel);
            modelRouter.registerClient("baidu:" + baiduModel, baiduClient);
            log.info("Registered Baidu client: {}", baiduModel);
        }
    }

    public ChatClient getClient(String modelName) {
        return modelRouter.getClient(modelName);
    }

    public ChatResponse chat(String modelName, List<Message> messages) {
        return chat(modelName, messages, ChatOptions.defaults());
    }

    public ChatResponse chat(String modelName, List<Message> messages, ChatOptions options) {
        return modelRouter.routeAndChat(modelName, messages, options);
    }

    public void streamChat(String modelName, List<Message> messages, StreamCallback callback) {
        streamChat(modelName, messages, ChatOptions.defaults(), callback);
    }

    public void streamChat(String modelName, List<Message> messages, ChatOptions options, StreamCallback callback) {
        modelRouter.routeAndStreamChat(modelName, messages, options, callback);
    }

    public List<String> getAvailableModels() {
        return modelRouter.getAvailableModels();
    }

    public Map<String, Boolean> getModelHealthStatus() {
        Map<String, Boolean> status = new java.util.HashMap<>();
        for (ModelCandidate candidate : healthChecker.getHealthyCandidates()) {
            status.put(candidate.getName(), true);
        }
        return status;
    }

    public List<AIProvider> getProviders() {
        return List.of(AIProvider.OPENAI, AIProvider.CLAUDE, AIProvider.GEMINI,
                AIProvider.AZURE, AIProvider.QWEN, AIProvider.DOUBAO, AIProvider.DEEPSEEK,
                AIProvider.ZHIPU, AIProvider.MOONSHOT, AIProvider.OLLAMA, AIProvider.CUSTOM);
    }
}

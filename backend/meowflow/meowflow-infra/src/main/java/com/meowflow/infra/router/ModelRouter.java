package com.meowflow.infra.router;

import com.meowflow.infra.client.ChatClient;
import com.meowflow.infra.client.ChatClient.ChatOptions;
import com.meowflow.infra.client.ChatClient.ChatResponse;
import com.meowflow.infra.client.ChatClient.Message;
import com.meowflow.infra.client.ChatClient.StreamCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 模型路由器 - 支持多模型切换和降级
 */
@Slf4j
@Component
public class ModelRouter {

    private final Map<String, ChatClient> clients = new ConcurrentHashMap<>();
    private final Map<String, List<ModelCandidate>> fallbackChains = new ConcurrentHashMap<>();
    private final HealthChecker healthChecker;

    public ModelRouter(HealthChecker healthChecker) {
        this.healthChecker = healthChecker;
    }

    public void registerClient(String name, ChatClient client) {
        clients.put(name, client);
        ModelCandidate candidate = new ModelCandidate(name, getProvider(name), client);
        healthChecker.registerCandidate(candidate);
    }

    public void registerFallbackChain(String primaryModel, List<String> fallbackModels) {
        List<ModelCandidate> chain = new CopyOnWriteArrayList<>();

        ChatClient primary = clients.get(primaryModel);
        if (primary != null) {
            chain.add(new ModelCandidate(primaryModel, getProvider(primaryModel), primary, 100, 1.0));
        }

        for (int i = 0; i < fallbackModels.size(); i++) {
            String model = fallbackModels.get(i);
            ChatClient client = clients.get(model);
            if (client != null) {
                chain.add(new ModelCandidate(model, getProvider(model), client, 100 - (i + 1) * 10, 0.5));
            }
        }

        fallbackChains.put(primaryModel, chain);
    }

    public ChatResponse routeAndChat(String modelName, List<Message> messages, ChatOptions options) {
        List<ModelCandidate> chain = fallbackChains.getOrDefault(modelName, getDefaultChain(modelName));

        for (ModelCandidate candidate : chain) {
            if (!candidate.isHealthy()) continue;

            try {
                long start = System.currentTimeMillis();
                ChatResponse response = candidate.getClient().chat(messages, options);
                response = new ChatResponse(
                        response.content(),
                        candidate.getName(),
                        response.promptTokens(),
                        response.completionTokens(),
                        response.totalTokens(),
                        System.currentTimeMillis() - start,
                        response.finishReason()
                );
                log.info("Model {} responded successfully, latency={}ms", candidate.getName(), response.latencyMs());
                return response;
            } catch (Exception e) {
                log.warn("Model {} failed: {}, trying next...", candidate.getName(), e.getMessage());
                candidate.markHealthy(false);
            }
        }

        throw new RuntimeException("All models in fallback chain are unavailable");
    }

    public void routeAndStreamChat(String modelName, List<Message> messages, ChatOptions options, StreamCallback callback) {
        List<ModelCandidate> chain = fallbackChains.getOrDefault(modelName, getDefaultChain(modelName));

        for (ModelCandidate candidate : chain) {
            if (!candidate.isHealthy()) continue;

            try {
                candidate.getClient().streamChat(messages, options, callback);
                return;
            } catch (Exception e) {
                log.warn("Model {} stream failed: {}, trying next...", candidate.getName(), e.getMessage());
                candidate.markHealthy(false);
            }
        }

        callback.onError(new RuntimeException("All models in fallback chain are unavailable"));
    }

    public ChatClient getClient(String modelName) {
        ChatClient client = clients.get(modelName);
        if (client == null) {
            client = clients.values().stream().findFirst().orElse(null);
        }
        return client;
    }

    public List<String> getAvailableModels() {
        return clients.keySet().stream().toList();
    }

    private List<ModelCandidate> getDefaultChain(String modelName) {
        List<ModelCandidate> chain = new ArrayList<>();
        ChatClient client = clients.get(modelName);
        if (client != null) {
            chain.add(new ModelCandidate(modelName, getProvider(modelName), client));
        }
        for (Map.Entry<String, ChatClient> entry : clients.entrySet()) {
            if (!entry.getKey().equals(modelName)) {
                chain.add(new ModelCandidate(entry.getKey(), getProvider(entry.getKey()), entry.getValue()));
            }
        }
        return chain;
    }

    private String getProvider(String modelName) {
        String lower = modelName.toLowerCase();
        if (lower.contains("gpt") || lower.contains("openai")) return "openai";
        if (lower.contains("claude") || lower.contains("anthropic")) return "anthropic";
        if (lower.contains("qwen") || lower.contains("ali") || lower.contains("tongyi")) return "ali";
        if (lower.contains("ernie") || lower.contains("baidu") || lower.contains("wenxin")) return "baidu";
        return "unknown";
    }
}

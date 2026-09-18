package com.meowflow.infra.service;

import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatRequest;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.infra.chat.Message;
import com.meowflow.infra.embedding.EmbeddingClient;
import com.meowflow.infra.embedding.EmbeddingClientFactory;
import com.meowflow.infra.entity.AIModelEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 使用数据库中保存的模型配置执行真实连通性测试。
 */
@Service
@RequiredArgsConstructor
public class AIModelConnectionTester {

    private final AIModelService modelService;
    private final ChatModelGateway chatModelGateway;
    private final EmbeddingClientFactory embeddingClientFactory;

    public Map<String, Object> test(Long id) {
        AIModelEntity model = modelService.getById(id);
        if (model == null) {
            return Map.of("success", false, "message", "模型不存在");
        }
        try {
            if (model.getCapabilities() != null && model.getCapabilities().toLowerCase().contains("embedding")) {
                EmbeddingClient client = embeddingClientFactory.create(model);
                EmbeddingClient.EmbeddingResult result = client.embed("ping");
                return Map.of(
                        "success", true,
                        "message", "Embedding 连接成功",
                        "model", client.getModelName(),
                        "dimension", result.vector().length
                );
            }
            ChatResponse response = chatModelGateway.complete(ChatRequest.builder()
                    .model(String.valueOf(model.getId()))
                    .messages(List.of(Message.user("Reply with OK.")))
                    .temperature(0.0)
                    .maxTokens(8)
                    .build());
            return Map.of(
                    "success", true,
                    "message", "连接成功",
                    "model", response.getModel() == null ? model.getModelKey() : response.getModel(),
                    "latencyMs", response.getLatencyMs() == null ? 0L : response.getLatencyMs()
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage() == null ? "连接失败" : e.getMessage());
        }
    }
}

package com.meowflow.infra.controller;

import com.meowflow.common.result.Result;
import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatRequest;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.infra.chat.Message;
import com.meowflow.infra.service.ChatClientFactory.AIProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * AI 对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/infra/chat")
@Tag(name = "AI 对话", description = "AI 模型对话接口")
public class ChatClientController {

    private final ChatModelGateway chatModelGateway;

    public ChatClientController(ChatModelGateway chatModelGateway) {
        this.chatModelGateway = chatModelGateway;
    }

    @PostMapping("/chat")
    @Operation(summary = "对话请求", description = "使用前端保存的模型配置发送对话请求")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        return Result.success(chatModelGateway.complete(toGatewayRequest(request)));
    }

    @PostMapping("/stream")
    @Operation(summary = "流式对话", description = "向 AI 模型发送流式对话请求（Server-Sent Events）")
    public ResponseBodyEmitter streamChat(@RequestBody ChatRequest request) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(120_000L);
        chatModelGateway.stream(toGatewayRequest(request)).subscribe(
                content -> {
                    try {
                        String event = String.format("data: {\"type\":\"chunk\",\"content\":\"%s\"}\n\n", escapeForSse(content));
                        emitter.send(event.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("Stream chat error for model {}: {}", request.getModel(), error.getMessage(), error);
                    emitter.completeWithError(error);
                },
                () -> emitter.complete()
        );
        return emitter;
    }

    private com.meowflow.infra.chat.ChatRequest toGatewayRequest(ChatRequest request) {
        List<Message> messages = request.getMessages() == null ? List.of() : request.getMessages().stream()
                .map(m -> new Message(m.getRole(), m.getContent()))
                .toList();
        return com.meowflow.infra.chat.ChatRequest.builder()
                .model(request.getModel())
                .messages(messages)
                .temperature(request.getOptions() != null ? request.getOptions().getTemperature() : null)
                .maxTokens(request.getOptions() != null ? request.getOptions().getMaxTokens() : null)
                .build();
    }

    private static String escapeForSse(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @GetMapping("/providers")
    @Operation(summary = "获取 AI 提供商", description = "获取当前支持的 AI Provider 列表")
    public Result<List<AIProvider>> getProviders() {
        return Result.success(List.of(AIProvider.values()));
    }

    @GetMapping("/models")
    @Operation(summary = "获取可用模型", description = "获取当前可用的 AI 模型列表")
    public Result<List<String>> getModels() {
        return Result.success(chatModelGateway.getSupportedModels());
    }

    @GetMapping("/health")
    @Operation(summary = "获取模型健康状态", description = "检查指定模型是否可用")
    public Result<Map<String, Boolean>> getModelHealth(
            @RequestParam(defaultValue = "default") String model) {
        return Result.success(Map.of(model, chatModelGateway.isHealthy(model)));
    }

    @Data
    public static class ChatRequest {
        @Parameter(description = "模型名称")
        private String model;

        @Parameter(description = "消息列表")
        private List<MessageDTO> messages;

        @Parameter(description = "请求选项")
        private OptionsDTO options;
    }

    @Data
    public static class MessageDTO {
        @Parameter(description = "角色: system/user/assistant")
        private String role;

        @Parameter(description = "消息内容")
        private String content;
    }

    @Data
    public static class OptionsDTO {
        @Parameter(description = "温度参数")
        private Double temperature;

        @Parameter(description = "最大 token 数")
        private Integer maxTokens;

        @Parameter(description = "top_p 参数")
        private Double topP;

        @Parameter(description = "停止词")
        private List<String> stop;
    }
}


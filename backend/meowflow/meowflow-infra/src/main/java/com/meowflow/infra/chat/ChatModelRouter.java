package com.meowflow.infra.chat;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.infra.entity.AIModelEntity;
import com.meowflow.infra.service.AIModelService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 模型路由组件。
 * 优先使用前端保存到数据库的模型配置，并兼容原有的环境变量客户端。
 */
@Slf4j
@Component("chatModelRouter")
public class ChatModelRouter {

    private final Map<String, ChatClient> chatClients;
    private final Map<String, ChatClient> clientsByProvider;
    private final CircuitBreakerRegistry circuitBreakerFactory;
    private final AIModelService modelService;
    private final AIModelClientFactory modelClientFactory;

    public ChatModelRouter(
            Map<String, ChatClient> chatClients,
            @Qualifier("aiCircuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerFactory,
            AIModelService modelService,
            AIModelClientFactory modelClientFactory) {
        this.chatClients = chatClients;
        this.clientsByProvider = chatClients.values().stream()
                .collect(Collectors.toMap(
                        client -> normalizeProvider(client.getProviderName()),
                        Function.identity(),
                        (first, ignored) -> first));
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.modelService = modelService;
        this.modelClientFactory = modelClientFactory;
    }

    public ChatResponse routeWithCircuitBreaker(ChatRequest request) {
        ResolvedModel resolved = resolve(request);
        CircuitBreaker circuitBreaker = circuitBreakerFactory.circuitBreaker(resolved.provider());
        try {
            log.info("Routing LLM request: provider={}, model={}", resolved.provider(), resolved.request().getModel());
            return circuitBreaker.executeSupplier(() -> resolved.client().complete(resolved.request()));
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE,
                    "模型服务熔断中: " + resolved.provider());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM request failed: provider={}, model={}", resolved.provider(),
                    resolved.request().getModel(), e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE,
                    "LLM 调用失败: " + e.getMessage());
        }
    }

    public Flux<String> routeAndStream(ChatRequest request) {
        ResolvedModel resolved = resolve(request);
        log.info("Routing streaming LLM request: provider={}, model={}",
                resolved.provider(), resolved.request().getModel());
        return resolved.client().streamComplete(resolved.request());
    }

    public boolean isModelSupported(String model) {
        try {
            resolve(ChatRequest.builder().model(model).messages(List.of()).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getSupportedModels() {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        for (ChatClient client : chatClients.values()) {
            if (client.isAvailable()) {
                models.addAll(client.getSupportedModels());
            }
        }
        modelService.listEnabled().stream()
                .map(AIModelEntity::getModelKey)
                .filter(value -> value != null && !value.isBlank())
                .forEach(models::add);
        return new ArrayList<>(models);
    }

    public ChatClient getClient(String provider) {
        return clientsByProvider.get(normalizeProvider(provider));
    }

    public List<String> getAvailableProviders() {
        return new ArrayList<>(clientsByProvider.keySet());
    }

    private ResolvedModel resolve(ChatRequest request) {
        String modelRef = request.getModel();
        AIModelEntity configured = modelService.resolveForCall(modelRef);
        if (configured != null) {
            ChatClient client = modelClientFactory.create(configured);
            return new ResolvedModel(
                    client,
                    copyWithModel(request, configured.getModelKey()),
                    normalizeProvider(configured.getProvider()));
        }

        String providerHint = null;
        String modelName = modelRef == null ? "" : modelRef.trim();
        int separator = modelName.indexOf(':');
        if (separator > 0) {
            providerHint = modelName.substring(0, separator);
            modelName = modelName.substring(separator + 1);
        }

        ChatClient client = providerHint == null ? resolveEnvClient(modelName) : getClient(providerHint);
        if (client == null || !client.isAvailable()) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "模型未配置或不可用: " + (modelRef == null ? "default" : modelRef));
        }
        String effectiveModel = modelName;
        if (effectiveModel.isBlank() || "default".equalsIgnoreCase(effectiveModel)) {
            effectiveModel = client.getSupportedModels().stream().findFirst()
                    .orElseThrow(() -> new BizException(ResultCode.PARAM_ERROR, "模型未配置 modelKey"));
        }
        return new ResolvedModel(client, copyWithModel(request, effectiveModel),
                normalizeProvider(client.getProviderName()));
    }

    private ChatClient resolveEnvClient(String model) {
        AIProvider provider = AIProvider.getByModel(model);
        if (provider != AIProvider.UNKNOWN) {
            ChatClient matched = getClient(provider.getCode());
            if (matched != null) return matched;
        }
        String lower = model.toLowerCase(Locale.ROOT);
        return clientsByProvider.values().stream()
                .filter(client -> client.getSupportedModels().stream()
                        .anyMatch(supported -> supported.equalsIgnoreCase(lower)
                                || supported.toLowerCase(Locale.ROOT).startsWith(lower)
                                || lower.startsWith(supported.toLowerCase(Locale.ROOT))))
                .findFirst()
                .orElse(null);
    }

    private ChatRequest copyWithModel(ChatRequest source, String model) {
        return ChatRequest.builder()
                .model(model)
                .messages(source.getMessages())
                .temperature(source.getTemperature())
                .maxTokens(source.getMaxTokens())
                .extraParams(source.getExtraParams())
                .build();
    }

    private static String normalizeProvider(String provider) {
        return provider == null ? "unknown" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private record ResolvedModel(ChatClient client, ChatRequest request, String provider) {
    }
}

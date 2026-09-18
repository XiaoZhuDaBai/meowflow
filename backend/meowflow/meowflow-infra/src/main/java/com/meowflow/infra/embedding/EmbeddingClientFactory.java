package com.meowflow.infra.embedding;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.infra.entity.AIModelEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class EmbeddingClientFactory {

    public EmbeddingClient create(AIModelEntity model) {
        if (model == null || model.getApiKey() == null || model.getApiKey().isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "Embedding 模型未配置 API Key");
        }
        String provider = model.getProvider() == null ? "" : model.getProvider().toLowerCase(Locale.ROOT);
        String baseUrl = model.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            if (provider.contains("ali") || provider.contains("qwen") || provider.contains("dashscope")) {
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
            } else {
                baseUrl = "https://api.openai.com/v1";
            }
        }
        return new OpenAICompatibleEmbeddingClient(baseUrl, model.getApiKey(), model.getModelKey());
    }
}

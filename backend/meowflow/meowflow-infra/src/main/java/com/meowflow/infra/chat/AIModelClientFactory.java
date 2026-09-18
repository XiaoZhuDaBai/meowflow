package com.meowflow.infra.chat;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.infra.entity.AIModelEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 使用前端保存的模型配置动态创建 ChatClient。
 */
@Component
public class AIModelClientFactory {

    public ChatClient create(AIModelEntity model) {
        if (model == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "模型配置不存在");
        }
        String provider = model.getProvider() == null ? "" : model.getProvider().trim().toLowerCase(Locale.ROOT);
        String apiKey = model.getApiKey() == null ? "" : model.getApiKey().trim();
        if (apiKey.isBlank() && !"ollama".equals(provider)) {
            throw new BizException(ResultCode.PARAM_ERROR, "模型未配置 API Key");
        }
        String baseUrl = resolveBaseUrl(provider, model.getBaseUrl());
        if (provider.contains("anthropic") || provider.contains("claude")) {
            return new ClaudeChatClient(apiKey, baseUrl);
        }
        if (provider.contains("baidu") || provider.contains("wenxin")) {
            throw new BizException(ResultCode.PARAM_ERROR, "当前运行时暂不支持百度文心动态模型，请改用 OpenAI 兼容提供商");
        }
        return new OpenAIChatClient(apiKey, baseUrl);
    }

    private String resolveBaseUrl(String provider, String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        if (provider.contains("deepseek")) return "https://api.deepseek.com/v1";
        if (provider.contains("ali") || provider.contains("qwen") || provider.contains("dashscope")) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        if (provider.contains("zhipu") || provider.contains("glm")) {
            return "https://open.bigmodel.cn/api/paas/v4";
        }
        if (provider.contains("moonshot")) return "https://api.moonshot.cn/v1";
        if (provider.contains("ollama")) return "http://localhost:11434/v1";
        return "https://api.openai.com/v1";
    }
}

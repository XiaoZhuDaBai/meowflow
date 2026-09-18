package com.meowflow.infra.chat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public enum AIProvider {
    OPENAI("openai", Arrays.asList("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo")),
    ANTHROPIC("anthropic", Arrays.asList("claude-3-5-sonnet", "claude-3-opus", "claude-3-sonnet")),
    BAIDU("baidu", Arrays.asList("ernie-4.0", "ernie-3.5", "ernie-bot")),
    ALI("ali", Arrays.asList("qwen-turbo", "qwen-max", "qwen-plus")),
    DEEPSEEK("deepseek", Arrays.asList("deepseek-chat", "deepseek-coder")),
    UNKNOWN("unknown", Collections.emptyList());

    private final String code;
    private final List<String> models;

    AIProvider(String code, List<String> models) {
        this.code = code;
        this.models = models;
    }

    public String getCode() {
        return code;
    }

    public List<String> getModels() {
        return models;
    }

    public static AIProvider getByCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (AIProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        return UNKNOWN;
    }

    public static AIProvider getByModel(String model) {
        if (model == null) {
            return UNKNOWN;
        }
        String modelLower = model.toLowerCase(Locale.ROOT);
        for (AIProvider provider : values()) {
            if (provider.models.contains(modelLower)) {
                return provider;
            }
        }
        return UNKNOWN;
    }

    public boolean supports(String model) {
        if (model == null) {
            return false;
        }
        return models.contains(model.toLowerCase(Locale.ROOT));
    }
}

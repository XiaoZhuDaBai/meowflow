package com.meowflow.infra.aop;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.infra.client.ChatClient;
import com.meowflow.infra.service.AIInvokeLog;
import com.meowflow.infra.service.AIInvokeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * AI 调用日志切面
 * <p>
 * 自动记录 ChatClientFactory 中所有 AI 调用
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AICallLoggingAspect {

    private final AIInvokeLogService aiInvokeLogService;

    @Pointcut("execution(* com.meowflow.infra.service.ChatClientFactory.chat(..))")
    public void chatMethods() {
    }

    @Around("chatMethods()")
    public Object aroundChat(ProceedingJoinPoint joinPoint) throws Throwable {
        String modelName = extractModelName(joinPoint);
        long startTime = System.currentTimeMillis();

        AIInvokeLog invokeLog = new AIInvokeLog();
        invokeLog.setUserId(Long.valueOf(UserContextHolder.getUserId()));
        invokeLog.setModel(modelName);
        invokeLog.setProvider(extractProvider(modelName));

        Object[] args = joinPoint.getArgs();
        if (args.length >= 2 && args[1] instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<ChatClient.Message> messages = (java.util.List<ChatClient.Message>) args[1];
            invokeLog.setPrompt(buildPromptFromMessages(messages));
        }

        try {
            Object result = joinPoint.proceed();

            if (result instanceof ChatClient.ChatResponse response) {
                invokeLog.setCompletion(response.content());
                invokeLog.setPromptTokens(response.promptTokens());
                invokeLog.setCompletionTokens(response.completionTokens());
                invokeLog.setTotalTokens(response.totalTokens());
                invokeLog.setCostMs(response.latencyMs());
                invokeLog.setStatus("success");
            }

            return result;
        } catch (Exception e) {
            invokeLog.setStatus("failed");
            invokeLog.setErrorMessage(e.getMessage());
            invokeLog.setCostMs(System.currentTimeMillis() - startTime);
            throw e;
        } finally {
            invokeLog.setCostMs(System.currentTimeMillis() - startTime);
            aiInvokeLogService.logInvoke(invokeLog);
        }
    }

    @Pointcut("execution(* com.meowflow.infra.service.ChatClientFactory.streamChat(..))")
    public void streamChatMethods() {
    }

    @Around("streamChatMethods()")
    public Object aroundStreamChat(ProceedingJoinPoint joinPoint) throws Throwable {
        String modelName = extractModelName(joinPoint);

        AIInvokeLog invokeLog = new AIInvokeLog();
        invokeLog.setUserId(Long.valueOf(UserContextHolder.getUserId()));
        invokeLog.setModel(modelName);
        invokeLog.setProvider(extractProvider(modelName));
        invokeLog.setStatus("success");
        invokeLog.setCostMs(0L);

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            invokeLog.setStatus("failed");
            invokeLog.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            aiInvokeLogService.logInvoke(invokeLog);
        }
    }

    private String extractModelName(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return "unknown";
    }

    private String extractProvider(String modelName) {
        if (modelName == null) return "unknown";
        String lower = modelName.toLowerCase();
        if (lower.contains("gpt") || lower.contains("openai")) return "openai";
        if (lower.contains("claude") || lower.contains("anthropic")) return "anthropic";
        if (lower.contains("qwen") || lower.contains("ali") || lower.contains("tongyi")) return "ali";
        if (lower.contains("ernie") || lower.contains("baidu") || lower.contains("wenxin")) return "baidu";
        if (lower.contains("deepseek")) return "deepseek";
        if (lower.contains("glm") || lower.contains("zhipu")) return "zhipu";
        return "unknown";
    }

    private String buildPromptFromMessages(java.util.List<ChatClient.Message> messages) {
        if (messages == null || messages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ChatClient.Message msg : messages) {
            sb.append(msg.role()).append(": ").append(msg.content()).append("\n");
        }
        return sb.toString().trim();
    }
}


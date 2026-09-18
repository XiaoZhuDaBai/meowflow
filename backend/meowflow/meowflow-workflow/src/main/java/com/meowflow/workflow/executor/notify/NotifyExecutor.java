package com.meowflow.workflow.executor.notify;

import com.meowflow.infra.integration.IntegrationConfig;
import com.meowflow.infra.integration.IntegrationSender;
import com.meowflow.infra.service.IntegrationService;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyExecutor extends AbstractNodeExecutor {

    private final IntegrationService integrationService;

    @Override
    public NodeType getNodeType() {
        return NodeType.NOTIFY;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String channel = input.get("platform") != null
                ? input.get("platform").toString()
                : input.get("channel") != null ? input.get("channel").toString() : null;
        if (channel == null) {
            channel = "dingtalk";
        }
        String recipient = input.get("recipient") != null ? input.get("recipient").toString() : null;
        String title = input.get("title") != null ? input.get("title").toString() : null;
        String content = input.get("text") != null ? input.get("text").toString()
                : input.get("content") != null ? input.get("content").toString() : "";

        content = context.resolveExpression(content != null ? content : "").toString();
        title = context.resolveExpression(title != null ? title : "").toString();

        ensureWebhookOverride(channel, input);
        IntegrationSender.SendResult result = integrationService.send(channel, content);

        Map<String, Object> output = new HashMap<>();
        output.put("channel", channel);
        output.put("recipient", recipient);
        output.put("title", title);
        output.put("sent", result.success());
        output.put("messageId", result.messageId());
        output.put("errorMessage", result.errorMessage());
        output.put("sentAt", java.time.LocalDateTime.now().toString());

        log.info("Notification: channel={}, recipient={}, title={}, sent={}",
                channel, recipient, title, result.success());
        if (!result.success()) {
            throw new com.meowflow.common.exception.NodeException(
                    node.getId(), node.getType().getCode(), result.errorMessage());
        }
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private void ensureWebhookOverride(String channel, Map<String, Object> input) {
        Object webhook = input.get("webhook");
        if (webhook == null || webhook.toString().isBlank()) return;

        IntegrationConfig config = switch (channel) {
            case "dingtalk" -> IntegrationConfig.dingtalk(
                    webhook.toString(),
                    input.get("secret") != null ? input.get("secret").toString() : null);
            case "feishu" -> IntegrationConfig.feishu(webhook.toString());
            case "wxwork" -> IntegrationConfig.wxwork(webhook.toString());
            default -> null;
        };
        if (config != null) {
            integrationService.saveConfig(config);
        }
    }
}

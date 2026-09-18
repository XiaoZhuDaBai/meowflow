package com.meowflow.monitor.notifier;

import cn.hutool.http.HttpUtil;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.monitor.entity.AlertRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DingtalkNotifier implements AlertNotifier {

    @Value("${monitor.dingtalk.webhook-url:}")
    private String webhookUrl;

    @Value("${monitor.dingtalk.secret:}")
    private String secret;

    @Override
    public String getChannel() {
        return "dingtalk";
    }

    @Override
    public void send(AlertRecord alert) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("DingTalk webhook URL is not configured");
            return;
        }

        try {
            Map<String, Object> msg = buildMessage(alert);
            String response = HttpUtil.post(webhookUrl, JsonUtils.toJson(msg), 5000);

            if (response != null && response.contains("errcode\":0")) {
                log.info("DingTalk notification sent successfully for alert: {}", alert.getId());
            } else {
                log.error("DingTalk notification failed: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to send DingTalk notification", e);
        }
    }

    private Map<String, Object> buildMessage(AlertRecord alert) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("msgtype", "markdown");

        Map<String, Object> markdown = new HashMap<>();
        markdown.put("title", buildTitle(alert));
        markdown.put("text", buildContent(alert));

        msg.put("markdown", markdown);
        return msg;
    }

    private String buildTitle(AlertRecord alert) {
        return String.format("MeowFlow 告警 - %s", alert.getTitle());
    }

    private String buildContent(AlertRecord alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("### MeowFlow 告警通知\n\n");
        sb.append("> **告警ID**: ").append(alert.getId()).append("\n\n");
        sb.append("> **标题**: ").append(alert.getTitle()).append("\n\n");
        sb.append("> **状态**: ").append(alert.getStatus()).append("\n\n");
        sb.append("> **严重程度**: ").append(alert.getSeverity()).append("\n\n");
        sb.append("> **告警类型**: ").append(alert.getAlertType()).append("\n\n");
        sb.append("> **触发值**: ").append(alert.getTriggerValue()).append("\n\n");
        sb.append("> **阈值**: ").append(alert.getThresholdValue()).append("\n\n");
        sb.append("> **触发时间**: ").append(alert.getFiredAt()).append("\n\n");
        if (alert.getMessage() != null) {
            sb.append("> **详情**: ").append(alert.getMessage()).append("\n\n");
        }
        return sb.toString();
    }

    @Override
    public boolean supports(String channel) {
        return "dingtalk".equalsIgnoreCase(channel);
    }
}

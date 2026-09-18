package com.meowflow.monitor.notifier;

import com.meowflow.monitor.entity.AlertRecord;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotifier implements AlertNotifier {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:noreply@meowflow.com}")
    private String fromEmail;

    @Value("${monitor.email.recipients:admin@meowflow.com}")
    private String recipients;

    @Value("${monitor.email.enabled:false}")
    private boolean enabled;

    public EmailNotifier(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @PostConstruct
    void warnIfMisconfigured() {
        if (mailSenderProvider.getIfAvailable() == null) {
            log.warn("[EmailNotifier] 未找到 JavaMailSender Bean，邮件告警功能不可用。"
                    + "请在 application.yml 中配置 spring.mail.* (host/port/username/password)。");
        }
    }

    @Override
    public String getChannel() {
        return "email";
    }

    @Override
    public void send(AlertRecord alert) {
        if (!enabled) {
            log.debug("Email notification is disabled, skip alert: {}", alert.getId());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender not available, skip email alert: {}", alert.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipients.split(","));
            message.setSubject(buildSubject(alert));
            message.setText(buildContent(alert));

            mailSender.send(message);
            log.info("Email notification sent successfully for alert: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send email notification", e);
        }
    }

    private String buildSubject(AlertRecord alert) {
        return String.format("[MeowFlow 告警] %s - %s",
                alert.getTitle(),
                alert.getSeverity());
    }

    private String buildContent(AlertRecord alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("MeowFlow 监控告警通知\n\n");
        sb.append("=".repeat(50)).append("\n\n");
        sb.append("告警ID: ").append(alert.getId()).append("\n\n");
        sb.append("告警标题: ").append(alert.getTitle()).append("\n\n");
        sb.append("告警类型: ").append(alert.getAlertType()).append("\n\n");
        sb.append("告警状态: ").append(alert.getStatus()).append("\n\n");
        sb.append("严重程度: ").append(alert.getSeverity()).append("\n\n");
        sb.append("触发值: ").append(alert.getTriggerValue()).append("\n\n");
        sb.append("阈值: ").append(alert.getThresholdValue()).append("\n\n");
        sb.append("触发时间: ").append(alert.getFiredAt()).append("\n\n");
        if (alert.getMessage() != null) {
            sb.append("\n详细信息:\n").append(alert.getMessage()).append("\n\n");
        }
        sb.append("=".repeat(50)).append("\n\n");
        sb.append("此邮件由 MeowFlow 监控系统自动发送\n");
        return sb.toString();
    }

    @Override
    public boolean supports(String channel) {
        return "email".equalsIgnoreCase(channel);
    }
}

package com.meowflow.monitor.notifier;

import com.meowflow.monitor.entity.AlertRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsNotifier implements AlertNotifier {

    @Value("${monitor.sms.enabled:false}")
    private boolean enabled;

    @Value("${monitor.sms.endpoint:}")
    private String endpoint;

    @Value("${monitor.sms.api-key:}")
    private String apiKey;

    @Value("${monitor.sms.recipients:}")
    private String recipients;

    @Override
    public String getChannel() {
        return "sms";
    }

    @Override
    public void send(AlertRecord alert) {
        if (!enabled) {
            log.debug("SMS notification is disabled");
            return;
        }

        if (endpoint == null || endpoint.isEmpty() || recipients == null || recipients.isEmpty()) {
            log.warn("SMS configuration is incomplete");
            return;
        }

        try {
            String message = buildSmsContent(alert);
            String[] phoneNumbers = recipients.split(",");

            for (String phone : phoneNumbers) {
                sendSms(phone.trim(), message);
            }

            log.info("SMS notification sent successfully for alert: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send SMS notification", e);
        }
    }

    private String buildSmsContent(AlertRecord alert) {
        return String.format("【MeowFlow】告警: %s, 严重度: %s, 类型: %s, 请及时处理。",
                alert.getTitle(),
                alert.getSeverity(),
                alert.getAlertType());
    }

    private void sendSms(String phoneNumber, String message) {
        log.debug("Sending SMS to {}: {}", phoneNumber, message);
    }

    @Override
    public boolean supports(String channel) {
        return "sms".equalsIgnoreCase(channel);
    }
}

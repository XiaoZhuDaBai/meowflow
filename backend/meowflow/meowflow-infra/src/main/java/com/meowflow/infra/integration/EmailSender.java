package com.meowflow.infra.integration;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * 邮件发送器
 */
@Slf4j
public class EmailSender implements IntegrationSender {

    private final IntegrationConfig config;

    public EmailSender(IntegrationConfig config) {
        this.config = config;
    }

    @Override
    public SendResult send(String message) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> cfg = config.getConfig();
            String host = (String) cfg.get("host");
            int port = (Integer) cfg.get("port");
            String username = (String) cfg.get("username");
            String password = (String) cfg.get("password");

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(username));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse((String) cfg.get("to")));
            msg.setSubject("MeowFlow Notification");
            msg.setText(message);

            Transport.send(msg);
            return SendResult.success("email-" + System.currentTimeMillis(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Email send error", e);
            return SendResult.failure(e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    @Override
    public void sendAsync(String message, SendCallback callback) {
        try {
            new Thread(() -> {
                SendResult result = send(message);
                if (result.success()) {
                    callback.onSuccess(result.messageId());
                } else {
                    callback.onFailure(result.errorMessage());
                }
            }).start();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    @Override
    public String getType() {
        return IntegrationConfig.TYPE_EMAIL;
    }
}

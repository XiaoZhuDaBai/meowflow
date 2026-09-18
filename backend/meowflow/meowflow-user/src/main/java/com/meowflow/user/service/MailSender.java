package com.meowflow.user.service;

/**
 * 邮件发送接口。
 * 通过配置切换实现（目前提供 LogMailSender，后续可扩展 SmtpMailSender）。
 */
public interface MailSender {

    /**
     * 发送邮件。
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param htmlBody HTML 格式的邮件正文
     */
    void send(String to, String subject, String htmlBody);
}

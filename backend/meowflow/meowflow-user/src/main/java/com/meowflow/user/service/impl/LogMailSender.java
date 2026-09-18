package com.meowflow.user.service.impl;

import com.meowflow.user.service.MailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 开发/测试环境邮件发送实现：仅将邮件内容打印到日志，不真正发送。
 * 通过配置 meowflow.mail.mode=log 启用（默认）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "meowflow.mail.mode", havingValue = "log", matchIfMissing = true)
public class LogMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("\n========== 邮件发送（开发模式，仅日志）==========\n" +
                 "收件人: {}\n主题: {}\n内容:\n{}\n" +
                 "===========================================", to, subject, htmlBody);
    }
}

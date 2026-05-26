package com.matador.notification.internal;

import com.matador.notification.internal.ChannelSenders.EmailSender;
import com.matador.notification.internal.ChannelSenders.LoggingEmailSender;
import com.matador.notification.internal.ChannelSenders.LoggingSmsSender;
import com.matador.notification.internal.ChannelSenders.PostmarkEmailSender;
import com.matador.notification.internal.ChannelSenders.SmsSender;
import com.matador.notification.internal.ChannelSenders.TwilioSmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NotificationSenderConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderConfig.class);

    @Bean
    EmailSender emailSender(
        @Value("${matador.postmark.server-token:}") String token,
        @Value("${matador.postmark.from-address:no-reply@matador.com}") String from) {
        if (token == null || token.isBlank()) {
            log.warn("No Postmark token configured; emails will be logged, not sent.");
            return new LoggingEmailSender();
        }
        return new PostmarkEmailSender(token, from);
    }

    @Bean
    SmsSender smsSender(
        @Value("${matador.twilio.account-sid:}") String sid,
        @Value("${matador.twilio.auth-token:}") String token,
        @Value("${matador.twilio.from-number:}") String from) {
        if (sid == null || sid.isBlank() || token == null || token.isBlank()) {
            log.warn("No Twilio credentials configured; SMS will be logged, not sent.");
            return new LoggingSmsSender();
        }
        return new TwilioSmsSender(sid, token, from);
    }
}

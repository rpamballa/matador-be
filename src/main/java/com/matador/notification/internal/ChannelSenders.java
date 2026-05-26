package com.matador.notification.internal;

import com.matador.shared.error.ExternalServiceException;
import com.twilio.Twilio;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Email and SMS sender abstractions plus their logging and provider-backed implementations. */
public final class ChannelSenders {

    private ChannelSenders() {}

    public interface EmailSender {
        void send(String to, String subject, String body);
    }

    public interface SmsSender {
        void send(String to, String body);
    }

    /** Logs instead of sending; used in dev/test or when no provider is configured. */
    static class LoggingEmailSender implements EmailSender {
        private static final Logger log = LoggerFactory.getLogger("com.matador.notification.email");

        @Override
        public void send(String to, String subject, String body) {
            log.info("[mock-email] to={} subject={}", to, subject);
        }
    }

    static class LoggingSmsSender implements SmsSender {
        private static final Logger log = LoggerFactory.getLogger("com.matador.notification.sms");

        @Override
        public void send(String to, String body) {
            log.info("[mock-sms] to={} body={}", to, body);
        }
    }

    static class PostmarkEmailSender implements EmailSender {
        private final com.postmarkapp.postmark.client.ApiClient client;
        private final String from;

        PostmarkEmailSender(String serverToken, String from) {
            this.client = com.postmarkapp.postmark.Postmark.getApiClient(serverToken);
            this.from = from;
        }

        @Override
        public void send(String to, String subject, String body) {
            try {
                var message =
                    new com.postmarkapp.postmark.client.data.model.message.Message(from, to, subject, body);
                client.deliverMessage(message);
            } catch (Exception e) {
                throw new ExternalServiceException("Postmark send failed", e);
            }
        }
    }

    static class TwilioSmsSender implements SmsSender {
        private final String fromNumber;

        TwilioSmsSender(String accountSid, String authToken, String fromNumber) {
            Twilio.init(accountSid, authToken);
            this.fromNumber = fromNumber;
        }

        @Override
        public void send(String to, String body) {
            try {
                com.twilio.rest.api.v2010.account.Message.creator(
                        new PhoneNumber(to), new PhoneNumber(fromNumber), body)
                    .create();
            } catch (Exception e) {
                throw new ExternalServiceException("Twilio send failed", e);
            }
        }
    }
}

package com.matador.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matador.notification.internal.ChannelSenders.EmailSender;
import com.matador.notification.internal.ChannelSenders.SmsSender;
import com.matador.notification.internal.Notification;
import com.matador.notification.internal.Notification.Channel;
import com.matador.notification.internal.NotificationRepository;
import com.matador.shared.id.IdGenerator;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the notification module: queue notifications and flush them in the background. */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public NotificationService(
        NotificationRepository notifications,
        EmailSender emailSender,
        SmsSender smsSender,
        ObjectMapper objectMapper,
        IdGenerator idGenerator,
        Clock clock) {
        this.notifications = notifications;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public void queueEmail(UUID customerId, String template, String recipient, Map<String, Object> payload) {
        queue(customerId, Channel.EMAIL, template, recipient, payload);
    }

    @Transactional
    public void queueSms(UUID customerId, String template, String recipient, Map<String, Object> payload) {
        queue(customerId, Channel.SMS, template, recipient, payload);
    }

    private void queue(
        UUID customerId, Channel channel, String template, String recipient, Map<String, Object> payload) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        notifications.save(
            new Notification(
                idGenerator.newId(), customerId, channel, template, writeJson(payload), recipient, clock.instant()));
    }

    /** Flushes pending notifications. Single-instance via ShedLock. */
    @Scheduled(fixedDelayString = "PT30S")
    @SchedulerLock(name = "notification-flush", lockAtMostFor = "PT25S")
    @Transactional
    public void flushPending() {
        for (Notification n :
            notifications.findByStatusOrderByCreatedAtAsc(
                Notification.Status.PENDING.name(), Limit.of(100))) {
            try {
                if (n.getChannel() == Channel.EMAIL) {
                    emailSender.send(n.getRecipient(), subjectFor(n.getTemplate()), n.getPayload());
                } else {
                    smsSender.send(n.getRecipient(), subjectFor(n.getTemplate()));
                }
                n.markSent(clock.instant());
            } catch (RuntimeException e) {
                log.warn("Notification {} failed: {}", n.getId(), e.getMessage());
                n.markFailed(e.getMessage());
            }
        }
    }

    private String subjectFor(String template) {
        return switch (template) {
            case "customer-welcome" -> "Welcome to Matador";
            case "verification-completed" -> "Your identity is verified";
            case "booking-confirmed" -> "Your booking is confirmed";
            case "booking-cancelled" -> "Your booking was cancelled";
            case "payment-failed" -> "Payment problem with your booking";
            case "trip-started" -> "Your trip has started";
            case "trip-ended" -> "Your trip has ended";
            default -> "Matador notification";
        };
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception e) {
            return "{}";
        }
    }
}

package com.matador.notification.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification")
public class Notification {

    public enum Channel {
        EMAIL,
        SMS
    }

    public enum Status {
        PENDING,
        SENT,
        FAILED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "template", nullable = false)
    private String template;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error")
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {}

    public Notification(
        UUID id,
        UUID customerId,
        Channel channel,
        String template,
        String payload,
        String recipient,
        Instant now) {
        this.id = id;
        this.customerId = customerId;
        this.channel = channel.name();
        this.template = template;
        this.payload = payload;
        this.recipient = recipient;
        this.status = Status.PENDING.name();
        this.createdAt = now;
    }

    public void markSent(Instant at) {
        this.status = Status.SENT.name();
        this.sentAt = at;
        this.error = null;
    }

    public void markFailed(String error) {
        this.status = Status.FAILED.name();
        this.error = error;
    }

    public UUID getId() {
        return id;
    }

    public Channel getChannel() {
        return Channel.valueOf(channel);
    }

    public String getTemplate() {
        return template;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getPayload() {
        return payload;
    }
}

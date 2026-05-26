package com.matador.shared.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Record of an inbound provider webhook, used for audit and idempotency. */
@Entity
@Table(name = "webhook_event")
public class WebhookEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;

    @Column(name = "event_type")
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected WebhookEvent() {}

    WebhookEvent(
        UUID id,
        String provider,
        String providerEventId,
        String eventType,
        String payload,
        Instant receivedAt) {
        this.id = id;
        this.provider = provider;
        this.providerEventId = providerEventId;
        this.eventType = eventType;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.processedAt = receivedAt;
    }

    public UUID getId() {
        return id;
    }
}

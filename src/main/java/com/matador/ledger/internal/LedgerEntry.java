package com.matador.ledger.internal;

import com.matador.ledger.LedgerEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Immutable ledger entry. Never updated or deleted. */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "booking_id", updatable = false)
    private UUID bookingId;

    @Column(name = "trip_id", updatable = false)
    private UUID tripId;

    @Column(name = "incident_id", updatable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, updatable = false)
    private LedgerEntryType entryType;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3, nullable = false, updatable = false)
    private String currency;

    @Column(name = "description", nullable = false, updatable = false)
    private String description;

    @Column(name = "payment_intent_id", updatable = false)
    private UUID paymentIntentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", updatable = false)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {}

    public LedgerEntry(
        UUID id,
        Instant occurredAt,
        UUID customerId,
        UUID bookingId,
        UUID tripId,
        UUID incidentId,
        LedgerEntryType entryType,
        long amountCents,
        String description,
        UUID paymentIntentId,
        String metadata,
        Instant createdAt) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.customerId = customerId;
        this.bookingId = bookingId;
        this.tripId = tripId;
        this.incidentId = incidentId;
        this.entryType = entryType;
        this.amountCents = amountCents;
        this.currency = "USD";
        this.description = description;
        this.paymentIntentId = paymentIntentId;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getTripId() {
        return tripId;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }
}

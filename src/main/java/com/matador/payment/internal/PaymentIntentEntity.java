package com.matador.payment.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intent")
public class PaymentIntentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "stripe_intent_id", nullable = false, unique = true)
    private String stripeIntentId;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "captured_amount_cents", nullable = false)
    private long capturedAmountCents;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentIntentEntity() {}

    public PaymentIntentEntity(
        UUID id,
        UUID customerId,
        UUID bookingId,
        UUID tripId,
        String stripeIntentId,
        String purpose,
        long amountCents,
        String status,
        Instant now) {
        this.id = id;
        this.customerId = customerId;
        this.bookingId = bookingId;
        this.tripId = tripId;
        this.stripeIntentId = stripeIntentId;
        this.purpose = purpose;
        this.amountCents = amountCents;
        this.status = status;
        this.capturedAmountCents = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateStatus(String status, Instant at) {
        this.status = status;
        this.updatedAt = at;
    }

    public void recordCapture(long capturedAmountCents, String status, Instant at) {
        this.capturedAmountCents = capturedAmountCents;
        this.status = status;
        this.updatedAt = at;
    }

    public void recordError(String error, Instant at) {
        this.lastError = error;
        this.updatedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getTripId() {
        return tripId;
    }

    public String getStripeIntentId() {
        return stripeIntentId;
    }

    public String getPurpose() {
        return purpose;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public long getCapturedAmountCents() {
        return capturedAmountCents;
    }

    public String getStatus() {
        return status;
    }
}

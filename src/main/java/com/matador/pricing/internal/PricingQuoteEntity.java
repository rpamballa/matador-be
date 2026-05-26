package com.matador.pricing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pricing_quote")
public class PricingQuoteEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "vehicle_class_id", nullable = false)
    private UUID vehicleClassId;

    @Column(name = "pickup_at", nullable = false)
    private Instant pickupAt;

    @Column(name = "dropoff_at", nullable = false)
    private Instant dropoffAt;

    @Column(name = "dropoff_in_zone", nullable = false)
    private boolean dropoffInZone;

    @Column(name = "insurance_tier", nullable = false)
    private String insuranceTier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "line_items", nullable = false)
    private String lineItems;

    @Column(name = "subtotal_cents", nullable = false)
    private long subtotalCents;

    @Column(name = "tax_cents", nullable = false)
    private long taxCents;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(name = "deposit_cents", nullable = false)
    private long depositCents;

    @Column(name = "promo_code")
    private String promoCode;

    @Column(name = "discount_cents", nullable = false)
    private long discountCents;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PricingQuoteEntity() {}

    public PricingQuoteEntity(
        UUID id,
        UUID customerId,
        UUID vehicleClassId,
        Instant pickupAt,
        Instant dropoffAt,
        boolean dropoffInZone,
        String insuranceTier,
        String lineItems,
        long subtotalCents,
        long taxCents,
        long totalCents,
        long depositCents,
        String promoCode,
        long discountCents,
        Instant expiresAt,
        Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.vehicleClassId = vehicleClassId;
        this.pickupAt = pickupAt;
        this.dropoffAt = dropoffAt;
        this.dropoffInZone = dropoffInZone;
        this.insuranceTier = insuranceTier;
        this.lineItems = lineItems;
        this.subtotalCents = subtotalCents;
        this.taxCents = taxCents;
        this.totalCents = totalCents;
        this.depositCents = depositCents;
        this.promoCode = promoCode;
        this.discountCents = discountCents;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getVehicleClassId() {
        return vehicleClassId;
    }

    public Instant getPickupAt() {
        return pickupAt;
    }

    public Instant getDropoffAt() {
        return dropoffAt;
    }

    public boolean isDropoffInZone() {
        return dropoffInZone;
    }

    public String getInsuranceTier() {
        return insuranceTier;
    }

    public String getLineItems() {
        return lineItems;
    }

    public long getSubtotalCents() {
        return subtotalCents;
    }

    public long getTaxCents() {
        return taxCents;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public long getDepositCents() {
        return depositCents;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}

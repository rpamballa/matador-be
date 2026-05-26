package com.matador.pricing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promo_code")
public class PromoCode {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "discount_type", nullable = false)
    private String discountType; // PERCENT or FIXED

    @Column(name = "discount_value", nullable = false)
    private int discountValue;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PromoCode() {}

    public PromoCode(
        UUID id,
        String code,
        String discountType,
        int discountValue,
        Integer maxUses,
        Instant startsAt,
        Instant expiresAt,
        Instant createdAt) {
        this.id = id;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxUses = maxUses;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.active = true;
        this.createdAt = createdAt;
    }

    public boolean isValid(Instant now) {
        if (!active || now.isBefore(startsAt)) {
            return false;
        }
        if (expiresAt != null && now.isAfter(expiresAt)) {
            return false;
        }
        return maxUses == null || usedCount < maxUses;
    }

    /** Discount in cents applied to the given subtotal. */
    public long discountFor(long subtotalCents) {
        if ("PERCENT".equals(discountType)) {
            return Math.round(subtotalCents * (Math.min(discountValue, 100) / 100.0));
        }
        return Math.min(discountValue, subtotalCents);
    }

    public void incrementUse() {
        this.usedCount++;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }
}

package com.matador.pricing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pricing_rate")
public class PricingRate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vehicle_class_id", nullable = false)
    private UUID vehicleClassId;

    @Column(name = "daily_rate_cents", nullable = false)
    private long dailyRateCents;

    @Column(name = "delivery_fee_cents", nullable = false)
    private long deliveryFeeCents;

    @Column(name = "out_of_zone_dropoff_fee_cents", nullable = false)
    private long outOfZoneDropoffFeeCents;

    @Column(name = "insurance_tier", nullable = false)
    private String insuranceTier;

    @Column(name = "insurance_daily_cents", nullable = false)
    private long insuranceDailyCents;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected PricingRate() {}

    public PricingRate(
        UUID id,
        UUID vehicleClassId,
        long dailyRateCents,
        long deliveryFeeCents,
        long outOfZoneDropoffFeeCents,
        String insuranceTier,
        long insuranceDailyCents,
        Instant effectiveFrom,
        Instant createdAt,
        UUID createdBy) {
        this.id = id;
        this.vehicleClassId = vehicleClassId;
        this.dailyRateCents = dailyRateCents;
        this.deliveryFeeCents = deliveryFeeCents;
        this.outOfZoneDropoffFeeCents = outOfZoneDropoffFeeCents;
        this.insuranceTier = insuranceTier;
        this.insuranceDailyCents = insuranceDailyCents;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVehicleClassId() {
        return vehicleClassId;
    }

    public long getDailyRateCents() {
        return dailyRateCents;
    }

    public long getDeliveryFeeCents() {
        return deliveryFeeCents;
    }

    public long getOutOfZoneDropoffFeeCents() {
        return outOfZoneDropoffFeeCents;
    }

    public long getInsuranceDailyCents() {
        return insuranceDailyCents;
    }
}

package com.matador.pricing;

import java.time.Instant;
import java.util.UUID;

/**
 * Input to a price quote. The caller (booking) supplies {@code dropoffInZone} and the
 * applicable {@code outOfZoneFeeCents} (resolved from the zone), keeping pricing decoupled
 * from the zone module.
 */
public record QuoteRequest(
    UUID customerId,
    UUID vehicleClassId,
    Instant pickupAt,
    Instant dropoffAt,
    boolean dropoffInZone,
    long outOfZoneFeeCents,
    String insuranceTier,
    String promoCode) {

    public String insuranceTierOrDefault() {
        return insuranceTier == null || insuranceTier.isBlank() ? "STANDARD" : insuranceTier;
    }
}

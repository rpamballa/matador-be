package com.matador.pricing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Quote projection returned to bookings and customers. */
public record QuoteInfo(
    UUID quoteId,
    UUID vehicleClassId,
    Instant pickupAt,
    Instant dropoffAt,
    boolean dropoffInZone,
    String insuranceTier,
    List<LineItem> lineItems,
    long subtotalCents,
    long discountCents,
    long taxCents,
    long totalCents,
    long depositCents,
    Instant expiresAt) {}

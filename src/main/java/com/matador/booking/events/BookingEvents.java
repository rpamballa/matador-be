package com.matador.booking.events;

import java.time.Instant;
import java.util.UUID;

/** Domain events published by the booking module. */
public final class BookingEvents {

    private BookingEvents() {}

    public record BookingCreated(
        UUID bookingId, UUID customerId, UUID vehicleClassId, Instant pickupAt, Instant dropoffAt, Instant createdAt) {}

    public record BookingConfirmed(UUID bookingId, UUID customerId, Instant confirmedAt) {}

    public record BookingCancelled(
        UUID bookingId, String reason, Instant cancelledAt, long refundAmountCents) {}

    /** Published on activation; the trip module creates the Trip with the provided tripId. */
    public record BookingActivated(
        UUID bookingId,
        UUID tripId,
        UUID customerId,
        UUID vehicleId,
        Instant pickupAt,
        long odometerStart,
        double pickupLat,
        double pickupLng,
        long quotedTotalCents,
        Instant activatedAt) {}

    public record BookingCompleted(UUID bookingId, UUID tripId, Instant completedAt) {}
}

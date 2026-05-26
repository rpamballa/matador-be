package com.matador.trip.events;

import java.time.Instant;
import java.util.UUID;

/** Domain events published by the trip module. */
public final class TripEvents {

    private TripEvents() {}

    public record TripStarted(UUID tripId, UUID bookingId, UUID vehicleId, Instant startedAt) {}

    public record TripEnded(UUID tripId, Instant endedAt, Integer milesDriven) {}

    public record TripClosed(
        UUID tripId, UUID bookingId, UUID vehicleId, long finalChargesCents, Instant closedAt) {}
}

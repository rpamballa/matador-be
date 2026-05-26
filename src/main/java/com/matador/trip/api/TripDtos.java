package com.matador.trip.api;

import com.matador.trip.TripStatus;
import java.time.Instant;
import java.util.UUID;

public final class TripDtos {

    private TripDtos() {}

    public record TripResponse(
        UUID id,
        UUID bookingId,
        UUID vehicleId,
        TripStatus status,
        Instant actualPickupAt,
        Instant actualDropoffAt,
        int odometerStart,
        Integer odometerEnd,
        Integer milesDriven,
        Long finalChargesCents) {}
}

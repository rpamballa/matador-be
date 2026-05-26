package com.matador.trip;

import java.util.UUID;

/** Trip projection shared with other modules (e.g. incident). */
public record TripInfo(UUID tripId, UUID customerId, UUID vehicleId, TripStatus status) {}

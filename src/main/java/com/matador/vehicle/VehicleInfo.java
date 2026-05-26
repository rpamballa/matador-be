package com.matador.vehicle;

import java.util.UUID;

/** Vehicle projection shared with other modules (booking, trip). */
public record VehicleInfo(
    UUID id,
    String vin,
    UUID classId,
    VehicleStatus status,
    UUID homeZoneId,
    String make,
    String model,
    int year,
    String color,
    int odometerMiles,
    String telematicsProvider) {}

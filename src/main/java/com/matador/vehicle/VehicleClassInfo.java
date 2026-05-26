package com.matador.vehicle;

import java.util.UUID;

/** Vehicle-class projection shared with other modules (pricing, booking). */
public record VehicleClassInfo(
    UUID id,
    String name,
    Drivetrain drivetrain,
    int seats,
    long baseDailyRateCents,
    boolean active) {}

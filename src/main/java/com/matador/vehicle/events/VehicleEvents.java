package com.matador.vehicle.events;

import com.matador.vehicle.VehicleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Domain events published by the vehicle module. */
public final class VehicleEvents {

    private VehicleEvents() {}

    public record VehicleStatusChanged(
        UUID vehicleId,
        VehicleStatus fromStatus,
        VehicleStatus toStatus,
        Instant changedAt,
        UUID changedBy) {}

    public record VehicleAcquired(UUID vehicleId, String vin, LocalDate acquiredOn) {}

    public record VehicleRetired(UUID vehicleId, LocalDate retiredOn) {}
}

package com.matador.telematics;

import com.matador.vehicle.VehicleInfo;
import java.time.Instant;

/** The abstraction the rest of the system uses to talk to a telematics provider. */
public interface TelematicsProvider {

    String providerName();

    boolean supports(VehicleInfo vehicle);

    CommandOutcome lock(VehicleInfo vehicle);

    CommandOutcome unlock(VehicleInfo vehicle);

    VehicleSnapshot snapshot(VehicleInfo vehicle);

    record GeoPoint(double lat, double lng) {}

    record VehicleSnapshot(
        GeoPoint location,
        Integer odometerMiles,
        Integer fuelChargePercent,
        Integer rangeMiles,
        Instant recordedAt) {}

    record CommandOutcome(boolean succeeded, String detail) {}
}

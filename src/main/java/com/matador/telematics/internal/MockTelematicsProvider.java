package com.matador.telematics.internal;

import com.matador.telematics.TelematicsProvider;
import com.matador.vehicle.VehicleInfo;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** Deterministic provider for dev/test and vehicles with no real telematics link. */
@Component
class MockTelematicsProvider implements TelematicsProvider {

    private final Clock clock;

    MockTelematicsProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String providerName() {
        return "MOCK";
    }

    @Override
    public boolean supports(VehicleInfo vehicle) {
        return true;
    }

    @Override
    public CommandOutcome lock(VehicleInfo vehicle) {
        return new CommandOutcome(true, "locked (mock)");
    }

    @Override
    public CommandOutcome unlock(VehicleInfo vehicle) {
        return new CommandOutcome(true, "unlocked (mock)");
    }

    @Override
    public VehicleSnapshot snapshot(VehicleInfo vehicle) {
        // Centered on the Triangle zone with a small deterministic jitter from the odometer.
        double jitter = (vehicle.odometerMiles() % 100) / 1000.0;
        return new VehicleSnapshot(
            new GeoPoint(35.90 + jitter, -78.78 - jitter),
            vehicle.odometerMiles(),
            80,
            240,
            clock.instant());
    }
}

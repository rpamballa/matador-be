package com.matador.telematics.internal;

import com.matador.telematics.TelematicsProvider.VehicleSnapshot;
import com.matador.telematics.TelematicsService;
import com.matador.trip.TripService;
import com.matador.vehicle.VehicleInfo;
import com.matador.vehicle.VehicleService;
import com.matador.vehicle.VehicleStatus;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls telematics snapshots for vehicles currently with customers, persisting samples and
 * updating the vehicle's last-known state. Runs single-instance via ShedLock.
 */
@Component
class TelematicsPollingJob {

    private final TelematicsService telematicsService;
    private final VehicleService vehicleService;
    private final TripService tripService;

    TelematicsPollingJob(
        TelematicsService telematicsService, VehicleService vehicleService, TripService tripService) {
        this.telematicsService = telematicsService;
        this.vehicleService = vehicleService;
        this.tripService = tripService;
    }

    @Scheduled(fixedDelayString = "PT5M")
    @SchedulerLock(name = "telematics-poll-active", lockAtMostFor = "PT4M")
    void pollActiveVehicles() {
        for (VehicleInfo vehicle : vehicleService.findByStatus(VehicleStatus.WITH_CUSTOMER)) {
            telematicsService
                .snapshot(vehicle.id())
                .ifPresent(snapshot -> apply(vehicle, snapshot));
        }
    }

    private void apply(VehicleInfo vehicle, VehicleSnapshot snapshot) {
        Double lat = snapshot.location() == null ? null : snapshot.location().lat();
        Double lng = snapshot.location() == null ? null : snapshot.location().lng();
        vehicleService.applyTelematicsSnapshot(
            vehicle.id(),
            lat,
            lng,
            snapshot.odometerMiles(),
            snapshot.fuelChargePercent(),
            snapshot.rangeMiles());
        if (lat != null && lng != null) {
            tripService.recordSampleForVehicle(
                vehicle.id(), lat, lng, snapshot.odometerMiles(), snapshot.fuelChargePercent());
        }
    }
}

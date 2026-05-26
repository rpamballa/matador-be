package com.matador.trip.internal;

import com.matador.inspection.events.InspectionEvents.InspectionSubmitted;
import com.matador.trip.TripService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Records inspections against trips as they are submitted. */
@Component
class TripLifecycleListener {

    private final TripService tripService;

    TripLifecycleListener(TripService tripService) {
        this.tripService = tripService;
    }

    @ApplicationModuleListener
    void on(InspectionSubmitted event) {
        tripService.attachInspection(
            event.tripId(), event.phase(), event.inspectionId(), event.odometerMiles());
    }
}

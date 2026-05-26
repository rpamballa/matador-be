package com.matador.inspection.events;

import com.matador.inspection.InspectionEnums.Phase;
import java.time.Instant;
import java.util.UUID;

public final class InspectionEvents {

    private InspectionEvents() {}

    public record InspectionSubmitted(
        UUID inspectionId, UUID tripId, Phase phase, Integer odometerMiles, Instant submittedAt) {}
}

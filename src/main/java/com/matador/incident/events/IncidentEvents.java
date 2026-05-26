package com.matador.incident.events;

import com.matador.incident.IncidentEnums.Type;
import java.util.UUID;

/** Domain events published by the incident module. */
public final class IncidentEvents {

    private IncidentEvents() {}

    public record IncidentReported(UUID incidentId, Type type, UUID tripId, UUID vehicleId) {}

    public record IncidentResolved(UUID incidentId, long chargedAmountCents) {}
}

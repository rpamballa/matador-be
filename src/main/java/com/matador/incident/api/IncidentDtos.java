package com.matador.incident.api;

import com.matador.incident.IncidentEnums.Severity;
import com.matador.incident.IncidentEnums.Status;
import com.matador.incident.IncidentEnums.Type;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class IncidentDtos {

    private IncidentDtos() {}

    public record CustomerReportRequest(
        @NotNull Type type, @NotBlank String description, Instant occurredAt) {}

    public record StaffCreateRequest(
        UUID tripId,
        @NotNull UUID vehicleId,
        UUID customerId,
        @NotNull Type type,
        @NotNull Severity severity,
        @NotBlank String description,
        Instant occurredAt) {}

    public record UpdateIncidentRequest(
        Status status, String resolutionNotes, Long chargedAmountCents) {}

    public record IncidentResponse(
        UUID id,
        UUID tripId,
        UUID vehicleId,
        UUID customerId,
        Type type,
        Severity severity,
        Status status,
        long chargedAmountCents) {}
}

package com.matador.incident.internal;

import com.matador.incident.IncidentEnums.Severity;
import com.matador.incident.IncidentEnums.Status;
import com.matador.incident.IncidentEnums.Type;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident")
public class Incident {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(name = "reported_by_role", nullable = false)
    private String reportedByRole;

    @Column(name = "reported_by_id", nullable = false)
    private UUID reportedById;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "charged_amount_cents", nullable = false)
    private long chargedAmountCents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Incident() {}

    public Incident(
        UUID id,
        UUID tripId,
        UUID vehicleId,
        UUID customerId,
        Type type,
        Severity severity,
        String description,
        Instant occurredAt,
        String reportedByRole,
        UUID reportedById,
        Instant now) {
        this.id = id;
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.customerId = customerId;
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.occurredAt = occurredAt;
        this.reportedAt = now;
        this.reportedByRole = reportedByRole;
        this.reportedById = reportedById;
        this.status = Status.OPEN;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(Status status, String resolutionNotes, Long chargedAmountCents, UUID actor, Instant at) {
        if (status != null) {
            this.status = status;
            if (status == Status.RESOLVED || status == Status.DISMISSED) {
                this.resolvedAt = at;
                this.resolvedBy = actor;
            }
        }
        if (resolutionNotes != null) {
            this.resolutionNotes = resolutionNotes;
        }
        if (chargedAmountCents != null) {
            this.chargedAmountCents = chargedAmountCents;
        }
        this.updatedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTripId() {
        return tripId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Type getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Status getStatus() {
        return status;
    }

    public long getChargedAmountCents() {
        return chargedAmountCents;
    }
}

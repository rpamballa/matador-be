package com.matador.telematics.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "telematics_command_log")
public class TelematicsCommandLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "command", nullable = false)
    private String command;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "succeeded")
    private Boolean succeeded;

    @Column(name = "error")
    private String error;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload")
    private String responsePayload;

    protected TelematicsCommandLog() {}

    public TelematicsCommandLog(
        UUID id,
        UUID vehicleId,
        UUID tripId,
        String command,
        UUID requestedBy,
        boolean succeeded,
        String error,
        Instant requestedAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.tripId = tripId;
        this.command = command;
        this.requestedBy = requestedBy;
        this.succeeded = succeeded;
        this.error = error;
        this.requestedAt = requestedAt;
    }
}

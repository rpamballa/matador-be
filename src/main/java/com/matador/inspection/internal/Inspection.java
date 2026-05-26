package com.matador.inspection.internal;

import com.matador.inspection.InspectionEnums.Phase;
import com.matador.inspection.InspectionEnums.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inspection")
public class Inspection {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private Phase phase;

    @Column(name = "odometer_miles")
    private Integer odometerMiles;

    @Column(name = "fuel_charge_percent")
    private Integer fuelChargePercent;

    @Column(name = "notes")
    private String notes;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "submitted_by_role", nullable = false)
    private String submittedByRole;

    @Column(name = "submitted_by_id", nullable = false)
    private UUID submittedById;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status")
    private ReviewStatus reviewStatus;

    @Column(name = "review_notes")
    private String reviewNotes;

    protected Inspection() {}

    public Inspection(
        UUID id,
        UUID tripId,
        Phase phase,
        Integer odometerMiles,
        Integer fuelChargePercent,
        String notes,
        String submittedByRole,
        UUID submittedById,
        Instant submittedAt) {
        this.id = id;
        this.tripId = tripId;
        this.phase = phase;
        this.odometerMiles = odometerMiles;
        this.fuelChargePercent = fuelChargePercent;
        this.notes = notes;
        this.submittedByRole = submittedByRole;
        this.submittedById = submittedById;
        this.submittedAt = submittedAt;
    }

    public void review(ReviewStatus status, UUID reviewer, String notes, Instant at) {
        this.reviewStatus = status;
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTripId() {
        return tripId;
    }

    public Phase getPhase() {
        return phase;
    }

    public Integer getOdometerMiles() {
        return odometerMiles;
    }

    public Integer getFuelChargePercent() {
        return fuelChargePercent;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }
}

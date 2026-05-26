package com.matador.trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.Point;

/** Aggregate root for a trip. */
@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TripStatus status;

    @Column(name = "actual_pickup_at", nullable = false)
    private Instant actualPickupAt;

    @Column(name = "actual_dropoff_at")
    private Instant actualDropoffAt;

    @Column(name = "actual_pickup_location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point actualPickupLocation;

    @Column(name = "actual_dropoff_location", columnDefinition = "geography(Point,4326)")
    private Point actualDropoffLocation;

    @Column(name = "actual_dropoff_address")
    private String actualDropoffAddress;

    @Column(name = "actual_dropoff_in_zone")
    private Boolean actualDropoffInZone;

    @Column(name = "odometer_start", nullable = false)
    private int odometerStart;

    @Column(name = "odometer_end")
    private Integer odometerEnd;

    @Column(name = "miles_driven")
    private Integer milesDriven;

    @Column(name = "quoted_total_cents", nullable = false)
    private long quotedTotalCents;

    @Column(name = "pickup_inspection_id")
    private UUID pickupInspectionId;

    @Column(name = "dropoff_inspection_id")
    private UUID dropoffInspectionId;

    @Column(name = "final_charges_cents")
    private Long finalChargesCents;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Trip() {}

    public Trip(
        UUID id,
        UUID bookingId,
        UUID customerId,
        UUID vehicleId,
        Instant actualPickupAt,
        Point actualPickupLocation,
        int odometerStart,
        long quotedTotalCents,
        Instant now) {
        this.id = id;
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.actualPickupAt = actualPickupAt;
        this.actualPickupLocation = actualPickupLocation;
        this.odometerStart = odometerStart;
        this.quotedTotalCents = quotedTotalCents;
        this.status = TripStatus.IN_PROGRESS;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void recordPickupInspection(UUID inspectionId) {
        this.pickupInspectionId = inspectionId;
        this.updatedAt = Instant.now();
    }

    public void end(Instant at, Point location, Boolean inZone, Integer odometerEnd) {
        status.requireTransitionTo(TripStatus.ENDED_PENDING_INSPECTION);
        status = TripStatus.ENDED_PENDING_INSPECTION;
        this.actualDropoffAt = at;
        this.actualDropoffLocation = location;
        this.actualDropoffInZone = inZone;
        if (odometerEnd != null) {
            this.odometerEnd = odometerEnd;
        }
        this.updatedAt = at;
    }

    public void recordDropoffInspection(UUID inspectionId, Integer odometerEnd) {
        this.dropoffInspectionId = inspectionId;
        if (odometerEnd != null) {
            this.odometerEnd = odometerEnd;
        }
        this.updatedAt = Instant.now();
    }

    public void close(int odometerEnd, long finalChargesCents, Instant at) {
        status.requireTransitionTo(TripStatus.CLOSED);
        status = TripStatus.CLOSED;
        this.odometerEnd = odometerEnd;
        this.milesDriven = Math.max(0, odometerEnd - odometerStart);
        this.finalChargesCents = finalChargesCents;
        this.closedAt = at;
        this.updatedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public TripStatus getStatus() {
        return status;
    }

    public int getOdometerStart() {
        return odometerStart;
    }

    public Integer getOdometerEnd() {
        return odometerEnd;
    }

    public Integer getMilesDriven() {
        return milesDriven;
    }

    public Long getFinalChargesCents() {
        return finalChargesCents;
    }

    public long getQuotedTotalCents() {
        return quotedTotalCents;
    }

    public Instant getActualPickupAt() {
        return actualPickupAt;
    }

    public Instant getActualDropoffAt() {
        return actualDropoffAt;
    }
}

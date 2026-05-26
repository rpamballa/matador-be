package com.matador.booking;

import com.matador.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.Point;

/** Aggregate root for a reservation. */
@Entity
@Table(name = "booking")
public class Booking extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "booking_number", nullable = false, unique = true)
    private String bookingNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "vehicle_class_id", nullable = false)
    private UUID vehicleClassId;

    @Column(name = "assigned_vehicle_id")
    private UUID assignedVehicleId;

    @Column(name = "pickup_at", nullable = false)
    private Instant pickupAt;

    @Column(name = "dropoff_at", nullable = false)
    private Instant dropoffAt;

    @Column(name = "pickup_location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point pickupLocation;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_zone_id", nullable = false)
    private UUID pickupZoneId;

    @Column(name = "dropoff_location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point dropoffLocation;

    @Column(name = "dropoff_address", nullable = false)
    private String dropoffAddress;

    @Column(name = "dropoff_zone_id")
    private UUID dropoffZoneId;

    @Column(name = "dropoff_in_zone", nullable = false)
    private boolean dropoffInZone;

    @Column(name = "quote_id", nullable = false)
    private UUID quoteId;

    @Column(name = "quoted_total_cents", nullable = false)
    private long quotedTotalCents;

    @Column(name = "deposit_hold_intent_id")
    private String depositHoldIntentId;

    @Column(name = "deposit_amount_cents", nullable = false)
    private long depositAmountCents;

    @Column(name = "insurance_tier", nullable = false)
    private String insuranceTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    protected Booking() {}

    public static Booking create(
        UUID id,
        String bookingNumber,
        UUID customerId,
        UUID vehicleClassId,
        Instant pickupAt,
        Instant dropoffAt,
        Point pickupLocation,
        String pickupAddress,
        UUID pickupZoneId,
        Point dropoffLocation,
        String dropoffAddress,
        UUID dropoffZoneId,
        boolean dropoffInZone,
        UUID quoteId,
        long quotedTotalCents,
        long depositAmountCents,
        String insuranceTier) {
        Booking b = new Booking();
        b.id = id;
        b.bookingNumber = bookingNumber;
        b.customerId = customerId;
        b.vehicleClassId = vehicleClassId;
        b.pickupAt = pickupAt;
        b.dropoffAt = dropoffAt;
        b.pickupLocation = pickupLocation;
        b.pickupAddress = pickupAddress;
        b.pickupZoneId = pickupZoneId;
        b.dropoffLocation = dropoffLocation;
        b.dropoffAddress = dropoffAddress;
        b.dropoffZoneId = dropoffZoneId;
        b.dropoffInZone = dropoffInZone;
        b.quoteId = quoteId;
        b.quotedTotalCents = quotedTotalCents;
        b.depositAmountCents = depositAmountCents;
        b.insuranceTier = insuranceTier;
        b.status = BookingStatus.PENDING_PAYMENT;
        return b;
    }

    public void attachDepositHold(String stripeIntentId) {
        this.depositHoldIntentId = stripeIntentId;
    }

    public void confirm() {
        status.requireTransitionTo(BookingStatus.CONFIRMED);
        status = BookingStatus.CONFIRMED;
    }

    public void assignVehicle(UUID vehicleId) {
        this.assignedVehicleId = vehicleId;
    }

    public void activate(Instant at) {
        status.requireTransitionTo(BookingStatus.ACTIVATED);
        status = BookingStatus.ACTIVATED;
        activatedAt = at;
    }

    public void complete() {
        status.requireTransitionTo(BookingStatus.COMPLETED);
        status = BookingStatus.COMPLETED;
    }

    public void cancel(String reason, Instant at) {
        status.requireTransitionTo(BookingStatus.CANCELLED);
        status = BookingStatus.CANCELLED;
        cancellationReason = reason;
        cancelledAt = at;
    }

    public void markNoShow(Instant at) {
        status.requireTransitionTo(BookingStatus.NO_SHOW);
        status = BookingStatus.NO_SHOW;
        cancelledAt = at;
    }

    public UUID getId() {
        return id;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getVehicleClassId() {
        return vehicleClassId;
    }

    public UUID getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public Instant getPickupAt() {
        return pickupAt;
    }

    public Instant getDropoffAt() {
        return dropoffAt;
    }

    public Point getPickupLocation() {
        return pickupLocation;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public UUID getPickupZoneId() {
        return pickupZoneId;
    }

    public Point getDropoffLocation() {
        return dropoffLocation;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public boolean isDropoffInZone() {
        return dropoffInZone;
    }

    public UUID getQuoteId() {
        return quoteId;
    }

    public long getQuotedTotalCents() {
        return quotedTotalCents;
    }

    public String getDepositHoldIntentId() {
        return depositHoldIntentId;
    }

    public long getDepositAmountCents() {
        return depositAmountCents;
    }

    public String getInsuranceTier() {
        return insuranceTier;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }
}

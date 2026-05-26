package com.matador.vehicle;

import com.matador.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A grouping of similar vehicles that customers book against. */
@Entity
@Table(name = "vehicle_class")
public class VehicleClass extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "seats", nullable = false)
    private int seats;

    @Column(name = "luggage_capacity", nullable = false)
    private int luggageCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "drivetrain", nullable = false)
    private Drivetrain drivetrain;

    @Column(name = "base_daily_rate_cents", nullable = false)
    private long baseDailyRateCents;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected VehicleClass() {}

    public VehicleClass(
        UUID id,
        String name,
        String description,
        int seats,
        int luggageCapacity,
        Drivetrain drivetrain,
        long baseDailyRateCents,
        int sortOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.seats = seats;
        this.luggageCapacity = luggageCapacity;
        this.drivetrain = drivetrain;
        this.baseDailyRateCents = baseDailyRateCents;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    public void update(
        String description, Long baseDailyRateCents, Integer sortOrder, Boolean active) {
        if (description != null) {
            this.description = description;
        }
        if (baseDailyRateCents != null) {
            this.baseDailyRateCents = baseDailyRateCents;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (active != null) {
            this.active = active;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getSeats() {
        return seats;
    }

    public int getLuggageCapacity() {
        return luggageCapacity;
    }

    public Drivetrain getDrivetrain() {
        return drivetrain;
    }

    public long getBaseDailyRateCents() {
        return baseDailyRateCents;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}

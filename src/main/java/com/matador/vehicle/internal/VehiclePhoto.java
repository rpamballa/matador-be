package com.matador.vehicle.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle_photo")
public class VehiclePhoto {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "label")
    private String label;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VehiclePhoto() {}

    public VehiclePhoto(
        UUID id, UUID vehicleId, String url, String label, boolean isPrimary, int sortOrder, Instant createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.url = url;
        this.label = label;
        this.isPrimary = isPrimary;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public String getUrl() {
        return url;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}

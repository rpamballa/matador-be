package com.matador.vehicle;

import com.matador.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

/** Aggregate root for a physical vehicle in the fleet. */
@Entity
@Table(name = "vehicle")
public class Vehicle extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vin", nullable = false, unique = true)
    private String vin;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "license_state", length = 2, nullable = false)
    private String licenseState;

    @Column(name = "make", nullable = false)
    private String make;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "color", nullable = false)
    private String color;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VehicleStatus status;

    @Column(name = "current_location", columnDefinition = "geography(Point,4326)")
    private Point currentLocation;

    @Column(name = "current_address")
    private String currentAddress;

    @Column(name = "odometer_miles", nullable = false)
    private int odometerMiles;

    @Column(name = "fuel_charge_percent")
    private Integer fuelChargePercent;

    @Column(name = "range_miles")
    private Integer rangeMiles;

    @Column(name = "home_zone_id", nullable = false)
    private UUID homeZoneId;

    @Column(name = "telematics_provider")
    private String telematicsProvider;

    @Column(name = "telematics_vehicle_id")
    private String telematicsVehicleId;

    @Column(name = "notes")
    private String notes;

    @Column(name = "acquired_on", nullable = false)
    private LocalDate acquiredOn;

    @Column(name = "retired_on")
    private LocalDate retiredOn;

    protected Vehicle() {}

    public Vehicle(
        UUID id,
        String vin,
        String licensePlate,
        String licenseState,
        String make,
        String model,
        int year,
        String color,
        UUID classId,
        UUID homeZoneId,
        LocalDate acquiredOn,
        String telematicsProvider,
        String telematicsVehicleId) {
        this.id = id;
        this.vin = vin;
        this.licensePlate = licensePlate;
        this.licenseState = licenseState;
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.classId = classId;
        this.homeZoneId = homeZoneId;
        this.acquiredOn = acquiredOn;
        this.telematicsProvider = telematicsProvider;
        this.telematicsVehicleId = telematicsVehicleId;
        this.status = VehicleStatus.AVAILABLE;
        this.odometerMiles = 0;
    }

    public void transitionTo(VehicleStatus target) {
        status.requireTransitionTo(target);
        this.status = target;
        if (target == VehicleStatus.RETIRED) {
            this.retiredOn = LocalDate.now();
        }
    }

    public void updateTelematicsSnapshot(
        Point location, String address, Integer odometerMiles, Integer fuelChargePercent, Integer rangeMiles) {
        if (location != null) {
            this.currentLocation = location;
        }
        if (address != null) {
            this.currentAddress = address;
        }
        if (odometerMiles != null) {
            this.odometerMiles = odometerMiles;
        }
        if (fuelChargePercent != null) {
            this.fuelChargePercent = fuelChargePercent;
        }
        if (rangeMiles != null) {
            this.rangeMiles = rangeMiles;
        }
    }

    public void updateMutableFields(String color, String licensePlate, String notes) {
        if (color != null) {
            this.color = color;
        }
        if (licensePlate != null) {
            this.licensePlate = licensePlate;
        }
        if (notes != null) {
            this.notes = notes;
        }
    }

    public boolean isBookable() {
        return status == VehicleStatus.AVAILABLE;
    }

    public UUID getId() {
        return id;
    }

    public String getVin() {
        return vin;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getLicenseState() {
        return licenseState;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public String getColor() {
        return color;
    }

    public UUID getClassId() {
        return classId;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public Point getCurrentLocation() {
        return currentLocation;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public int getOdometerMiles() {
        return odometerMiles;
    }

    public Integer getFuelChargePercent() {
        return fuelChargePercent;
    }

    public Integer getRangeMiles() {
        return rangeMiles;
    }

    public UUID getHomeZoneId() {
        return homeZoneId;
    }

    public String getTelematicsProvider() {
        return telematicsProvider;
    }

    public String getTelematicsVehicleId() {
        return telematicsVehicleId;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDate getAcquiredOn() {
        return acquiredOn;
    }

    public LocalDate getRetiredOn() {
        return retiredOn;
    }
}

package com.matador.trip.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "trip_location_sample")
public class TripLocationSample {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "sampled_at", nullable = false)
    private Instant sampledAt;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "speed_mph")
    private BigDecimal speedMph;

    @Column(name = "odometer_miles")
    private Integer odometerMiles;

    @Column(name = "fuel_charge_percent")
    private Integer fuelChargePercent;

    protected TripLocationSample() {}

    public TripLocationSample(
        UUID id,
        UUID tripId,
        Instant sampledAt,
        Point location,
        BigDecimal speedMph,
        Integer odometerMiles,
        Integer fuelChargePercent) {
        this.id = id;
        this.tripId = tripId;
        this.sampledAt = sampledAt;
        this.location = location;
        this.speedMph = speedMph;
        this.odometerMiles = odometerMiles;
        this.fuelChargePercent = fuelChargePercent;
    }
}

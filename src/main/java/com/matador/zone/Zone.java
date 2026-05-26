package com.matador.zone;

import com.matador.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** Aggregate root for an operational zone. */
@Entity
@Table(name = "zone")
public class Zone extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "boundary", nullable = false, columnDefinition = "geography(Polygon,4326)")
    private Polygon boundary;

    @Column(name = "center", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point center;

    @Column(name = "out_of_zone_dropoff_fee_cents", nullable = false)
    private long outOfZoneDropoffFeeCents;

    @Column(name = "out_of_zone_dropoff_allowed", nullable = false)
    private boolean outOfZoneDropoffAllowed = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Zone() {}

    public Zone(
        UUID id,
        String name,
        String slug,
        Polygon boundary,
        Point center,
        long outOfZoneDropoffFeeCents,
        boolean outOfZoneDropoffAllowed) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.boundary = boundary;
        this.center = center;
        this.outOfZoneDropoffFeeCents = outOfZoneDropoffFeeCents;
        this.outOfZoneDropoffAllowed = outOfZoneDropoffAllowed;
        this.active = true;
    }

    public void update(Long feeCents, Boolean dropoffAllowed, Boolean active) {
        if (feeCents != null) {
            this.outOfZoneDropoffFeeCents = feeCents;
        }
        if (dropoffAllowed != null) {
            this.outOfZoneDropoffAllowed = dropoffAllowed;
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

    public String getSlug() {
        return slug;
    }

    public Polygon getBoundary() {
        return boundary;
    }

    public Point getCenter() {
        return center;
    }

    public long getOutOfZoneDropoffFeeCents() {
        return outOfZoneDropoffFeeCents;
    }

    public boolean isOutOfZoneDropoffAllowed() {
        return outOfZoneDropoffAllowed;
    }

    public boolean isActive() {
        return active;
    }
}

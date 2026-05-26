package com.matador.customer.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "customer_address")
public class CustomerAddress {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "label")
    private String label;

    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "line2")
    private String line2;

    @Column(name = "city", nullable = false)
    private String city;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "state", length = 2, nullable = false)
    private String state;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country", length = 2, nullable = false)
    private String country;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CustomerAddress() {}

    public CustomerAddress(
        UUID id,
        UUID customerId,
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        Point location,
        boolean isDefault,
        Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.label = label;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.location = location;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getLabel() {
        return label;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public Point getLocation() {
        return location;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

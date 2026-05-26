package com.matador.payment.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_method")
public class PaymentMethodEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "stripe_payment_method_id", nullable = false, unique = true)
    private String stripePaymentMethodId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "brand")
    private String brand;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "last4", length = 4)
    private String last4;

    @Column(name = "exp_month")
    private Integer expMonth;

    @Column(name = "exp_year")
    private Integer expYear;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "detached_at")
    private Instant detachedAt;

    protected PaymentMethodEntity() {}

    public PaymentMethodEntity(
        UUID id,
        UUID customerId,
        String stripePaymentMethodId,
        String type,
        String brand,
        String last4,
        Integer expMonth,
        Integer expYear,
        boolean isDefault,
        Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.stripePaymentMethodId = stripePaymentMethodId;
        this.type = type;
        this.brand = brand;
        this.last4 = last4;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    public void detach(Instant at) {
        this.detachedAt = at;
        this.isDefault = false;
    }

    public void setDefault(boolean value) {
        this.isDefault = value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getStripePaymentMethodId() {
        return stripePaymentMethodId;
    }

    public String getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    public String getLast4() {
        return last4;
    }

    public Integer getExpMonth() {
        return expMonth;
    }

    public Integer getExpYear() {
        return expYear;
    }

    public boolean isDefault() {
        return isDefault;
    }
}

package com.matador.customer;

import com.matador.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Aggregate root for a customer account. */
@Entity
@Table(name = "customer")
public class Customer extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "license_number")
    private String licenseNumber;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "license_state", length = 2)
    private String licenseState;

    @Column(name = "license_expires_on")
    private LocalDate licenseExpiresOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    @Column(name = "verification_completed_at")
    private Instant verificationCompletedAt;

    @Column(name = "stripe_customer_id", unique = true)
    private String stripeCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;

    protected Customer() {}

    public static Customer register(
        UUID id,
        String email,
        String phone,
        String passwordHash,
        String firstName,
        String lastName,
        LocalDate dateOfBirth) {
        Customer c = new Customer();
        c.id = id;
        c.email = email;
        c.phone = phone;
        c.passwordHash = passwordHash;
        c.firstName = firstName;
        c.lastName = lastName;
        c.dateOfBirth = dateOfBirth;
        c.verificationStatus = VerificationStatus.UNVERIFIED;
        c.status = CustomerStatus.ACTIVE;
        return c;
    }

    public void startVerification() {
        verificationStatus.requireTransitionTo(VerificationStatus.IN_PROGRESS);
        verificationStatus = VerificationStatus.IN_PROGRESS;
    }

    public void completeVerification(
        boolean success,
        String licenseNumber,
        String licenseState,
        LocalDate licenseExpiresOn,
        Instant completedAt) {
        VerificationStatus target = success ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED;
        verificationStatus.requireTransitionTo(target);
        verificationStatus = target;
        verificationCompletedAt = completedAt;
        if (success) {
            this.licenseNumber = licenseNumber;
            this.licenseState = licenseState;
            this.licenseExpiresOn = licenseExpiresOn;
        }
    }

    public void expireVerification() {
        verificationStatus.requireTransitionTo(VerificationStatus.EXPIRED);
        verificationStatus = VerificationStatus.EXPIRED;
    }

    public void suspend() {
        this.status = CustomerStatus.SUSPENDED;
    }

    public void deactivate() {
        this.status = CustomerStatus.DEACTIVATED;
    }

    public void reactivate() {
        this.status = CustomerStatus.ACTIVE;
    }

    public void updateProfile(String firstName, String lastName, String phone) {
        if (firstName != null) {
            this.firstName = firstName;
        }
        if (lastName != null) {
            this.lastName = lastName;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    public void linkStripeCustomer(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    /** A customer may book only when fully verified and active. */
    public boolean canBook() {
        return verificationStatus == VerificationStatus.VERIFIED && status == CustomerStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getLicenseState() {
        return licenseState;
    }

    public LocalDate getLicenseExpiresOn() {
        return licenseExpiresOn;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public Instant getVerificationCompletedAt() {
        return verificationCompletedAt;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public CustomerStatus getStatus() {
        return status;
    }
}

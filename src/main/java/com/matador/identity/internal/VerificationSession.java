package com.matador.identity.internal;

import com.matador.identity.VerificationSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "verification_session")
public class VerificationSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "provider_session_id", nullable = false, unique = true)
    private String providerSessionId;

    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VerificationSessionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_payload")
    private String resultPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected VerificationSession() {}

    public VerificationSession(
        UUID id,
        UUID customerId,
        String provider,
        String providerSessionId,
        String clientSecret,
        Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.provider = provider;
        this.providerSessionId = providerSessionId;
        this.clientSecret = clientSecret;
        this.status = VerificationSessionStatus.CREATED;
        this.createdAt = createdAt;
    }

    public void applyResult(
        VerificationSessionStatus status, String resultPayload, Instant completedAt) {
        this.status = status;
        this.resultPayload = resultPayload;
        if (status.isTerminal()) {
            this.completedAt = completedAt;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public VerificationSessionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}

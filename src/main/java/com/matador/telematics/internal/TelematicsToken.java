package com.matador.telematics.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telematics_token")
public class TelematicsToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "access_token_expires_at", nullable = false)
    private Instant accessTokenExpiresAt;

    @Column(name = "scopes", nullable = false)
    private String scopes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TelematicsToken() {}

    public TelematicsToken(
        UUID id,
        UUID vehicleId,
        String provider,
        String encryptedAccessToken,
        String encryptedRefreshToken,
        Instant accessTokenExpiresAt,
        String scopes,
        Instant now) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.provider = provider;
        this.accessToken = encryptedAccessToken;
        this.refreshToken = encryptedRefreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.scopes = scopes;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getAccessToken() {
        return accessToken;
    }
}

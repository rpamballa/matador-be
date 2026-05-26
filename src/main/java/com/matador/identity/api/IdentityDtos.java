package com.matador.identity.api;

import com.matador.identity.VerificationSessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class IdentityDtos {

    private IdentityDtos() {}

    /** Returned when starting a session (carries client_secret) and when fetching status. */
    public record VerificationResponse(
        UUID sessionId,
        String clientSecret,
        VerificationSessionStatus status,
        Instant completedAt) {}

    /**
     * Simplified webhook payload accepted in dev/test when no Stripe identity webhook
     * secret is configured. Mirrors the fields the real Stripe handler extracts.
     */
    public record MockVerificationEvent(
        String providerSessionId,
        VerificationSessionStatus status,
        String licenseNumber,
        String licenseState,
        LocalDate licenseExpiresOn) {}
}

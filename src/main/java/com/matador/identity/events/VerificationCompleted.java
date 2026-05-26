package com.matador.identity.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Published when a verification session reaches a terminal result. On success the
 * extracted driver's-license details are included so the customer module can persist
 * them without querying the identity module.
 */
public record VerificationCompleted(
    UUID customerId,
    UUID sessionId,
    boolean success,
    String licenseNumber,
    String licenseState,
    LocalDate licenseExpiresOn,
    Instant completedAt) {}

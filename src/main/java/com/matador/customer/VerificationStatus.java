package com.matador.customer;

import com.matador.shared.error.ConflictException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Customer identity verification state. Transitions are enforced via
 * {@link #canTransitionTo(VerificationStatus)}.
 */
public enum VerificationStatus {
    UNVERIFIED,
    IN_PROGRESS,
    VERIFIED,
    REJECTED,
    EXPIRED;

    private static final Map<VerificationStatus, Set<VerificationStatus>> TRANSITIONS =
        Map.of(
            UNVERIFIED, EnumSet.of(IN_PROGRESS),
            IN_PROGRESS, EnumSet.of(VERIFIED, REJECTED),
            VERIFIED, EnumSet.of(EXPIRED),
            REJECTED, EnumSet.of(IN_PROGRESS),
            EXPIRED, EnumSet.noneOf(VerificationStatus.class));

    public boolean canTransitionTo(VerificationStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public void requireTransitionTo(VerificationStatus target) {
        if (!canTransitionTo(target)) {
            throw new ConflictException(
                "INVALID_VERIFICATION_TRANSITION",
                "Cannot move verification from %s to %s".formatted(this, target));
        }
    }
}

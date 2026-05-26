package com.matador.trip;

import com.matador.shared.error.ConflictException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TripStatus {
    IN_PROGRESS,
    ENDED_PENDING_INSPECTION,
    CLOSED;

    private static final Map<TripStatus, Set<TripStatus>> TRANSITIONS =
        Map.of(
            IN_PROGRESS, EnumSet.of(ENDED_PENDING_INSPECTION),
            ENDED_PENDING_INSPECTION, EnumSet.of(CLOSED),
            CLOSED, EnumSet.noneOf(TripStatus.class));

    public boolean canTransitionTo(TripStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public void requireTransitionTo(TripStatus target) {
        if (!canTransitionTo(target)) {
            throw new ConflictException(
                "INVALID_TRIP_TRANSITION", "Cannot move trip from %s to %s".formatted(this, target));
        }
    }
}

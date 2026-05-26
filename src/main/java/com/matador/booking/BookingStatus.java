package com.matador.booking;

import com.matador.shared.error.ConflictException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    ACTIVATED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS =
        Map.of(
            PENDING_PAYMENT, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(ACTIVATED, CANCELLED, NO_SHOW),
            ACTIVATED, EnumSet.of(COMPLETED),
            COMPLETED, EnumSet.noneOf(BookingStatus.class),
            CANCELLED, EnumSet.noneOf(BookingStatus.class),
            NO_SHOW, EnumSet.noneOf(BookingStatus.class));

    public boolean canTransitionTo(BookingStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public void requireTransitionTo(BookingStatus target) {
        if (!canTransitionTo(target)) {
            throw new ConflictException(
                "INVALID_BOOKING_TRANSITION",
                "Cannot move booking from %s to %s".formatted(this, target));
        }
    }
}

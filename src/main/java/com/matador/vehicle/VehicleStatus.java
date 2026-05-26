package com.matador.vehicle;

import com.matador.shared.error.ConflictException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Vehicle lifecycle status with an enforced transition graph (BACKEND.md §6.3). */
public enum VehicleStatus {
    AVAILABLE,
    RESERVED,
    IN_PREPARATION,
    EN_ROUTE_TO_CUSTOMER,
    WITH_CUSTOMER,
    EN_ROUTE_TO_WAREHOUSE,
    AWAITING_INSPECTION,
    IN_CLEANING,
    IN_MAINTENANCE,
    OUT_OF_SERVICE,
    RETIRED;

    private static final Map<VehicleStatus, Set<VehicleStatus>> TRANSITIONS =
        Map.ofEntries(
            Map.entry(AVAILABLE, EnumSet.of(RESERVED, OUT_OF_SERVICE, RETIRED)),
            Map.entry(RESERVED, EnumSet.of(IN_PREPARATION, AVAILABLE, RETIRED)),
            Map.entry(IN_PREPARATION, EnumSet.of(EN_ROUTE_TO_CUSTOMER, RETIRED)),
            Map.entry(EN_ROUTE_TO_CUSTOMER, EnumSet.of(WITH_CUSTOMER, RETIRED)),
            Map.entry(WITH_CUSTOMER, EnumSet.of(EN_ROUTE_TO_WAREHOUSE, RETIRED)),
            Map.entry(EN_ROUTE_TO_WAREHOUSE, EnumSet.of(AWAITING_INSPECTION, RETIRED)),
            Map.entry(AWAITING_INSPECTION, EnumSet.of(IN_CLEANING, IN_MAINTENANCE, RETIRED)),
            Map.entry(IN_CLEANING, EnumSet.of(AVAILABLE, RETIRED)),
            Map.entry(IN_MAINTENANCE, EnumSet.of(AVAILABLE, RETIRED)),
            Map.entry(OUT_OF_SERVICE, EnumSet.of(AVAILABLE, RETIRED)),
            Map.entry(RETIRED, EnumSet.noneOf(VehicleStatus.class)));

    public boolean canTransitionTo(VehicleStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public void requireTransitionTo(VehicleStatus target) {
        if (!canTransitionTo(target)) {
            throw new ConflictException(
                "INVALID_VEHICLE_TRANSITION",
                "Cannot move vehicle from %s to %s".formatted(this, target));
        }
    }
}

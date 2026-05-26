package com.matador.inspection;

import java.util.EnumSet;
import java.util.Set;

public final class InspectionEnums {

    private InspectionEnums() {}

    public enum Phase {
        PICKUP,
        DROPOFF
    }

    public enum Angle {
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        INTERIOR_FRONT,
        INTERIOR_REAR,
        ODOMETER;

        /** All angles are required for a Phase 1 inspection. */
        public static final Set<Angle> REQUIRED = EnumSet.allOf(Angle.class);
    }

    public enum ReviewStatus {
        PASSED,
        FLAGGED
    }
}

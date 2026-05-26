package com.matador.incident;

public final class IncidentEnums {

    private IncidentEnums() {}

    public enum Type {
        DAMAGE,
        OUT_OF_ZONE,
        LATE_RETURN,
        ACCIDENT,
        TICKET,
        OTHER
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum Status {
        OPEN,
        IN_REVIEW,
        RESOLVED,
        DISMISSED
    }
}

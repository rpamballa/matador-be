package com.matador.identity;

public enum VerificationSessionStatus {
    CREATED,
    PROCESSING,
    VERIFIED,
    REQUIRES_INPUT,
    CANCELED;

    public boolean isTerminal() {
        return this == VERIFIED || this == CANCELED;
    }
}

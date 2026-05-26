package com.matador.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.matador.shared.error.ConflictException;
import org.junit.jupiter.api.Test;

class VerificationStatusTest {

    @Test
    void allowsValidTransitions() {
        assertThat(VerificationStatus.UNVERIFIED.canTransitionTo(VerificationStatus.IN_PROGRESS))
            .isTrue();
        assertThat(VerificationStatus.IN_PROGRESS.canTransitionTo(VerificationStatus.VERIFIED))
            .isTrue();
        assertThat(VerificationStatus.IN_PROGRESS.canTransitionTo(VerificationStatus.REJECTED))
            .isTrue();
        assertThat(VerificationStatus.VERIFIED.canTransitionTo(VerificationStatus.EXPIRED)).isTrue();
        assertThat(VerificationStatus.REJECTED.canTransitionTo(VerificationStatus.IN_PROGRESS))
            .isTrue();
    }

    @Test
    void rejectsInvalidTransitions() {
        assertThat(VerificationStatus.UNVERIFIED.canTransitionTo(VerificationStatus.VERIFIED))
            .isFalse();
        assertThat(VerificationStatus.VERIFIED.canTransitionTo(VerificationStatus.IN_PROGRESS))
            .isFalse();
        assertThat(VerificationStatus.EXPIRED.canTransitionTo(VerificationStatus.IN_PROGRESS))
            .isFalse();
    }

    @Test
    void requireTransitionThrowsConflictOnInvalid() {
        assertThatThrownBy(
                () -> VerificationStatus.VERIFIED.requireTransitionTo(VerificationStatus.IN_PROGRESS))
            .isInstanceOf(ConflictException.class);
    }
}

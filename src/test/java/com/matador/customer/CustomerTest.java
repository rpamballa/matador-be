package com.matador.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.matador.shared.error.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTest {

    private Customer newCustomer() {
        return Customer.register(
            UUID.randomUUID(),
            "jane@example.com",
            "+19195550100",
            "hash",
            "Jane",
            "Doe",
            LocalDate.of(1990, 1, 1));
    }

    @Test
    void registersUnverifiedAndActive() {
        Customer c = newCustomer();
        assertThat(c.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
        assertThat(c.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(c.canBook()).isFalse();
    }

    @Test
    void canBookOnlyWhenVerifiedAndActive() {
        Customer c = newCustomer();
        c.startVerification();
        c.completeVerification(true, "D1234567", "NC", LocalDate.of(2030, 1, 1), Instant.now());
        assertThat(c.canBook()).isTrue();

        c.suspend();
        assertThat(c.canBook()).isFalse();
    }

    @Test
    void rejectsReverificationOnceVerified() {
        Customer c = newCustomer();
        c.startVerification();
        c.completeVerification(true, "D1", "NC", LocalDate.of(2030, 1, 1), Instant.now());
        assertThatThrownBy(c::startVerification).isInstanceOf(ConflictException.class);
    }

    @Test
    void failedVerificationCanRetry() {
        Customer c = newCustomer();
        c.startVerification();
        c.completeVerification(false, null, null, null, Instant.now());
        assertThat(c.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        c.startVerification();
        assertThat(c.getVerificationStatus()).isEqualTo(VerificationStatus.IN_PROGRESS);
    }
}

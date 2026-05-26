package com.matador.payment.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, UUID> {

    Optional<PaymentIntentEntity> findByStripeIntentId(String stripeIntentId);

    Optional<PaymentIntentEntity> findFirstByBookingIdAndPurposeOrderByCreatedAtDesc(
        UUID bookingId, String purpose);
}

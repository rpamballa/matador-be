package com.matador.payment.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, UUID> {

    List<PaymentMethodEntity> findByCustomerIdAndDetachedAtIsNull(UUID customerId);

    Optional<PaymentMethodEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<PaymentMethodEntity> findByStripePaymentMethodId(String stripePaymentMethodId);
}

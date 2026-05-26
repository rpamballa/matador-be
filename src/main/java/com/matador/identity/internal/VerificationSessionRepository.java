package com.matador.identity.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationSessionRepository extends JpaRepository<VerificationSession, UUID> {

    Optional<VerificationSession> findFirstByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<VerificationSession> findByProviderSessionId(String providerSessionId);
}

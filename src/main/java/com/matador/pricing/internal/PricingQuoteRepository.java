package com.matador.pricing.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingQuoteRepository extends JpaRepository<PricingQuoteEntity, UUID> {}

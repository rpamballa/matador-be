package com.matador.pricing.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PricingRateRepository extends JpaRepository<PricingRate, UUID> {

    @Query(
        """
        select r from PricingRate r
        where r.vehicleClassId = :classId
          and r.insuranceTier = :tier
          and r.effectiveFrom <= :at
          and (r.effectiveTo is null or r.effectiveTo > :at)
        order by r.effectiveFrom desc
        """)
    List<PricingRate> findActiveRate(
        @Param("classId") UUID classId,
        @Param("tier") String tier,
        @Param("at") Instant at,
        Limit limit);
}

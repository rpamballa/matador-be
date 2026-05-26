package com.matador.ledger.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTripIdOrderByOccurredAtAsc(UUID tripId);

    Page<LedgerEntry> findByCustomerIdOrderByOccurredAtDesc(UUID customerId, Pageable pageable);

    @Query("select coalesce(sum(e.amountCents), 0) from LedgerEntry e where e.customerId = :customerId")
    long sumByCustomer(@Param("customerId") UUID customerId);

    @Query("select coalesce(sum(e.amountCents), 0) from LedgerEntry e where e.tripId = :tripId")
    long sumByTrip(@Param("tripId") UUID tripId);
}

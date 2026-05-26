package com.matador.trip.internal;

import com.matador.trip.Trip;
import com.matador.trip.TripStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    Optional<Trip> findByCustomerIdAndStatus(UUID customerId, TripStatus status);

    Page<Trip> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    List<Trip> findByVehicleIdAndStatus(UUID vehicleId, TripStatus status);

    Optional<Trip> findByBookingId(UUID bookingId);

    @Query(
        """
        select t from Trip t
        where (:status is null or t.status = :status)
          and (:customerId is null or t.customerId = :customerId)
        """)
    Page<Trip> search(
        @Param("status") TripStatus status,
        @Param("customerId") UUID customerId,
        Pageable pageable);
}

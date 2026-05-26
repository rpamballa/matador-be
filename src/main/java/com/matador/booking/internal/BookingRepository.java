package com.matador.booking.internal;

import com.matador.booking.Booking;
import com.matador.booking.BookingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query(
        """
        select b from Booking b
        where (:status is null or b.status = :status)
          and (:customerId is null or b.customerId = :customerId)
          and (:vehicleId is null or b.assignedVehicleId = :vehicleId)
        """)
    Page<Booking> search(
        @Param("status") BookingStatus status,
        @Param("customerId") UUID customerId,
        @Param("vehicleId") UUID vehicleId,
        Pageable pageable);

    /** Count reservations for a class overlapping the window in capacity-consuming states. */
    @Query(
        """
        select count(b) from Booking b
        where b.vehicleClassId = :classId
          and b.status in (com.matador.booking.BookingStatus.PENDING_PAYMENT,
                           com.matador.booking.BookingStatus.CONFIRMED,
                           com.matador.booking.BookingStatus.ACTIVATED)
          and b.pickupAt < :dropoffAt
          and b.dropoffAt > :pickupAt
        """)
    long countOverlapping(
        @Param("classId") UUID classId,
        @Param("pickupAt") Instant pickupAt,
        @Param("dropoffAt") Instant dropoffAt);

    boolean existsByCustomerIdAndStatus(UUID customerId, BookingStatus status);

    @Query(
        """
        select b from Booking b
        where b.status = com.matador.booking.BookingStatus.CONFIRMED
          and b.pickupAt < :threshold
        """)
    List<Booking> findConfirmedPastPickup(@Param("threshold") Instant threshold);

    @Query(value = "select nextval('booking_number_seq')", nativeQuery = true)
    long nextBookingSequence();
}

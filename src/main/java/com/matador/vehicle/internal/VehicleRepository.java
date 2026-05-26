package com.matador.vehicle.internal;

import com.matador.vehicle.Vehicle;
import com.matador.vehicle.VehicleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    boolean existsByVin(String vin);

    long countByClassIdAndStatus(UUID classId, VehicleStatus status);

    List<Vehicle> findByClassIdAndStatus(UUID classId, VehicleStatus status);

    List<Vehicle> findByStatus(VehicleStatus status);

    @Query(
        """
        select v from Vehicle v
        where (:status is null or v.status = :status)
          and (:classId is null or v.classId = :classId)
          and (:zoneId is null or v.homeZoneId = :zoneId)
        """)
    Page<Vehicle> search(
        @Param("status") VehicleStatus status,
        @Param("classId") UUID classId,
        @Param("zoneId") UUID zoneId,
        Pageable pageable);
}

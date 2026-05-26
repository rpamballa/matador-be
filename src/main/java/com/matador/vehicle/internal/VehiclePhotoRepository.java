package com.matador.vehicle.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, UUID> {

    List<VehiclePhoto> findByVehicleIdOrderBySortOrderAsc(UUID vehicleId);

    Optional<VehiclePhoto> findFirstByVehicleIdAndIsPrimaryTrue(UUID vehicleId);
}

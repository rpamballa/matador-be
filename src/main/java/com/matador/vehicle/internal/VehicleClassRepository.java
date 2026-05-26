package com.matador.vehicle.internal;

import com.matador.vehicle.VehicleClass;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleClassRepository extends JpaRepository<VehicleClass, UUID> {

    boolean existsByName(String name);

    List<VehicleClass> findByActiveTrueOrderBySortOrderAsc();
}

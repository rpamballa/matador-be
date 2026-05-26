package com.matador.telematics.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelematicsTokenRepository extends JpaRepository<TelematicsToken, UUID> {

    boolean existsByVehicleIdAndProvider(UUID vehicleId, String provider);
}

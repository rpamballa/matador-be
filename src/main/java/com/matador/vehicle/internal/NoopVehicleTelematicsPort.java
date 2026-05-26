package com.matador.vehicle.internal;

import com.matador.vehicle.VehicleTelematicsPort;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Fallback used until the telematics module provides a real implementation. */
@Configuration
class NoopVehicleTelematicsPort {

    @Bean
    @ConditionalOnMissingBean(VehicleTelematicsPort.class)
    VehicleTelematicsPort noopVehicleTelematicsPort() {
        return new VehicleTelematicsPort() {
            @Override
            public CommandResult lock(UUID vehicleId) {
                return CommandResult.unavailable();
            }

            @Override
            public CommandResult unlock(UUID vehicleId) {
                return CommandResult.unavailable();
            }
        };
    }
}

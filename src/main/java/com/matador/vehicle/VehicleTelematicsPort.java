package com.matador.vehicle;

import java.util.UUID;

/**
 * Outbound port the vehicle module uses to issue telematics commands. The telematics
 * module supplies the real implementation; a no-op fallback keeps the vehicle module
 * runnable on its own.
 */
public interface VehicleTelematicsPort {

    CommandResult lock(UUID vehicleId);

    CommandResult unlock(UUID vehicleId);

    record CommandResult(boolean succeeded, String detail) {

        public static CommandResult ok() {
            return new CommandResult(true, "ok");
        }

        public static CommandResult unavailable() {
            return new CommandResult(false, "telematics provider unavailable");
        }
    }
}

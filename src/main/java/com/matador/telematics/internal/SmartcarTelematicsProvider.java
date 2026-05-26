package com.matador.telematics.internal;

import com.matador.telematics.TelematicsProvider;
import com.matador.vehicle.VehicleInfo;
import org.springframework.stereotype.Component;

/**
 * Smartcar-backed provider. Full OAuth/token integration requires per-vehicle Smartcar
 * tokens (stored encrypted in {@code telematics_token}); until a vehicle is linked this
 * provider reports unavailable. The provider is selected for vehicles whose
 * {@code telematics_provider = SMARTCAR}.
 */
@Component
class SmartcarTelematicsProvider implements TelematicsProvider {

    private final TelematicsTokenStore tokenStore;

    SmartcarTelematicsProvider(TelematicsTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public String providerName() {
        return "SMARTCAR";
    }

    @Override
    public boolean supports(VehicleInfo vehicle) {
        return vehicle != null
            && "SMARTCAR".equalsIgnoreCase(vehicle.telematicsProvider())
            && tokenStore.hasToken(vehicle.id(), "SMARTCAR");
    }

    @Override
    public CommandOutcome lock(VehicleInfo vehicle) {
        return requireLinked(vehicle, "lock");
    }

    @Override
    public CommandOutcome unlock(VehicleInfo vehicle) {
        return requireLinked(vehicle, "unlock");
    }

    @Override
    public VehicleSnapshot snapshot(VehicleInfo vehicle) {
        // Without a linked Smartcar token, no live snapshot is available.
        return null;
    }

    private CommandOutcome requireLinked(VehicleInfo vehicle, String command) {
        if (!tokenStore.hasToken(vehicle.id(), "SMARTCAR")) {
            return new CommandOutcome(false, "Smartcar vehicle not linked");
        }
        // A real implementation would call the Smartcar SDK using the decrypted access token.
        return new CommandOutcome(false, "Smartcar " + command + " not yet implemented");
    }
}

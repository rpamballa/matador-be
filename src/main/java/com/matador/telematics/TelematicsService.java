package com.matador.telematics;

import com.matador.shared.id.IdGenerator;
import com.matador.shared.security.CurrentUser;
import com.matador.telematics.TelematicsProvider.CommandOutcome;
import com.matador.telematics.TelematicsProvider.VehicleSnapshot;
import com.matador.telematics.internal.TelematicsCommandLog;
import com.matador.telematics.internal.TelematicsCommandLogRepository;
import com.matador.vehicle.VehicleInfo;
import com.matador.vehicle.VehicleService;
import com.matador.vehicle.VehicleTelematicsPort;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API for the telematics module. Implements the vehicle module's command port so
 * lock/unlock requests route through the appropriate provider, and exposes snapshots.
 */
@Service
public class TelematicsService implements VehicleTelematicsPort {

    private final Map<String, TelematicsProvider> providersByName;
    private final TelematicsProvider mockProvider;
    private final VehicleService vehicleService;
    private final TelematicsCommandLogRepository commandLog;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public TelematicsService(
        List<TelematicsProvider> providers,
        VehicleService vehicleService,
        TelematicsCommandLogRepository commandLog,
        IdGenerator idGenerator,
        Clock clock) {
        this.providersByName =
            providers.stream()
                .collect(Collectors.toMap(p -> p.providerName().toUpperCase(), Function.identity()));
        this.mockProvider = providersByName.get("MOCK");
        this.vehicleService = vehicleService;
        this.commandLog = commandLog;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CommandResult lock(UUID vehicleId) {
        return runCommand(vehicleId, "LOCK");
    }

    @Override
    @Transactional
    public CommandResult unlock(UUID vehicleId) {
        return runCommand(vehicleId, "UNLOCK");
    }

    @Transactional(readOnly = true)
    public Optional<VehicleSnapshot> snapshot(UUID vehicleId) {
        VehicleInfo vehicle = vehicleService.requireVehicle(vehicleId);
        return Optional.ofNullable(providerFor(vehicle).snapshot(vehicle));
    }

    private CommandResult runCommand(UUID vehicleId, String command) {
        VehicleInfo vehicle = vehicleService.requireVehicle(vehicleId);
        TelematicsProvider provider = providerFor(vehicle);
        CommandOutcome outcome =
            command.equals("LOCK") ? provider.lock(vehicle) : provider.unlock(vehicle);
        UUID actor = CurrentUser.find().map(u -> u.id()).orElse(CurrentUser.SYSTEM_ID);
        commandLog.save(
            new TelematicsCommandLog(
                idGenerator.newId(),
                vehicleId,
                null,
                command,
                actor,
                outcome.succeeded(),
                outcome.succeeded() ? null : outcome.detail(),
                clock.instant()));
        return new CommandResult(outcome.succeeded(), outcome.detail());
    }

    private TelematicsProvider providerFor(VehicleInfo vehicle) {
        String name = vehicle.telematicsProvider();
        if (name == null) {
            return mockProvider;
        }
        return providersByName.getOrDefault(name.toUpperCase(), mockProvider);
    }
}

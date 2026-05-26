package com.matador.vehicle;

import com.matador.shared.error.ConflictException;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.geo.GeoSupport;
import com.matador.shared.id.IdGenerator;
import com.matador.shared.security.CurrentUser;
import com.matador.vehicle.api.VehicleDtos.AvailableClassResponse;
import com.matador.vehicle.api.VehicleDtos.CommandResponse;
import com.matador.vehicle.api.VehicleDtos.CreateVehicleClassRequest;
import com.matador.vehicle.api.VehicleDtos.CreateVehicleRequest;
import com.matador.vehicle.api.VehicleDtos.UpdateVehicleClassRequest;
import com.matador.vehicle.api.VehicleDtos.UpdateVehicleRequest;
import com.matador.vehicle.api.VehicleDtos.VehicleClassResponse;
import com.matador.vehicle.api.VehicleDtos.VehicleResponse;
import com.matador.vehicle.events.VehicleEvents.VehicleAcquired;
import com.matador.vehicle.events.VehicleEvents.VehicleStatusChanged;
import com.matador.vehicle.internal.VehicleClassRepository;
import com.matador.vehicle.internal.VehiclePhoto;
import com.matador.vehicle.internal.VehiclePhotoRepository;
import com.matador.vehicle.internal.VehicleRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the vehicle module. */
@Service
public class VehicleService {

    private final VehicleRepository vehicles;
    private final VehicleClassRepository classes;
    private final VehiclePhotoRepository photos;
    private final VehicleTelematicsPort telematics;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public VehicleService(
        VehicleRepository vehicles,
        VehicleClassRepository classes,
        VehiclePhotoRepository photos,
        // @Lazy breaks the runtime bean cycle: TelematicsService implements this port and
        // itself depends on VehicleService. The module boundary stays clean (vehicle only
        // knows the port interface it owns).
        @org.springframework.context.annotation.Lazy VehicleTelematicsPort telematics,
        IdGenerator idGenerator,
        Clock clock,
        ApplicationEventPublisher events) {
        this.vehicles = vehicles;
        this.classes = classes;
        this.photos = photos;
        this.telematics = telematics;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.events = events;
    }

    // ---- Vehicle classes ----

    @Transactional
    public VehicleClassResponse createClass(CreateVehicleClassRequest req) {
        if (classes.existsByName(req.name())) {
            throw new ConflictException("CLASS_NAME_TAKEN", "Vehicle class name already exists.");
        }
        VehicleClass vc =
            new VehicleClass(
                idGenerator.newId(),
                req.name(),
                req.description(),
                req.seats(),
                req.luggageCapacity(),
                req.drivetrain(),
                req.baseDailyRateCents(),
                req.sortOrder());
        return toClassResponse(classes.save(vc));
    }

    @Transactional
    public VehicleClassResponse updateClass(UUID id, UpdateVehicleClassRequest req) {
        VehicleClass vc = requireClassEntity(id);
        vc.update(req.description(), req.baseDailyRateCents(), req.sortOrder(), req.active());
        return toClassResponse(vc);
    }

    @Transactional(readOnly = true)
    public List<VehicleClassResponse> listClasses() {
        return classes.findAll().stream().map(this::toClassResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleClassResponse> listActiveClasses() {
        return classes.findByActiveTrueOrderBySortOrderAsc().stream().map(this::toClassResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehicleClassInfo requireClass(UUID classId) {
        return toClassInfo(requireClassEntity(classId));
    }

    // ---- Vehicles ----

    @Transactional
    public VehicleResponse create(CreateVehicleRequest req) {
        if (vehicles.existsByVin(req.vin())) {
            throw new ConflictException("VIN_TAKEN", "A vehicle with this VIN already exists.");
        }
        requireClassEntity(req.classId());
        Vehicle vehicle =
            new Vehicle(
                idGenerator.newId(),
                req.vin(),
                req.licensePlate(),
                req.licenseState(),
                req.make(),
                req.model(),
                req.year(),
                req.color(),
                req.classId(),
                req.homeZoneId(),
                req.acquiredOn(),
                req.telematicsProvider(),
                req.telematicsVehicleId());
        vehicles.save(vehicle);
        events.publishEvent(new VehicleAcquired(vehicle.getId(), vehicle.getVin(), vehicle.getAcquiredOn()));
        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse update(UUID id, UpdateVehicleRequest req) {
        Vehicle v = requireVehicleEntity(id);
        v.updateMutableFields(req.color(), req.licensePlate(), req.notes());
        return toResponse(v);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(UUID id) {
        return toResponse(requireVehicleEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> search(
        VehicleStatus status, UUID classId, UUID zoneId, Pageable pageable) {
        return vehicles.search(status, classId, zoneId, pageable).map(this::toResponse);
    }

    /** Transitions a vehicle's status, enforcing the state machine, and publishes an event. */
    @Transactional
    public VehicleResponse transitionStatus(UUID id, VehicleStatus target) {
        Vehicle v = requireVehicleEntity(id);
        VehicleStatus from = v.getStatus();
        v.transitionTo(target);
        UUID actor = CurrentUser.find().map(u -> u.id()).orElse(CurrentUser.SYSTEM_ID);
        events.publishEvent(new VehicleStatusChanged(id, from, target, clock.instant(), actor));
        return toResponse(v);
    }

    @Transactional
    public CommandResponse lock(UUID id) {
        requireVehicleEntity(id);
        var result = telematics.lock(id);
        return new CommandResponse(result.succeeded(), result.detail());
    }

    @Transactional
    public CommandResponse unlock(UUID id) {
        requireVehicleEntity(id);
        var result = telematics.unlock(id);
        return new CommandResponse(result.succeeded(), result.detail());
    }

    // ---- Customer availability ----

    /**
     * Lists bookable classes for a window. Phase 1 simplification: counts vehicles in
     * AVAILABLE status per active class; precise per-window overlap is enforced at booking
     * creation (see booking module). pickupAt/dropoffAt/location are accepted for forward
     * compatibility.
     */
    @Transactional(readOnly = true)
    public List<AvailableClassResponse> availableClasses() {
        return classes.findByActiveTrueOrderBySortOrderAsc().stream()
            .map(this::toAvailableClass)
            .filter(c -> c.countAvailable() > 0)
            .toList();
    }

    // ---- Lifecycle transitions driven by booking/trip ----
    // The driver dispatch app (which would drive the intermediate prep/transit states) is
    // deferred in Phase 1, so these helpers fast-forward through the valid transition path.

    @Transactional
    public void reserveForBooking(UUID vehicleId) {
        walk(vehicleId, VehicleStatus.RESERVED);
    }

    @Transactional
    public void releaseReservation(UUID vehicleId) {
        walk(vehicleId, VehicleStatus.AVAILABLE);
    }

    @Transactional
    public void startCustomerUse(UUID vehicleId) {
        walk(
            vehicleId,
            VehicleStatus.IN_PREPARATION,
            VehicleStatus.EN_ROUTE_TO_CUSTOMER,
            VehicleStatus.WITH_CUSTOMER);
    }

    @Transactional
    public void returnToWarehouse(UUID vehicleId) {
        walk(vehicleId, VehicleStatus.EN_ROUTE_TO_WAREHOUSE, VehicleStatus.AWAITING_INSPECTION);
    }

    @Transactional
    public void returnToAvailable(UUID vehicleId) {
        walk(vehicleId, VehicleStatus.IN_CLEANING, VehicleStatus.AVAILABLE);
    }

    /** Applies one or more sequential transitions, publishing a single status-changed event. */
    private void walk(UUID vehicleId, VehicleStatus... path) {
        Vehicle v = requireVehicleEntity(vehicleId);
        VehicleStatus from = v.getStatus();
        for (VehicleStatus next : path) {
            if (v.getStatus() != next) {
                v.transitionTo(next);
            }
        }
        UUID actor = CurrentUser.find().map(u -> u.id()).orElse(CurrentUser.SYSTEM_ID);
        events.publishEvent(
            new VehicleStatusChanged(vehicleId, from, v.getStatus(), clock.instant(), actor));
    }

    // ---- Cross-module API ----

    @Transactional(readOnly = true)
    public VehicleInfo requireVehicle(UUID id) {
        return toInfo(requireVehicleEntity(id));
    }

    @Transactional(readOnly = true)
    public List<VehicleInfo> findAvailableByClass(UUID classId) {
        return vehicles.findByClassIdAndStatus(classId, VehicleStatus.AVAILABLE).stream()
            .map(this::toInfo)
            .toList();
    }

    @Transactional(readOnly = true)
    public int currentOdometer(UUID vehicleId) {
        return requireVehicleEntity(vehicleId).getOdometerMiles();
    }

    @Transactional(readOnly = true)
    public List<VehicleInfo> findByStatus(VehicleStatus status) {
        return vehicles.findByStatus(status).stream().map(this::toInfo).toList();
    }

    @Transactional
    public void applyTelematicsSnapshot(
        UUID vehicleId,
        Double lat,
        Double lng,
        Integer odometerMiles,
        Integer fuelChargePercent,
        Integer rangeMiles) {
        Vehicle v = requireVehicleEntity(vehicleId);
        var point = (lat != null && lng != null) ? GeoSupport.point(lng, lat) : null;
        v.updateTelematicsSnapshot(point, null, odometerMiles, fuelChargePercent, rangeMiles);
    }

    // ---- helpers ----

    private Vehicle requireVehicleEntity(UUID id) {
        return vehicles.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Vehicle", id));
    }

    private VehicleClass requireClassEntity(UUID id) {
        return classes.findById(id).orElseThrow(() -> ResourceNotFoundException.of("VehicleClass", id));
    }

    private AvailableClassResponse toAvailableClass(VehicleClass vc) {
        long count = vehicles.countByClassIdAndStatus(vc.getId(), VehicleStatus.AVAILABLE);
        String photoUrl =
            vehicles.findByClassIdAndStatus(vc.getId(), VehicleStatus.AVAILABLE).stream()
                .findFirst()
                .flatMap(v -> photos.findFirstByVehicleIdAndIsPrimaryTrue(v.getId()))
                .map(VehiclePhoto::getUrl)
                .orElse(null);
        return new AvailableClassResponse(
            vc.getId(),
            vc.getName(),
            vc.getDescription(),
            vc.getDrivetrain(),
            vc.getSeats(),
            vc.getLuggageCapacity(),
            count,
            vc.getBaseDailyRateCents(),
            photoUrl);
    }

    private VehicleInfo toInfo(Vehicle v) {
        return new VehicleInfo(
            v.getId(),
            v.getVin(),
            v.getClassId(),
            v.getStatus(),
            v.getHomeZoneId(),
            v.getMake(),
            v.getModel(),
            v.getYear(),
            v.getColor(),
            v.getOdometerMiles(),
            v.getTelematicsProvider());
    }

    private VehicleClassInfo toClassInfo(VehicleClass vc) {
        return new VehicleClassInfo(
            vc.getId(), vc.getName(), vc.getDrivetrain(), vc.getSeats(), vc.getBaseDailyRateCents(), vc.isActive());
    }

    private VehicleClassResponse toClassResponse(VehicleClass vc) {
        return new VehicleClassResponse(
            vc.getId(),
            vc.getName(),
            vc.getDescription(),
            vc.getSeats(),
            vc.getLuggageCapacity(),
            vc.getDrivetrain(),
            vc.getBaseDailyRateCents(),
            vc.getSortOrder(),
            vc.isActive());
    }

    private VehicleResponse toResponse(Vehicle v) {
        Double lat = v.getCurrentLocation() == null ? null : GeoSupport.lat(v.getCurrentLocation());
        Double lng = v.getCurrentLocation() == null ? null : GeoSupport.lng(v.getCurrentLocation());
        return new VehicleResponse(
            v.getId(),
            v.getVin(),
            v.getLicensePlate(),
            v.getLicenseState(),
            v.getMake(),
            v.getModel(),
            v.getYear(),
            v.getColor(),
            v.getClassId(),
            v.getStatus(),
            lat,
            lng,
            v.getCurrentAddress(),
            v.getOdometerMiles(),
            v.getFuelChargePercent(),
            v.getRangeMiles(),
            v.getHomeZoneId(),
            v.getTelematicsProvider(),
            v.getAcquiredOn(),
            v.getRetiredOn());
    }
}

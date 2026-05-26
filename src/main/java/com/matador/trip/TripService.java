package com.matador.trip;

import com.matador.inspection.InspectionEnums.Phase;
import com.matador.inspection.InspectionService;
import com.matador.inspection.events.InspectionEvents.InspectionSubmitted;
import com.matador.payment.PaymentService;
import com.matador.shared.error.ConflictException;
import com.matador.shared.error.ForbiddenException;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.geo.GeoSupport;
import com.matador.shared.id.IdGenerator;
import com.matador.shared.money.Money;
import com.matador.trip.api.TripDtos.TripResponse;
import com.matador.trip.events.TripEvents.TripClosed;
import com.matador.trip.events.TripEvents.TripEnded;
import com.matador.trip.events.TripEvents.TripStarted;
import com.matador.trip.internal.TripLocationSample;
import com.matador.trip.internal.TripLocationSampleRepository;
import com.matador.trip.internal.TripRepository;
import com.matador.vehicle.VehicleService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the trip module. */
@Service
public class TripService {

    private final TripRepository trips;
    private final TripLocationSampleRepository samples;
    private final PaymentService paymentService;
    private final VehicleService vehicleService;
    private final InspectionService inspectionService;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public TripService(
        TripRepository trips,
        TripLocationSampleRepository samples,
        PaymentService paymentService,
        VehicleService vehicleService,
        InspectionService inspectionService,
        IdGenerator idGenerator,
        Clock clock,
        ApplicationEventPublisher events) {
        this.trips = trips;
        this.samples = samples;
        this.paymentService = paymentService;
        this.vehicleService = vehicleService;
        this.inspectionService = inspectionService;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.events = events;
    }

    /** Records a telematics location sample for the active trip of a vehicle, if any. */
    @Transactional
    public void recordSampleForVehicle(
        UUID vehicleId, double lat, double lng, Integer odometerMiles, Integer fuelChargePercent) {
        trips
            .findByVehicleIdAndStatus(vehicleId, TripStatus.IN_PROGRESS)
            .forEach(
                trip ->
                    samples.save(
                        new TripLocationSample(
                            idGenerator.newId(),
                            trip.getId(),
                            clock.instant(),
                            GeoSupport.point(lng, lat),
                            (BigDecimal) null,
                            odometerMiles,
                            fuelChargePercent)));
    }

    /**
     * Creates a trip on booking activation. Called directly by the booking module (a one-way
     * booking -> trip dependency) to keep the modules acyclic.
     */
    @Transactional
    public void createForBooking(
        UUID tripId,
        UUID bookingId,
        UUID customerId,
        UUID vehicleId,
        java.time.Instant pickupAt,
        double pickupLat,
        double pickupLng,
        int odometerStart,
        long quotedTotalCents) {
        if (trips.findById(tripId).isPresent()) {
            return; // idempotent
        }
        Trip trip =
            new Trip(
                tripId,
                bookingId,
                customerId,
                vehicleId,
                pickupAt,
                GeoSupport.point(pickupLng, pickupLat),
                odometerStart,
                quotedTotalCents,
                clock.instant());
        trips.save(trip);
        events.publishEvent(
            new TripStarted(trip.getId(), trip.getBookingId(), trip.getVehicleId(), clock.instant()));
    }

    @Transactional
    public void attachInspection(UUID tripId, Phase phase, UUID inspectionId, Integer odometer) {
        trips
            .findById(tripId)
            .ifPresent(
                trip -> {
                    if (phase == Phase.PICKUP) {
                        trip.recordPickupInspection(inspectionId);
                    } else {
                        trip.recordDropoffInspection(inspectionId, odometer);
                    }
                });
    }

    @Transactional(readOnly = true)
    public TripResponse currentForCustomer(UUID customerId) {
        return trips
            .findByCustomerIdAndStatus(customerId, TripStatus.IN_PROGRESS)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No active trip."));
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> historyForCustomer(UUID customerId, Pageable pageable) {
        return trips.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TripResponse getForCustomer(UUID customerId, UUID tripId) {
        Trip trip = require(tripId);
        if (!trip.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Not your trip.");
        }
        return toResponse(trip);
    }

    @Transactional
    public TripResponse endByCustomer(UUID customerId, UUID tripId) {
        Trip trip = require(tripId);
        if (!trip.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Not your trip.");
        }
        Integer odometer = vehicleService.currentOdometer(trip.getVehicleId());
        trip.end(clock.instant(), null, null, odometer);
        events.publishEvent(new TripEnded(trip.getId(), clock.instant(), trip.getMilesDriven()));
        return toResponse(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> search(TripStatus status, UUID customerId, Pageable pageable) {
        return trips.search(status, customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TripResponse getById(UUID tripId) {
        return toResponse(require(tripId));
    }

    @Transactional(readOnly = true)
    public TripInfo info(UUID tripId) {
        Trip t = require(tripId);
        return new TripInfo(t.getId(), t.getCustomerId(), t.getVehicleId(), t.getStatus());
    }

    /** Closes a trip after the dropoff inspection: computes charges, settles payment. */
    @Transactional
    public TripResponse close(UUID tripId) {
        Trip trip = require(tripId);
        if (trip.getStatus() != TripStatus.ENDED_PENDING_INSPECTION) {
            throw new ConflictException("TRIP_NOT_ENDED", "Trip must be ended before closing.");
        }
        if (!inspectionService.hasSubmittedInspection(tripId, Phase.DROPOFF)) {
            throw new ConflictException(
                "DROPOFF_INSPECTION_REQUIRED", "A dropoff inspection must be submitted before closing.");
        }
        int odometerEnd =
            trip.getOdometerEnd() != null
                ? trip.getOdometerEnd()
                : vehicleService.currentOdometer(trip.getVehicleId());
        long finalCharges = trip.getQuotedTotalCents();
        trip.close(odometerEnd, finalCharges, clock.instant());

        // Charge the rental total off-session and release the security deposit hold.
        paymentService.chargeOffSession(
            trip.getCustomerId(), trip.getId(), Money.usd(finalCharges), "Rental " + trip.getId());
        paymentService.releaseDepositHold(trip.getBookingId());
        vehicleService.returnToWarehouse(trip.getVehicleId());

        events.publishEvent(
            new TripClosed(
                trip.getId(), trip.getBookingId(), trip.getVehicleId(), finalCharges, clock.instant()));
        return toResponse(trip);
    }

    private Trip require(UUID tripId) {
        return trips.findById(tripId).orElseThrow(() -> ResourceNotFoundException.of("Trip", tripId));
    }

    private TripResponse toResponse(Trip t) {
        return new TripResponse(
            t.getId(),
            t.getBookingId(),
            t.getVehicleId(),
            t.getStatus(),
            t.getActualPickupAt(),
            t.getActualDropoffAt(),
            t.getOdometerStart(),
            t.getOdometerEnd(),
            t.getMilesDriven(),
            t.getFinalChargesCents());
    }
}

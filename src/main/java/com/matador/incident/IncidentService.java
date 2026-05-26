package com.matador.incident;

import com.matador.incident.IncidentEnums.Severity;
import com.matador.incident.IncidentEnums.Status;
import com.matador.incident.api.IncidentDtos.CustomerReportRequest;
import com.matador.incident.api.IncidentDtos.IncidentResponse;
import com.matador.incident.api.IncidentDtos.StaffCreateRequest;
import com.matador.incident.api.IncidentDtos.UpdateIncidentRequest;
import com.matador.incident.events.IncidentEvents.IncidentReported;
import com.matador.incident.events.IncidentEvents.IncidentResolved;
import com.matador.incident.internal.Incident;
import com.matador.incident.internal.IncidentRepository;
import com.matador.payment.PaymentService;
import com.matador.shared.error.ForbiddenException;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.money.Money;
import com.matador.shared.security.CurrentUser;
import com.matador.trip.TripInfo;
import com.matador.trip.TripService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the incident module. */
@Service
public class IncidentService {

    private final IncidentRepository incidents;
    private final TripService tripService;
    private final PaymentService paymentService;
    private final com.matador.shared.id.IdGenerator idGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public IncidentService(
        IncidentRepository incidents,
        TripService tripService,
        PaymentService paymentService,
        com.matador.shared.id.IdGenerator idGenerator,
        Clock clock,
        ApplicationEventPublisher events) {
        this.incidents = incidents;
        this.tripService = tripService;
        this.paymentService = paymentService;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public IncidentResponse reportByCustomer(UUID customerId, UUID tripId, CustomerReportRequest request) {
        TripInfo trip = tripService.info(tripId);
        if (!trip.customerId().equals(customerId)) {
            throw new ForbiddenException("Not your trip.");
        }
        Incident incident =
            new Incident(
                idGenerator.newId(),
                tripId,
                trip.vehicleId(),
                customerId,
                request.type(),
                Severity.MEDIUM,
                request.description(),
                request.occurredAt() == null ? clock.instant() : request.occurredAt(),
                "CUSTOMER",
                customerId,
                clock.instant());
        incidents.save(incident);
        events.publishEvent(
            new IncidentReported(incident.getId(), incident.getType(), tripId, trip.vehicleId()));
        return toResponse(incident);
    }

    @Transactional
    public IncidentResponse createByStaff(StaffCreateRequest request) {
        UUID actor = CurrentUser.find().map(u -> u.id()).orElse(CurrentUser.SYSTEM_ID);
        Incident incident =
            new Incident(
                idGenerator.newId(),
                request.tripId(),
                request.vehicleId(),
                request.customerId(),
                request.type(),
                request.severity(),
                request.description(),
                request.occurredAt() == null ? clock.instant() : request.occurredAt(),
                "STAFF",
                actor,
                clock.instant());
        incidents.save(incident);
        events.publishEvent(
            new IncidentReported(
                incident.getId(), incident.getType(), request.tripId(), request.vehicleId()));
        return toResponse(incident);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> search(
        Status status, IncidentEnums.Type type, UUID tripId, Pageable pageable) {
        return incidents.search(status, type, tripId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(UUID incidentId) {
        return toResponse(require(incidentId));
    }

    @Transactional
    public IncidentResponse update(UUID incidentId, UpdateIncidentRequest request) {
        Incident incident = require(incidentId);
        UUID actor = CurrentUser.find().map(u -> u.id()).orElse(CurrentUser.SYSTEM_ID);
        boolean resolving = request.status() == Status.RESOLVED && incident.getStatus() != Status.RESOLVED;
        incident.update(
            request.status(), request.resolutionNotes(), request.chargedAmountCents(), actor, clock.instant());

        if (resolving && incident.getChargedAmountCents() > 0 && incident.getCustomerId() != null) {
            // Charge the customer off-session; the resulting PaymentCaptured event is recorded
            // by the ledger as INCIDENT_CHARGED.
            paymentService.chargeOffSession(
                incident.getCustomerId(),
                incident.getTripId(),
                Money.usd(incident.getChargedAmountCents()),
                "Incident " + incident.getId());
        }
        if (request.status() == Status.RESOLVED) {
            events.publishEvent(new IncidentResolved(incident.getId(), incident.getChargedAmountCents()));
        }
        return toResponse(incident);
    }

    private Incident require(UUID incidentId) {
        return incidents.findById(incidentId).orElseThrow(() -> ResourceNotFoundException.of("Incident", incidentId));
    }

    private IncidentResponse toResponse(Incident i) {
        return new IncidentResponse(
            i.getId(),
            i.getTripId(),
            i.getVehicleId(),
            i.getCustomerId(),
            i.getType(),
            i.getSeverity(),
            i.getStatus(),
            i.getChargedAmountCents());
    }
}

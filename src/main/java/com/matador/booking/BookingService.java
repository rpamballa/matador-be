package com.matador.booking;

import com.matador.booking.api.BookingDtos.AssignVehicleRequest;
import com.matador.booking.api.BookingDtos.BookingResponse;
import com.matador.booking.api.BookingDtos.CreateBookingRequest;
import com.matador.booking.api.BookingDtos.QuoteRequest;
import com.matador.booking.api.BookingDtos.QuoteResponse;
import com.matador.booking.events.BookingEvents.BookingActivated;
import com.matador.booking.events.BookingEvents.BookingCancelled;
import com.matador.booking.events.BookingEvents.BookingCompleted;
import com.matador.booking.events.BookingEvents.BookingConfirmed;
import com.matador.booking.events.BookingEvents.BookingCreated;
import com.matador.booking.internal.BookingRepository;
import com.matador.customer.CustomerService;
import com.matador.payment.PaymentIntentResult;
import com.matador.payment.PaymentService;
import com.matador.pricing.PricingService;
import com.matador.pricing.QuoteInfo;
import com.matador.shared.error.ConflictException;
import com.matador.shared.error.ForbiddenException;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.error.ValidationException;
import com.matador.shared.geo.GeoSupport;
import com.matador.shared.id.IdGenerator;
import com.matador.shared.money.Money;
import com.matador.vehicle.VehicleInfo;
import com.matador.vehicle.VehicleService;
import com.matador.vehicle.VehicleStatus;
import com.matador.zone.ZoneInfo;
import com.matador.zone.ZoneService;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the booking module. */
@Service
public class BookingService {

    private final BookingRepository bookings;
    private final PricingService pricingService;
    private final PaymentService paymentService;
    private final VehicleService vehicleService;
    private final ZoneService zoneService;
    private final CustomerService customerService;
    private final com.matador.trip.TripService tripService;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final BookingPolicyProperties policy;

    public BookingService(
        BookingRepository bookings,
        PricingService pricingService,
        PaymentService paymentService,
        VehicleService vehicleService,
        ZoneService zoneService,
        CustomerService customerService,
        com.matador.trip.TripService tripService,
        IdGenerator idGenerator,
        Clock clock,
        ApplicationEventPublisher events,
        BookingPolicyProperties policy) {
        this.bookings = bookings;
        this.pricingService = pricingService;
        this.paymentService = paymentService;
        this.vehicleService = vehicleService;
        this.zoneService = zoneService;
        this.customerService = customerService;
        this.tripService = tripService;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.events = events;
        this.policy = policy;
    }

    // ---- Quote ----

    @Transactional
    public QuoteResponse quote(UUID customerId, QuoteRequest request) {
        validateWindow(request.pickupAt(), request.dropoffAt());
        ZoneInfo pickupZone =
            zoneService
                .findContaining(request.pickupLat(), request.pickupLng())
                .orElseThrow(
                    () -> new ValidationException("PICKUP_OUT_OF_ZONE", "Pickup is outside any active zone."));
        boolean dropoffInZone =
            zoneService.findContaining(request.dropoffLat(), request.dropoffLng()).isPresent();

        QuoteInfo info =
            pricingService.createQuote(
                new com.matador.pricing.QuoteRequest(
                    customerId,
                    request.vehicleClassId(),
                    request.pickupAt(),
                    request.dropoffAt(),
                    dropoffInZone,
                    pickupZone.outOfZoneDropoffFeeCents(),
                    request.insuranceTier(),
                    request.promoCode()));
        return new QuoteResponse(
            info.quoteId(),
            info.dropoffInZone(),
            info.lineItems(),
            info.subtotalCents(),
            info.discountCents(),
            info.taxCents(),
            info.totalCents(),
            info.depositCents(),
            info.expiresAt());
    }

    // ---- Create ----

    @Transactional
    public BookingResponse create(UUID customerId, CreateBookingRequest request) {
        if (!customerService.canBook(customerId)) {
            throw new ForbiddenException("Customer must be verified and active to book.");
        }
        if (!pricingService.isQuoteValid(request.quoteId())) {
            throw new ConflictException("QUOTE_EXPIRED", "Quote has expired; request a new quote.");
        }
        QuoteInfo quote = pricingService.getQuote(request.quoteId());
        validateWindow(quote.pickupAt(), quote.dropoffAt());

        ZoneInfo pickupZone =
            zoneService
                .findContaining(request.pickupLat(), request.pickupLng())
                .orElseThrow(
                    () -> new ValidationException("PICKUP_OUT_OF_ZONE", "Pickup is outside any active zone."));
        Optional<ZoneInfo> dropoffZone =
            zoneService.findContaining(request.dropoffLat(), request.dropoffLng());

        // Capacity check: at least one AVAILABLE vehicle in the class beyond overlapping
        // reservations. Vehicles are assigned post-confirmation, so this is a class-level
        // capacity guard rather than per-vehicle row locking (BACKEND.md §6.5).
        long available = vehicleService.findAvailableByClass(quote.vehicleClassId()).size();
        long overlapping =
            bookings.countOverlapping(quote.vehicleClassId(), quote.pickupAt(), quote.dropoffAt());
        if (overlapping >= available) {
            throw new ConflictException("NO_CAPACITY", "No vehicles available for this window.");
        }

        String bookingNumber = nextBookingNumber();
        Booking booking =
            Booking.create(
                idGenerator.newId(),
                bookingNumber,
                customerId,
                quote.vehicleClassId(),
                quote.pickupAt(),
                quote.dropoffAt(),
                GeoSupport.point(request.pickupLng(), request.pickupLat()),
                request.pickupAddress() == null ? "" : request.pickupAddress(),
                pickupZone.id(),
                GeoSupport.point(request.dropoffLng(), request.dropoffLat()),
                request.dropoffAddress() == null ? "" : request.dropoffAddress(),
                dropoffZone.map(ZoneInfo::id).orElse(null),
                dropoffZone.isPresent(),
                quote.quoteId(),
                quote.totalCents(),
                quote.depositCents(),
                quote.insuranceTier());
        bookings.save(booking);

        PaymentIntentResult hold =
            paymentService.createDepositHold(
                customerId, booking.getId(), Money.usd(quote.depositCents()), request.paymentMethodId());
        booking.attachDepositHold(hold.stripeIntentId());

        events.publishEvent(
            new BookingCreated(
                booking.getId(),
                customerId,
                quote.vehicleClassId(),
                quote.pickupAt(),
                quote.dropoffAt(),
                clock.instant()));
        return toResponse(booking, hold.clientSecret());
    }

    // ---- Confirmation (driven by PaymentHeld) ----

    @Transactional
    public void confirmFromPayment(UUID bookingId) {
        Booking booking = require(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }
        booking.confirm();
        pricingService.consumePromoForQuote(booking.getQuoteId());
        events.publishEvent(new BookingConfirmed(bookingId, booking.getCustomerId(), clock.instant()));
    }

    // ---- Customer reads / cancel ----

    @Transactional(readOnly = true)
    public Page<BookingResponse> listForCustomer(UUID customerId, Pageable pageable) {
        return bookings.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
            .map(b -> toResponse(b, null));
    }

    @Transactional(readOnly = true)
    public BookingResponse getForCustomer(UUID customerId, UUID bookingId) {
        Booking booking = require(bookingId);
        if (!booking.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Not your booking.");
        }
        return toResponse(booking, null);
    }

    @Transactional
    public BookingResponse cancelByCustomer(UUID customerId, UUID bookingId, String reason) {
        Booking booking = require(bookingId);
        if (!booking.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Not your booking.");
        }
        return doCancel(booking, reason);
    }

    @Transactional
    public BookingResponse cancelByAdmin(UUID bookingId, String reason) {
        return doCancel(require(bookingId), reason);
    }

    private BookingResponse doCancel(Booking booking, String reason) {
        long subtotal = pricingService.getQuote(booking.getQuoteId()).subtotalCents();
        long hoursToPickup = Duration.between(clock.instant(), booking.getPickupAt()).toHours();
        long chargeCents;
        if (hoursToPickup > policy.cancellationFullRefundHours()) {
            chargeCents = 0;
        } else if (hoursToPickup > policy.cancellationPartialHours()) {
            chargeCents = Math.round(subtotal * (policy.cancellationPartialPercentBps() / 10000.0));
        } else {
            chargeCents = subtotal;
        }

        boolean hasHold = booking.getStatus() == BookingStatus.CONFIRMED
            || booking.getStatus() == BookingStatus.PENDING_PAYMENT;
        if (hasHold) {
            if (chargeCents > 0) {
                paymentService.captureDepositHold(booking.getId(), Money.usd(chargeCents));
            } else {
                paymentService.releaseDepositHold(booking.getId());
            }
        }
        booking.cancel(reason, clock.instant());
        if (booking.getAssignedVehicleId() != null) {
            vehicleService.releaseReservation(booking.getAssignedVehicleId());
        }
        events.publishEvent(new BookingCancelled(booking.getId(), reason, clock.instant(), 0));
        return toResponse(booking, null);
    }

    // ---- Admin operations ----

    @Transactional(readOnly = true)
    public Page<BookingResponse> search(
        BookingStatus status, UUID customerId, UUID vehicleId, Pageable pageable) {
        return bookings.search(status, customerId, vehicleId, pageable).map(b -> toResponse(b, null));
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(UUID bookingId) {
        return toResponse(require(bookingId), null);
    }

    @Transactional
    public BookingResponse assignVehicle(UUID bookingId, AssignVehicleRequest request) {
        Booking booking = require(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("BOOKING_NOT_CONFIRMED", "Booking must be confirmed to assign a vehicle.");
        }
        VehicleInfo vehicle = vehicleService.requireVehicle(request.vehicleId());
        if (!vehicle.classId().equals(booking.getVehicleClassId())) {
            throw new ValidationException("WRONG_CLASS", "Vehicle is not in the booked class.");
        }
        if (vehicle.status() != VehicleStatus.AVAILABLE) {
            throw new ConflictException("VEHICLE_UNAVAILABLE", "Vehicle is not available.");
        }
        vehicleService.reserveForBooking(vehicle.id());
        booking.assignVehicle(vehicle.id());
        return toResponse(booking, null);
    }

    @Transactional
    public BookingResponse activate(UUID bookingId) {
        Booking booking = require(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("BOOKING_NOT_CONFIRMED", "Booking must be confirmed to activate.");
        }
        if (booking.getAssignedVehicleId() == null) {
            throw new ConflictException("NO_VEHICLE", "Assign a vehicle before activation.");
        }
        if (bookings.existsByCustomerIdAndStatus(booking.getCustomerId(), BookingStatus.ACTIVATED)) {
            throw new ConflictException("ALREADY_ACTIVE", "Customer already has an active booking.");
        }
        VehicleInfo vehicle = vehicleService.requireVehicle(booking.getAssignedVehicleId());
        vehicleService.startCustomerUse(vehicle.id());
        UUID tripId = idGenerator.newId();
        booking.activate(clock.instant());

        // Direct call into the trip module (one-way booking -> trip dependency).
        tripService.createForBooking(
            tripId,
            booking.getId(),
            booking.getCustomerId(),
            vehicle.id(),
            clock.instant(),
            GeoSupport.lat(booking.getPickupLocation()),
            GeoSupport.lng(booking.getPickupLocation()),
            vehicle.odometerMiles(),
            booking.getQuotedTotalCents());

        events.publishEvent(
            new BookingActivated(
                booking.getId(),
                tripId,
                booking.getCustomerId(),
                vehicle.id(),
                clock.instant(),
                vehicle.odometerMiles(),
                GeoSupport.lat(booking.getPickupLocation()),
                GeoSupport.lng(booking.getPickupLocation()),
                booking.getQuotedTotalCents(),
                clock.instant()));
        return toResponse(booking, null);
    }

    // ---- Lifecycle hooks (driven by trip / no-show) ----

    @Transactional
    public void completeFromTrip(UUID bookingId, UUID tripId) {
        Booking booking = require(bookingId);
        if (booking.getStatus() == BookingStatus.ACTIVATED) {
            booking.complete();
            events.publishEvent(new BookingCompleted(bookingId, tripId, clock.instant()));
        }
    }

    @Transactional
    public void markNoShow(UUID bookingId) {
        Booking booking = require(bookingId);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.markNoShow(clock.instant());
            if (booking.getAssignedVehicleId() != null) {
                vehicleService.releaseReservation(booking.getAssignedVehicleId());
            }
            events.publishEvent(new BookingCancelled(bookingId, "NO_SHOW", clock.instant(), 0));
        }
    }

    // ---- helpers ----

    private void validateWindow(java.time.Instant pickupAt, java.time.Instant dropoffAt) {
        java.time.Instant now = clock.instant();
        if (pickupAt.isBefore(now.plus(Duration.ofMinutes(policy.pickupLeadTimeMinutes())))) {
            throw new ValidationException(
                "PICKUP_TOO_SOON",
                "Pickup must be at least %d minutes in the future.".formatted(policy.pickupLeadTimeMinutes()));
        }
        Duration duration = Duration.between(pickupAt, dropoffAt);
        if (duration.toHours() < policy.minBookingHours()) {
            throw new ValidationException(
                "TOO_SHORT", "Minimum booking is %d hours.".formatted(policy.minBookingHours()));
        }
        if (duration.toDays() > policy.maxBookingDays()) {
            throw new ValidationException(
                "TOO_LONG", "Maximum booking is %d days.".formatted(policy.maxBookingDays()));
        }
    }

    private String nextBookingNumber() {
        long seq = bookings.nextBookingSequence();
        int year = clock.instant().atZone(ZoneOffset.UTC).getYear();
        return "MTD-%d-%08d".formatted(year, seq);
    }

    private Booking require(UUID bookingId) {
        return bookings.findById(bookingId).orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
    }

    private BookingResponse toResponse(Booking b, String clientSecret) {
        return new BookingResponse(
            b.getId(),
            b.getBookingNumber(),
            b.getStatus(),
            b.getVehicleClassId(),
            b.getAssignedVehicleId(),
            b.getPickupAt(),
            b.getDropoffAt(),
            b.getQuotedTotalCents(),
            b.getDepositAmountCents(),
            b.isDropoffInZone(),
            clientSecret);
    }
}

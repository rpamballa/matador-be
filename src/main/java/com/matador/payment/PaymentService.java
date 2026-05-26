package com.matador.payment;

import com.matador.customer.CustomerService;
import com.matador.payment.api.PaymentDtos.PaymentMethodResponse;
import com.matador.payment.api.PaymentDtos.SetupIntentResponse;
import com.matador.payment.events.PaymentEvents.PaymentCaptured;
import com.matador.payment.events.PaymentEvents.PaymentHeld;
import com.matador.payment.events.PaymentEvents.PaymentReleased;
import com.matador.payment.events.PaymentEvents.PaymentRefunded;
import com.matador.payment.internal.PaymentGateway;
import com.matador.payment.internal.PaymentGateway.IntentResult;
import com.matador.payment.internal.PaymentIntentEntity;
import com.matador.payment.internal.PaymentIntentRepository;
import com.matador.payment.internal.PaymentMethodEntity;
import com.matador.payment.internal.PaymentMethodRepository;
import com.matador.shared.error.ConflictException;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.id.IdGenerator;
import com.matador.shared.money.Money;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the payment module. */
@Service
public class PaymentService {

    public static final String PURPOSE_DEPOSIT_HOLD = "DEPOSIT_HOLD";
    public static final String PURPOSE_RENTAL_CHARGE = "RENTAL_CHARGE";
    public static final String PURPOSE_INCIDENT_CHARGE = "INCIDENT_CHARGE";

    private final PaymentMethodRepository methods;
    private final PaymentIntentRepository intents;
    private final PaymentGateway gateway;
    private final CustomerService customerService;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public PaymentService(
        PaymentMethodRepository methods,
        PaymentIntentRepository intents,
        PaymentGateway gateway,
        CustomerService customerService,
        IdGenerator idGenerator,
        Clock clock,
        ApplicationEventPublisher events) {
        this.methods = methods;
        this.intents = intents;
        this.gateway = gateway;
        this.customerService = customerService;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.events = events;
    }

    // ---- Payment methods (customer self-service) ----

    @Transactional
    public SetupIntentResponse createSetupIntent(UUID customerId) {
        String stripeCustomerId = ensureStripeCustomer(customerId);
        PaymentGateway.SetupIntent intent = gateway.createSetupIntent(stripeCustomerId);
        return new SetupIntentResponse(intent.id(), intent.clientSecret());
    }

    @Transactional
    public PaymentMethodResponse attachPaymentMethod(
        UUID customerId, String stripePaymentMethodId, boolean makeDefault) {
        if (methods.findByStripePaymentMethodId(stripePaymentMethodId).isPresent()) {
            throw new ConflictException("PAYMENT_METHOD_EXISTS", "Payment method already attached.");
        }
        ensureStripeCustomer(customerId);
        PaymentGateway.PaymentMethodDetails details =
            gateway.describePaymentMethod(stripePaymentMethodId);
        boolean isDefault = makeDefault || methods.findByCustomerIdAndDetachedAtIsNull(customerId).isEmpty();
        if (isDefault) {
            clearDefault(customerId);
        }
        PaymentMethodEntity entity =
            new PaymentMethodEntity(
                idGenerator.newId(),
                customerId,
                stripePaymentMethodId,
                details.type(),
                details.brand(),
                details.last4(),
                details.expMonth(),
                details.expYear(),
                isDefault,
                clock.instant());
        methods.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> listPaymentMethods(UUID customerId) {
        return methods.findByCustomerIdAndDetachedAtIsNull(customerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void detachPaymentMethod(UUID customerId, UUID paymentMethodId) {
        PaymentMethodEntity pm = requireMethod(customerId, paymentMethodId);
        pm.detach(clock.instant());
    }

    @Transactional
    public void setDefaultPaymentMethod(UUID customerId, UUID paymentMethodId) {
        PaymentMethodEntity pm = requireMethod(customerId, paymentMethodId);
        clearDefault(customerId);
        pm.setDefault(true);
    }

    // ---- Operations called by other modules ----

    @Transactional
    public PaymentIntentResult createDepositHold(
        UUID customerId, UUID bookingId, Money amount, UUID paymentMethodId) {
        String stripeCustomerId = ensureStripeCustomer(customerId);
        PaymentMethodEntity pm = resolveMethod(customerId, paymentMethodId);
        IntentResult result =
            gateway.createDepositHold(stripeCustomerId, pm.getStripePaymentMethodId(), amount.amountCents());
        PaymentIntentEntity intent =
            new PaymentIntentEntity(
                idGenerator.newId(),
                customerId,
                bookingId,
                null,
                result.stripeIntentId(),
                PURPOSE_DEPOSIT_HOLD,
                amount.amountCents(),
                result.status(),
                clock.instant());
        intents.save(intent);
        if ("requires_capture".equals(result.status()) || "succeeded".equals(result.status())) {
            events.publishEvent(new PaymentHeld(intent.getId(), customerId, bookingId, amount.amountCents()));
        }
        return toResult(intent, result.clientSecret());
    }

    @Transactional
    public PaymentIntentResult captureDepositHold(UUID bookingId, Money amount) {
        PaymentIntentEntity intent = requireBookingHold(bookingId);
        IntentResult result = gateway.capture(intent.getStripeIntentId(), amount.amountCents());
        intent.recordCapture(amount.amountCents(), result.status(), clock.instant());
        events.publishEvent(
            new PaymentCaptured(
                intent.getId(),
                intent.getCustomerId(),
                bookingId,
                intent.getTripId(),
                PURPOSE_DEPOSIT_HOLD,
                amount.amountCents()));
        return toResult(intent, null);
    }

    @Transactional
    public void releaseDepositHold(UUID bookingId) {
        intents
            .findFirstByBookingIdAndPurposeOrderByCreatedAtDesc(bookingId, PURPOSE_DEPOSIT_HOLD)
            .ifPresent(
                intent -> {
                    IntentResult result = gateway.cancel(intent.getStripeIntentId());
                    intent.updateStatus(result.status(), clock.instant());
                    events.publishEvent(
                        new PaymentReleased(
                            intent.getId(), intent.getCustomerId(), bookingId, intent.getAmountCents()));
                });
    }

    @Transactional
    public PaymentIntentResult chargeOffSession(
        UUID customerId, UUID tripId, Money amount, String description) {
        String stripeCustomerId = ensureStripeCustomer(customerId);
        PaymentMethodEntity pm = resolveMethod(customerId, null);
        IntentResult result =
            gateway.chargeOffSession(
                stripeCustomerId, pm.getStripePaymentMethodId(), amount.amountCents(), description);
        PaymentIntentEntity intent =
            new PaymentIntentEntity(
                idGenerator.newId(),
                customerId,
                null,
                tripId,
                result.stripeIntentId(),
                PURPOSE_RENTAL_CHARGE,
                amount.amountCents(),
                result.status(),
                clock.instant());
        intents.save(intent);
        if ("succeeded".equals(result.status())) {
            intent.recordCapture(amount.amountCents(), result.status(), clock.instant());
            events.publishEvent(
                new PaymentCaptured(
                    intent.getId(), customerId, null, tripId, PURPOSE_RENTAL_CHARGE, amount.amountCents()));
        }
        return toResult(intent, result.clientSecret());
    }

    @Transactional
    public void refundForBooking(UUID bookingId, Money amount, String reason) {
        PaymentIntentEntity intent = requireBookingHold(bookingId);
        gateway.refund(intent.getStripeIntentId(), amount.amountCents(), reason);
        events.publishEvent(
            new PaymentRefunded(intent.getId(), intent.getCustomerId(), amount.amountCents(), reason));
    }

    @Transactional(readOnly = true)
    public boolean hasPaymentMethod(UUID customerId) {
        return !methods.findByCustomerIdAndDetachedAtIsNull(customerId).isEmpty();
    }

    // ---- helpers ----

    private String ensureStripeCustomer(UUID customerId) {
        return customerService
            .stripeCustomerId(customerId)
            .orElseGet(
                () -> {
                    String email = customerService.customerEmail(customerId);
                    String stripeCustomerId = gateway.ensureCustomer(customerId, email);
                    customerService.linkStripeCustomer(customerId, stripeCustomerId);
                    return stripeCustomerId;
                });
    }

    private PaymentMethodEntity resolveMethod(UUID customerId, UUID paymentMethodId) {
        if (paymentMethodId != null) {
            return requireMethod(customerId, paymentMethodId);
        }
        return methods.findByCustomerIdAndDetachedAtIsNull(customerId).stream()
            .filter(PaymentMethodEntity::isDefault)
            .findFirst()
            .or(() -> methods.findByCustomerIdAndDetachedAtIsNull(customerId).stream().findFirst())
            .orElseThrow(
                () -> new ConflictException("NO_PAYMENT_METHOD", "Customer has no payment method."));
    }

    private PaymentMethodEntity requireMethod(UUID customerId, UUID paymentMethodId) {
        return methods
            .findByIdAndCustomerId(paymentMethodId, customerId)
            .orElseThrow(() -> ResourceNotFoundException.of("PaymentMethod", paymentMethodId));
    }

    private PaymentIntentEntity requireBookingHold(UUID bookingId) {
        return intents
            .findFirstByBookingIdAndPurposeOrderByCreatedAtDesc(bookingId, PURPOSE_DEPOSIT_HOLD)
            .orElseThrow(() -> ResourceNotFoundException.of("DepositHold", bookingId));
    }

    private void clearDefault(UUID customerId) {
        methods.findByCustomerIdAndDetachedAtIsNull(customerId).stream()
            .filter(PaymentMethodEntity::isDefault)
            .forEach(pm -> pm.setDefault(false));
    }

    private PaymentMethodResponse toResponse(PaymentMethodEntity pm) {
        return new PaymentMethodResponse(
            pm.getId(), pm.getBrand(), pm.getLast4(), pm.getExpMonth(), pm.getExpYear(), pm.isDefault());
    }

    private PaymentIntentResult toResult(PaymentIntentEntity intent, String clientSecret) {
        return new PaymentIntentResult(
            intent.getId(),
            intent.getStripeIntentId(),
            intent.getStatus(),
            clientSecret,
            intent.getAmountCents());
    }
}

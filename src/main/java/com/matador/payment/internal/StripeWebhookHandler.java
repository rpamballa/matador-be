package com.matador.payment.internal;

import com.matador.payment.events.PaymentEvents.PaymentCaptured;
import com.matador.payment.events.PaymentEvents.PaymentDisputeCreated;
import com.matador.payment.events.PaymentEvents.PaymentFailed;
import com.matador.payment.events.PaymentEvents.PaymentReleased;
import com.matador.payment.internal.PaymentIntentRepository;
import com.matador.shared.webhook.WebhookEventStore;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Maps verified Stripe events to local payment-intent updates and domain events. */
@Component
public class StripeWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookHandler.class);
    private static final String PROVIDER = "stripe";

    private final PaymentIntentRepository intents;
    private final WebhookEventStore eventStore;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public StripeWebhookHandler(
        PaymentIntentRepository intents,
        WebhookEventStore eventStore,
        ApplicationEventPublisher events,
        Clock clock) {
        this.intents = intents;
        this.eventStore = eventStore;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void handle(Event event, String payload) {
        if (!eventStore.markProcessed(PROVIDER, event.getId(), event.getType(), payload)) {
            return;
        }
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(object instanceof PaymentIntent pi)) {
            log.debug("Ignoring Stripe event {} (no payment intent payload)", event.getType());
            return;
        }
        intents
            .findByStripeIntentId(pi.getId())
            .ifPresent(
                intent -> {
                    switch (event.getType()) {
                        case "payment_intent.succeeded" -> {
                            long amount = pi.getAmountReceived() == null ? intent.getAmountCents() : pi.getAmountReceived();
                            intent.recordCapture(amount, "succeeded", clock.instant());
                            events.publishEvent(
                                new PaymentCaptured(
                                    intent.getId(),
                                    intent.getCustomerId(),
                                    intent.getBookingId(),
                                    intent.getTripId(),
                                    intent.getPurpose(),
                                    amount));
                        }
                        case "payment_intent.payment_failed" -> {
                            intent.updateStatus("payment_failed", clock.instant());
                            events.publishEvent(
                                new PaymentFailed(intent.getId(), intent.getCustomerId(), "payment_failed"));
                        }
                        case "payment_intent.canceled" -> {
                            intent.updateStatus("canceled", clock.instant());
                            events.publishEvent(
                                new PaymentReleased(
                                    intent.getId(),
                                    intent.getCustomerId(),
                                    intent.getBookingId(),
                                    intent.getAmountCents()));
                        }
                        case "charge.dispute.created" ->
                            events.publishEvent(
                                new PaymentDisputeCreated(
                                    intent.getId(), intent.getCustomerId(), intent.getAmountCents()));
                        default -> log.debug("Unhandled Stripe event type {}", event.getType());
                    }
                });
    }
}
